package io.metersphere.controller.request;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TestRequest extends DockerLoginRequest {

    private int size;
    private String fileString;
    private String testId;
    private String image;
    private Map<String, String> testData = new HashMap<>();
    private Map<String, String> env = new HashMap<>();
}
