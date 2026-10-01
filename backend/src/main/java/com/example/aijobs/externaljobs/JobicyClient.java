package com.example.aijobs.externaljobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.TextNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Component
public class JobicyClient {
    private static final URI ENDPOINT = URI.create("https://jobicy.com/api/v2/remote-jobs?count=200");
    private static final int MAX_BODY_BYTES = 6 * 1024 * 1024;
    private final ObjectMapper mapper;

    public JobicyClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<ExternalJobResponse> fetch() throws IOException {
        var connection = (HttpURLConnection) ENDPOINT.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(3_000);
        connection.setReadTimeout(8_000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "AIJobPlatform/1.0 (Jobicy public feed)");
        try {
            if (connection.getResponseCode() != 200) {
                throw new IOException("Jobicy returned an unsuccessful status");
            }
            try (var input = connection.getInputStream()) {
                var bytes = input.readNBytes(MAX_BODY_BYTES + 1);
                if (bytes.length > MAX_BODY_BYTES) {
                    throw new IOException("Jobicy response exceeded the size limit");
                }
                return parse(new String(bytes, StandardCharsets.UTF_8));
            }
        } finally {
            connection.disconnect();
        }
    }

    List<ExternalJobResponse> parse(String payload) throws IOException {
        var root = mapper.readTree(payload);
        if (root == null || !root.isObject() || !root.path("jobs").isArray()
                || (root.has("success") && !root.path("success").asBoolean())
                || (root.has("statusCode") && root.path("statusCode").asInt() != 200)) {
            throw new IOException("Jobicy returned an invalid jobs envelope");
        }
        var jobs = root.path("jobs");
        var results = new LinkedHashMap<String, ExternalJobResponse>();
        for (var row : jobs) {
            if (results.size() >= 200) break;
            var id = row.path("id");
            var title = plain(row.path("jobTitle").asText(""), 500);
            var company = plain(row.path("companyName").asText(""), 500);
            var url = row.path("url").asText("");
            if (!id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0
                    || title.isBlank() || company.isBlank() || !safeSourceUrl(url)) continue;
            var types = stringArray(row.path("jobType"));
            var tags = new LinkedHashSet<>(stringArray(row.path("jobIndustry")));
            tags.addAll(types);
            var location = plain(row.path("jobGeo").asText(""), 500);
            var result = new ExternalJobResponse(id.asText(), title, company,
                    location.isBlank() ? "未提供地区限制" : location,
                    types.isEmpty() ? "未提供" : String.join(" · ", types), salary(row),
                    publishedAt(row.path("pubDate").asText("")),
                    description(row.path("jobDescription").asText("")),
                    tags.stream().limit(16).toList(), url);
            results.putIfAbsent(result.id(), result);
        }
        if (!jobs.isEmpty() && results.isEmpty()) {
            throw new IOException("Jobicy returned no usable jobs in a nonempty dataset");
        }
        return List.copyOf(results.values());
    }

    private boolean safeSourceUrl(String value) {
        try {
            var uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && List.of("jobicy.com", "www.jobicy.com").contains(uri.getHost().toLowerCase(Locale.ROOT))
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443)
                    && uri.getPath() != null && uri.getPath().startsWith("/jobs/");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private List<String> stringArray(JsonNode values) {
        var result = new ArrayList<String>();
        if (values.isArray()) {
            for (var value : values) {
                if (!value.isTextual()) continue;
                var text = plain(value.asText(), 100);
                if (!text.isBlank() && !result.contains(text)) result.add(text);
                if (result.size() >= 16) break;
            }
        }
        return result;
    }

    private String plain(String html, int limit) {
        return truncate(Jsoup.parseBodyFragment(html).text().strip(), limit);
    }

    private String description(String html) {
        var document = Jsoup.parseBodyFragment(html);
        document.select("script, style, noscript, iframe, svg").remove();
        document.select("p, div, li, h1, h2, h3, h4, br").forEach(element -> element.before(new TextNode("\n")));
        var text = document.body().wholeText().replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\n *", "\n").replaceAll("\n{3,}", "\n\n").strip();
        return text.isBlank() ? "未提供" : truncate(text, 20_000);
    }

    private String truncate(String value, int limit) {
        return value.length() > limit ? value.substring(0, limit) + "…" : value;
    }

    private Instant publishedAt(String value) {
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String salary(JsonNode row) {
        var min = amount(row.path("salaryMin"));
        var max = amount(row.path("salaryMax"));
        if ((min == null && max == null) || (min != null && max != null && min.compareTo(max) > 0)) return "未提供";
        var currency = plain(row.path("salaryCurrency").asText(""), 20).toUpperCase(Locale.ROOT);
        if (currency.isBlank()) currency = "币种未提供";
        var period = plain(row.path("salaryPeriod").asText(""), 50);
        period = switch (period.toLowerCase(Locale.ROOT)) {
            case "yearly", "annual", "year" -> "年";
            case "monthly", "month" -> "月";
            case "weekly", "week" -> "周";
            case "hourly", "hour" -> "小时";
            case "daily", "day" -> "天";
            case "" -> "周期未提供";
            default -> period;
        };
        var range = min == null ? max.toPlainString() + " 以内"
                : max == null ? min.toPlainString() + " 起"
                : min.toPlainString() + " – " + max.toPlainString();
        return currency + " " + range + " / " + period;
    }

    private BigDecimal amount(JsonNode value) {
        if (!value.isNumber()) return null;
        var amount = value.decimalValue();
        return amount.signum() < 0 || amount.precision() > 15 ? null : amount.stripTrailingZeros();
    }
}
