package com.smartparking.floormap.application;

import com.smartparking.global.error.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

final class MapDrawingValidator {
    static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> SVG_ELEMENTS = Set.of("svg", "g", "rect", "circle", "ellipse",
        "line", "polyline", "polygon", "path", "text", "tspan");
    private static final Set<String> SVG_ATTRIBUTES = Set.of("width", "height", "viewBox", "x", "y",
        "x1", "y1", "x2", "y2", "cx", "cy", "r", "rx", "ry", "points", "d", "transform",
        "fill", "stroke", "stroke-width", "opacity", "fill-opacity", "stroke-opacity",
        "font-size", "font-family", "text-anchor", "xmlns");

    private MapDrawingValidator() {}

    static Drawing validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw invalid();
        }
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' &&
            bytes[3] == 'G' && bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10) {
            return raster(bytes, "png", "image/png");
        }
        if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 &&
            bytes[2] == (byte) 0xff) {
            return raster(bytes, "JPEG", "image/jpeg");
        }
        return svg(bytes);
    }

    private static Drawing raster(byte[] bytes, String format, String mediaType) {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (!reader.getFormatName().equalsIgnoreCase(format)) throw invalid();
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 16_000_000) throw invalid();
                if (reader.read(0) == null) throw invalid();
                return new Drawing(mediaType, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof FloorMapException domain) throw domain;
            throw invalid();
        }
    }

    private static Drawing svg(byte[] bytes) {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
            for (var child = document.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child.getNodeType() != Node.ELEMENT_NODE) throw invalid();
            }
            var root = document.getDocumentElement();
            if (!"svg".equals(root.getLocalName())) throw invalid();
            inspect(root);
            int width = positiveDimension(root.getAttribute("width"));
            int height = positiveDimension(root.getAttribute("height"));
            return new Drawing("image/svg+xml", width, height);
        } catch (FloorMapException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private static void inspect(Element element) {
        if (!"http://www.w3.org/2000/svg".equals(element.getNamespaceURI()) ||
            !SVG_ELEMENTS.contains(element.getLocalName())) throw invalid();
        var attributes = element.getAttributes();
        for (int index = 0; index < attributes.getLength(); index++) {
            var attribute = attributes.item(index);
            var name = attribute.getNodeName();
            var value = attribute.getNodeValue().toLowerCase(java.util.Locale.ROOT);
            if (!SVG_ATTRIBUTES.contains(name) || value.contains("url(") || value.contains("javascript:") ||
                value.contains("data:") || value.contains("<") || value.contains(">")) throw invalid();
        }
        for (var child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) inspect((Element) child);
            else if (child.getNodeType() != Node.TEXT_NODE && child.getNodeType() != Node.CDATA_SECTION_NODE)
                throw invalid();
        }
    }

    private static int positiveDimension(String value) {
        try {
            int dimension = Integer.parseInt(value);
            if (dimension >= 1 && dimension <= 10000) return dimension;
        } catch (NumberFormatException ignored) {
        }
        throw invalid();
    }

    private static FloorMapException invalid() {
        return new FloorMapException(ErrorCode.VALIDATION_FAILED, "Invalid floor drawing");
    }

    record Drawing(String mediaType, int width, int height) {}
}
