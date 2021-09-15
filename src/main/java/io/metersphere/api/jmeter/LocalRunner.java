package io.metersphere.api.jmeter;

import io.metersphere.api.jmeter.utils.MessageCache;
import io.metersphere.node.util.LogUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.jmeter.engine.JMeterEngineException;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jorphan.collections.HashTree;

import java.util.List;

public class LocalRunner {
    private HashTree jmxTree;

    public LocalRunner(HashTree jmxTree) {
        this.jmxTree = jmxTree;
    }

    public LocalRunner() {
    }

    public void run(String report) {
        StandardJMeterEngine engine = new StandardJMeterEngine();
        engine.configure(jmxTree);
        try {
            engine.runTest();
            MessageCache.runningEngine.put(report, engine);
        } catch (JMeterEngineException e) {
            engine.stopTest(true);
        }
    }

    public void stop(List<String> reports) {
        try {
            if (CollectionUtils.isNotEmpty(reports)) {
                for (String report : reports) {
                    StandardJMeterEngine engine = MessageCache.runningEngine.get(report);
                    if (engine != null) {
                        engine.stopTest();
                        MessageCache.runningEngine.remove(report);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error(e);
        }
    }
}