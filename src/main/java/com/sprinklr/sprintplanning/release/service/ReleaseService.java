package com.sprinklr.sprintplanning.release.service;

import com.sprinklr.sprintplanning.release.dto.CreateReleaseRequest;
import com.sprinklr.sprintplanning.release.dto.ReleaseResponse;
import com.sprinklr.sprintplanning.release.dto.UpdateReleaseCapacityRequest;
import com.sprinklr.sprintplanning.release.dto.UpdateReleaseRequest;
import com.sprinklr.sprintplanning.release.model.ReleaseConfigDocument;

import java.util.List;

public interface ReleaseService {

  List<ReleaseResponse> listReleases(String podId);

  ReleaseResponse getRelease(String podId, String releaseId);

  ReleaseResponse createRelease(String podId, CreateReleaseRequest request);

  ReleaseResponse updateRelease(String podId, String releaseId, UpdateReleaseRequest request);

  ReleaseResponse deactivateRelease(String podId, String releaseId);

  ReleaseResponse updateCapacity(String podId, String releaseId, UpdateReleaseCapacityRequest request);

  ReleaseConfigDocument getActiveReleaseDocument(String podId, String releaseId);
}
