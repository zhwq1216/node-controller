package io.metersphere.api.jmeter;

import io.metersphere.api.jmeter.queue.ExecThreadPoolExecutor;
import io.metersphere.api.jmeter.utils.FixedCapacityUtils;
import io.metersphere.api.jmeter.utils.JmeterProperties;
import io.metersphere.api.jmeter.utils.MSException;
import io.metersphere.constants.RunModeConstants;
import io.metersphere.dto.JmeterRunRequestDTO;
import io.metersphere.jmeter.JMeterBase;
import io.metersphere.jmeter.LocalRunner;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;

@Service
public class JMeterService {

    @Resource
    private JmeterProperties jmeterProperties;
    @Resource
    private ExecThreadPoolExecutor execThreadPoolExecutor;

    @PostConstruct
    public void init() {
        String JMETER_HOME = getJmeterHome();
        String JMETER_PROPERTIES = JMETER_HOME + "/bin/jmeter.properties";
        JMeterUtils.loadJMeterProperties(JMETER_PROPERTIES);
        JMeterUtils.setJMeterHome(JMETER_HOME);
        JMeterUtils.setLocale(LocaleContextHolder.getLocale());
    }

    public String getJmeterHome() {
        String home = getClass().getResource("/").getPath() + "jmeter";
        try {
            File file = new File(home);
            if (file.exists()) {
                return home;
            } else {
                return jmeterProperties.getHome();
            }
        } catch (Exception e) {
            return jmeterProperties.getHome();
        }
    }

    private String getReportId(HashTree tree, JmeterRunRequestDTO runRequest) {
        if (StringUtils.equals(runRequest.getReportType(), RunModeConstants.SET_REPORT.toString())) {
            String reportId = StringUtils.join(runRequest.getReportId(), "_", runRequest.getTestId());
            for (Object key : tree.keySet()) {
                HashTree node = tree.get(key);
                if (key instanceof ThreadGroup) {
                    ((ThreadGroup) key).setName(reportId);
                    break;
                } else {
                    getReportId(node, runRequest);
                }
            }
            return reportId;
        } else {
            return runRequest.getReportId();
        }
    }

    public void runLocal(JmeterRunRequestDTO runRequest, HashTree testPlan) {
        try {
            init();
            String reportId = getReportId(runRequest.getHashTree(), runRequest);
            if (!FixedCapacityUtils.containsKey(reportId)) {
                FixedCapacityUtils.put(reportId, new StringBuffer(""));
            }
            runRequest.setHashTree(testPlan);
            JMeterBase.addBackendListener(runRequest, runRequest.getHashTree(), MsApiBackendListener.class.getCanonicalName());
            LocalRunner runner = new LocalRunner(testPlan);
            runner.run(runRequest.getReportId());
        } catch (Exception e) {
            LoggerUtil.error("Local执行异常", runRequest.getReportId(), e);
            MSException.throwException("读取脚本失败");
        }
    }

    public void run(JmeterRunRequestDTO request) {
        if (request.getCorePoolSize() > 0) {
            execThreadPoolExecutor.setCorePoolSize(request.getCorePoolSize());
        }
        execThreadPoolExecutor.addTask(request);
    }

    public void addQueue(JmeterRunRequestDTO request) {
        this.runLocal(request, request.getHashTree());
    }
}
