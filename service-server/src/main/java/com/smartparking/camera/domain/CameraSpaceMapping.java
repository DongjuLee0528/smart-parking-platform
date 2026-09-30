package com.smartparking.camera.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CameraSpaceMapping {
    @Column(name = "camera_id", nullable = false)
    private UUID cameraId;
    @Column(name = "image_polygon", nullable = false, columnDefinition = "text")
    private String imagePolygon;
    @Column(nullable = false)
    private int priority;
    @Column(name = "config_version", nullable = false)
    private long configVersion;

    public CameraSpaceMapping(UUID cameraId, String imagePolygon, int priority, long configVersion) {
        this.cameraId = cameraId;
        this.imagePolygon = imagePolygon;
        this.priority = priority;
        this.configVersion = configVersion;
    }
}
