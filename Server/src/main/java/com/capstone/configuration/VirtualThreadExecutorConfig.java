package com.capstone.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirtualThreadExecutorConfig {

  @Bean(destroyMethod = "close")
  ExecutorService vehicleDataExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }
}
