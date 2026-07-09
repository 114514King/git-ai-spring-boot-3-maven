<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Briefcase,
  CircleCheck,
  Document,
  MagicStick,
  Refresh,
  SwitchButton,
} from '@element-plus/icons-vue';
import { extractApiError } from '../api/http';
import {
  analyzeHrJobJd,
  createHrJob,
  createHrMatch,
  generateHrFollowUpAdvice,
  generateHrInterviewKit,
  listHrApplications,
  listHrCandidateRecommendations,
  listHrJobs,
  listHrMatches,
  updateHrApplicationStatus,
  updateHrJob,
  updateHrJobStatus,
} from '../api/hr';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref('jobs');
const loading = reactive({
  jobs: false,
  applications: false,
  matches: false,
  recommendations: false,
  saveJob: false,
  createMatch: false,
  jdAnalysis: false,
  interviewKit: false,
  followUpAdvice: false,
});

const jobs = ref([]);
const applications = ref([]);
const matches = ref([]);
const recommendations = ref([]);
const interviewKit = ref(null);
const followUpAdvice = ref(null);
const selectedJobId = ref('');
const jdAnalysisJobId = ref('');
const selectedInterviewApplicationId = ref('');
const selectedFollowUpApplicationId = ref('');
const jdAnalysis = ref(null);
const editingJobId = ref(null);
const jobFormRef = ref();
const jobForm = reactive(emptyJobForm());
const matchForm = reactive({
  jobId: '',
  resumeId: '',
});

const jobRules = {
  title: [
    { required: true, message: '请输入岗位标题', trigger: 'blur' },
    { max: 100, message: '标题最长 100 个字符', trigger: 'blur' },
  ],
  companyName: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { max: 100, message: '公司名称最长 100 个字符', trigger: 'blur' },
  ],
  city: [
    { required: true, message: '请输入城市', trigger: 'blur' },
    { max: 50, message: '城市最长 50 个字符', trigger: 'blur' },
  ],
  employmentType: [{ required: true, message: '请选择用工类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入岗位描述', trigger: 'blur' }],
  requirements: [{ required: true, message: '请输入岗位要求', trigger: 'blur' }],
};

const dashboardStats = computed(() => [
  { label: '我的岗位', value: jobs.value.length },
  { label: '已发布岗位', value: jobs.value.filter((job) => job.status === 'PUBLISHED').length },
  { label: '收到投递', value: applications.value.length },
  { label: '匹配结果', value: matches.value.length },
  { label: '推荐候选人', value: recommendations.value.length },
]);

const selectedJob = computed(() => jobs.value.find((job) => job.id === selectedJobId.value));
const filteredApplications = computed(() => {
  if (!selectedJobId.value) return applications.value;
  return applications.value.filter((application) => application.jobId === selectedJobId.value);
});
const matchResumeOptions = computed(() => {
  const seen = new Set();
  return filteredApplications.value
    .filter((application) => application.status !== 'WITHDRAWN')
    .filter((application) => {
      const key = `${application.jobId}:${application.resumeId}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
});
const interviewApplicationOptions = computed(() =>
  filteredApplications.value.filter((application) => application.status !== 'WITHDRAWN')
);
const followUpApplicationOptions = computed(() => filteredApplications.value);

function emptyJobForm() {
  return {
    title: '',
    companyName: '',
    city: '',
    employmentType: 'FULL_TIME',
    salaryMin: '',
    salaryMax: '',
    description: '',
    requirements: '',
  };
}

function normalizeParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== '' && value !== null));
}

function toJobPayload(form) {
  return {
    title: form.title,
    companyName: form.companyName,
    city: form.city,
    employmentType: form.employmentType,
    salaryMin: form.salaryMin === '' ? null : Number(form.salaryMin),
    salaryMax: form.salaryMax === '' ? null : Number(form.salaryMax),
    description: form.description,
    requirements: form.requirements,
  };
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

function salaryText(job) {
  if (!job?.salaryMin && !job?.salaryMax) return '薪资面议';
  if (job.salaryMin && job.salaryMax) return `${job.salaryMin} - ${job.salaryMax}`;
  if (job.salaryMin) return `${job.salaryMin} 起`;
  return `${job.salaryMax} 以内`;
}

function statusText(status) {
  const map = {
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

function statusType(status) {
  if (['PUBLISHED', 'OFFERED'].includes(status)) return 'success';
  if (['CLOSED', 'REJECTED', 'WITHDRAWN'].includes(status)) return 'danger';
  if (['REVIEWING', 'INTERVIEW', 'SUBMITTED'].includes(status)) return 'warning';
  return 'info';
}

function scoreType(score) {
  const value = Number(score || 0);
  if (value >= 75) return 'success';
  if (value >= 50) return 'warning';
  return 'danger';
}

async function refreshJobs() {
  loading.jobs = true;
  try {
    jobs.value = await listHrJobs();
    if (!selectedJobId.value && jobs.value.length > 0) {
      selectedJobId.value = jobs.value[0].id;
      matchForm.jobId = jobs.value[0].id;
      jdAnalysisJobId.value = jobs.value[0].id;
    } else if (selectedJobId.value && !jobs.value.some((job) => job.id === selectedJobId.value)) {
      selectedJobId.value = '';
      matchForm.jobId = '';
    }
    if (jdAnalysisJobId.value && !jobs.value.some((job) => job.id === jdAnalysisJobId.value)) {
      jdAnalysisJobId.value = '';
      jdAnalysis.value = null;
    }
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.jobs = false;
  }
}

async function refreshApplications() {
  loading.applications = true;
  try {
    applications.value = await listHrApplications(normalizeParams({ jobId: selectedJobId.value }));
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.applications = false;
  }
}

async function refreshMatches() {
  loading.matches = true;
  try {
    matches.value = await listHrMatches(normalizeParams({ jobId: selectedJobId.value }));
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.matches = false;
  }
}

async function refreshRecommendations() {
  loading.recommendations = true;
  try {
    recommendations.value = await listHrCandidateRecommendations(normalizeParams({ jobId: selectedJobId.value }));
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.recommendations = false;
  }
}

async function refreshAll() {
  await refreshJobs();
  await Promise.all([refreshApplications(), refreshMatches(), refreshRecommendations()]);
}

async function selectJob(id) {
  selectedJobId.value = id;
  matchForm.jobId = id;
  matchForm.resumeId = '';
  selectedInterviewApplicationId.value = '';
  selectedFollowUpApplicationId.value = '';
  interviewKit.value = null;
  followUpAdvice.value = null;
  if (!jdAnalysisJobId.value) jdAnalysisJobId.value = id;
  await Promise.all([refreshApplications(), refreshMatches(), refreshRecommendations()]);
}

function editJob(job) {
  editingJobId.value = job.id;
  Object.assign(jobForm, {
    title: job.title || '',
    companyName: job.companyName || '',
    city: job.city || '',
    employmentType: job.employmentType || 'FULL_TIME',
    salaryMin: job.salaryMin ?? '',
    salaryMax: job.salaryMax ?? '',
    description: job.description || '',
    requirements: job.requirements || '',
  });
  activeTab.value = 'jobs';
}

function resetJobForm() {
  editingJobId.value = null;
  Object.assign(jobForm, emptyJobForm());
  jobFormRef.value?.clearValidate();
}

async function saveJob() {
  await jobFormRef.value.validate();
  loading.saveJob = true;
  try {
    if (editingJobId.value) {
      await updateHrJob(editingJobId.value, toJobPayload(jobForm));
      ElMessage.success('岗位已更新');
    } else {
      await createHrJob(toJobPayload(jobForm));
      ElMessage.success('岗位草稿已创建');
    }
    resetJobForm();
    await refreshJobs();
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.saveJob = false;
  }
}

async function changeJobStatus(job, status) {
  try {
    const actionText = status === 'PUBLISHED' ? '发布' : status === 'CLOSED' ? '关闭' : '设为草稿';
    await ElMessageBox.confirm(`确认${actionText}岗位「${job.title}」吗？`, '更新岗位状态', { type: 'warning' });
    await updateHrJobStatus(job.id, status);
    ElMessage.success(`岗位已${actionText}`);
    await refreshJobs();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(extractApiError(error));
    }
  }
}

async function changeApplicationStatus(application, status) {
  try {
    await updateHrApplicationStatus(application.id, status);
    ElMessage.success('投递状态已更新');
    await refreshApplications();
  } catch (error) {
    ElMessage.error(extractApiError(error));
  }
}

async function generateMatch() {
  if (!matchForm.jobId || !matchForm.resumeId) {
    ElMessage.warning('请选择岗位和已投递简历');
    return;
  }

  loading.createMatch = true;
  try {
    await createHrMatch({
      jobId: Number(matchForm.jobId),
      resumeId: Number(matchForm.resumeId),
    });
    ElMessage.success('AI 匹配已生成');
    await refreshMatches();
    activeTab.value = 'matches';
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.createMatch = false;
  }
}

async function analyzeJd() {
  if (!jdAnalysisJobId.value) {
    ElMessage.warning('请选择需要分析的岗位');
    return;
  }

  loading.jdAnalysis = true;
  try {
    jdAnalysis.value = await analyzeHrJobJd(jdAnalysisJobId.value);
    ElMessage.success('JD 分析已生成');
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.jdAnalysis = false;
  }
}

async function generateInterviewKit() {
  if (!selectedInterviewApplicationId.value) {
    ElMessage.warning('请选择需要生成面试题的投递');
    return;
  }

  loading.interviewKit = true;
  try {
    interviewKit.value = await generateHrInterviewKit(selectedInterviewApplicationId.value);
    ElMessage.success('AI 面试题已生成');
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.interviewKit = false;
  }
}

async function generateFollowUpAdvice() {
  if (!selectedFollowUpApplicationId.value) {
    ElMessage.warning('请选择需要生成跟进建议的投递');
    return;
  }

  loading.followUpAdvice = true;
  try {
    followUpAdvice.value = await generateHrFollowUpAdvice(selectedFollowUpApplicationId.value);
    ElMessage.success('AI 跟进建议已生成');
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.followUpAdvice = false;
  }
}

async function logout() {
  authStore.logout();
  ElMessage.success('已退出登录');
  await router.push('/login');
}

onMounted(refreshAll);
</script>

<template>
  <main class="student-shell">
    <header class="student-header">
      <div>
        <p class="eyebrow">HR 端</p>
        <h1>招聘工作台</h1>
        <p>发布和维护岗位、处理学生投递，并查看本岗位范围内的 AI 匹配结果。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" :icon="SwitchButton" @click="logout">退出</el-button>
      </div>
    </header>

    <section class="stat-strip" aria-label="HR 端数据概览">
      <article v-for="stat in dashboardStats" :key="stat.label" class="stat-item">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </article>
    </section>

    <el-tabs v-model="activeTab" class="student-tabs">
      <el-tab-pane label="岗位管理" name="jobs">
        <section class="workspace-grid">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>岗位编辑</h2>
                <p>创建草稿或编辑现有岗位，发布后学生端才能浏览和投递。</p>
              </div>
              <el-button @click="resetJobForm">新建草稿</el-button>
            </div>

            <el-form
              ref="jobFormRef"
              :model="jobForm"
              :rules="jobRules"
              class="resume-form"
              label-position="top"
              @submit.prevent="saveJob"
            >
              <div class="form-grid">
                <el-form-item label="岗位标题" prop="title">
                  <el-input v-model.trim="jobForm.title" placeholder="例如：Java 后端工程师" />
                </el-form-item>
                <el-form-item label="公司名称" prop="companyName">
                  <el-input v-model.trim="jobForm.companyName" />
                </el-form-item>
                <el-form-item label="城市" prop="city">
                  <el-input v-model.trim="jobForm.city" />
                </el-form-item>
                <el-form-item label="用工类型" prop="employmentType">
                  <el-select v-model="jobForm.employmentType">
                    <el-option label="全职" value="FULL_TIME" />
                    <el-option label="兼职" value="PART_TIME" />
                    <el-option label="实习" value="INTERNSHIP" />
                  </el-select>
                </el-form-item>
                <el-form-item label="最低薪资">
                  <el-input-number v-model="jobForm.salaryMin" :min="0" controls-position="right" />
                </el-form-item>
                <el-form-item label="最高薪资">
                  <el-input-number v-model="jobForm.salaryMax" :min="0" controls-position="right" />
                </el-form-item>
              </div>
              <el-form-item label="岗位描述" prop="description">
                <el-input v-model.trim="jobForm.description" type="textarea" :rows="4" />
              </el-form-item>
              <el-form-item label="岗位要求" prop="requirements">
                <el-input v-model.trim="jobForm.requirements" type="textarea" :rows="4" />
              </el-form-item>
              <el-button type="primary" native-type="submit" :loading="loading.saveJob">
                {{ editingJobId ? '保存修改' : '创建草稿' }}
              </el-button>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>我的岗位</h2>
                <p>岗位状态会影响学生端是否可见，也会限制投递入口。</p>
              </div>
            </div>

            <div v-loading="loading.jobs" class="stack-list">
              <article v-for="job in jobs" :key="job.id" class="stack-card">
                <div>
                  <h3>{{ job.title }}</h3>
                  <p>{{ job.companyName }} · {{ job.city }} · {{ salaryText(job) }}</p>
                </div>
                <el-tag :type="statusType(job.status)">{{ statusText(job.status) }}</el-tag>
                <div class="button-row">
                  <el-button size="small" :icon="Document" @click="editJob(job)">编辑</el-button>
                  <el-button size="small" :icon="Briefcase" @click="selectJob(job.id)">查看投递</el-button>
                  <el-button
                    v-if="job.status !== 'PUBLISHED'"
                    size="small"
                    type="success"
                    :icon="CircleCheck"
                    @click="changeJobStatus(job, 'PUBLISHED')"
                  >
                    发布
                  </el-button>
                  <el-button v-if="job.status !== 'DRAFT'" size="small" @click="changeJobStatus(job, 'DRAFT')">
                    设为草稿
                  </el-button>
                  <el-button v-if="job.status !== 'CLOSED'" size="small" type="danger" @click="changeJobStatus(job, 'CLOSED')">
                    关闭
                  </el-button>
                </div>
              </article>
              <el-empty v-if="!loading.jobs && jobs.length === 0" description="暂无岗位" />
            </div>
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="投递管理" name="applications">
        <section class="work-panel">
          <div class="section-heading">
            <div>
              <h2>收到的投递</h2>
              <p>只展示当前 HR 自己岗位收到的投递，可按岗位筛选并推进状态。</p>
            </div>
            <div class="header-actions">
              <el-select v-model="selectedJobId" clearable placeholder="全部岗位" @change="selectJob">
                <el-option v-for="job in jobs" :key="job.id" :label="job.title" :value="job.id" />
              </el-select>
              <el-button :icon="Refresh" @click="refreshApplications">刷新投递</el-button>
            </div>
          </div>

          <el-table v-loading="loading.applications" :data="filteredApplications" class="data-table">
            <el-table-column prop="id" label="投递 ID" width="100" />
            <el-table-column prop="jobId" label="岗位 ID" width="100" />
            <el-table-column prop="studentId" label="学生 ID" width="100" />
            <el-table-column prop="resumeId" label="简历 ID" width="100" />
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="投递时间">
              <template #default="{ row }">{{ formatDate(row.appliedAt) }}</template>
            </el-table-column>
            <el-table-column label="处理" width="180">
              <template #default="{ row }">
                <el-select
                  :model-value="row.status"
                  :disabled="row.status === 'WITHDRAWN'"
                  size="small"
                  @change="(status) => changeApplicationStatus(row, status)"
                >
                  <el-option label="筛选中" value="REVIEWING" />
                  <el-option label="面试" value="INTERVIEW" />
                  <el-option label="已录用" value="OFFERED" />
                  <el-option label="未通过" value="REJECTED" />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="候选人推荐" name="recommendations">
        <section class="work-panel">
          <div class="section-heading">
            <div>
              <h2>候选人推荐排序</h2>
              <p>基于投递简历、岗位要求和已有 AI 匹配结果生成本地规则推荐，不自动变更投递状态。</p>
            </div>
            <div class="header-actions">
              <el-select v-model="selectedJobId" clearable placeholder="全部岗位" @change="selectJob">
                <el-option v-for="job in jobs" :key="job.id" :label="job.title" :value="job.id" />
              </el-select>
              <el-button :icon="Refresh" @click="refreshRecommendations">刷新推荐</el-button>
            </div>
          </div>

          <div v-loading="loading.recommendations" class="recommendation-list">
            <article
              v-for="candidate in recommendations"
              :key="candidate.applicationId"
              class="stack-card recommendation-card"
            >
              <div class="recommendation-score">
                <el-tag :type="scoreType(candidate.recommendationScore)" size="large">
                  {{ candidate.recommendationScore }} 分
                </el-tag>
                <small>{{ candidate.scoreSource }}</small>
              </div>
              <div>
                <h3>简历 {{ candidate.resumeId }} / 学生 {{ candidate.studentId }}</h3>
                <p>
                  岗位 {{ candidate.jobId }} · 投递 {{ candidate.applicationId }} ·
                  {{ statusText(candidate.applicationStatus) }} · {{ formatDate(candidate.appliedAt) }}
                </p>
                <dl class="match-explain">
                  <div>
                    <dt>推荐理由</dt>
                    <dd>{{ candidate.recommendationReason }}</dd>
                  </div>
                  <div>
                    <dt>风险摘要</dt>
                    <dd>{{ candidate.riskSummary }}</dd>
                  </div>
                  <div>
                    <dt>建议动作</dt>
                    <dd>{{ candidate.suggestedAction }}</dd>
                  </div>
                  <div>
                    <dt>关键词</dt>
                    <dd>
                      已匹配：{{ candidate.matchedKeywords.join('、') || '暂无' }}；
                      待核验：{{ candidate.missingKeywords.join('、') || '暂无' }}
                    </dd>
                  </div>
                </dl>
              </div>
            </article>
            <el-empty v-if="!loading.recommendations && recommendations.length === 0" description="暂无候选人推荐" />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="跟进建议" name="follow-up">
        <section class="workspace-grid">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>AI 投递跟进建议</h2>
                <p>基于单个投递的当前状态、岗位要求、简历内容和已有匹配分生成下一步建议。</p>
              </div>
            </div>

            <el-form class="action-form" label-position="top">
              <el-form-item label="投递">
                <el-select v-model="selectedFollowUpApplicationId" placeholder="请选择投递">
                  <el-option
                    v-for="application in followUpApplicationOptions"
                    :key="application.id"
                    :label="`投递 ${application.id} / ${statusText(application.status)} / 简历 ${application.resumeId}`"
                    :value="application.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="loading.followUpAdvice"
                @click="generateFollowUpAdvice"
              >
                生成跟进建议
              </el-button>
              <p class="form-hint">建议不会自动修改投递状态，HR 仍需人工确认后再操作。</p>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>跟进结果</h2>
                <p>{{ followUpAdvice ? followUpAdvice.modelName : '选择投递后生成本地规则建议' }}</p>
              </div>
            </div>

            <article v-if="followUpAdvice" class="stack-card match-card">
              <div>
                <div class="recommendation-score">
                  <el-tag :type="scoreType(followUpAdvice.matchScore)" size="large">
                    {{ followUpAdvice.matchScore }} 分
                  </el-tag>
                  <small>{{ followUpAdvice.scoreSource }} · {{ followUpAdvice.priorityLevel }}</small>
                </div>
                <h3>投递 {{ followUpAdvice.applicationId }} / 简历 {{ followUpAdvice.resumeId }}</h3>
                <p>{{ followUpAdvice.decisionSummary }}</p>

                <dl class="match-explain">
                  <div>
                    <dt>建议状态</dt>
                    <dd>{{ statusText(followUpAdvice.nextStatusSuggestion) }}</dd>
                  </div>
                  <div>
                    <dt>关键词</dt>
                    <dd>
                      已匹配：{{ followUpAdvice.matchedKeywords.join('、') || '暂无' }}；
                      待核验：{{ followUpAdvice.missingKeywords.join('、') || '暂无' }}
                    </dd>
                  </div>
                  <div>
                    <dt>风险提醒</dt>
                    <dd>{{ followUpAdvice.riskAlerts.join('；') }}</dd>
                  </div>
                  <div>
                    <dt>建议动作</dt>
                    <dd>{{ followUpAdvice.recommendedActions.join('；') }}</dd>
                  </div>
                  <div>
                    <dt>沟通提示</dt>
                    <dd>{{ followUpAdvice.communicationTips.join('；') }}</dd>
                  </div>
                </dl>
              </div>
            </article>
            <el-empty v-else description="暂无跟进建议" />
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 面试题" name="interview-kit">
        <section class="workspace-grid">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>AI 面试题生成</h2>
                <p>基于当前岗位范围内的单个投递生成本地规则面试题和评分维度，不会自动变更投递状态。</p>
              </div>
            </div>

            <el-form class="action-form" label-position="top">
              <el-form-item label="投递">
                <el-select v-model="selectedInterviewApplicationId" placeholder="请选择投递">
                  <el-option
                    v-for="application in interviewApplicationOptions"
                    :key="application.id"
                    :label="`投递 ${application.id} / 简历 ${application.resumeId} / 学生 ${application.studentId}`"
                    :value="application.id"
                  />
                </el-select>
              </el-form-item>
              <el-button type="primary" :icon="MagicStick" :loading="loading.interviewKit" @click="generateInterviewKit">
                生成 AI 面试题
              </el-button>
              <p class="form-hint">题目用于面试准备和评分参考，不新增面试记录持久化表。</p>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>面试题与评分维度</h2>
                <p>{{ interviewKit ? interviewKit.modelName : '选择投递后生成本地规则面试题' }}</p>
              </div>
            </div>

            <article v-if="interviewKit" class="stack-card match-card">
              <div>
                <h3>投递 {{ interviewKit.applicationId }} / 简历 {{ interviewKit.resumeId }}</h3>
                <p>{{ interviewKit.summary }}</p>

                <dl class="match-explain">
                  <div>
                    <dt>面试题</dt>
                    <dd>
                      <ol class="ordered-list">
                        <li v-for="question in interviewKit.questions" :key="question.category">
                          <strong>{{ question.category }}：</strong>{{ question.question }}
                          <small>{{ question.evaluationFocus }} / {{ question.expectedEvidence }}</small>
                        </li>
                      </ol>
                    </dd>
                  </div>
                  <div>
                    <dt>评分维度</dt>
                    <dd>
                      <ol class="ordered-list">
                        <li v-for="dimension in interviewKit.scoringDimensions" :key="dimension.name">
                          <strong>{{ dimension.name }}（{{ dimension.weight }}%）：</strong>{{ dimension.highScoreSignal }}
                          <small>低分风险：{{ dimension.lowScoreRisk }}</small>
                        </li>
                      </ol>
                    </dd>
                  </div>
                  <div>
                    <dt>风险关注</dt>
                    <dd>{{ interviewKit.riskFocus.join('；') }}</dd>
                  </div>
                  <div>
                    <dt>建议追问</dt>
                    <dd>{{ interviewKit.followUpSuggestions.join('；') }}</dd>
                  </div>
                </dl>
              </div>
            </article>
            <el-empty v-else description="暂无面试题结果" />
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 匹配" name="matches">
        <section class="workspace-grid">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>生成岗位匹配</h2>
                <p>HR 只能为已投递到本人岗位的简历生成匹配结果。</p>
              </div>
            </div>

            <el-form class="action-form" :model="matchForm" label-position="top">
              <el-form-item label="岗位">
                <el-select v-model="matchForm.jobId" placeholder="请选择岗位" @change="selectJob">
                  <el-option v-for="job in jobs" :key="job.id" :label="job.title" :value="job.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="已投递简历">
                <el-select v-model="matchForm.resumeId" placeholder="请选择简历">
                  <el-option
                    v-for="application in matchResumeOptions"
                    :key="`${application.jobId}-${application.resumeId}`"
                    :label="`简历 ${application.resumeId} / 学生 ${application.studentId}`"
                    :value="application.resumeId"
                  />
                </el-select>
              </el-form-item>
              <el-button type="primary" :icon="MagicStick" :loading="loading.createMatch" @click="generateMatch">
                生成 AI 匹配
              </el-button>
              <p v-if="matchResumeOptions.length === 0" class="form-hint">当前岗位暂无可用于匹配的投递简历。</p>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>匹配结果</h2>
                <p>{{ selectedJob ? selectedJob.title : '全部岗位' }} 范围内的历史匹配结果。</p>
              </div>
              <el-button :icon="Refresh" @click="refreshMatches">刷新</el-button>
            </div>

            <div v-loading="loading.matches" class="stack-list">
              <article v-for="match in matches" :key="match.id" class="stack-card match-card">
                <div>
                  <h3>{{ match.score }} 分</h3>
                  <p>{{ match.analysis }}</p>
                  <dl class="match-explain">
                    <div>
                      <dt>匹配优势</dt>
                      <dd>{{ match.strengthSummary || '暂无优势摘要' }}</dd>
                    </div>
                    <div>
                      <dt>匹配缺口</dt>
                      <dd>{{ match.gapSummary || '暂无缺口摘要' }}</dd>
                    </div>
                    <div>
                      <dt>建议动作</dt>
                      <dd>{{ match.actionSuggestions || '暂无建议动作' }}</dd>
                    </div>
                  </dl>
                  <small>简历 {{ match.resumeId }} · 岗位 {{ match.jobId }} · {{ match.modelName }}</small>
                </div>
              </article>
              <el-empty v-if="!loading.matches && matches.length === 0" description="暂无匹配结果" />
            </div>
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="JD 分析" name="jd-analysis">
        <section class="workspace-grid">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>岗位 JD 智能解析</h2>
                <p>基于本人岗位标题、描述和要求生成本地规则分析，不调用外部模型。</p>
              </div>
            </div>

            <el-form class="action-form" label-position="top">
              <el-form-item label="岗位">
                <el-select v-model="jdAnalysisJobId" placeholder="请选择岗位">
                  <el-option
                    v-for="job in jobs"
                    :key="job.id"
                    :label="`${job.title} / ${statusText(job.status)}`"
                    :value="job.id"
                  />
                </el-select>
              </el-form-item>
              <el-button type="primary" :icon="MagicStick" :loading="loading.jdAnalysis" @click="analyzeJd">
                生成 JD 分析
              </el-button>
              <p class="form-hint">分析结果只用于优化岗位描述，不会改写或发布岗位。</p>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>分析结果</h2>
                <p>{{ jdAnalysis ? jdAnalysis.modelName : '选择岗位后生成本地规则建议' }}</p>
              </div>
            </div>

            <article v-if="jdAnalysis" class="stack-card match-card">
              <div>
                <h3>岗位 {{ jdAnalysis.jobId }}</h3>
                <p>{{ jdAnalysis.summary }}</p>
                <dl class="match-explain">
                  <div>
                    <dt>关键技能</dt>
                    <dd>{{ jdAnalysis.keySkills.join('、') || '暂无识别结果' }}</dd>
                  </div>
                  <div>
                    <dt>岗位亮点</dt>
                    <dd>{{ jdAnalysis.highlights.join('；') }}</dd>
                  </div>
                  <div>
                    <dt>信息缺口</dt>
                    <dd>{{ jdAnalysis.gaps.join('；') }}</dd>
                  </div>
                  <div>
                    <dt>优化建议</dt>
                    <dd>{{ jdAnalysis.suggestions.join('；') }}</dd>
                  </div>
                </dl>
              </div>
            </article>
            <el-empty v-else description="暂无 JD 分析结果" />
          </aside>
        </section>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>
