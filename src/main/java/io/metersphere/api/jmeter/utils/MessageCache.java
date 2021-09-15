package io.metersphere.api.jmeter.utils;

import org.apache.jmeter.engine.StandardJMeterEngine;

import java.util.concurrent.ConcurrentHashMap;

public class MessageCache {
    public static ConcurrentHashMap<String, StandardJMeterEngine> runningEngine = new ConcurrentHashMap<>();

}
