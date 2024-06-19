package com.capstone;

import com.capstone.data.VehicleInfoRepository;
import com.capstone.models.VehicleInfo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class App{
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    CommandLineRunner commandLineRunner(VehicleInfoRepository vehicleInfoRepository) {
        return args -> {
            VehicleInfo vehicle = new VehicleInfo(2006, "Toyota", "Tundra", "A Url");
            vehicleInfoRepository.save(vehicle);
        };
    }
}