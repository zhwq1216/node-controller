package io.metersphere.node.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.Namespaces;
import io.kubernetes.client.util.Yaml;
import io.metersphere.node.controller.request.TestRequest;
import io.metersphere.node.service.KafkaProducerService;
import io.metersphere.utils.LoggerUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.OkHttpClient;

@Slf4j
@Service
public class K8sPodClientService implements InitializingBean {

    @Resource
    private KafkaProducerService kafkaProducerService;


    private Map<String, K8sLogWatch> logWatchMap = new HashMap<>();

    private Map<String, ScheduledFuture> podCheckScheduleFutures = new HashMap<>();


    @Setter
    @Getter
    private String nodeNamespace;

    @Setter
    @Getter
    private String nodeNameSuffix = "";

    @Setter
    @Getter
    private ScheduledThreadPoolExecutor scheduledExecutorService;


    public void init() {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = new ScheduledThreadPoolExecutor(10);
        }

        if (!this.isInK8sCluster()) {
            return;
        }
        try {
            ApiClient apiClient = Config.fromCluster();
            // infinite timeout
            OkHttpClient httpClient =
                apiClient.getHttpClient().newBuilder().readTimeout(0, TimeUnit.SECONDS).build();
            apiClient.setHttpClient(httpClient);
            Configuration.setDefaultApiClient(apiClient);
            nodeNamespace = Namespaces.getPodNamespace();
            // 获取主机名-分隔的最后一部分作为节点名称后缀
            String nodePodName = System.getenv("HOSTNAME");
            String[] aTmp = nodePodName.split("-");
            nodeNameSuffix = "-" + aTmp[aTmp.length-1];
        } catch (Exception ex) {
            log.warn("初始化k8sapiclient失败:{}", ex.getMessage(), ex);
            LoggerUtil.warn("初始化k8sapiclient失败:{}", ex.getMessage());
        }


    }
    
    protected String getPodName(String testId) {
        return "jmeter-" + testId + nodeNameSuffix;
    }

    public V1Pod podStatus(String testId) {
        CoreV1Api api = new CoreV1Api();
        //指定分类
        // Yaml.addModelMap("v1", "Pod", V1Pod.class);
        String podName = getPodName(testId);
        try{
            V1Pod v1Pod = api.readNamespacedPodStatus(podName, nodeNamespace, null);
            return v1Pod;
        }catch (ApiException ex) {
            return null;
        }

    }

    public String podLog(String testId) {
        CoreV1Api api = new CoreV1Api();
        //指定分类
        // Yaml.addModelMap("v1", "Pod", V1Pod.class);
        String podName = getPodName(testId);

        try {
            String logMessage = api.readNamespacedPodLog(podName, nodeNamespace, "jmeter", false,
                true,
                null, null, false, null, null, false);
            return logMessage;
        } catch (ApiException e) {
            LoggerUtil.warn("获取日志失败,testId="+testId+", responseBody="+e.getResponseBody());
            return "";
        }

    }

    public void stopTestPod(String testId) {
        CoreV1Api api = new CoreV1Api();
        String podName = getPodName(testId);
        try {
            LoggerUtil.info("尝试删除pod, testId="+testId);
            api.deleteNamespacedPod(podName, nodeNamespace, null, null, 10, null, null, null);
            LoggerUtil.info("删除pod成功, testId="+testId);

        } catch (ApiException e) {
            if (e.getCode() == 404) {
                LoggerUtil.info("pod已不存在, testId=" + testId);
            } else {
                LoggerUtil.error("删除pod失败, testId=" + testId + ", responseBody=" + e.getResponseBody());
            }
        }

        K8sLogWatch k8sLogWatch = logWatchMap.get(testId);
        if (k8sLogWatch != null) {
            try {
                k8sLogWatch.close();
            } catch (IOException e) {
                // ignore
            }
            logWatchMap.remove(testId);
        }
        ScheduledFuture scheduledFuture = podCheckScheduleFutures.get(testId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            podCheckScheduleFutures.remove(testId);
        }
    }

    public void checkPodStatus(String testId) {
        CoreV1Api api = new CoreV1Api();

        String podName = getPodName(testId);


    }

    public void startTestPod(TestRequest testRequest) {
        Map<String, String> env = testRequest.getEnv();
        String testId = env.get("TEST_ID");
        String topic = testRequest.getEnv().getOrDefault("LOG_TOPIC", "JMETER_LOGS");
        String reportId = testRequest.getEnv().get("REPORT_ID");
        try {

            List<V1EnvVar> envVars = new ArrayList<>();
            env.forEach((k, v) -> {
                V1EnvVar envVar = new V1EnvVar();
                envVar.setName(k);
                envVar.setValue(v);
                envVars.add(envVar);
            });

            CoreV1Api api = new CoreV1Api();
            //指定分类
            // Yaml.addModelMap("v1", "Pod", V1Pod.class);
            String podName = getPodName(testId);
            // 加载配置文件
            // File file = ResourceUtils.getFile("classpath:jmeter-pod.yaml");
            String ymlString = Files.readString(Paths.get(
                    ResourceUtils.getURL("classpath:jmeter-pod.yaml").toURI()), Charset.forName("UTF-8"));
            ymlString = ymlString.replace("${testId}", testId);
            V1Pod v1Pod = Yaml.loadAs(ymlString, V1Pod.class);
            v1Pod.getMetadata().name(podName);
            v1Pod.getMetadata().putLabelsItem("app", podName);
            v1Pod.getMetadata().putLabelsItem("test", testId);

            for (V1Container container : v1Pod.getSpec().getContainers()) {
                envVars.forEach(envItem -> {
                    container.addEnvItem(envItem);
                });
                if ("jmeter".equals(container.getName()) || "report".equals(container.getName())) {
                    container.setImage(testRequest.getImage());
                }
            }

            LoggerUtil.info("创建pod yaml内容为:" + Yaml.dump(v1Pod));
            v1Pod = api.createNamespacedPod(nodeNamespace, v1Pod, "false", null, null);

            LoggerUtil.info("创建pod成功, podId=" + v1Pod.getMetadata().getUid());

            ScheduledFuture<?> scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    V1Pod v1PodTmp = api.readNamespacedPodStatus(podName, nodeNamespace, null);
                    String podPhase = v1PodTmp.getStatus().getPhase();
                    if ("Running".equals(podPhase)) {
                        synchronized (logWatchMap) {
                            if (logWatchMap.containsKey(testId)) {
                                return;
                            }
                            Call logcall = api
                                .readNamespacedPodLogCall(podName, nodeNamespace, "jmeter", true, true, null,
                                    null, null, null,
                                    null, false, null);
                            K8sLogWatch watch = K8sLogWatch.createLogWatch(Config.defaultClient(), logcall);
                            logWatchMap.put(testId, watch);
                            Thread logThread = new Thread(() -> watch.forEach(logResp -> {
                                String log = logResp.object.trim();
                                String oomMessage = "There is insufficient memory for the Java Runtime Environment to continue.";
                                String oomMessage2 = "java.lang.OutOfMemoryError";
                                if (StringUtils.contains(log, oomMessage) || StringUtils.contains(log, oomMessage2)) {
                                    LoggerUtil.info("handle out of memory error.");
                                    // oom 退出
                                    String[] contents = new String[]{reportId, "none", "0", oomMessage};
                                    String message = StringUtils.join(contents, " ");
                                    kafkaProducerService.sendMessage(topic, message);
                                    stopTestPod(testId);
                                }
                                LoggerUtil.info(log);
                            }));
                            logThread.setName("log-wath-" + testId);
                            logThread.setDaemon(true);
                            logThread.start();
                            LoggerUtil.info("启动pod日志监听成功, testId=" + testId);
                        }

                    } else if ("Succeeded".equals(podPhase) || "Failed".equals(podPhase)) {
                        stopTestPod(testId);
                    }

                } catch (Exception ex) {
                    if (ex instanceof ApiException) {
                        int code = ((ApiException) ex).getCode();
                        if (code == 404) {
                            stopTestPod(testId);
                        } else {
                            LoggerUtil.error("获取pod状态异常, testId=" + testId + ", responseBody=" + ((ApiException) ex)
                                .getResponseBody());
                        }
                    } else {
                        LoggerUtil.error(ex);
                    }

                }
            }, 1, 1, TimeUnit.SECONDS);

            podCheckScheduleFutures.put(testId, scheduledFuture);

        } catch (Exception ex) {
            if (ex instanceof ApiException) {
                ApiException apiEx = (ApiException) ex;
                String responseBody = apiEx.getResponseBody();
                LoggerUtil.info("创建pod失败,testId=" + testId + ", responseBody=" + responseBody);
            } else {
                LoggerUtil.error(ex);
                LoggerUtil.info("创建pod失败,testId=" + testId + ", ex=" + ex.getMessage());
            }

        }

    }

    public boolean isInK8sCluster() {
        return Paths.get(Config.SERVICEACCOUNT_CA_PATH).toFile().exists()
            && Paths.get(Config.SERVICEACCOUNT_NAMESPACE_PATH).toFile().exists();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.init();
    }
}
