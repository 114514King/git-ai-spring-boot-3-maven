<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { InfoFilled, Refresh, Search, TopRight } from '@element-plus/icons-vue';
import { listExternalJobs } from '../api/externalJobs';
import { extractApiError } from '../api/http';

const filters = reactive({ keyword: '', location: '' });
const appliedFilters = reactive({ keyword: '', location: '' });
const feed = ref(null);
const loading = ref(false);
const errorMessage = ref('');
const selectedJob = ref(null);
const detailPanel = ref();
const page = ref(1);
const pageSize = 20;
let requestController;
let requestId = 0;

const jobs = computed(() => feed.value?.jobs || []);
const selectedSourceUrl = computed(() => safeSourceUrl(selectedJob.value?.sourceUrl));

function safeSourceUrl(value) {
  if (typeof value !== 'string') return null;
  try {
    const url = new URL(value);
    if (url.protocol !== 'https:' || !['jobicy.com', 'www.jobicy.com'].includes(url.hostname)) return null;
    if (url.username || url.password || url.port) return null;
    return url.href;
  } catch {
    return null;
  }
}

function formatDate(value) {
  if (!value) return '未提供';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '未提供';
  return date.toLocaleString('zh-CN', { dateStyle: 'short', timeStyle: 'short', hour12: false });
}

async function loadJobs() {
  requestController?.abort();
  requestController = new AbortController();
  const currentRequestId = ++requestId;
  const selectedId = selectedJob.value?.id;
  loading.value = true;
  errorMessage.value = '';

  try {
    const result = await listExternalJobs({
      keyword: appliedFilters.keyword || undefined,
      location: appliedFilters.location || undefined,
      page: page.value,
      pageSize,
    }, requestController.signal);
    if (currentRequestId !== requestId) return;
    feed.value = result;
    page.value = result.page;
    selectedJob.value = result.jobs.find((job) => job.id === selectedId) || result.jobs[0] || null;
  } catch (error) {
    if (currentRequestId !== requestId || error.code === 'ERR_CANCELED') return;
    errorMessage.value = extractApiError(error);
    feed.value = null;
    selectedJob.value = null;
  } finally {
    if (currentRequestId === requestId) loading.value = false;
  }
}

function searchJobs() {
  Object.assign(appliedFilters, filters);
  page.value = 1;
  loadJobs();
}

function changePage(nextPage) {
  page.value = nextPage;
  loadJobs();
}

async function selectJob(job) {
  selectedJob.value = job;
  if (window.matchMedia('(max-width: 960px)').matches) {
    await nextTick();
    detailPanel.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}

onMounted(loadJobs);
onBeforeUnmount(() => {
  requestId += 1;
  requestController?.abort();
});
</script>

<template>
  <section class="external-jobs" aria-label="Jobicy 外部岗位">
    <div class="work-panel external-source-panel">
      <div class="external-source-main">
        <div>
          <h2>发现远程工作机会</h2>
          <p>来自 <a href="https://jobicy.com" target="_blank" rel="noopener noreferrer">Jobicy</a> 的公开岗位，需在原站查看并申请。</p>
        </div>
        <div class="external-sync-row">
          <span class="chip">来源：Jobicy</span>
          <span>最近同步：{{ formatDate(feed?.lastSyncedAt) }}</span>
          <el-tag v-if="feed?.stale" type="warning" size="small">暂存数据</el-tag>
          <el-button :icon="Refresh" :loading="loading" @click="loadJobs">刷新岗位</el-button>
        </div>
      </div>
      <p class="notice external-source-note">
        <el-icon aria-hidden="true"><InfoFilled /></el-icon>
        <span>以英文岗位为主，远程岗位仍可能限制国家或地区，请核对工作地点。<span class="external-coverage" v-text="feed?.coverage || '数据通常延迟约 3 小时，当前来源覆盖最近 7 天内最多 200 条岗位。'" /></span>
      </p>
      <el-alert v-if="feed?.warning || feed?.stale" class="external-warning" :title="feed.warning || '暂时无法同步最新岗位，当前展示上次同步的数据。'" type="warning" :closable="false" show-icon />
    </div>

    <section class="workspace-grid wide-main external-workspace" :aria-busy="loading">
      <div class="work-panel">
        <div class="section-heading">
          <div>
            <h2>外部岗位</h2>
            <p v-if="feed" role="status">筛选结果共 {{ feed.total }} 条<span v-if="feed.totalPages > 0"> · 第 {{ feed.page }} / {{ feed.totalPages }} 页</span></p>
            <p v-else>按岗位、公司或工作地区筛选。</p>
          </div>
        </div>

        <el-form class="external-filter-grid" :model="filters" label-position="top" @submit.prevent="searchJobs">
          <el-form-item label="关键词">
            <el-input v-model.trim="filters.keyword" :prefix-icon="Search" maxlength="100" clearable placeholder="例如 Developer、Design" />
          </el-form-item>
          <el-form-item label="工作地区">
            <el-input v-model.trim="filters.location" maxlength="100" clearable placeholder="例如 Anywhere、USA" />
          </el-form-item>
          <el-button type="primary" native-type="submit" :icon="Search" :loading="loading">筛选</el-button>
        </el-form>

        <div v-if="loading && !feed" class="external-loading" role="status">
          <el-skeleton :rows="6" animated />
          <p>正在读取 Jobicy 岗位…</p>
        </div>
        <div v-else-if="errorMessage" class="external-error" role="alert">
          <el-alert :title="errorMessage" type="error" :closable="false" show-icon />
          <el-button :icon="Refresh" @click="loadJobs">重新加载</el-button>
        </div>
        <div v-else v-loading="loading" class="job-list">
          <button
            v-for="job in jobs"
            :key="job.id"
            type="button"
            class="job-row external-job-row"
            :class="{ active: selectedJob?.id === job.id }"
            :aria-pressed="selectedJob?.id === job.id"
            @click="selectJob(job)"
          >
            <span class="job-row-main external-job-main">
              <strong>{{ job.title || '未提供' }}</strong>
              <small>{{ job.companyName || '未提供' }}</small>
              <span class="chip-row">
                <span class="chip accent">远程</span>
                <span class="chip">{{ job.employmentType || '用工类型未提供' }}</span>
                <span class="chip">{{ job.location || '工作地区未提供' }}</span>
              </span>
              <small class="external-published">发布于 {{ formatDate(job.publishedAt) }}</small>
            </span>
            <span class="job-salary external-job-salary" :class="{ muted: !job.salaryText }">{{ job.salaryText || '薪资未提供' }}</span>
          </button>
          <el-empty v-if="!loading && jobs.length === 0" description="当前来源范围内暂无匹配岗位，试试其他关键词或地区" />
        </div>

        <el-pagination
          v-if="feed?.totalPages > 1"
          class="external-pagination"
          :current-page="page"
          :page-size="pageSize"
          :total="feed.total"
          :pager-count="5"
          layout="prev, pager, next"
          :disabled="loading"
          @current-change="changePage"
        />
      </div>

      <aside ref="detailPanel" class="work-panel detail-panel external-detail" aria-label="外部岗位详情">
        <template v-if="selectedJob">
          <div class="job-detail-head">
            <h2>{{ selectedJob.title || '未提供' }}</h2>
            <span class="company">{{ selectedJob.companyName || '公司未提供' }}</span>
            <div class="job-detail-meta">
              <el-tag>远程</el-tag>
              <el-tag type="info">{{ selectedJob.employmentType || '用工类型未提供' }}</el-tag>
              <span>发布于 {{ formatDate(selectedJob.publishedAt) }}</span>
            </div>
          </div>
          <dl class="external-facts">
            <div><dt>工作地区</dt><dd>{{ selectedJob.location || '未提供' }}</dd></div>
            <div><dt>薪资</dt><dd class="external-salary">{{ selectedJob.salaryText || '未提供' }}</dd></div>
            <div><dt>来源</dt><dd>Jobicy</dd></div>
          </dl>
          <div v-if="selectedJob.tags?.length" class="external-tags" aria-label="岗位标签">
            <el-tag v-for="tag in selectedJob.tags" :key="tag" effect="plain" size="small">{{ tag }}</el-tag>
          </div>
          <div class="external-apply">
            <a v-if="selectedSourceUrl" class="external-apply-link" :href="selectedSourceUrl" target="_blank" rel="noopener noreferrer">
              前往 Jobicy 查看并申请 <el-icon aria-hidden="true"><TopRight /></el-icon>
            </a>
            <p v-else class="form-hint">原站链接暂不可用，请稍后刷新岗位。</p>
            <p class="external-apply-note">将在新窗口打开原站；申请与招聘沟通在 Jobicy 原站或招聘方网站完成，本平台不代为投递。</p>
          </div>
          <section class="detail-copy">
            <h3>岗位描述</h3>
            <div class="external-description" tabindex="0" role="region" aria-label="完整岗位描述，可滚动阅读">
              <p v-text="selectedJob.description || '未提供'" />
            </div>
          </section>
        </template>
        <el-empty v-else description="选择岗位查看详情" />
      </aside>
    </section>
  </section>
</template>

<style scoped>
.external-jobs { display: grid; gap: var(--space-4); }
.external-source-panel { display: grid; gap: var(--space-3); padding: var(--space-4) var(--space-5); }
.external-source-main { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: var(--space-3) var(--space-5); }
.external-source-main h2 { font-size: var(--text-xl); }
.external-source-main p { margin-top: 2px; color: var(--color-muted); }
.external-sync-row { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-2) var(--space-3); color: var(--color-muted); font-size: var(--text-sm); }
.external-coverage { display: block; margin-top: 2px; color: var(--color-muted); }
.external-loading p, .external-apply-note { color: var(--color-muted); font-size: var(--text-sm); line-height: var(--leading-normal); }
.external-filter-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 0.75fr) auto; align-items: end; gap: var(--space-3); margin-bottom: var(--space-4); }
.external-filter-grid .el-form-item { min-width: 0; margin-bottom: 0; }
.external-loading { padding: var(--space-4) 0; }
.external-error { display: grid; justify-items: start; gap: var(--space-4); }
.external-error .el-alert { width: 100%; }
.external-job-salary { max-width: 38%; white-space: normal; overflow-wrap: anywhere; line-height: 1.5; }
.external-published { font-size: var(--text-xs) !important; color: var(--color-subtle) !important; }
.external-pagination { margin-top: var(--space-5); justify-content: center; flex-wrap: wrap; }
.external-detail { scroll-margin-top: calc(var(--header-height) + 16px); }
.external-facts { display: grid; gap: var(--space-2); margin: var(--space-4) 0; }
.external-facts > div { display: grid; grid-template-columns: 72px minmax(0, 1fr); gap: var(--space-3); line-height: var(--leading-normal); }
.external-facts dt { color: var(--color-muted); }
.external-facts dd { margin: 0; overflow-wrap: anywhere; }
.external-salary { color: var(--color-accent-strong); font-weight: 600; }
.external-tags { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.external-tags .el-tag { max-width: 100%; height: auto; min-height: 24px; white-space: normal; overflow-wrap: anywhere; }
.external-apply { display: grid; gap: var(--space-2); margin-top: var(--space-4); padding: var(--space-4); background: var(--color-surface-muted); border: 1px solid var(--color-line); border-radius: var(--radius-md); }
.external-apply-link { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-2); min-height: 42px; padding: 0 var(--space-4); color: #fff; background: var(--color-accent); border-radius: var(--radius-md); font-size: var(--text-md); }
.external-apply-link:hover { color: #fff; background: var(--color-accent-strong); text-decoration: none; }
.external-apply-link:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 3px; }
.external-description { max-height: 460px; overflow-y: auto; padding-right: var(--space-2); scrollbar-width: thin; }
.external-description:focus-visible { outline: 2px solid var(--color-focus); outline-offset: 4px; border-radius: 4px; }
@media (max-width: 1180px) {
  .external-filter-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .external-filter-grid .el-button { grid-column: 1 / -1; }
}
@media (max-width: 640px) {
  .external-source-panel { padding: var(--space-4); }
  .external-filter-grid { grid-template-columns: 1fr; }
  .external-job-salary { max-width: 100%; }
  .external-pagination { --el-pagination-button-width: 28px; --el-pagination-button-height: 30px; }
  .external-description { max-height: 360px; }
}
</style>
