package com.sprinklr.sprintplanning.release.service;

import com.sprinklr.sprintplanning.common.exception.ApiException;
import com.sprinklr.sprintplanning.common.exception.ResourceNotFoundException;
import com.sprinklr.sprintplanning.common.util.StringListNormalizer;
import com.sprinklr.sprintplanning.release.dto.CreateReleaseRequest;
import com.sprinklr.sprintplanning.release.dto.ReleaseBasicFiltersDto;
import com.sprinklr.sprintplanning.release.dto.ReleaseResponse;
import com.sprinklr.sprintplanning.release.dto.UpdateReleaseRequest;
import com.sprinklr.sprintplanning.release.mapper.ReleaseMapper;
import com.sprinklr.sprintplanning.release.model.ReleaseBasicFilters;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;
import com.sprinklr.sprintplanning.release.repository.ReleaseConfigRepository;
import com.sprinklr.sprintplanning.team.model.PodDocument;
import com.sprinklr.sprintplanning.team.service.TeamService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
    resolvePod(podId);
    return releaseMapper.toReleaseResponses(
        releaseConfigRepository.findByPodIdAndActiveTrueOrderByNameAsc(podId));
  }

  @Override
  public ReleaseResponse getRelease(String podId, String releaseId) {
    return releaseMapper.toReleaseResponse(getActiveReleaseDocument(podId, releaseId));
  }

  @Override
  public ReleaseResponse createRelease(String podId, CreateReleaseRequest request) {
    PodDocument pod = resolvePod(podId);
    Instant now = Instant.now();

    ReleaseConfigDocument document = new ReleaseConfigDocument();
    document.setTeamId(pod.getTeamId());
    document.setPodId(podId);
    applyRequest(document, request.name(), request.description(),
        request.fixVersionIncludes(), request.fixVersionExcludes(), request.basicFilters());
    document.setActive(true);
    document.setCreatedAt(now);
    document.setUpdatedAt(now);

    return releaseMapper.toReleaseResponse(save(document));
  }

  @Override
  public ReleaseResponse updateRelease(String podId, String releaseId, UpdateReleaseRequest request) {
    ReleaseConfigDocument document = getActiveReleaseDocument(podId, releaseId);
    applyRequest(document, request.name(), request.description(),
        request.fixVersionIncludes(), request.fixVersionExcludes(), request.basicFilters());
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
  public ReleaseConfigDocument getActiveReleaseDocument(String podId, String releaseId) {
    resolvePod(podId);
    return releaseConfigRepository.findByIdAndPodIdAndActiveTrue(releaseId, podId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "RELEASE_NOT_FOUND", "Release not found: " + releaseId));
  }

  private PodDocument resolvePod(String podId) {
    return teamService.getActivePodDocument(podId);
  }

  private void applyRequest(
      ReleaseConfigDocument document,
      String name,
      String description,
      List<String> fixVersionIncludes,
      List<String> fixVersionExcludes,
      ReleaseBasicFiltersDto basicFilters) {
    String trimmedName = name != null ? name.trim() : null;
    if (trimmedName == null || trimmedName.isEmpty()) {
      throw new ApiException("VALIDATION_ERROR", "Release name is required", HttpStatus.BAD_REQUEST);
    }

    document.setName(trimmedName);
    document.setDescription(description != null ? description.trim() : null);
    document.setFixVersionIncludes(StringListNormalizer.normalize(fixVersionIncludes));
    document.setFixVersionExcludes(StringListNormalizer.normalize(fixVersionExcludes));
    document.setBasicFilters(normalizeBasicFilters(basicFilters));
  }

  private ReleaseBasicFilters normalizeBasicFilters(ReleaseBasicFiltersDto dto) {
    ReleaseBasicFilters filters = new ReleaseBasicFilters();
    if (dto == null) {
      return filters;
    }
    filters.setIssueTypes(StringListNormalizer.normalize(dto.issueTypes()));
    filters.setStatuses(StringListNormalizer.normalize(dto.statuses()));
    filters.setDomains(StringListNormalizer.normalize(dto.domains()));
    filters.setPriorities(StringListNormalizer.normalize(dto.priorities()));
    filters.setAssigneeIds(StringListNormalizer.normalize(dto.assigneeIds()));
    return filters;
  }

  private ReleaseConfigDocument save(ReleaseConfigDocument document) {
    try {
      return releaseConfigRepository.save(document);
    } catch (DuplicateKeyException ex) {
      throw new ApiException(
          "RELEASE_NAME_CONFLICT",
          "Release name already exists for pod: " + document.getPodId(),
          HttpStatus.CONFLICT);
    }
  }
}
