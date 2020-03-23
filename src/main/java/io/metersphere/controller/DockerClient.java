package io.metersphere.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DockerClient {

    @GetMapping("/status")
    public String getStatus() {
        return "OK";
    }
}
