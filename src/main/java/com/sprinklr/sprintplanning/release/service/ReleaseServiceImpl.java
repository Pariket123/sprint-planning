package com.sprinklr.sprintplanning.release.service;

import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.exception.ResourceNotFoundException;
import com.sprinklr.sprintplanning.release.dto.CreateReleaseRequest;
import com.sprinklr.sprintplanning.release.dto.ReleaseResponse;
import com.sprinklr.sprintplanning.release.dto.UpdateReleaseCapacityRequest;
import com.sprinklr.sprintplanning.release.dto.UpdateReleaseRequest;
import com.sprinklr.sprintplanning.release.mapper.ReleaseMapper;
import com.sprinklr.sprintplanning.planning.model.CapacityAllocationPercents;
import com.sprinklr.sprintplanning.planning.model.PersonCapacity;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.release.repository.ReleaseConfigRepository;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReleaseServiceImpl implements ReleaseService {

  private final TeamService teamService;
  private final ReleaseConfigRepository releaseConfigRepository;
  private final ReleaseMapper releaseMapper;

  public ReleaseServiceImpl(
      TeamService teamService,
      ReleaseConfigRepository releaseConfigRepository,
      ReleaseMapper releaseMapper) {
    this.teamService = teamService;
    this.releaseConfigRepository = releaseConfigRepository;
    this.releaseMapper = releaseMapper;
  }

  @Override
  public List<ReleaseResponse> listReleases(String podId) {
    String teamId = resolveTeamId(podId);
    return releaseMapper.toReleaseResponses(
        releaseConfigRepository.findByTeamIdAndActiveTrueOrderByNameAsc(teamId));
  }

  @Override
  public ReleaseResponse getRelease(String podId, String releaseId) {
    return releaseMapper.toReleaseResponse(getActiveReleaseDocument(podId, releaseId));
  }

  @Override
  public ReleaseResponse createRelease(String podId, CreateReleaseRequest request) {
    String teamId = resolveTeamId(podId);
    Instant now = Instant.now();

    ReleaseConfigDocument document = new ReleaseConfigDocument();
    document.setTeamId(teamId);
    applyRequest(
        document,
        request.name(),
        request.description(),
        request.baseJql(),
        request.durationDays(),
        request.startDate());
    document.setActive(true);
    document.setCreatedAt(now);
    document.setUpdatedAt(now);

    return releaseMapper.toReleaseResponse(save(document));
  }

  @Override
  public ReleaseResponse updateRelease(String podId, String releaseId, UpdateReleaseRequest request) {
    ReleaseConfigDocument document = getActiveReleaseDocument(podId, releaseId);
    applyRequest(
        document,
        request.name(),
        request.description(),
        request.baseJql(),
        request.durationDays(),
        request.startDate());
    document.setUpdatedAt(Instant.now());

    return releaseMapper.toReleaseResponse(save(document));
  }

  @Override
  public ReleaseResponse deactivateRelease(String podId, String releaseId) {
    ReleaseConfigDocument document = getActiveReleaseDocument(podId, releaseId);
    document.setActive(false);
    document.setUpdatedAt(Instant.now());
    return releaseMapper.toReleaseResponse(releaseConfigRepository.save(document));
  }

  @Override
  public ReleaseResponse updateCapacity(String podId, String releaseId, UpdateReleaseCapacityRequest request) {
    ReleaseConfigDocument document = getActiveReleaseDocument(podId, releaseId);
    document.setCapacity(copyCapacity(request.capacity()));
    document.setLeavePercent(normalizeLeavePercent(request.leavePercent()));
    if (request.capacityAllocation() != null) {
      document.setCapacityAllocation(copyCapacityAllocation(request.capacityAllocation()));
    }
    document.setUpdatedAt(Instant.now());
    return releaseMapper.toReleaseResponse(save(document));
  }

  @Override
  public ReleaseResponse updateCapacityAllocation(
      String podId,
      String releaseId,
      List<CapacityAllocationPercents> capacityAllocation) {
    ReleaseConfigDocument document = getActiveReleaseDocument(podId, releaseId);
    document.setCapacityAllocation(copyCapacityAllocation(capacityAllocation));
    document.setUpdatedAt(Instant.now());
    return releaseMapper.toReleaseResponse(save(document));
  }

  @Override
  public ReleaseConfigDocument getActiveReleaseDocument(String podId, String releaseId) {
    String teamId = resolveTeamId(podId);
    return releaseConfigRepository.findByIdAndTeamIdAndActiveTrue(releaseId, teamId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "RELEASE_NOT_FOUND", "Release not found: " + releaseId));
  }

  private String resolveTeamId(String podId) {
    return resolvePod(podId).getTeamId();
  }

  private PodDocument resolvePod(String podId) {
    return teamService.getActivePodDocument(podId);
  }

  private void applyRequest(
      ReleaseConfigDocument document,
      String name,
      String description,
      String baseJql,
      Integer durationDays,
      LocalDate startDate) {
    String trimmedName = name != null ? name.trim() : null;
    if (trimmedName == null || trimmedName.isEmpty()) {
      throw new ApiException("VALIDATION_ERROR", "Release name is required", HttpStatus.BAD_REQUEST);
    }

    document.setName(trimmedName);
    document.setDescription(description != null ? description.trim() : null);
    document.setBaseJql(normalizeJql(baseJql));
    document.setDurationDays(durationDays);
    document.setStartDate(startDate);
  }

  private String normalizeJql(String jql) {
    if (jql == null || jql.isBlank()) {
      return null;
    }
    return jql.trim();
  }

  private ReleaseConfigDocument save(ReleaseConfigDocument document) {
    try {
      return releaseConfigRepository.save(document);
    } catch (DuplicateKeyException ex) {
      throw new ApiException(
          "RELEASE_NAME_CONFLICT",
          "Release name already exists for team: " + document.getTeamId(),
          HttpStatus.CONFLICT);
    }
  }

  private List<PersonCapacity> copyCapacity(List<PersonCapacity> capacity) {
    if (capacity == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(capacity);
  }

  private List<CapacityAllocationPercents> copyCapacityAllocation(
      List<CapacityAllocationPercents> capacityAllocation) {
    if (capacityAllocation == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(capacityAllocation);
  }

  private double normalizeLeavePercent(Double leavePercent) {
    if (leavePercent == null) {
      return 0.0;
    }
    return Math.min(100.0, Math.max(0.0, leavePercent));
  }
}
