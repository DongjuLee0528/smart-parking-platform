package com.smartparking.floormap.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "map_versions", uniqueConstraints = @UniqueConstraint(columnNames = {"floor_id", "version"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "floor_id", nullable = false)
    private UUID floorId;
    @Column(nullable = false)
    private long version;
    @Column(name = "media_type", nullable = false, length = 30)
    private String mediaType;
    @Column(nullable = false)
    private int width;
    @Column(nullable = false)
    private int height;
    @Lob @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] content;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public MapVersion(UUID floorId, long version, String mediaType, int width, int height, byte[] content) {
        this.floorId = floorId;
        this.version = version;
        this.mediaType = mediaType;
        this.width = width;
        this.height = height;
        this.content = content.clone();
        this.createdAt = Instant.now();
    }
}
