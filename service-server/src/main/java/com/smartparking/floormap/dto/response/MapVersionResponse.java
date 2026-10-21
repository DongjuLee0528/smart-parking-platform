package com.smartparking.floormap.dto.response;

public record MapVersionResponse(long version, String url, String mediaType, int width, int height) {}
