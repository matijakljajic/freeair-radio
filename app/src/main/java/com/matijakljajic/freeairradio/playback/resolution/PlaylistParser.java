package com.matijakljajic.freeairradio.playback.resolution;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

final class PlaylistParser {

    private static final int MAX_PLAYLIST_ENTRIES = 32;
    private static final int MAX_LINE_LENGTH = 4096;

    static boolean isHlsManifest(@NonNull String url,
                                 @Nullable String contentType,
                                 @NonNull String body) {
        String normalizedContentType = normalizeNullable(contentType);
        if (normalizedContentType != null
                && (normalizedContentType.contains("mpegurl")
                || normalizedContentType.contains("vnd.apple.mpegurl")
                || normalizedContentType.contains("x-mpegurl"))) {
            return true;
        }

        String normalizedBody = body.toUpperCase(Locale.ROOT);
        return normalizedBody.contains("#EXT-X-STREAM-INF")
                || normalizedBody.contains("#EXT-X-TARGETDURATION")
                || normalizedBody.contains("#EXT-X-MEDIA-SEQUENCE")
                || normalizedBody.contains("#EXT-X-VERSION")
                || url.toLowerCase(Locale.ROOT).endsWith(".m3u8");
    }

    static boolean isPlaylistResponse(@NonNull String url,
                                      @Nullable String contentType,
                                      @NonNull String body) {
        if (isHlsManifest(url, contentType, body)) {
            return false;
        }

        if (isPlaylistUrl(url)) {
            return true;
        }

        String normalizedContentType = normalizeNullable(contentType);
        if (normalizedContentType != null
                && (normalizedContentType.contains("scpls")
                || normalizedContentType.contains("mpegurl")
                || normalizedContentType.contains("xspf")
                || normalizedContentType.contains("playlist")
                || normalizedContentType.contains("xml"))) {
            return true;
        }

        String normalizedBody = body.toUpperCase(Locale.ROOT);
        return normalizedBody.contains("#EXTM3U")
                || normalizedBody.contains("[PLAYLIST]")
                || normalizedBody.contains("<ASX")
                || normalizedBody.contains("<XSPF")
                || normalizedBody.contains("<LOCATION")
                || normalizedBody.contains("<REF ")
                || normalizedBody.contains("FILE1=")
                || normalizedBody.contains("URL1=");
    }

    @NonNull
    List<String> parse(@NonNull String body, @NonNull String baseUrl) {
        String trimmedBody = body.trim();
        if (trimmedBody.isEmpty()) {
            return new ArrayList<>();
        }

        if (looksLikeXmlPlaylist(trimmedBody)) {
            return extractXmlPlaylistTargets(trimmedBody, baseUrl);
        }

        List<String> targets = new ArrayList<>();
        boolean m3uPlaylist = isM3uPlaylist(baseUrl, trimmedBody);
        String[] lines = trimmedBody.split("\\r?\\n");
        for (String line : lines) {
            if (targets.size() >= MAX_PLAYLIST_ENTRIES) {
                break;
            }

            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()
                    || trimmedLine.length() > MAX_LINE_LENGTH
                    || trimmedLine.startsWith("#")) {
                continue;
            }

            if (isPlaylistEntry(trimmedLine)) {
                addResolvedUrl(targets, baseUrl, trimmedLine.substring(trimmedLine.indexOf('=') + 1));
            } else if (looksLikeUrl(trimmedLine) || m3uPlaylist) {
                addResolvedUrl(targets, baseUrl, trimmedLine);
            }
        }

        if (targets.isEmpty() && looksLikeUrl(trimmedBody)) {
            addResolvedUrl(targets, baseUrl, trimmedBody);
        }
        return deduplicateUrls(targets);
    }

    private static boolean isPlaylistUrl(@NonNull String url) {
        String lowerCaseUrl = url.toLowerCase(Locale.ROOT);
        return lowerCaseUrl.endsWith(".m3u")
                || lowerCaseUrl.endsWith(".m3u8")
                || lowerCaseUrl.endsWith(".pls")
                || lowerCaseUrl.endsWith(".xspf")
                || lowerCaseUrl.endsWith(".asx");
    }

    private static boolean isM3uPlaylist(@NonNull String baseUrl, @NonNull String body) {
        return baseUrl.toLowerCase(Locale.ROOT).endsWith(".m3u")
                || body.toUpperCase(Locale.ROOT).contains("#EXTM3U");
    }

    private static boolean isPlaylistEntry(@NonNull String line) {
        int equalsIndex = line.indexOf('=');
        if (equalsIndex <= 0) {
            return false;
        }

        String key = line.substring(0, equalsIndex).trim();
        return key.regionMatches(true, 0, "File", 0, 4)
                || key.regionMatches(true, 0, "Url", 0, 3);
    }

    @NonNull
    private static List<String> extractXmlPlaylistTargets(@NonNull String body, @NonNull String baseUrl) {
        List<String> targets = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(true);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));
            collectXmlTargets(document.getDocumentElement(), baseUrl, targets);
        } catch (ParserConfigurationException | IOException | org.xml.sax.SAXException ignored) {
        }
        return deduplicateUrls(targets);
    }

    private static void collectXmlTargets(@Nullable Node node,
                                          @NonNull String baseUrl,
                                          @NonNull List<String> targets) {
        if (node == null || targets.size() >= MAX_PLAYLIST_ENTRIES) {
            return;
        }

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            String nodeName = element.getNodeName().toLowerCase(Locale.ROOT);
            if ("location".equals(nodeName) || "ref".equals(nodeName)) {
                String value = element.hasAttribute("href")
                        ? element.getAttribute("href")
                        : element.getTextContent();
                addResolvedUrl(targets, baseUrl, value);
            }
        }

        NodeList childNodes = node.getChildNodes();
        for (int index = 0; index < childNodes.getLength(); index++) {
            collectXmlTargets(childNodes.item(index), baseUrl, targets);
        }
    }

    private static void addResolvedUrl(@NonNull List<String> targets,
                                       @NonNull String baseUrl,
                                       @Nullable String value) {
        if (value == null) {
            return;
        }

        try {
            URI resolvedUri = URI.create(baseUrl).resolve(value.trim());
            String scheme = resolvedUri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                targets.add(resolvedUri.toString());
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private static boolean looksLikeXmlPlaylist(@NonNull String body) {
        String normalizedBody = body.toUpperCase(Locale.ROOT);
        return normalizedBody.contains("<XSPF")
                || normalizedBody.contains("<ASX")
                || normalizedBody.contains("<LOCATION")
                || normalizedBody.contains("<REF ");
    }

    private static boolean looksLikeUrl(@NonNull String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    @NonNull
    private static List<String> deduplicateUrls(@NonNull List<String> urls) {
        Set<String> uniqueUrls = new LinkedHashSet<>(urls);
        return new ArrayList<>(uniqueUrls);
    }

    @Nullable
    private static String normalizeNullable(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
