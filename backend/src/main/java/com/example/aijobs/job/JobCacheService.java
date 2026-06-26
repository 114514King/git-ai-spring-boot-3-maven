package com.example.aijobs.job;

import com.example.aijobs.job.dto.JobResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class JobCacheService {
    private static final String DETAIL_KEY_PREFIX = "jobs:published:detail:";
    private static final String LIST_KEY_PREFIX = "jobs:published:list:";
    private static final TypeReference<List<JobResponse>> JOB_LIST_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration jobTtl;

    public JobCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
                           @Value("${app.cache.job-ttl:PT10M}") Duration jobTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.jobTtl = jobTtl;
    }

    public Optional<List<JobResponse>> getPublishedList(String keyword, String city, String employmentType) {
        return read(listKey(keyword, city, employmentType), JOB_LIST_TYPE);
    }

    public void putPublishedList(String keyword, String city, String employmentType, List<JobResponse> jobs) {
        write(listKey(keyword, city, employmentType), jobs);
    }

    public Optional<JobResponse> getPublishedDetail(Long id) {
        return read(detailKey(id), JobResponse.class);
    }

    public void putPublishedDetail(Long id, JobResponse job) {
        write(detailKey(id), job);
    }

    public void evictPublished(Long id) {
        try {
            if (id != null) redisTemplate.delete(detailKey(id));
            Set<String> listKeys = redisTemplate.keys(LIST_KEY_PREFIX + "*");
            if (listKeys != null && !listKeys.isEmpty()) redisTemplate.delete(listKeys);
        } catch (RuntimeException ignored) {
            // Redis is an optimization only; database writes must still succeed.
        }
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, type));
        } catch (RuntimeException | java.io.IOException ignored) {
            return Optional.empty();
        }
    }

    private <T> Optional<T> read(String key, TypeReference<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) return Optional.empty();
            return Optional.of(objectMapper.readValue(json, type));
        } catch (RuntimeException | java.io.IOException ignored) {
            return Optional.empty();
        }
    }

    private void write(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), jobTtl);
        } catch (RuntimeException | java.io.IOException ignored) {
            // Ignore cache write failures and keep the request backed by MySQL.
        }
    }

    private String listKey(String keyword, String city, String employmentType) {
        return LIST_KEY_PREFIX
                + "keyword=" + normalize(keyword)
                + ":city=" + normalize(city)
                + ":employmentType=" + normalize(employmentType);
    }

    private String detailKey(Long id) {
        return DETAIL_KEY_PREFIX + id;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) return "_";
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8);
    }
}
