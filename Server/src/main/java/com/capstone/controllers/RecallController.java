package com.capstone.controllers;

import com.capstone.domain.RecallTrackingService;
import com.capstone.models.User;
import com.capstone.models.dto.CompleteRecallRequest;
import com.capstone.models.dto.CompletedRecallResponse;
import com.capstone.models.dto.RecallResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.capstone.models.dto.VinValidation.VIN_MESSAGE;
import static com.capstone.models.dto.VinValidation.VIN_PATTERN;

@RestController
@RequestMapping("/api/recall")
@Validated
public class RecallController {

    private final RecallTrackingService recallTrackingService;

    public RecallController(RecallTrackingService recallTrackingService) {
        this.recallTrackingService = recallTrackingService;
    }

    @GetMapping("/{vin}/completed")
    public ResponseEntity<?> getCompletedRecalls(@AuthenticationPrincipal User user,
                                                 @PathVariable @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE) String vin) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<CompletedRecallResponse> completedRecalls = recallTrackingService.findCompletedRecalls(user, vin);
        if (completedRecalls.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(completedRecalls, HttpStatus.OK);
    }

    @GetMapping("/{vin}/uncompleted")
    public ResponseEntity<?> getUncompletedRecalls(@AuthenticationPrincipal User user,
                                                   @PathVariable @Pattern(regexp = VIN_PATTERN, flags = Pattern.Flag.CASE_INSENSITIVE, message = VIN_MESSAGE) String vin) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        List<RecallResponse> uncompletedRecalls = recallTrackingService.findUncompletedRecalls(user, vin);
        if (uncompletedRecalls.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(uncompletedRecalls, HttpStatus.OK);
    }

    @PostMapping("/completed")
    public ResponseEntity<?> completeRecall(@AuthenticationPrincipal User user,
                                            @Valid @RequestBody CompleteRecallRequest request) {
        if (user == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(recallTrackingService.completeRecall(user, request), HttpStatus.CREATED);
    }
}
