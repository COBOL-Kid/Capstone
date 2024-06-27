package com.capstone.controllers;

import com.capstone.models.Maintenance;
import com.capstone.models.MaintenanceResponse;
import com.capstone.models.VehicleInfo;
import com.capstone.models.VinResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.List;

@RestController
@RequestMapping("/api/external/")
public class CarWrapperController {

    private final WebClient webClient;

    @Autowired
    public CarWrapperController(WebClient webClient) {
        this.webClient = webClient;
    }

    @GetMapping("/find_vin/{vin}")
    public Mono<VehicleInfo> getVehicleInfo(@PathVariable String vin) {
        if (vin == null) {
            return Mono.error(new IllegalArgumentException("VIN cannot be null"));
        }
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/decode")
                        .queryParam("vin", vin)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(new Exception("Error while calling external service")))
                .bodyToMono(VinResponse.class)
                .map(VinResponse::getData)
                .map(vehicleInfo -> Tuples.of(vin, vehicleInfo))
                .flatMap(this::updateVehicleInfo);
    }

    private Mono<VehicleInfo> updateVehicleInfo(Tuple2<String, VehicleInfo> tuple) {
        String vin = tuple.getT1();
        VehicleInfo vehicleInfo = tuple.getT2();
        return webClient.get().uri(uriBuilder -> uriBuilder
                        .path("/image")
                        .queryParam("vin", vin)
                        .build())
                .retrieve().bodyToMono(VinResponse.class)
                .map(VinResponse::getData)
                .map(additionalData -> {
                    vehicleInfo.setImage(additionalData.getImage());
                    return vehicleInfo;
                });
    }

    @GetMapping("/find_maintenance/{vin}/{mileage}")
    public Mono<List<Maintenance>> getData(
            @PathVariable String vin,
            @PathVariable String mileage) { // add mileage path variable here
        return this.webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/maint")
                        .queryParam("vin", vin)
                        .queryParam("mileage", mileage) // add mileage query parameter here
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> Mono.error(new Exception("Error while calling external service")))
                .bodyToMono(MaintenanceResponse.class)
                .map(maintenanceResponse -> maintenanceResponse.getData());
    }
}

