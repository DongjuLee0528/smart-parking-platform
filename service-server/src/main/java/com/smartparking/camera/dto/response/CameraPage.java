package com.smartparking.camera.dto.response;

import java.util.List;

public record CameraPage(List<CameraResponse> items, int page, int size, long totalElements) {}
