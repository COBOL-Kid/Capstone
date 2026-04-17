package com.capstone.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.domain.VinService;
import com.capstone.models.Result;
import com.capstone.models.Vin;

@RestController
@RequestMapping("/api/vin")
public class VinController {

    VinService vinService;

    public VinController(VinService vinService) {
        this.vinService = vinService;
    }

    @GetMapping("/{vinId}")
    public ResponseEntity<?> getVinsByOwnerId(@PathVariable Long vinId) {
        List<Vin> vinList = vinService.findVinsByOwnerId(vinId);
        if (vinList.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(vinList, HttpStatus.OK);
        }
    }

    @PostMapping
    public ResponseEntity<?> createVin(@RequestBody Vin vin) {
        Result<Vin> vinResult = vinService.createVin(vin);
        if (!vinResult.isSuccess()) {
            return new ResponseEntity<>(vinResult.getErrors(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(vinResult.getPayload(), HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<?> updateVin(@RequestBody Vin vin) {
        Result<Vin> vinResult = vinService.updateVin(vin);
        if (!vinResult.isSuccess()) {
            return new ResponseEntity<>(vinResult.getErrors(), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(vinResult.getPayload(), HttpStatus.OK);
    }

    @DeleteMapping("/{vinId}")
    public ResponseEntity<?> deleteVin(@PathVariable Long vinId) {
        Result<Vin> result = vinService.deleteVinById(vinId);
        if (result.isSuccess()) {
            return new ResponseEntity<>(result.getPayload(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result.getErrors(), HttpStatus.BAD_REQUEST);
        }
    }
}
