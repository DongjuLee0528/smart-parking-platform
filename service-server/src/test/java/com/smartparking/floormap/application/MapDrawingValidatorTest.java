package com.smartparking.floormap.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class MapDrawingValidatorTest {
    @Test
    void acceptsStaticSvg() {
        var drawing = MapDrawingValidator.validate(bytes(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"120\" height=\"80\"><path d=\"M0 0 L1 1\"/></svg>"));
        assertThat(drawing.mediaType()).isEqualTo("image/svg+xml");
        assertThat(drawing.width()).isEqualTo(120);
    }

    @Test
    void rejectsActiveAndExternalSvgContent() {
        assertInvalid("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><script/></svg>");
        assertInvalid("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\" onload=\"x()\"/>");
        assertInvalid("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"><rect fill=\"url(https://host/x)\"/></svg>");
        assertInvalid("<?xml-stylesheet href=\"https://host/x\"?><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"/>");
        assertInvalid("<!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"/>");
    }

    @Test
    void rejectsMalformedAndOversizedFiles() {
        assertThatThrownBy(() -> MapDrawingValidator.validate(bytes("not an image")))
            .isInstanceOf(FloorMapException.class);
        assertThatThrownBy(() -> MapDrawingValidator.validate(new byte[MapDrawingValidator.MAX_BYTES + 1]))
            .isInstanceOf(FloorMapException.class);
    }

    @Test
    void acceptsRealPngAndRejectsTruncatedPng() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 3, BufferedImage.TYPE_INT_RGB), "png", output);
        var drawing = MapDrawingValidator.validate(output.toByteArray());
        assertThat(drawing.mediaType()).isEqualTo("image/png");
        assertThat(drawing.width()).isEqualTo(2);
        assertThat(drawing.height()).isEqualTo(3);
        assertThatThrownBy(() -> MapDrawingValidator.validate(java.util.Arrays.copyOf(output.toByteArray(), 24)))
            .isInstanceOf(FloorMapException.class);
    }

    private void assertInvalid(String svg) {
        assertThatThrownBy(() -> MapDrawingValidator.validate(bytes(svg)))
            .isInstanceOf(FloorMapException.class);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
