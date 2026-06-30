<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Histogram, Refresh, SwitchButton } from '@element-plus/icons-vue';
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
const scoreChartRef = ref();
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

function makeChart(element, option) {
  if (!element) return;
  const chart = echarts.init(element);
  chart.setOption(option);
  chartInstances.push(chart);
}

function baseTextStyle() {
  return {
    color: '#18202f',
    fontFamily: 'Inter, Microsoft YaHei, PingFang SC, Arial, sans-serif',
  };
}

async function renderCharts() {
  chartInstances.splice(0).forEach((chart) => chart.dispose());
  await nextTick();

  const data = dashboard.value;
  if (!data) return;

  makeChart(roleChartRef.value, {
    textStyle: baseTextStyle(),
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '42%'],
        itemStyle: { borderColor: '#ffffff', borderWidth: 3 },
        data: chartData(data.userRoleCounts),
      },
    ],
  });

  makeChart(jobChartRef.value, barOption(chartData(data.jobStatusCounts), '#2563eb'));
  makeChart(applicationChartRef.value, barOption(chartData(data.applicationStatusCounts), '#0f766e'));
  makeChart(scoreChartRef.value, {
    textStyle: baseTextStyle(),
    tooltip: { trigger: 'axis' },
    grid: { top: 20, right: 12, bottom: 32, left: 42 },
    xAxis: {
      type: 'category',
      data: ['当前均分'],
      axisLine: { lineStyle: { color: '#dce3ee' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#edf2f7' } },
    },
    series: [
      {
        type: 'bar',
        barWidth: 48,
        data: [Number(data.averageMatchScore || 0)],
        itemStyle: { color: '#7c3aed', borderRadius: [6, 6, 0, 0] },
      },
    ],
  });
}

function barOption(data, color) {
  return {
    textStyle: baseTextStyle(),
    tooltip: { trigger: 'axis' },
    grid: { top: 20, right: 12, bottom: 34, left: 42 },
    xAxis: {
      type: 'category',
      data: data.map((item) => item.name),
      axisLine: { lineStyle: { color: '#dce3ee' } },
      axisTick: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#edf2f7' } },
    },
    series: [
      {
        type: 'bar',
        barWidth: 32,
        data: data.map((item) => item.value),
        itemStyle: { color, borderRadius: [6, 6, 0, 0] },
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
        <p class="eyebrow">管理员端</p>
        <h1>平台运营看板</h1>
        <p>汇总用户、岗位、简历、投递和 AI 匹配数据，帮助管理员快速查看平台当前运行状态。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" :loading="loading" @click="refreshDashboard">刷新</el-button>
        <el-button type="primary" :icon="SwitchButton" @click="logout">退出</el-button>
      </div>
    </header>

    <section v-loading="loading" class="stat-strip admin-stat-strip" aria-label="管理员统计概览">
      <article v-for="stat in summaryStats" :key="stat.label" class="stat-item">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <small>{{ stat.hint }}</small>
      </article>
    </section>

    <section class="admin-grid">
      <article class="work-panel admin-chart-panel">
        <div class="section-heading">
          <div>
            <h2>用户角色分布</h2>
            <p>按学生、HR、管理员三类角色统计当前账号构成。</p>
          </div>
          <el-icon><Histogram /></el-icon>
        </div>
        <div ref="roleChartRef" class="chart-box" />
      </article>

      <article class="work-panel admin-chart-panel">
        <div class="section-heading">
          <div>
            <h2>岗位状态</h2>
            <p>展示草稿、已发布和已关闭岗位数量。</p>
          </div>
        </div>
        <div ref="jobChartRef" class="chart-box" />
      </article>

      <article class="work-panel admin-chart-panel">
        <div class="section-heading">
          <div>
            <h2>投递流转</h2>
            <p>按投递处理状态查看招聘流程进展。</p>
          </div>
        </div>
        <div ref="applicationChartRef" class="chart-box" />
      </article>

      <article class="work-panel admin-chart-panel">
        <div class="section-heading">
          <div>
            <h2>AI 匹配质量</h2>
            <p>展示已生成匹配结果的当前平均分。</p>
          </div>
        </div>
        <div ref="scoreChartRef" class="chart-box" />
      </article>
    </section>
  </main>
</template>
