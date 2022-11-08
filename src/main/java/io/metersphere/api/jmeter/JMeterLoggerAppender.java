package io.metersphere.api.jmeter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.metersphere.api.jmeter.utils.DateUtils;
import io.metersphere.api.jmeter.utils.FixedCapacityUtil;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMeterLoggerAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
    public static Logger logger = LoggerFactory.getLogger("JMETER");
    public static boolean enable = false;
    private final static String THREAD_SPLIT = " ";

    @Override
    public void append(ILoggingEvent event) {
        try {
            if (!event.getLevel().levelStr.equals(LoggerUtil.DEBUG) && StringUtils.isNotEmpty(event.getThreadName())) {
                StringBuffer message = new StringBuffer();
                String threadName = StringUtils.substringBeforeLast(event.getThreadName(), THREAD_SPLIT);
                message.append(DateUtils.getTimeStr(event.getTimeStamp())).append(" ")
                        .append(event.getLevel()).append(" ")
                        .append(event.getThreadName()).append(" ")
                        .append(event.getFormattedMessage()).append("\n");

                if (event.getThrowableProxy() != null) {
                    message.append(event.getThrowableProxy().getMessage()).append("\n");
                    message.append(event.getThrowableProxy().getClassName()).append("\n");
                    if (event.getThrowableProxy().getStackTraceElementProxyArray() != null) {
                        for (StackTraceElementProxy stackTraceElementProxy : event.getThrowableProxy().getStackTraceElementProxyArray()) {
                            message.append("   ").append(stackTraceElementProxy.getSTEAsString()).append("\n");
                        }
                    }
                }
                if (message != null && !message.toString().contains("java.net.UnknownHostException")
                        && FixedCapacityUtil.containsKey(threadName)) {
                    FixedCapacityUtil.get(threadName).append(message);
                }
                if (!enable) {
                    logger.info("JMETER-LOG ", message);
                }
            }
            if (enable) {
                logger.info("JMETER-DEBUG-LOG " + event.getFormattedMessage());
            }
        } catch (Exception e) {
            LoggerUtil.error(e);
        }
    }
}