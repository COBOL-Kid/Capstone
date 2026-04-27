package com.capstone.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capstone.domain.RecallTrackingService;
import com.capstone.domain.dto.CompleteRecallRequest;
import com.capstone.domain.dto.CompletedRecallResponse;
import com.capstone.models.User;

@RestController
@RequestMapping("/api/recall")
public class RecallController {

    private final RecallTrackingService recallTrackingService;

    public RecallController(RecallTrackingService recallTrackingService) {
        this.recallTrackingService = recallTrackingService;
    }

    @GetMapping("/{vin}/completed")
    public ResponseEntity<?> getCompletedRecalls(@AuthenticationPrincipal User user, @PathVariable String vin) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<CompletedRecallResponse> completedRecalls = recallTrackingService.findCompletedRecalls(user, vin);
        if (completedRecalls.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(completedRecalls, HttpStatus.OK);
    }

    @PostMapping("/completed")
    public ResponseEntity<?> completeRecall(@AuthenticationPrincipal User user,
            @RequestBody CompleteRecallRequest request) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(recallTrackingService.completeRecall(user, request), HttpStatus.CREATED);
    }
}