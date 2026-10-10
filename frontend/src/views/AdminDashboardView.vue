<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Aim, Briefcase, DataAnalysis, Guide, Histogram, List, Refresh, SwitchButton, Warning } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { getAdminDashboard } from '../api/admin';
import { extractApiError } from '../api/http';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const dashboard = ref(null);
const roleChartRef = ref();
const jobChartRef = ref();
const applicationChartRef = ref();
const chartInstances = [];

const summaryStats = computed(() => {
  const data = dashboard.value;
  if (!data) return [];

  return [
    { label: '平台用户', value: data.totalUsers, hint: `活跃 ${data.activeUsers}` },
    { label: '发布岗位', value: data.publishedJobs, hint: `全部 ${data.totalJobs}` },
    { label: '发布简历', value: data.publishedResumes, hint: `全部 ${data.totalResumes}` },
    { label: '有效投递', value: data.activeApplications, hint: `累计 ${data.totalApplications}` },
    { label: 'AI 匹配', value: data.totalMatches, hint: `均分 ${data.averageMatchScore}` },
  ];
});

const aiInsight = computed(() => dashboard.value?.aiOperationInsight || null);

function statusText(status) {
  const map = {
    STUDENT: '学生',
    HR: 'HR',
    ADMIN: '管理员',
    ACTIVE: '启用',
    DRAFT: '草稿',
    PUBLISHED: '已发布',
    CLOSED: '已关闭',
    SUBMITTED: '已投递',
    REVIEWING: '筛选中',
    INTERVIEW: '面试',
    OFFERED: '已录用',
    REJECTED: '未通过',
    WITHDRAWN: '已撤回',
  };
  return map[status] || status;
}

function chartData(rows = []) {
  return rows.map((row) => ({
    name: statusText(row.name),
    value: row.count,
  }));
}

const CHART_COLOR = '#0d9488';

// Per-chart rows for the template: legends, totals and empty states all read the same data as the bars.
const chartRows = computed(() => {
  const data = dashboard.value;
  if (!data) return { roles: [], jobs: [], applications: [] };
  return {
    roles: chartData(data.userRoleCounts),
    jobs: chartData(data.jobStatusCounts),
    applications: chartData(data.applicationStatusCounts),
  };
});
const averageScore = computed(() => {
  const value = Number(dashboard.value?.averageMatchScore);
  return Number.isFinite(value) ? Math.min(Math.max(value, 0), 100) : null;
});

function hasRows(rows) {
  return rows.some((row) => Number(row.value) > 0);
}

function makeChart(element, option) {
  if (!element) return;
  const chart = echarts.init(element);
  chart.setOption(option);
  chartInstances.push(chart);
}

function baseTextStyle() {
  return {
    color: '#2a3b43',
    fontFamily: 'PingFang SC, Microsoft YaHei, Noto Sans SC, system-ui, sans-serif',
    fontSize: 13,
  };
}

async function renderCharts() {
  chartInstances.splice(0).forEach((chart) => chart.dispose());
  await nextTick();
  if (!dashboard.value) return;
  const rows = chartRows.value;
  if (hasRows(rows.roles)) makeChart(roleChartRef.value, barOption(rows.roles, '人数'));
  if (hasRows(rows.jobs)) makeChart(jobChartRef.value, barOption(rows.jobs, '岗位数'));
  if (hasRows(rows.applications)) makeChart(applicationChartRef.value, barOption(rows.applications, '投递数'));
}

// Single-series horizontal bars: one colour for every bar, direct value labels, recessive axes.
function barOption(data, unit) {
  return {
    textStyle: baseTextStyle(),
    tooltip: {
      trigger: 'item',
      confine: true,
      formatter: (item) => `${item.name}<br/><b>${item.value}</b> ${unit}`,
    },
    grid: { top: 8, right: 48, bottom: 8, left: 8, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#7a8a91' },
      splitLine: { lineStyle: { color: '#e9eef0' } },
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: data.map((item) => item.name),
      axisLine: { lineStyle: { color: '#cdd7db' } },
      axisTick: { show: false },
      axisLabel: { color: '#2a3b43', fontSize: 13 },
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 22,
        barCategoryGap: '40%',
        data: data.map((item) => item.value),
        itemStyle: { color: CHART_COLOR, borderRadius: [0, 4, 4, 0] },
        emphasis: { itemStyle: { color: '#0b7a70' } },
        label: { show: true, position: 'right', color: '#15262e', fontWeight: 600 },
      },
    ],
  };
}

async function refreshDashboard() {
  loading.value = true;
  try {
    dashboard.value = await getAdminDashboard();
    await renderCharts();
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.value = false;
  }
}

function resizeCharts() {
  chartInstances.forEach((chart) => chart.resize());
}

async function logout() {
  authStore.logout();
  ElMessage.success('已退出登录');
  await router.push('/login');
}

onMounted(async () => {
  await refreshDashboard();
  window.addEventListener('resize', resizeCharts);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts);
  chartInstances.splice(0).forEach((chart) => chart.dispose());
});
</script>

<template>
  <main class="student-shell admin-shell">
    <header class="student-header admin-header">
      <div>
        <h1>平台运营看板</h1>
        <p>从用户到投递，掌握平台的运行状态与招聘进展。数据来自平台统计接口，刷新后更新。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="refreshDashboard">刷新数据</el-button>
        <el-button :icon="SwitchButton" @click="logout">退出登录</el-button>
      </div>
    </header>

    <section v-loading="loading" class="stat-strip admin-stat-strip" aria-label="管理员统计概览">
      <article v-for="stat in summaryStats" :key="stat.label" class="stat-item">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <small>{{ stat.hint }}</small>
      </article>
      <el-empty v-if="!loading && !dashboard" description="暂未获取到统计数据，请刷新重试" :image-size="56" />
    </section>

    <section v-if="aiInsight" class="work-panel admin-insight-panel">
      <div class="section-heading">
        <div>
          <h2>AI 运营洞察</h2>
          <p>{{ aiInsight.healthSummary }}</p>
        </div>
        <el-icon class="panel-icon" aria-hidden="true"><DataAnalysis /></el-icon>
      </div>

      <div class="insight-metrics">
        <div>
          <span>洞察来源</span>
          <strong class="insight-model">{{ aiInsight.modelName }}</strong>
        </div>
        <div>
          <span>匹配覆盖率</span>
          <strong>{{ aiInsight.matchCoverageRate }}%</strong>
        </div>
        <div>
          <span>低分匹配</span>
          <strong>{{ aiInsight.lowScoreMatches }}</strong>
        </div>
      </div>

      <div class="insight-grid">
        <section>
          <h3><el-icon aria-hidden="true"><Aim /></el-icon>重点关注</h3>
          <ul class="suggestion-list">
            <li v-for="item in aiInsight.focusAreas" :key="item">{{ item }}</li>
          </ul>
        </section>
        <section>
          <h3><el-icon aria-hidden="true"><Warning /></el-icon>风险提醒</h3>
          <ul class="suggestion-list">
            <li v-for="item in aiInsight.riskAlerts" :key="item">{{ item }}</li>
          </ul>
        </section>
        <section>
          <h3><el-icon aria-hidden="true"><Guide /></el-icon>建议动作</h3>
          <ul class="suggestion-list">
            <li v-for="item in aiInsight.suggestedActions" :key="item">{{ item }}</li>
          </ul>
        </section>
      </div>
    </section>

    <section v-if="dashboard" class="admin-grid">
      <article class="work-panel admin-chart-panel">
        <div class="section-heading">
          <div>
            <h2>用户角色分布</h2>
            <p>按学生、HR、管理员三类角色统计账号数量。</p>
          </div>
          <el-icon class="panel-icon" aria-hidden="true"><Histogram /></el-icon>
        </div>
        <div v-show="hasRows(chartRows.roles)" ref="roleChartRef" class="chart-box" role="img"
          :aria-label="`用户角色分布：${chartRows.roles.map((row) => `${row.name} ${row.value} 人`).join('，')}`" />
        <p v-if="!hasRows(chartRows.roles)" class="chart-empty">暂无角色统计数据</p>
      </article>

      <article class="work-panel admin-chart-panel">
        <div class="section-heading">
          <div>
            <h2>岗位状态</h2>
            <p>草稿、已发布和已关闭岗位数量。</p>
          </div>
          <el-icon class="panel-icon" aria-hidden="true"><Briefcase /></el-icon>
        </div>
        <div v-show="hasRows(chartRows.jobs)" ref="jobChartRef" class="chart-box" role="img"
          :aria-label="`岗位状态：${chartRows.jobs.map((row) => `${row.name} ${row.value} 个`).join('，')}`" />
        <p v-if="!hasRows(chartRows.jobs)" class="chart-empty">暂无岗位状态数据</p>
      </article>

      <article class="work-panel admin-chart-panel">
        <div class="section-heading">
          <div>
            <h2>投递流转</h2>
            <p>按投递处理状态统计数量，反映招聘流程进展。</p>
          </div>
          <el-icon class="panel-icon" aria-hidden="true"><List /></el-icon>
        </div>
        <div v-show="hasRows(chartRows.applications)" ref="applicationChartRef" class="chart-box" role="img"
          :aria-label="`投递流转：${chartRows.applications.map((row) => `${row.name} ${row.value} 条`).join('，')}`" />
        <p v-if="!hasRows(chartRows.applications)" class="chart-empty">暂无投递状态数据</p>
      </article>

      <article class="work-panel admin-chart-panel">
        <div class="section-heading">
          <div>
            <h2>AI 匹配质量</h2>
            <p>已生成匹配结果的平均分（满分 100），共 {{ dashboard.totalMatches }} 条结果。</p>
          </div>
          <el-icon class="panel-icon" aria-hidden="true"><DataAnalysis /></el-icon>
        </div>
        <div class="score-hero">
          <span class="score-hero-value">{{ dashboard.averageMatchScore ?? '—' }}</span>
          <span class="score-hero-unit">/ 100 平均分</span>
        </div>
        <div v-if="averageScore != null" class="coverage-meter" role="img" :aria-label="`平均分 ${dashboard.averageMatchScore}，满分 100`">
          <div class="coverage-track"><div class="coverage-fill" :style="{ width: `${averageScore}%` }" /></div>
          <div class="coverage-caption"><span>0</span><span>100</span></div>
        </div>
        <p class="form-hint">新结果按技能覆盖率计分；历史结果保留生成时的版本。分数不代表录用概率。</p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.insight-model { font-family: var(--font-mono); font-size: var(--text-base) !important; }
.score-hero { display: flex; align-items: baseline; gap: var(--space-2); margin: var(--space-4) 0 var(--space-3); }
.score-hero-value { color: var(--color-ink); font-size: 44px; font-weight: 650; line-height: 1; font-variant-numeric: tabular-nums; }
.score-hero-unit { color: var(--color-muted); font-size: var(--text-base); }
.admin-chart-panel .form-hint { margin-top: var(--space-4); }
.admin-stat-strip .el-empty { grid-column: 1 / -1; padding: var(--space-4) 0; }
</style>
