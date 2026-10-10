<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  Briefcase,
  ChatDotRound,
  ChatLineSquare,
  CircleCheck,
  CircleClose,
  DataAnalysis,
  Document,
  EditPen,
  Guide,
  InfoFilled,
  List,
  MagicStick,
  Memo,
  QuestionFilled,
  Refresh,
  Star,
  SwitchButton,
  User,
  Warning,
} from '@element-plus/icons-vue';
import { extractApiError } from '../api/http';
import {
  analyzeHrJobJd,
  createHrJob,
  createHrMatch,
  generateHrCommunicationDraft,
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
import RecruitmentAssistantPanel from '../components/RecruitmentAssistantPanel.vue';
import WorkspaceNav from '../components/WorkspaceNav.vue';
import MatchResultCard from '../components/MatchResultCard.vue';

const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref('jobs');
const navGroups = [
  { title: '岗位与投递', items: [
    { name: 'jobs', label: '岗位管理', icon: Briefcase },
    { name: 'applications', label: '投递管理', icon: List },
    { name: 'recommendations', label: '候选人推荐', icon: Star },
    { name: 'matches', label: 'AI 匹配', icon: DataAnalysis },
  ] },
  { title: '辅助建议', items: [
    { name: 'follow-up', label: '跟进建议', icon: Guide },
    { name: 'communication-draft', label: '沟通草稿', icon: ChatLineSquare },
    { name: 'interview-kit', label: 'AI 面试题', icon: QuestionFilled },
    { name: 'jd-analysis', label: 'JD 分析', icon: Memo },
  ] },
  { title: '助手', items: [
    { name: 'assistant', label: '招聘助手', icon: ChatDotRound },
  ] },
];
const reviewingApplicationId = ref(null);
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
  communicationDraft: false,
});

const jobs = ref([]);
const applications = ref([]);
const matches = ref([]);
const recommendations = ref([]);
const interviewKit = ref(null);
const followUpAdvice = ref(null);
const communicationDraft = ref(null);
const selectedJobId = ref('');
const jdAnalysisJobId = ref('');
const selectedInterviewApplicationId = ref('');
const selectedFollowUpApplicationId = ref('');
const selectedCommunicationApplicationId = ref('');
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
const communicationApplicationOptions = computed(() => filteredApplications.value);

function emptyJobForm() {
  return {
    title: '',
    companyName: '',
    city: '',
    employmentType: 'FULL_TIME',
    salaryMin: null,
    salaryMax: null,
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
    salaryMin: form.salaryMin == null || form.salaryMin === '' ? null : Number(form.salaryMin),
    salaryMax: form.salaryMax == null || form.salaryMax === '' ? null : Number(form.salaryMax),
    description: form.description,
    requirements: form.requirements,
  };
}

function jobLabel(jobId) {
  const job = jobs.value.find((item) => item.id === jobId);
  return job ? job.title : `岗位 #${jobId}`;
}

function applicationLabel(application) {
  return `${jobLabel(application.jobId)} / 投递 #${application.id} / 学生 #${application.studentId} / ${statusText(application.status)}`;
}

function sourceKind(modelName) {
  return typeof modelName === 'string' && modelName.startsWith('local-') ? '本地规则建议' : '模型辅助建议';
}

function hasSalary(job) {
  return Boolean(job?.salaryMin || job?.salaryMax);
}

function employmentTypeText(value) {
  return { FULL_TIME: '全职', PART_TIME: '兼职', INTERNSHIP: '实习' }[value] || value;
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
  selectedCommunicationApplicationId.value = '';
  interviewKit.value = null;
  followUpAdvice.value = null;
  communicationDraft.value = null;
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
    salaryMin: job.salaryMin ?? null,
    salaryMax: job.salaryMax ?? null,
    description: job.description || '',
    requirements: job.requirements || '',
  });
  activeTab.value = 'jobs';
  nextTick(() => document.getElementById('job-editor')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
}

function resetJobForm() {
  editingJobId.value = null;
  Object.assign(jobForm, emptyJobForm());
  jobFormRef.value?.clearValidate();
}

async function saveJob() {
  if (!await jobFormRef.value.validate().catch(() => false)) return;
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
  if (reviewingApplicationId.value || application.status === status) return;
  reviewingApplicationId.value = application.id;
  try {
    await ElMessageBox.confirm(
      `将投递 ${application.id} 从「${statusText(application.status)}」改为「${statusText(status)}」。请核对简历、岗位要求和规则分后确认此人工决定。`,
      '人工复核投递状态',
      { type: 'warning', confirmButtonText: '确认更新', cancelButtonText: '取消', distinguishCancelAndClose: true }
    );
    await updateHrApplicationStatus(application.id, status);
    ElMessage.success('投递状态已更新');
    await refreshApplications();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(extractApiError(error));
  } finally {
    reviewingApplicationId.value = null;
  }
}

function reviewAssistantApplication(application) {
  selectedJobId.value = application.jobId;
  activeTab.value = 'applications';
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

async function generateCommunicationDraft() {
  if (!selectedCommunicationApplicationId.value) {
    ElMessage.warning('请选择需要生成沟通话术的投递');
    return;
  }

  loading.communicationDraft = true;
  try {
    communicationDraft.value = await generateHrCommunicationDraft(selectedCommunicationApplicationId.value);
    ElMessage.success('AI 沟通话术已生成');
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.communicationDraft = false;
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
        <h1>招聘工作台</h1>
        <p>管理岗位与候选人；所有招聘状态变化都需要你人工确认。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="refreshAll">刷新数据</el-button>
        <el-button :icon="SwitchButton" @click="logout">退出登录</el-button>
      </div>
    </header>

    <section class="stat-strip" aria-label="HR 端数据概览">
      <article v-for="stat in dashboardStats" :key="stat.label" class="stat-item">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </article>
    </section>

    <WorkspaceNav v-model="activeTab" :groups="navGroups" label="HR 工作台功能" />

    <el-tabs v-model="activeTab" class="student-tabs">
      <el-tab-pane label="招聘助手" name="assistant" lazy>
        <RecruitmentAssistantPanel role="hr" :jobs="jobs" :applications="applications" @review="reviewAssistantApplication" />
      </el-tab-pane>

      <el-tab-pane label="岗位管理" name="jobs">
        <section class="workspace-grid narrow-main">
          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>我的岗位</h2>
                <p>发布后学生端可见并可投递；关闭后停止投递入口。</p>
              </div>
              <el-button :icon="Document" @click="resetJobForm">新建草稿</el-button>
            </div>

            <div v-loading="loading.jobs" class="stack-list">
              <article v-for="job in jobs" :key="job.id" class="stack-card" :class="{ 'is-editing': editingJobId === job.id }">
                <div>
                  <h3>{{ job.title }}</h3>
                  <p>{{ job.companyName }} · {{ job.city }}</p>
                  <div class="chip-row">
                    <span class="chip accent">{{ employmentTypeText(job.employmentType) }}</span>
                    <span class="chip">{{ salaryText(job) }}</span>
                  </div>
                </div>
                <el-tag :type="statusType(job.status)">{{ statusText(job.status) }}</el-tag>
                <div class="card-actions">
                  <el-button size="small" :icon="EditPen" @click="editJob(job)">编辑</el-button>
                  <el-button size="small" :icon="List" @click="selectJob(job.id)">查看投递</el-button>
                  <span class="spacer" />
                  <span class="table-secondary">状态：</span>
                  <el-button
                    v-if="job.status !== 'PUBLISHED'"
                    size="small"
                    type="primary"
                    plain
                    :icon="CircleCheck"
                    @click="changeJobStatus(job, 'PUBLISHED')"
                  >
                    发布
                  </el-button>
                  <el-button v-if="job.status !== 'DRAFT'" size="small" @click="changeJobStatus(job, 'DRAFT')">
                    设为草稿
                  </el-button>
                  <el-button v-if="job.status !== 'CLOSED'" size="small" type="danger" plain :icon="CircleClose" @click="changeJobStatus(job, 'CLOSED')">
                    关闭
                  </el-button>
                </div>
              </article>
              <el-empty v-if="!loading.jobs && jobs.length === 0" description="还没有岗位，先在右侧创建草稿" />
            </div>
          </aside>

          <div id="job-editor" class="work-panel">
            <div class="section-heading">
              <div>
                <h2>{{ editingJobId ? '编辑岗位' : '新建岗位草稿' }}</h2>
                <p>{{ editingJobId ? `正在编辑「${jobForm.title || jobLabel(editingJobId)}」，保存不会改变发布状态。` : '保存后为草稿，可在左侧列表中发布。带 * 为必填项。' }}</p>
              </div>
            </div>

            <el-form
              ref="jobFormRef"
              :model="jobForm"
              :rules="jobRules"
              class="resume-form"
              label-position="top"
              @submit.prevent="saveJob"
            >
              <p class="form-section-title">基本信息</p>
              <div class="form-grid">
                <el-form-item label="岗位标题" prop="title">
                  <el-input v-model.trim="jobForm.title" maxlength="100" placeholder="例如：Java 后端工程师" />
                </el-form-item>
                <el-form-item label="公司名称" prop="companyName">
                  <el-input v-model.trim="jobForm.companyName" maxlength="100" />
                </el-form-item>
                <el-form-item label="城市" prop="city">
                  <el-input v-model.trim="jobForm.city" maxlength="50" />
                </el-form-item>
                <el-form-item label="用工类型" prop="employmentType">
                  <el-select v-model="jobForm.employmentType">
                    <el-option label="全职" value="FULL_TIME" />
                    <el-option label="兼职" value="PART_TIME" />
                    <el-option label="实习" value="INTERNSHIP" />
                  </el-select>
                </el-form-item>
                <el-form-item label="最低薪资">
                  <el-input-number v-model="jobForm.salaryMin" :min="0" controls-position="right" placeholder="不填为面议" />
                </el-form-item>
                <el-form-item label="最高薪资">
                  <el-input-number v-model="jobForm.salaryMax" :min="0" controls-position="right" placeholder="不填为面议" />
                </el-form-item>
              </div>
              <p class="form-section-title">岗位内容</p>
              <el-form-item label="岗位描述" prop="description">
                <el-input v-model.trim="jobForm.description" type="textarea" :autosize="{ minRows: 4, maxRows: 10 }" />
              </el-form-item>
              <el-form-item label="岗位要求" prop="requirements">
                <el-input v-model.trim="jobForm.requirements" type="textarea" :autosize="{ minRows: 4, maxRows: 10 }" placeholder="列出技能要求，规则匹配会识别其中的技能关键词" />
              </el-form-item>
              <div class="panel-actions">
                <el-button type="primary" native-type="submit" :loading="loading.saveJob">
                  {{ editingJobId ? '保存修改' : '创建草稿' }}
                </el-button>
                <el-button v-if="editingJobId" :disabled="loading.saveJob" @click="resetJobForm">取消编辑</el-button>
              </div>
            </el-form>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="投递管理" name="applications">
        <section class="work-panel">
          <div class="section-heading">
            <div>
              <h2>收到的投递</h2>
              <p>只展示你本人岗位收到的投递，可按岗位筛选。</p>
            </div>
            <div class="header-actions">
              <el-select v-model="selectedJobId" clearable placeholder="全部岗位" aria-label="岗位范围" @change="selectJob">
                <el-option v-for="job in jobs" :key="job.id" :label="job.title" :value="job.id" />
              </el-select>
              <el-button :icon="Refresh" @click="refreshApplications">刷新投递</el-button>
            </div>
          </div>
          <p class="notice warning" role="note">
            <el-icon aria-hidden="true"><User /></el-icon>
            <span><strong>人工决定：</strong>在“处理”列选择新状态后会弹出确认框，请核对简历、岗位要求和规则分后再确认；取消不会提交任何变更。</span>
          </p>

          <div class="table-scroll">
            <el-table v-loading="loading.applications" :data="filteredApplications" class="data-table" empty-text="暂无投递">
              <el-table-column label="岗位 / 投递" min-width="200">
                <template #default="{ row }">
                  <span class="table-primary">{{ jobLabel(row.jobId) }}</span>
                  <span class="table-secondary">投递 #{{ row.id }} · 当前：{{ statusText(row.status) }}</span>
                </template>
              </el-table-column>
              <el-table-column label="候选人" min-width="130">
                <template #default="{ row }">
                  <span class="table-primary">学生 #{{ row.studentId }}</span>
                  <span class="table-secondary">简历 #{{ row.resumeId }}</span>
                </template>
              </el-table-column>
              <el-table-column label="当前状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="投递时间" min-width="170">
                <template #default="{ row }">{{ formatDate(row.appliedAt) }}</template>
              </el-table-column>
              <el-table-column label="处理（需确认）" width="160" fixed="right">
                <template #default="{ row }">
                  <el-select
                    class="status-select"
                    :model-value="null"
                    :disabled="row.status === 'WITHDRAWN' || reviewingApplicationId !== null"
                    :placeholder="row.status === 'WITHDRAWN' ? '已撤回，不可变更' : '变更为…'"
                    size="small"
                    :aria-label="`变更投递 ${row.id} 的状态，当前为${statusText(row.status)}`"
                    @change="(status) => changeApplicationStatus(row, status)"
                  >
                    <el-option label="筛选中" value="REVIEWING" :disabled="row.status === 'REVIEWING'" />
                    <el-option label="面试" value="INTERVIEW" :disabled="row.status === 'INTERVIEW'" />
                    <el-option label="已录用" value="OFFERED" :disabled="row.status === 'OFFERED'" />
                    <el-option label="未通过" value="REJECTED" :disabled="row.status === 'REJECTED'" />
                  </el-select>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="候选人推荐" name="recommendations">
        <section class="work-panel">
          <div class="section-heading">
            <div>
              <h2>候选人推荐排序</h2>
              <p>基于投递简历、岗位要求和已有匹配结果的本地规则排序，不会自动变更投递状态。</p>
            </div>
            <div class="header-actions">
              <el-select v-model="selectedJobId" clearable placeholder="全部岗位" aria-label="岗位范围" @change="selectJob">
                <el-option v-for="job in jobs" :key="job.id" :label="job.title" :value="job.id" />
              </el-select>
              <el-button :icon="Refresh" @click="refreshRecommendations">刷新推荐</el-button>
            </div>
          </div>

          <div v-loading="loading.recommendations" class="recommendation-list">
            <article
              v-for="candidate in recommendations"
              :key="candidate.applicationId"
              class="recommendation-card"
            >
              <div class="score-col">
                <div class="score-badge">
                  <span class="score-label">推荐分 · 规则计算</span>
                  <span class="score-value">{{ candidate.recommendationScore }}</span>
                </div>
                <span class="table-secondary">来源：{{ candidate.scoreSource }}</span>
              </div>
              <div class="main-col">
                <div class="result-head">
                  <div>
                    <h3>学生 #{{ candidate.studentId }} · 简历 #{{ candidate.resumeId }}</h3>
                    <p>{{ jobLabel(candidate.jobId) }} · 投递 #{{ candidate.applicationId }} · {{ formatDate(candidate.appliedAt) }}</p>
                  </div>
                  <el-tag :type="statusType(candidate.applicationStatus)">{{ statusText(candidate.applicationStatus) }}</el-tag>
                </div>
                <div class="result-grid">
                  <div class="result-section positive">
                    <h4><el-icon aria-hidden="true"><CircleCheck /></el-icon>推荐理由</h4>
                    <p>{{ candidate.recommendationReason }}</p>
                  </div>
                  <div class="result-section risk">
                    <h4><el-icon aria-hidden="true"><Warning /></el-icon>风险摘要</h4>
                    <p>{{ candidate.riskSummary }}</p>
                  </div>
                </div>
                <div class="result-section">
                  <h4>关键词</h4>
                  <div class="chip-row">
                    <span class="table-secondary">已匹配</span>
                    <span v-for="item in candidate.matchedKeywords" :key="`m-${item}`" class="chip accent">{{ item }}</span>
                    <span v-if="!candidate.matchedKeywords.length" class="table-secondary">暂无</span>
                  </div>
                  <div class="chip-row">
                    <span class="table-secondary">待核验</span>
                    <span v-for="item in candidate.missingKeywords" :key="`g-${item}`" class="chip">{{ item }}</span>
                    <span v-if="!candidate.missingKeywords.length" class="table-secondary">暂无</span>
                  </div>
                </div>
                <div class="result-section action">
                  <h4><el-icon aria-hidden="true"><Guide /></el-icon>建议动作</h4>
                  <p>{{ candidate.suggestedAction }}</p>
                </div>
                <div class="card-actions">
                  <span class="table-secondary">推荐仅供参考，状态变更请在投递管理中人工确认。</span>
                  <span class="spacer" />
                  <el-button size="small" :icon="List" @click="activeTab = 'applications'">前往投递管理</el-button>
                </div>
              </div>
            </article>
            <el-empty v-if="!loading.recommendations && recommendations.length === 0" description="暂无候选人推荐" />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="跟进建议" name="follow-up">
        <section class="workspace-grid narrow-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>投递跟进建议</h2>
                <p>基于单个投递的状态、岗位要求、简历内容和已有匹配分生成下一步建议。</p>
              </div>
            </div>

            <el-form class="action-form" label-position="top">
              <el-form-item label="投递">
                <el-select v-model="selectedFollowUpApplicationId" placeholder="请选择投递">
                  <el-option
                    v-for="application in followUpApplicationOptions"
                    :key="application.id"
                    :label="applicationLabel(application)"
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
                <p>{{ followUpAdvice ? '请结合简历原文核对后再采取行动。' : '选择投递后生成建议。' }}</p>
              </div>
            </div>

            <article v-if="followUpAdvice" class="result-card">
              <div class="result-head">
                <div>
                  <h3>投递 #{{ followUpAdvice.applicationId }} · 简历 #{{ followUpAdvice.resumeId }}</h3>
                  <p>优先级：{{ followUpAdvice.priorityLevel }}</p>
                </div>
                <span class="kind-flag rule">{{ sourceKind(followUpAdvice.modelName) }}</span>
              </div>
              <div class="score-badge">
                <span class="score-label">规则匹配分 · 只读</span>
                <span class="score-value">{{ followUpAdvice.matchScore ?? '暂无' }}</span>
                <span class="table-secondary">来源：{{ followUpAdvice.scoreSource }}</span>
              </div>
              <p class="result-summary">{{ followUpAdvice.decisionSummary }}</p>
              <div class="notice warning">
                <el-icon aria-hidden="true"><User /></el-icon>
                <span>建议状态：<strong>{{ statusText(followUpAdvice.nextStatusSuggestion) }}</strong>（仅供参考，需在投递管理中人工确认）</span>
              </div>
              <div class="result-section">
                <h4>关键词</h4>
                <div class="chip-row">
                  <span class="table-secondary">已匹配</span>
                  <span v-for="item in followUpAdvice.matchedKeywords" :key="`m-${item}`" class="chip accent">{{ item }}</span>
                  <span v-if="!followUpAdvice.matchedKeywords.length" class="table-secondary">暂无</span>
                </div>
                <div class="chip-row">
                  <span class="table-secondary">待核验</span>
                  <span v-for="item in followUpAdvice.missingKeywords" :key="`g-${item}`" class="chip">{{ item }}</span>
                  <span v-if="!followUpAdvice.missingKeywords.length" class="table-secondary">暂无</span>
                </div>
              </div>
              <div class="result-section risk">
                <h4><el-icon aria-hidden="true"><Warning /></el-icon>风险提醒</h4>
                <ul class="suggestion-list"><li v-for="item in followUpAdvice.riskAlerts" :key="item">{{ item }}</li></ul>
              </div>
              <div class="result-section action">
                <h4><el-icon aria-hidden="true"><Guide /></el-icon>建议动作</h4>
                <ul class="suggestion-list"><li v-for="item in followUpAdvice.recommendedActions" :key="item">{{ item }}</li></ul>
              </div>
              <div class="result-section">
                <h4><el-icon aria-hidden="true"><ChatLineSquare /></el-icon>沟通提示</h4>
                <ul class="suggestion-list"><li v-for="item in followUpAdvice.communicationTips" :key="item">{{ item }}</li></ul>
              </div>
              <div class="source-line"><span>来源</span><code>{{ followUpAdvice.modelName }}</code></div>
            </article>
            <el-empty v-else description="暂无跟进建议" />
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="沟通草稿" name="communication-draft">
        <section class="workspace-grid narrow-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>候选人沟通话术</h2>
                <p>基于单个投递的状态、岗位要求、简历内容和匹配分生成沟通草稿，不会自动修改投递状态。</p>
              </div>
            </div>

            <el-form class="action-form" label-position="top">
              <el-form-item label="投递">
                <el-select v-model="selectedCommunicationApplicationId" placeholder="请选择投递">
                  <el-option
                    v-for="application in communicationApplicationOptions"
                    :key="application.id"
                    :label="applicationLabel(application)"
                    :value="application.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="loading.communicationDraft"
                @click="generateCommunicationDraft"
              >
                生成沟通草稿
              </el-button>
              <p class="form-hint">话术仅作为沟通参考，平台不会代为发送；发送前请结合实际招聘流程人工确认。</p>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>沟通草稿</h2>
                <p>{{ communicationDraft ? `场景：${communicationDraft.communicationScenario}` : '选择投递后生成草稿。' }}</p>
              </div>
            </div>

            <article v-if="communicationDraft" class="result-card">
              <div class="result-head">
                <div>
                  <h3>{{ communicationDraft.subject }}</h3>
                  <p>规则匹配分（只读）：{{ communicationDraft.matchScore ?? '暂无' }} · 来源：{{ communicationDraft.scoreSource }}</p>
                </div>
                <span class="kind-flag rule">{{ sourceKind(communicationDraft.modelName) }}</span>
              </div>
              <div class="result-section action">
                <h4><el-icon aria-hidden="true"><EditPen /></el-icon>开场草稿</h4>
                <p class="result-summary">{{ communicationDraft.openingMessage }}</p>
              </div>
              <div class="result-section">
                <h4><el-icon aria-hidden="true"><QuestionFilled /></el-icon>关键追问</h4>
                <ul class="suggestion-list"><li v-for="item in communicationDraft.keyQuestions" :key="item">{{ item }}</li></ul>
              </div>
              <div class="result-section risk">
                <h4><el-icon aria-hidden="true"><Warning /></el-icon>风险提示</h4>
                <ul class="suggestion-list"><li v-for="item in communicationDraft.riskNotes" :key="item">{{ item }}</li></ul>
              </div>
              <div class="result-section">
                <h4><el-icon aria-hidden="true"><Guide /></el-icon>后续动作</h4>
                <ul class="suggestion-list"><li v-for="item in communicationDraft.nextActions" :key="item">{{ item }}</li></ul>
              </div>
              <div class="source-line"><span>来源</span><code>{{ communicationDraft.modelName }}</code></div>
            </article>
            <el-empty v-else description="暂无沟通草稿" />
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 面试题" name="interview-kit">
        <section class="workspace-grid narrow-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>面试题生成</h2>
                <p>基于当前岗位范围内的单个投递生成面试题和评分维度，不会自动变更投递状态。</p>
              </div>
            </div>

            <el-form class="action-form" label-position="top">
              <el-form-item label="投递">
                <el-select v-model="selectedInterviewApplicationId" placeholder="请选择投递">
                  <el-option
                    v-for="application in interviewApplicationOptions"
                    :key="application.id"
                    :label="applicationLabel(application)"
                    :value="application.id"
                  />
                </el-select>
              </el-form-item>
              <el-button type="primary" :icon="MagicStick" :loading="loading.interviewKit" @click="generateInterviewKit">
                生成 AI 面试题
              </el-button>
              <p class="form-hint">题目用于面试准备和评分参考，不保存面试记录。</p>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>面试题与评分维度</h2>
                <p>{{ interviewKit ? '可按题目顺序准备追问与证据。' : '选择投递后生成面试题。' }}</p>
              </div>
            </div>

            <article v-if="interviewKit" class="result-card">
              <div class="result-head">
                <div>
                  <h3>投递 #{{ interviewKit.applicationId }} · 简历 #{{ interviewKit.resumeId }}</h3>
                </div>
                <span class="kind-flag rule">{{ sourceKind(interviewKit.modelName) }}</span>
              </div>
              <p class="result-summary">{{ interviewKit.summary }}</p>
              <div class="result-section">
                <h4><el-icon aria-hidden="true"><QuestionFilled /></el-icon>面试题</h4>
                <ol class="ordered-list">
                  <li v-for="question in interviewKit.questions" :key="question.category">
                    <strong>{{ question.category }}：</strong>{{ question.question }}
                    <small>考察重点：{{ question.evaluationFocus }}</small>
                    <small>期待证据：{{ question.expectedEvidence }}</small>
                  </li>
                </ol>
              </div>
              <div class="result-section action">
                <h4><el-icon aria-hidden="true"><DataAnalysis /></el-icon>评分维度</h4>
                <ol class="ordered-list">
                  <li v-for="dimension in interviewKit.scoringDimensions" :key="dimension.name">
                    <strong>{{ dimension.name }}（{{ dimension.weight }}%）：</strong>{{ dimension.highScoreSignal }}
                    <small>低分风险：{{ dimension.lowScoreRisk }}</small>
                  </li>
                </ol>
              </div>
              <div class="result-grid">
                <div class="result-section risk">
                  <h4><el-icon aria-hidden="true"><Warning /></el-icon>风险关注</h4>
                  <ul class="suggestion-list"><li v-for="item in interviewKit.riskFocus" :key="item">{{ item }}</li></ul>
                </div>
                <div class="result-section">
                  <h4><el-icon aria-hidden="true"><ChatLineSquare /></el-icon>建议追问</h4>
                  <ul class="suggestion-list"><li v-for="item in interviewKit.followUpSuggestions" :key="item">{{ item }}</li></ul>
                </div>
              </div>
              <div class="source-line"><span>来源</span><code>{{ interviewKit.modelName }}</code></div>
            </article>
            <el-empty v-else description="暂无面试题结果" />
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 匹配" name="matches">
        <section class="workspace-grid narrow-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>生成岗位匹配</h2>
                <p>只能为已投递到你本人岗位的简历生成匹配结果。</p>
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
            <div class="notice info" role="note">
              <el-icon aria-hidden="true"><InfoFilled /></el-icon>
              <span><strong>评分口径：</strong>技能覆盖率 = 简历文本提及的岗位技能数 ÷ 岗位要求中词典可识别的技能数；只衡量文本提及，不代表熟练度或录用概率。</span>
            </div>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>匹配结果</h2>
                <p>{{ selectedJob ? selectedJob.title : '全部岗位' }} 范围内的历史结果；最终决定由人工复核。</p>
              </div>
              <el-button :icon="Refresh" @click="refreshMatches">刷新</el-button>
            </div>

            <div v-loading="loading.matches" class="stack-list">
              <MatchResultCard
                v-for="match in matches"
                :key="match.id"
                :match="match"
                :job-label="jobLabel(match.jobId)"
              />
              <el-empty v-if="!loading.matches && matches.length === 0" description="暂无匹配结果" />
            </div>
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="JD 分析" name="jd-analysis">
        <section class="workspace-grid narrow-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>岗位 JD 解析</h2>
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
                <p>{{ jdAnalysis ? '可据此在“岗位管理”中手动修改岗位描述。' : '选择岗位后生成分析。' }}</p>
              </div>
            </div>

            <article v-if="jdAnalysis" class="result-card">
              <div class="result-head">
                <div>
                  <h3>{{ jobLabel(jdAnalysis.jobId) }}</h3>
                </div>
                <span class="kind-flag rule">{{ sourceKind(jdAnalysis.modelName) }}</span>
              </div>
              <p class="result-summary">{{ jdAnalysis.summary }}</p>
              <div class="result-section">
                <h4><el-icon aria-hidden="true"><DataAnalysis /></el-icon>识别到的关键技能</h4>
                <div v-if="jdAnalysis.keySkills.length" class="chip-row">
                  <span v-for="item in jdAnalysis.keySkills" :key="item" class="chip accent">{{ item }}</span>
                </div>
                <p v-else class="form-hint">暂无识别结果</p>
              </div>
              <div class="result-grid">
                <div class="result-section positive">
                  <h4><el-icon aria-hidden="true"><CircleCheck /></el-icon>岗位亮点</h4>
                  <ul class="suggestion-list"><li v-for="item in jdAnalysis.highlights" :key="item">{{ item }}</li></ul>
                </div>
                <div class="result-section gap">
                  <h4><el-icon aria-hidden="true"><Warning /></el-icon>信息缺口</h4>
                  <ul class="suggestion-list"><li v-for="item in jdAnalysis.gaps" :key="item">{{ item }}</li></ul>
                </div>
              </div>
              <div class="result-section action">
                <h4><el-icon aria-hidden="true"><EditPen /></el-icon>优化建议</h4>
                <ul class="suggestion-list"><li v-for="item in jdAnalysis.suggestions" :key="item">{{ item }}</li></ul>
              </div>
              <div class="source-line"><span>来源</span><code>{{ jdAnalysis.modelName }}</code></div>
            </article>
            <el-empty v-else description="暂无 JD 分析结果" />
          </aside>
        </section>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>
