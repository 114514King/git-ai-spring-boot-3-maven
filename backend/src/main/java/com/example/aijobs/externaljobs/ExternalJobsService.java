package com.example.aijobs.externaljobs;

import com.example.aijobs.common.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class ExternalJobsService {
    private static final Duration REFRESH_INTERVAL = Duration.ofHours(1);
    private static final Duration MAX_STALE_AGE = Duration.ofHours(24);
    private final JobicyClient client;
    private final Clock clock;
    private final boolean enabled;
    private Snapshot snapshot;
    private Instant nextAttemptAt = Instant.MIN;

    @Autowired
    public ExternalJobsService(JobicyClient client, @Value("${app.external-jobs.enabled:true}") boolean enabled) {
        this(client, Clock.systemUTC(), enabled);
    }

    ExternalJobsService(JobicyClient client, Clock clock, boolean enabled) {
        this.client = client;
        this.clock = clock;
        this.enabled = enabled;
    }

    public ExternalJobsPage list(String keyword, String location, int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 50
                || (keyword != null && keyword.length() > 100) || (location != null && location.length() > 100)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "页码须从 1 开始，每页 1–50 条，筛选词最多 100 个字符");
        }
        var loaded = load();
        var query = normalize(keyword);
        var geo = normalize(location);
        var filtered = loaded.snapshot.jobs.stream().filter(job -> matches(job, query))
                .filter(job -> normalize(job.location()).contains(geo)).toList();
        var total = filtered.size();
        var start = (long) (page - 1) * pageSize;
        var jobs = start >= total ? List.<ExternalJobResponse>of()
                : filtered.subList((int) start, Math.min((int) start + pageSize, total));
        return new ExternalJobsPage("JOBICY", "Jobicy", "https://jobicy.com", jobs, page, pageSize,
                total, (total + pageSize - 1) / pageSize, loaded.snapshot.syncedAt,
                loaded.stale, loaded.stale ? "来源暂时不可用，正在显示上次同步的数据，请到 Jobicy 确认岗位仍开放。" : null,
                "仅搜索本次同步的最多 200 条远程岗位；来源覆盖最近 7 天，约延迟 3 小时，每小时最多同步一次。");
    }

    private synchronized Loaded load() {
        if (!enabled) throw unavailable("外部岗位来源尚未启用");
        var now = clock.instant();
        if (snapshot != null && now.isBefore(snapshot.syncedAt.plus(REFRESH_INTERVAL))) {
            return new Loaded(snapshot, false);
        }
        if (!now.isBefore(nextAttemptAt)) {
            // All filters share this attempt limit, including upstream failures.
            nextAttemptAt = now.plus(REFRESH_INTERVAL);
            try {
                snapshot = new Snapshot(List.copyOf(client.fetch()), clock.instant());
                return new Loaded(snapshot, false);
            } catch (IOException e) {
                // Keep the last successful snapshot; never replace it with a failed refresh.
            }
        }
        if (snapshot != null && !now.isAfter(snapshot.syncedAt.plus(MAX_STALE_AGE))) {
            return new Loaded(snapshot, true);
        }
        throw unavailable("暂时无法读取 Jobicy 岗位，请稍后再试；来源请求每小时最多一次");
    }

    private boolean matches(ExternalJobResponse job, String query) {
        return query.isEmpty() || normalize(job.title()).contains(query)
                || normalize(job.companyName()).contains(query) || normalize(job.description()).contains(query)
                || job.tags().stream().anyMatch(tag -> normalize(tag).contains(query));
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private record Snapshot(List<ExternalJobResponse> jobs, Instant syncedAt) { }
    private record Loaded(Snapshot snapshot, boolean stale) { }
}
