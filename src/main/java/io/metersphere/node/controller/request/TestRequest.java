package io.metersphere.node.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class TestRequest extends DockerLoginRequest {

    private int size;
    private String fileString;
    private String testId;
    private String image;
    private Map<String, String> testData = new HashMap<>();
    private Map<String, String> env = new HashMap<>();
}
