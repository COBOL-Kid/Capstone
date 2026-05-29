package com.capstone.controllers;

import com.capstone.authentication.AuthenticatedUser;
import com.capstone.domain.RecallNotFoundException;
import com.capstone.domain.RecallTrackingService;
import com.capstone.models.dto.CompleteRecallRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recall")
@Validated
public class RecallController {

  private final RecallTrackingService recallTrackingService;

  public RecallController(RecallTrackingService recallTrackingService) {
    this.recallTrackingService = recallTrackingService;
  }

  @PostMapping("/completed")
  public ResponseEntity<?> completeRecall(
      @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody CompleteRecallRequest request) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    return new ResponseEntity<>(
        recallTrackingService.completeRecall(user.userId(), request), HttpStatus.CREATED);
  }

  @DeleteMapping("/completed/{completedRecallId}")
  public ResponseEntity<?> uncompleteRecall(
      @AuthenticationPrincipal AuthenticatedUser user,
      @PathVariable("completedRecallId") Long completedRecallId) {
    if (user == null) {
      return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }
    try {
      recallTrackingService.uncompleteRecall(user.userId(), completedRecallId);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (RecallNotFoundException ex) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
