package com.example.aijobs.externaljobs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JobicyClientTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final JobicyClient client = new JobicyClient(mapper);

    @Test
    void mapsRealSourceFieldNamesAndDecodesHtmlAsPlainText() throws Exception {
        var sourceJob = sourceJob(101);
        sourceJob.put("jobTitle", "Senior &amp; Software Developer");
        sourceJob.put("companyName", "Acme &amp; Partners");
        sourceJob.put("jobDescription", "<style>.hidden { color:red }</style><p>Build <strong>Java</strong> services.</p>"
                + "<script>alert('unsafe')</script><p>Work with &amp; support a team.</p>");
        sourceJob.putArray("jobType").add("full-time");
        sourceJob.putArray("jobIndustry").add("Engineering");
        sourceJob.put("jobGeo", "United States");
        sourceJob.put("pubDate", "2026-09-30T12:30:00+08:00");

        var result = client.parse(payload(sourceJob)).getFirst();

        assertEquals("101", result.id());
        assertEquals("Senior & Software Developer", result.title());
        assertEquals("Acme & Partners", result.companyName());
        assertEquals("United States", result.location());
        assertTrue(result.employmentType().contains("full-time"));
        assertTrue(result.tags().contains("Engineering"));
        assertEquals(Instant.parse("2026-09-30T04:30:00Z"), result.publishedAt());
        assertTrue(result.description().contains("Build Java services."));
        assertTrue(result.description().contains("Work with & support a team."));
        assertFalse(result.description().contains("<"));
        assertFalse(result.description().contains("alert"));
        assertFalse(result.description().contains("color:red"));
        assertEquals("https://jobicy.com/jobs/101-developer", result.sourceUrl());
    }

    @Test
    void salaryKeepsCurrencyAndExplicitYearPeriod() throws Exception {
        var job = sourceJob(101);
        job.put("salaryMin", 120000);
        job.put("salaryMax", 150000);
        job.put("salaryCurrency", "USD");
        job.put("salaryPeriod", "year");

        var salary = client.parse(payload(job)).getFirst().salaryText();

        assertTrue(salary.contains("USD"));
        assertTrue(salary.replace(",", "").contains("120000"));
        assertTrue(salary.replace(",", "").contains("150000"));
        assertTrue(salary.contains("年") || salary.toLowerCase().contains("year"), salary);
    }

    @Test
    void monthlySalaryKeepsOriginalCurrencyAndMonthPeriod() throws Exception {
        var job = sourceJob(101);
        job.put("salaryMin", 5000);
        job.put("salaryMax", 7000);
        job.put("salaryCurrency", "EUR");
        job.put("salaryPeriod", "month");

        var salary = client.parse(payload(job)).getFirst().salaryText();

        assertTrue(salary.contains("EUR"));
        assertTrue(salary.replace(",", "").contains("5000"));
        assertTrue(salary.replace(",", "").contains("7000"));
        assertTrue(salary.contains("月") || salary.toLowerCase().contains("month"), salary);
    }

    @Test
    void salaryIsNotConvertedOrInventedWhenOptionalFieldsAreMissing() throws Exception {
        var result = client.parse(payload(sourceJob(101))).getFirst();

        assertEquals("未提供", result.salaryText());
        assertNotNull(result.description());
        assertNotNull(result.tags());
        assertNull(result.publishedAt());
    }

    @Test
    void malformedOptionalPublicationDateDoesNotDiscardOtherwiseValidJob() throws Exception {
        var job = sourceJob(101);
        job.put("pubDate", "not a date");

        var results = client.parse(payload(job));

        assertEquals(1, results.size());
        assertNull(results.getFirst().publishedAt());
    }

    @Test
    void publicationDateWithoutTimezoneDoesNotInventAnInstant() throws Exception {
        var job = sourceJob(101);
        job.put("pubDate", "2026-09-30T12:30:00");

        var result = client.parse(payload(job)).getFirst();

        assertNull(result.publishedAt());
    }

    @Test
    void unsafeOrUnrelatedSourceLinksAreExcluded() throws Exception {
        var jobs = new ArrayList<ObjectNode>();
        jobs.add(sourceJob(101));
        var unsafeUrls = List.of(
                "http://jobicy.com/jobs/102-developer",
                "https://evil.example/jobs/102-developer",
                "https://jobicy.com.evil.example/jobs/102-developer",
                "https://jobicy.com@evil.example/jobs/102-developer",
                "https://visitor@jobicy.com/jobs/102-developer",
                "https://jobicy.com:8443/jobs/102-developer",
                "https://jobicy.com/about",
                "//jobicy.com/jobs/102-developer",
                "/jobs/102-developer",
                "javascript:alert(1)",
                "file:///jobs/102-developer"
        );
        int id = 102;
        for (var url : unsafeUrls) {
            var job = sourceJob(id++);
            job.put("url", url);
            jobs.add(job);
        }

        var results = client.parse(payload(jobs.toArray(ObjectNode[]::new)));

        assertEquals(1, results.size());
        assertEquals("101", results.getFirst().id());
    }

    @Test
    void requiredIdentityFieldsMustContainUsableValues() throws Exception {
        var missingId = sourceJob(102);
        missingId.remove("id");
        var emptyId = sourceJob(103);
        emptyId.put("id", "");
        var blankTitle = sourceJob(104);
        blankTitle.put("jobTitle", "   ");
        var missingCompany = sourceJob(105);
        missingCompany.remove("companyName");
        var blankCompany = sourceJob(106);
        blankCompany.put("companyName", "   ");
        var missingUrl = sourceJob(107);
        missingUrl.remove("url");

        var results = client.parse(payload(sourceJob(101), missingId, emptyId, blankTitle,
                missingCompany, blankCompany, missingUrl));

        assertEquals(1, results.size());
        assertEquals("101", results.getFirst().id());
    }

    @Test
    void repeatedSourceIdsAppearOnlyOnce() throws Exception {
        var duplicate = sourceJob(101);
        duplicate.put("jobTitle", "Duplicate record");

        var results = client.parse(payload(sourceJob(101), duplicate, sourceJob(102)));

        assertEquals(2, results.size());
        assertEquals(2, results.stream().map(ExternalJobResponse::id).distinct().count());
    }

    @Test
    void snapshotNeverContainsMoreThanTwoHundredJobs() throws Exception {
        var jobs = new ArrayList<ObjectNode>();
        for (int id = 1; id <= 201; id++) {
            jobs.add(sourceJob(id));
        }

        assertEquals(200, client.parse(payload(jobs.toArray(ObjectNode[]::new))).size());
    }

    @Test
    void validEmptyJobsArrayIsAccepted() throws Exception {
        assertTrue(client.parse("{\"jobs\":[]}").isEmpty());
    }

    @Test
    void malformedEnvelopeIsARefreshFailure() {
        for (var invalidPayload : List.of("", "not json", "null", "[]", "{}",
                "{\"jobs\":null}", "{\"jobs\":{}}", "{\"jobs\":\"unavailable\"}")) {
            assertThrows(IOException.class, () -> client.parse(invalidPayload), invalidPayload);
        }
    }

    @Test
    void nonemptyDatasetWithNoValidJobsIsARefreshFailure() throws Exception {
        var invalidJob = sourceJob(101);
        invalidJob.put("url", "https://evil.example/jobs/101-developer");

        assertThrows(IOException.class, () -> client.parse(payload(invalidJob)));
    }

    private ObjectNode sourceJob(int id) {
        var job = mapper.createObjectNode();
        job.put("id", id);
        job.put("jobTitle", "Software Developer");
        job.put("companyName", "Acme");
        job.put("url", "https://jobicy.com/jobs/" + id + "-developer");
        return job;
    }

    private String payload(ObjectNode... jobs) throws IOException {
        var root = mapper.createObjectNode();
        var array = root.putArray("jobs");
        for (var job : jobs) {
            array.add(job);
        }
        return mapper.writeValueAsString(root);
    }
}
