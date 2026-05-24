package com.capstone.domain;

import com.capstone.data.CompletedRecallRepositoryJPA;
import com.capstone.data.RecallRepositoryJPA;
import com.capstone.data.UserVinRepositoryJPA;
import com.capstone.models.CompletedRecall;
import com.capstone.models.Recall;
import com.capstone.models.User;
import com.capstone.models.UserVin;
import com.capstone.models.dto.CompleteRecallRequest;
import com.capstone.models.dto.CompletedRecallResponse;
import com.capstone.models.dto.RecallResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecallTrackingService {

  private final CompletedRecallRepositoryJPA completedRecallRepository;
  private final RecallRepositoryJPA recallRepository;
  private final UserVinRepositoryJPA userVinRepository;

  public RecallTrackingService(
      CompletedRecallRepositoryJPA completedRecallRepository,
      RecallRepositoryJPA recallRepository,
      UserVinRepositoryJPA userVinRepository) {
    this.completedRecallRepository = completedRecallRepository;
    this.recallRepository = recallRepository;
    this.userVinRepository = userVinRepository;
  }

  @Transactional(readOnly = true)
  public List<CompletedRecallResponse> findCompletedRecalls(User user, String vin) {
    return completedRecallRepository.findAllForUserVin(user.getUserId(), normalizeVin(vin)).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<RecallResponse> findUncompletedRecalls(User user, String vin) {
    String normalizedVin = normalizeVin(vin);
    return recallRepository.findUncompletedRecalls(user.getUserId(), normalizedVin).stream()
        .map(
            r ->
                new RecallResponse(
                    r.getRecallId(),
                    normalizedVin,
                    r.getNhtsaCampaignNumber(),
                    r.getReportReceivedDate(),
                    r.getComponent(),
                    r.getSummary(),
                    r.getConsequence(),
                    r.getRemedy()))
        .toList();
  }

  @Transactional
  public CompletedRecallResponse completeRecall(User user, CompleteRecallRequest request) {
    if (request == null || request.recallId() == null) {
      throw new IllegalArgumentException("Recall is required");
    }
    UserVin userVin =
        userVinRepository
            .findByUserUserIdAndVinVin(user.getUserId(), normalizeVin(request.vin()))
            .orElseThrow(VinNotAssociatedException::new);
    Recall recall =
        recallRepository.findById(request.recallId()).orElseThrow(RecallNotFoundException::new);
    CompletedRecall completedRecall =
        completedRecallRepository
            .findByUserVinAndRecall(userVin, recall)
            .orElseGet(() -> saveCompletedRecall(userVin, recall, request));
    return toResponse(completedRecall);
  }

  private CompletedRecall saveCompletedRecall(
      UserVin userVin, Recall recall, CompleteRecallRequest request) {
    CompletedRecall completedRecall =
        new CompletedRecall(
            userVin,
            recall,
            request.completedDate() != null ? request.completedDate() : LocalDate.now());
    completedRecall.setRepairShop(request.repairShop());
    completedRecall.setCost(request.cost());
    completedRecall.setNotes(request.notes());
    return completedRecallRepository.save(completedRecall);
  }

  @Transactional
  public boolean uncompleteRecall(User user, Long completedRecallId) {
    if (completedRecallId == null) {
      throw new IllegalArgumentException("Completed recall id is required");
    }
    CompletedRecall completedRecall =
        completedRecallRepository
            .findById(completedRecallId)
            .orElseThrow(RecallNotFoundException::new);
    if (!completedRecall.getUserVin().getId().getUserId().equals(user.getUserId())) {
      throw new RecallNotFoundException();
    }
    completedRecallRepository.delete(completedRecall);
    return true;
  }

  private CompletedRecallResponse toResponse(CompletedRecall completedRecall) {
    Recall recall = completedRecall.getRecall();
    return new CompletedRecallResponse(
        completedRecall.getCompletedRecallId(),
        completedRecall.getUserVin().getVin().getVin(),
        recall.getRecallId(),
        completedRecall.getCompletedDate(),
        completedRecall.getRepairShop(),
        completedRecall.getCost(),
        completedRecall.getNotes(),
        recall.getNhtsaCampaignNumber(),
        recall.getReportReceivedDate(),
        recall.getComponent(),
        recall.getSummary(),
        recall.getConsequence(),
        recall.getRemedy());
  }

  private String normalizeVin(String vin) {
    if (vin == null || vin.isBlank()) {
      throw new IllegalArgumentException("VIN is required");
    }
    return vin.trim().toUpperCase();
  }
}
