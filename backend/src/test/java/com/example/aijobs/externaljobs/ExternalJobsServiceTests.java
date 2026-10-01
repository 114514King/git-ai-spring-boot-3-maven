package com.example.aijobs.externaljobs;

import com.example.aijobs.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalJobsServiceTests {
    @Mock private JobicyClient client;
    private MutableClock clock;
    private ExternalJobsService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-01T00:00:00Z"));
        service = new ExternalJobsService(client, clock, true);
    }

    @Test
    void differentSearchesShareOneFreshSnapshot() throws Exception {
        when(client.fetch()).thenReturn(sampleJobs());

        assertEquals("1", service.list("JAVA", null, 1, 20).jobs().getFirst().id());
        assertEquals("2", service.list("zephyr", null, 1, 20).jobs().getFirst().id());
        assertEquals("3", service.list("vision", null, 1, 20).jobs().getFirst().id());
        assertEquals("3", service.list("mL", null, 1, 20).jobs().getFirst().id());
        assertEquals(2, service.list(null, "united", 1, 20).total());
        assertEquals(0, service.list("Java", "Europe", 1, 20).total());
        verify(client, times(1)).fetch();
    }

    @Test
    void paginationReportsFullFilteredCountAndAllowsEmptyLaterPages() throws Exception {
        when(client.fetch()).thenReturn(sampleJobs());

        var secondPage = service.list(null, null, 2, 2);
        assertEquals(3, secondPage.total());
        assertEquals(2, secondPage.totalPages());
        assertEquals(2, secondPage.page());
        assertEquals(2, secondPage.pageSize());
        assertEquals(1, secondPage.jobs().size());
        assertEquals("3", secondPage.jobs().getFirst().id());
        assertTrue(service.list(null, null, 4, 2).jobs().isEmpty());
        verify(client, times(1)).fetch();
    }

    @Test
    void emptyUpstreamDatasetIsSuccessfulAndCached() throws Exception {
        when(client.fetch()).thenReturn(List.of());

        var response = service.list(null, null, 1, 20);
        assertEquals(0, response.total());
        assertEquals(0, response.totalPages());
        assertTrue(response.jobs().isEmpty());
        assertFalse(response.stale());
        assertEquals(clock.instant(), response.lastSyncedAt());
        service.list("java", null, 1, 20);
        verify(client, times(1)).fetch();
    }

    @Test
    void successfulRefreshReplacesSnapshotAndTimestamp() throws Exception {
        when(client.fetch()).thenReturn(sampleJobs(), List.of(sampleJobs().getFirst()));
        var original = service.list(null, null, 1, 20);

        clock.advance(Duration.ofMinutes(61));
        var refreshed = service.list(null, null, 1, 20);

        assertEquals(3, original.total());
        assertEquals(1, refreshed.total());
        assertEquals(clock.instant(), refreshed.lastSyncedAt());
        assertFalse(refreshed.stale());
        verify(client, times(2)).fetch();
    }

    @Test
    void failedRefreshPreservesSnapshotAndRetriesAtMostOncePerHour() throws Exception {
        when(client.fetch()).thenReturn(sampleJobs()).thenThrow(new IOException("upstream unavailable"));
        var original = service.list(null, null, 1, 20);

        clock.advance(Duration.ofMinutes(61));
        var stale = service.list("Java", null, 1, 20);
        assertEquals("1", stale.jobs().getFirst().id());
        assertTrue(stale.stale());
        assertNotNull(stale.warning());
        assertFalse(stale.warning().isBlank());
        assertEquals(original.lastSyncedAt(), stale.lastSyncedAt());

        clock.advance(Duration.ofMinutes(30));
        assertTrue(service.list(null, null, 1, 20).stale());
        verify(client, times(2)).fetch();

        clock.advance(Duration.ofMinutes(31));
        assertTrue(service.list(null, null, 1, 20).stale());
        verify(client, times(3)).fetch();
    }

    @Test
    void firstFetchFailureReturnsServiceUnavailableAndDoesNotHammerUpstream() throws Exception {
        when(client.fetch()).thenThrow(new IOException("upstream unavailable"));

        assertStatus(503, () -> service.list(null, null, 1, 20));
        assertStatus(503, () -> service.list("Java", "Europe", 1, 20));
        verify(client, times(1)).fetch();

        clock.advance(Duration.ofMinutes(61));
        assertStatus(503, () -> service.list(null, null, 1, 20));
        verify(client, times(2)).fetch();
    }

    @Test
    void cachedDataOlderThanTwentyFourHoursCannotBeServedOnFailure() throws Exception {
        when(client.fetch()).thenReturn(sampleJobs()).thenThrow(new IOException("upstream unavailable"));
        service.list(null, null, 1, 20);

        clock.advance(Duration.ofHours(23).plusMinutes(50));
        assertTrue(service.list(null, null, 1, 20).stale());
        clock.advance(Duration.ofMinutes(20));
        assertStatus(503, () -> service.list(null, null, 1, 20));
        verify(client, times(2)).fetch();
    }

    @Test
    void laterSuccessfulRetryRecoversFromStaleState() throws Exception {
        when(client.fetch()).thenReturn(sampleJobs())
                .thenThrow(new IOException("upstream unavailable"))
                .thenReturn(List.of(sampleJobs().getFirst()));
        service.list(null, null, 1, 20);
        clock.advance(Duration.ofMinutes(61));
        assertTrue(service.list(null, null, 1, 20).stale());

        clock.advance(Duration.ofMinutes(61));
        var recovered = service.list(null, null, 1, 20);
        assertFalse(recovered.stale());
        assertEquals(1, recovered.total());
        assertEquals(clock.instant(), recovered.lastSyncedAt());
        verify(client, times(3)).fetch();
    }

    @Test
    void disabledSourceRejectsRequestWithoutCallingUpstream() {
        var disabled = new ExternalJobsService(client, clock, false);
        assertStatus(503, () -> disabled.list(null, null, 1, 20));
        verifyNoInteractions(client);
    }

    @Test
    void invalidPagingAndOversizedFiltersAreRejectedBeforeFetching() {
        assertStatus(400, () -> service.list(null, null, 0, 20));
        assertStatus(400, () -> service.list(null, null, 1, 0));
        assertStatus(400, () -> service.list(null, null, 1, 51));
        assertStatus(400, () -> service.list("a".repeat(101), null, 1, 20));
        assertStatus(400, () -> service.list(null, "a".repeat(101), 1, 20));
        verifyNoInteractions(client);
    }

    @Test
    void maximumPageNumberDoesNotOverflowPagination() throws Exception {
        when(client.fetch()).thenReturn(sampleJobs());
        assertTrue(service.list(null, null, Integer.MAX_VALUE, 50).jobs().isEmpty());
    }

    @Test
    void simultaneousRequestsPerformOnlyOneFetch() throws Exception {
        var fetchEntered = new CountDownLatch(1);
        var releaseFetch = new CountDownLatch(1);
        when(client.fetch()).thenAnswer(invocation -> {
            fetchEntered.countDown();
            if (!releaseFetch.await(5, TimeUnit.SECONDS)) {
                throw new IOException("test fetch was not released");
            }
            return sampleJobs();
        });

        try (var executor = Executors.newFixedThreadPool(6)) {
            var responses = new ArrayList<java.util.concurrent.Future<ExternalJobsPage>>();
            for (int i = 0; i < 6; i++) {
                responses.add(executor.submit(() -> service.list(null, null, 1, 20)));
            }
            try {
                assertTrue(fetchEntered.await(5, TimeUnit.SECONDS));
            } finally {
                releaseFetch.countDown();
            }
            for (var response : responses) {
                assertEquals(3, response.get(5, TimeUnit.SECONDS).total());
            }
        }
        verify(client, times(1)).fetch();
    }

    private void assertStatus(int status, org.junit.jupiter.api.function.Executable operation) {
        var exception = assertThrows(BusinessException.class, operation);
        assertEquals(status, exception.getStatus().value());
    }

    private List<ExternalJobResponse> sampleJobs() {
        return List.of(
                job("1", "Java Developer", "Acme", "United States", "Build distributed systems", List.of("Backend")),
                job("2", "Designer", "Zephyr", "Europe", "Build interfaces", List.of("Design")),
                job("3", "Research Engineer", "Orbit", "United Kingdom", "Develop computer vision models", List.of("ML"))
        );
    }

    private ExternalJobResponse job(String id, String title, String company, String location,
                                    String description, List<String> tags) {
        return new ExternalJobResponse(id, title, company, location, "full-time", "未提供",
                clock.instant().minusSeconds(Long.parseLong(id)), description, tags,
                "https://jobicy.com/jobs/" + id + "-job");
    }

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
