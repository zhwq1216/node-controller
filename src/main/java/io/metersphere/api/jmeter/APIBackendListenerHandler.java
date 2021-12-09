package io.metersphere.api.jmeter;


import com.alibaba.fastjson.JSON;
import io.metersphere.api.jmeter.utils.CommonBeanFactory;
import io.metersphere.api.service.JmeterExecuteService;
import io.metersphere.api.service.ProducerService;
import io.metersphere.constants.RunModeConstants;
import io.metersphere.dto.ResultDTO;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

public class APIBackendListenerHandler {

    private PrintStream oldPrintStream = System.out;

    private static ByteArrayOutputStream bos = new ByteArrayOutputStream();

    public static void setConsole() {
        System.setOut(new PrintStream(bos));
    }

    private String getConsole() {
        System.setOut(oldPrintStream);
        return bos.toString();
    }

    public void handleTeardownTest(ResultDTO dto, Map<String, Object> kafkaConfig) {
        ProducerService producerServer = CommonBeanFactory.getBean(ProducerService.class);
        try {
            dto.setConsole(getConsole());
            LoggerUtil.info("执行完成开始同步发送KAFKA【" + dto.getTestId() + "】");
            producerServer.send(JSON.toJSONString(dto), kafkaConfig);
            LoggerUtil.info("同步发送报告信息到KAFKA完成【" + dto.getTestId() + "】");
        } catch (Exception ex) {
            LoggerUtil.error("KAFKA 推送结果异常：[" + dto.getTestId() + "]" + ex.getMessage());
            // 补偿一个结果防止持续Running
            if (dto != null && dto.getRequestResults().size() > 0) {
                dto.getRequestResults().clear();
            }
            producerServer.send(JSON.toJSONString(dto), kafkaConfig);
        }

        if (StringUtils.equals(dto.getReportType(), RunModeConstants.SET_REPORT.name())) {
            LoggerUtil.info("接口收到集合报告ID：" + dto.getReportId());
            JmeterExecuteService jmeterExecuteService = CommonBeanFactory.getBean(JmeterExecuteService.class);
            jmeterExecuteService.remove(dto.getReportId(), dto.getTestId());
            LoggerUtil.info("正在执行中的并发报告数量：" + jmeterExecuteService.getRunningSize());
            LoggerUtil.info("正在执行中的场景[" + dto.getReportId() + "]的数量：" + jmeterExecuteService.getRunningTasks(dto.getReportId()));
            LoggerUtil.info("正在执行中的场景[" + dto.getReportId() + "]的内容：" + jmeterExecuteService.getRunningList(dto.getReportId()));
        }
    }
}
