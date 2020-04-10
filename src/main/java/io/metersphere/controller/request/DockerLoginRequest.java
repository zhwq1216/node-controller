package io.metersphere.controller.request;

import lombok.Data;

@Data
public class DockerLoginRequest {
    private String username;
    private String password;
    private String registry;
}
