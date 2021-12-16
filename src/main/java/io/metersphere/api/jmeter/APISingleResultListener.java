package io.metersphere.api.jmeter;

import com.alibaba.fastjson.JSON;
import io.metersphere.api.jmeter.queue.BlockingQueueUtil;
import io.metersphere.api.jmeter.utils.CommonBeanFactory;
import io.metersphere.api.service.JmeterExecuteService;
import io.metersphere.api.service.ProducerService;
import io.metersphere.constants.RunModeConstants;
import io.metersphere.dto.ResultDTO;
import io.metersphere.jmeter.MsExecListener;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class APISingleResultListener extends MsExecListener {
    private PrintStream oldPrintStream = System.out;

    private static ByteArrayOutputStream bos = new ByteArrayOutputStream();

    public static void setConsole() {
        System.setOut(new PrintStream(bos));
    }

    private String getConsole() {
        System.setOut(oldPrintStream);
        return bos.toString();
    }

    private void send(ResultDTO dto, Map<String, Object> kafkaConfig) {
        ProducerService producerServer = CommonBeanFactory.getBean(ProducerService.class);
        try {
            if (producerServer != null) {
                dto.setConsole(getConsole());
                LoggerUtil.info("执行完成开始同步发送KAFKA【" + dto.getTestId() + "】");
                producerServer.send(JSON.toJSONString(dto), kafkaConfig);
                LoggerUtil.info("同步发送报告信息到KAFKA完成【" + dto.getTestId() + "】");
            }
        } catch (Exception ex) {
            LoggerUtil.error("KAFKA 推送结果异常：[" + dto.getTestId() + "]" + ex.getMessage());
            // 补偿一个结果防止持续Running
            if (dto != null && dto.getRequestResults().size() > 0) {
                dto.getRequestResults().clear();
            }
            producerServer.send(JSON.toJSONString(dto), kafkaConfig);
        }

        if (dto != null && StringUtils.equals(dto.getReportType(), RunModeConstants.SET_REPORT.name())) {
            LoggerUtil.info("处理接口集合报告ID：" + dto.getReportId());
            JmeterExecuteService jmeterExecuteService = CommonBeanFactory.getBean(JmeterExecuteService.class);
            if (jmeterExecuteService != null) {
                jmeterExecuteService.remove(dto.getReportId(), dto.getTestId());
                LoggerUtil.info("正在执行中的并发报告数量：" + jmeterExecuteService.getRunningSize());
                LoggerUtil.info("正在执行中的场景[" + dto.getReportId() + "]的数量：" + jmeterExecuteService.getRunningTasks(dto.getReportId()));
                LoggerUtil.info("正在执行中的场景[" + dto.getReportId() + "]的内容：" + jmeterExecuteService.getRunningList(dto.getReportId()));
            }
        }
    }

    @Override
    public void handleTeardownTest(ResultDTO dto, Map<String, Object> kafkaConfig) {
        LoggerUtil.info("开始处理单条执行结果报告【" + dto.getReportId() + " 】,资源【 " + dto.getTestId() + " 】");
        dto.setConsole(getConsole());
        this.send(dto, kafkaConfig);
    }

    @Override
    public void testEnded(ResultDTO dto, Map<String, Object> kafkaConfig) {
        LoggerUtil.info("报告【" + dto.getReportId() + " 】执行完成");
        BlockingQueueUtil.remove(dto.getReportId());
        dto.setConsole(getConsole());
        if (dto.getArbitraryData() == null || dto.getArbitraryData().isEmpty()) {
            dto.setArbitraryData(new HashMap<String, Object>() {{
                this.put("TEST_END", true);
            }});
        } else {
            dto.getArbitraryData().put("TEST_END", true);
        }
        // 存储结果
        this.send(dto, kafkaConfig);
    }
}
