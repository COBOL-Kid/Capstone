package com.capstone.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/honestcar/external/")
public class CarWrapperController {


    private final WebClient webClient;

    @Autowired
    public CarWrapperController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping("/test")
    public Mono<String> get() {
        return this.webClient.get().uri("http://api.carmd.com/v3.0/decode?vin=1GNALDEK9FZ108495")
                .headers(httpHeaders -> {
                    httpHeaders.set("Content-Type", "application/json");
                    httpHeaders.set("Authorization", "");
                    httpHeaders.set("Partner-Token", "");
                })
                .retrieve().bodyToMono(String.class);
    }
}

