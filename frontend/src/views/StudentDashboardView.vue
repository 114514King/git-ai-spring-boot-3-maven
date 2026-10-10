<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  ArrowLeft,
  Briefcase,
  ChatDotRound,
  CircleCheck,
  Collection,
  DataAnalysis,
  Document,
  EditPen,
  Guide,
  InfoFilled,
  List,
  Location,
  MagicStick,
  Promotion,
  Refresh,
  Search,
  SwitchButton,
  Warning,
} from '@element-plus/icons-vue';
import { extractApiError } from '../api/http';
import {
  createMatch,
  createResume,
  generateApplicationActionPlan,
  getJob,
  listApplications,
  listJobs,
  listMatches,
  listResumes,
  optimizeResume,
  submitApplication,
  updateResume,
  updateResumeStatus,
  withdrawApplication,
} from '../api/student';
import { useAuthStore } from '../stores/auth';
import ResumeImportPanel from '../components/ResumeImportPanel.vue';
import ExternalJobsPanel from '../components/ExternalJobsPanel.vue';
import RecruitmentAssistantPanel from '../components/RecruitmentAssistantPanel.vue';
import WorkspaceNav from '../components/WorkspaceNav.vue';
import MatchResultCard from '../components/MatchResultCard.vue';

const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref('jobs');
const detailPanel = ref();
const navGroups = [
  { title: '找岗位', items: [
    { name: 'jobs', label: '岗位浏览', icon: Briefcase },
    { name: 'externalJobs', label: '外部岗位', icon: Promotion },
  ] },
  { title: '我的求职', items: [
    { name: 'resumes', label: '我的简历', icon: Document },
    { name: 'applications', label: '投递记录', icon: List },
  ] },
  { title: '匹配与建议', items: [
    { name: 'matches', label: 'AI 匹配', icon: DataAnalysis },
    { name: 'resumeOptimization', label: '简历优化', icon: EditPen },
    { name: 'actionPlan', label: '行动计划', icon: Guide },
  ] },
  { title: '助手', items: [
    { name: 'assistant', label: '求职助手', icon: ChatDotRound },
  ] },
];
const loading = reactive({
  jobs: false,
  resumes: false,
  applications: false,
  matches: false,
  saveResume: false,
  submitApplication: false,
  createMatch: false,
  resumeOptimization: false,
  applicationActionPlan: false,
});

const jobFilters = reactive({
  keyword: '',
  city: '',
  employmentType: '',
});
const jobs = ref([]);
const selectedJob = ref(null);
const selectedJobId = ref(null);

const resumes = ref([]);
const applications = ref([]);
const matches = ref([]);

const resumeFormRef = ref();
const resumeForm = reactive(emptyResumeForm());
const editingResumeId = ref(null);

const applicationForm = reactive({
  jobId: '',
  resumeId: '',
});

const matchForm = reactive({
  jobId: '',
  resumeId: '',
});

const optimizationForm = reactive({
  jobId: '',
  resumeId: '',
});
const resumeOptimization = ref(null);
const actionPlanForm = reactive({
  applicationId: '',
});
const applicationActionPlan = ref(null);

const resumeRules = {
  title: [
    { required: true, message: '请输入简历标题', trigger: 'blur' },
    { max: 100, message: '标题最长 100 个字符', trigger: 'blur' },
  ],
};

const publishedResumes = computed(() => resumes.value.filter((resume) => resume.status === 'PUBLISHED'));
const hasPublishedResume = computed(() => publishedResumes.value.length > 0);
const selectedJobApplications = computed(() => {
  if (!selectedJobId.value) return applications.value;
  return applications.value.filter((application) => application.jobId === selectedJobId.value);
});
const showAllApplications = ref(false);
const visibleApplications = computed(() => (showAllApplications.value ? applications.value : selectedJobApplications.value));

const dashboardStats = computed(() => [
  { label: '公开岗位', value: jobs.value.length, hint: '当前筛选下的已发布岗位' },
  { label: '我的简历', value: resumes.value.length, hint: `已发布 ${publishedResumes.value.length} 份，可用于投递` },
  { label: '投递记录', value: applications.value.length, hint: '查看求职进展与反馈' },
  { label: 'AI 匹配', value: matches.value.length, hint: '已保存的技能覆盖结果' },
]);
const editingResume = computed(() => resumes.value.find((resume) => resume.id === editingResumeId.value));

function jobLabel(jobId) {
  const job = jobs.value.find((item) => item.id === jobId);
  return job ? `${job.title} · ${job.companyName}` : `岗位 #${jobId}`;
}

function resumeLabel(resumeId) {
  const resume = resumes.value.find((item) => item.id === resumeId);
  return resume ? resume.title : `简历 #${resumeId}`;
}

function emptyResumeForm() {
  return {
    title: '',
    education: '',
    workExperience: '',
    projectExperience: '',
    skills: '',
    selfEvaluation: '',
    sourceText: '',
  };
}

function normalizeParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== ''));
}

function formatDate(value) {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

function hasSalary(job) {
  return Boolean(job?.salaryMin || job?.salaryMax);
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
  if (['REJECTED', 'WITHDRAWN'].includes(status)) return 'danger';
  if (['REVIEWING', 'INTERVIEW', 'SUBMITTED'].includes(status)) return 'warning';
  return 'info';
}

async function refreshJobs() {
  loading.jobs = true;
  try {
    jobs.value = await listJobs(normalizeParams(jobFilters));
    if (!selectedJobId.value && jobs.value.length > 0) {
      await selectJob(jobs.value[0].id);
    } else if (selectedJobId.value && !jobs.value.some((job) => job.id === selectedJobId.value)) {
      selectedJobId.value = null;
      selectedJob.value = null;
    }
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.jobs = false;
  }
}

async function selectJob(id, reveal = false) {
  selectedJobId.value = id;
  try {
    selectedJob.value = await getJob(id);
    applicationForm.jobId = id;
    matchForm.jobId = id;
    optimizationForm.jobId = id;
    if (reveal && window.matchMedia('(max-width: 960px)').matches) {
      await nextTick();
      detailPanel.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  } catch (error) {
    ElMessage.error(extractApiError(error));
  }
}

function backToJobList() {
  document.getElementById(`job-row-${selectedJobId.value}`)?.focus();
  document.getElementById(`job-row-${selectedJobId.value}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function employmentTypeText(value) {
  return { FULL_TIME: '全职', PART_TIME: '兼职', INTERNSHIP: '实习' }[value] || value;
}

async function refreshResumes() {
  loading.resumes = true;
  try {
    resumes.value = await listResumes();
    if (!applicationForm.resumeId && publishedResumes.value.length > 0) {
      applicationForm.resumeId = publishedResumes.value[0].id;
      matchForm.resumeId = publishedResumes.value[0].id;
      optimizationForm.resumeId = publishedResumes.value[0].id;
    }
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.resumes = false;
  }
}

async function refreshApplications() {
  loading.applications = true;
  try {
    applications.value = await listApplications();
    if (!actionPlanForm.applicationId && applications.value.length > 0) {
      actionPlanForm.applicationId = applications.value[0].id;
    }
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.applications = false;
  }
}

async function refreshMatches() {
  loading.matches = true;
  try {
    matches.value = await listMatches({});
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.matches = false;
  }
}

async function refreshAll() {
  await Promise.all([refreshJobs(), refreshResumes(), refreshApplications(), refreshMatches()]);
}

function editResume(resume) {
  editingResumeId.value = resume.id;
  Object.assign(resumeForm, {
    title: resume.title || '',
    education: resume.education || '',
    workExperience: resume.workExperience || '',
    projectExperience: resume.projectExperience || '',
    skills: resume.skills || '',
    selfEvaluation: resume.selfEvaluation || '',
    sourceText: resume.sourceText || '',
  });
  activeTab.value = 'resumes';
  nextTick(() => document.getElementById('resume-editor')?.scrollIntoView({ behavior: 'smooth', block: 'start' }));
}

function resetResumeForm() {
  editingResumeId.value = null;
  Object.assign(resumeForm, emptyResumeForm());
  resumeFormRef.value?.clearValidate();
}

async function saveResume() {
  if (!await resumeFormRef.value.validate().catch(() => false)) return;
  loading.saveResume = true;
  try {
    if (editingResumeId.value) {
      await updateResume(editingResumeId.value, resumeForm);
      ElMessage.success('简历已更新');
    } else {
      await createResume(resumeForm);
      ElMessage.success('简历草稿已创建');
    }
    resetResumeForm();
    await refreshResumes();
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.saveResume = false;
  }
}

async function changeResumeStatus(resume, status) {
  try {
    await updateResumeStatus(resume.id, status);
    ElMessage.success(status === 'PUBLISHED' ? '简历已发布' : '简历已设为草稿');
    await refreshResumes();
  } catch (error) {
    ElMessage.error(extractApiError(error));
  }
}

async function applyToJob() {
  if (!applicationForm.jobId || !applicationForm.resumeId) {
    ElMessage.warning('请选择岗位和已发布简历');
    return;
  }

  loading.submitApplication = true;
  try {
    await submitApplication({
      jobId: Number(applicationForm.jobId),
      resumeId: Number(applicationForm.resumeId),
    });
    ElMessage.success('投递成功');
    await refreshApplications();
    activeTab.value = 'applications';
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.submitApplication = false;
  }
}

async function withdraw(id) {
  try {
    await ElMessageBox.confirm('确认撤回这条投递吗？', '撤回投递', { type: 'warning' });
    await withdrawApplication(id);
    ElMessage.success('投递已撤回');
    await refreshApplications();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(extractApiError(error));
    }
  }
}

async function generateMatch() {
  if (!matchForm.jobId || !matchForm.resumeId) {
    ElMessage.warning('请选择岗位和已发布简历');
    return;
  }

  loading.createMatch = true;
  try {
    await createMatch({
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

async function generateResumeOptimization() {
  if (!optimizationForm.jobId || !optimizationForm.resumeId) {
    ElMessage.warning('请选择目标岗位和简历');
    return;
  }

  loading.resumeOptimization = true;
  try {
    resumeOptimization.value = await optimizeResume(optimizationForm.resumeId, {
      jobId: Number(optimizationForm.jobId),
    });
    ElMessage.success('简历优化建议已生成');
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.resumeOptimization = false;
  }
}

async function generateActionPlan() {
  if (!actionPlanForm.applicationId) {
    ElMessage.warning('请选择一条投递记录');
    return;
  }

  loading.applicationActionPlan = true;
  try {
    applicationActionPlan.value = await generateApplicationActionPlan(actionPlanForm.applicationId);
    ElMessage.success('求职行动计划已生成');
  } catch (error) {
    ElMessage.error(extractApiError(error));
  } finally {
    loading.applicationActionPlan = false;
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
        <h1>找到适合你的下一站</h1>
        <p>浏览岗位、完善简历，让每一次投递更有方向。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="refreshAll">刷新数据</el-button>
        <el-button :icon="SwitchButton" @click="logout">退出登录</el-button>
      </div>
    </header>

    <section class="stat-strip" aria-label="学生端数据概览">
      <article v-for="stat in dashboardStats" :key="stat.label" class="stat-item">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
        <small>{{ stat.hint }}</small>
      </article>
    </section>

    <WorkspaceNav v-model="activeTab" :groups="navGroups" label="学生工作台功能" />

    <el-tabs v-model="activeTab" class="student-tabs">
      <el-tab-pane label="求职助手" name="assistant" lazy>
        <RecruitmentAssistantPanel role="student" :jobs="jobs" :resumes="resumes" :applications="applications" />
      </el-tab-pane>

      <el-tab-pane label="岗位浏览" name="jobs">
        <section class="workspace-grid wide-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>公开岗位</h2>
                <p>按关键词、城市和用工类型筛选，选择岗位查看详情并投递。</p>
              </div>
            </div>

            <el-form class="filter-grid" :model="jobFilters" label-position="top" @submit.prevent="refreshJobs">
              <el-form-item label="关键词"><el-input v-model.trim="jobFilters.keyword" :prefix-icon="Search" clearable placeholder="岗位或公司关键词" /></el-form-item>
              <el-form-item label="城市"><el-input v-model.trim="jobFilters.city" clearable placeholder="例如：上海" /></el-form-item>
              <el-form-item label="用工类型"><el-select v-model="jobFilters.employmentType" clearable placeholder="全部类型">
                <el-option label="全职" value="FULL_TIME" />
                <el-option label="兼职" value="PART_TIME" />
                <el-option label="实习" value="INTERNSHIP" />
              </el-select></el-form-item>
              <el-button type="primary" native-type="submit" :icon="Search" :loading="loading.jobs">筛选</el-button>
            </el-form>

            <div class="list-meta" role="status">
              <span>{{ loading.jobs ? '正在加载岗位…' : `共 ${jobs.length} 个岗位` }}</span>
            </div>
            <div v-loading="loading.jobs" class="job-list">
              <button
                v-for="job in jobs"
                :id="`job-row-${job.id}`"
                :key="job.id"
                class="job-row"
                :class="{ active: selectedJobId === job.id }"
                :aria-pressed="selectedJobId === job.id"
                type="button"
                @click="selectJob(job.id, true)"
              >
                <span class="job-row-main">
                  <strong>{{ job.title }}</strong>
                  <small>{{ job.companyName }}</small>
                  <span class="chip-row">
                    <span class="chip accent">{{ employmentTypeText(job.employmentType) }}</span>
                    <span class="chip"><el-icon aria-hidden="true"><Location /></el-icon>{{ job.city }}</span>
                  </span>
                </span>
                <span class="job-salary" :class="{ muted: !hasSalary(job) }">{{ salaryText(job) }}</span>
              </button>
              <el-empty v-if="!loading.jobs && jobs.length === 0" description="没有符合条件的公开岗位，试试调整筛选条件" />
            </div>
          </div>

          <aside ref="detailPanel" class="work-panel detail-panel" aria-label="岗位详情">
            <template v-if="selectedJob">
              <el-button class="mobile-back" text :icon="ArrowLeft" @click="backToJobList">返回岗位列表</el-button>
              <div class="job-detail-head">
                <h2>{{ selectedJob.title }}</h2>
                <span class="company">{{ selectedJob.companyName }}</span>
                <span class="job-detail-salary">{{ salaryText(selectedJob) }}</span>
                <div class="job-detail-meta">
                  <el-tag>{{ employmentTypeText(selectedJob.employmentType) }}</el-tag>
                  <span class="meta-line"><span><el-icon aria-hidden="true"><Location /></el-icon>{{ selectedJob.city }}</span></span>
                  <span>发布于 {{ formatDate(selectedJob.publishedAt) }}</span>
                </div>
              </div>

              <el-form class="action-form apply-box" :model="applicationForm" label-position="top">
                <h3>投递或查看匹配</h3>
                <el-form-item label="选择已发布简历">
                  <el-select v-model="applicationForm.resumeId" :disabled="!hasPublishedResume" placeholder="请选择简历">
                    <el-option
                      v-for="resume in publishedResumes"
                      :key="resume.id"
                      :label="resume.title"
                      :value="resume.id"
                    />
                  </el-select>
                </el-form-item>
                <div class="button-row">
                  <el-button type="primary" :icon="Briefcase" :loading="loading.submitApplication" @click="applyToJob">
                    投递岗位
                  </el-button>
                  <el-button :icon="MagicStick" :loading="loading.createMatch" @click="generateMatch">
                    生成匹配
                  </el-button>
                </div>
                <div v-if="!hasPublishedResume" class="notice warning" role="note">
                  <el-icon aria-hidden="true"><Warning /></el-icon>
                  <span>请先发布至少一份简历，才能投递或生成匹配。<el-button link type="primary" @click="activeTab = 'resumes'">去发布简历</el-button></span>
                </div>
              </el-form>

              <section class="detail-copy">
                <h3>岗位描述</h3>
                <p>{{ selectedJob.description }}</p>
                <h3>岗位要求</h3>
                <p>{{ selectedJob.requirements }}</p>
              </section>
            </template>
            <el-empty v-else description="请选择岗位查看详情" />
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="外部岗位" name="externalJobs" lazy>
        <ExternalJobsPanel />
      </el-tab-pane>

      <el-tab-pane label="我的简历" name="resumes">
        <section class="workspace-grid narrow-main">
          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>简历列表</h2>
                <p>只有已发布简历可用于投递和 AI 匹配。</p>
              </div>
              <el-button :icon="Document" @click="resetResumeForm">新建草稿</el-button>
            </div>

            <div v-loading="loading.resumes" class="stack-list">
              <article v-for="resume in resumes" :key="resume.id" class="stack-card" :class="{ 'is-editing': editingResumeId === resume.id }">
                <div>
                  <h3>{{ resume.title }}</h3>
                  <p>{{ resume.skills || '未填写技能关键词' }}</p>
                </div>
                <el-tag :type="statusType(resume.status)">{{ statusText(resume.status) }}</el-tag>
                <div class="card-actions">
                  <el-button size="small" :icon="EditPen" @click="editResume(resume)">编辑</el-button>
                  <span class="spacer" />
                  <el-button
                    v-if="resume.status === 'DRAFT'"
                    size="small"
                    type="primary"
                    plain
                    :icon="CircleCheck"
                    @click="changeResumeStatus(resume, 'PUBLISHED')"
                  >
                    发布
                  </el-button>
                  <el-button v-else size="small" @click="changeResumeStatus(resume, 'DRAFT')">设为草稿</el-button>
                </div>
              </article>
              <el-empty v-if="!loading.resumes && resumes.length === 0" description="还没有简历，可以从文件导入或手动新建" />
            </div>
          </aside>

          <div class="resume-main">
            <ResumeImportPanel @imported="refreshResumes" />

            <div id="resume-editor" class="work-panel">
              <div class="section-heading">
                <div>
                  <h2>{{ editingResumeId ? '编辑简历' : '新建简历草稿' }}</h2>
                  <p>{{ editingResumeId ? `正在编辑「${editingResume?.title || resumeForm.title}」，保存后发布状态不变。` : '手动填写经历，保存后为草稿，发布后才可投递。' }}</p>
                </div>
              </div>

              <el-form
                ref="resumeFormRef"
                :model="resumeForm"
                :rules="resumeRules"
                class="resume-form"
                label-position="top"
                @submit.prevent="saveResume"
              >
                <el-form-item label="简历标题" prop="title">
                  <el-input v-model.trim="resumeForm.title" maxlength="100" placeholder="例如：Java 后端实习简历" />
                </el-form-item>
                <p class="form-section-title">经历</p>
                <el-form-item label="教育经历">
                  <el-input v-model.trim="resumeForm.education" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" placeholder="学校、专业、时间" />
                </el-form-item>
                <el-form-item label="工作经历">
                  <el-input v-model.trim="resumeForm.workExperience" type="textarea" :autosize="{ minRows: 3, maxRows: 8 }" />
                </el-form-item>
                <el-form-item label="项目经历">
                  <el-input v-model.trim="resumeForm.projectExperience" type="textarea" :autosize="{ minRows: 3, maxRows: 8 }" />
                </el-form-item>
                <p class="form-section-title">技能与评价</p>
                <el-form-item label="技能关键词">
                  <el-input v-model.trim="resumeForm.skills" placeholder="用逗号分隔，例如：Java, Spring Boot, MySQL" />
                </el-form-item>
                <el-form-item label="自我评价">
                  <el-input v-model.trim="resumeForm.selfEvaluation" type="textarea" :autosize="{ minRows: 3, maxRows: 8 }" />
                </el-form-item>
                <el-form-item label="导入简历文本（参与技能匹配）">
                  <el-input v-model="resumeForm.sourceText" type="textarea" :rows="5" maxlength="20000" show-word-limit />
                </el-form-item>
                <div class="panel-actions">
                  <el-button type="primary" native-type="submit" :loading="loading.saveResume">
                    {{ editingResumeId ? '保存修改' : '创建草稿' }}
                  </el-button>
                  <el-button v-if="editingResumeId" :disabled="loading.saveResume" @click="resetResumeForm">取消编辑</el-button>
                </div>
              </el-form>
            </div>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="投递记录" name="applications">
        <section class="work-panel">
          <div class="section-heading">
            <div>
              <h2>我的投递</h2>
              <p>查看投递状态变化；状态由 HR 人工更新，未撤回的投递可以撤回。</p>
            </div>
            <div class="header-actions">
              <el-button v-if="selectedJobId" @click="showAllApplications = !showAllApplications">
                {{ showAllApplications ? '仅看当前岗位' : '显示全部投递' }}
              </el-button>
              <el-button :icon="Refresh" @click="refreshApplications">刷新投递</el-button>
            </div>
          </div>
          <p v-if="selectedJobId && !showAllApplications" class="notice info" role="status">
            <el-icon aria-hidden="true"><InfoFilled /></el-icon>
            <span>当前仅显示岗位「{{ jobLabel(selectedJobId) }}」的投递，共 {{ visibleApplications.length }} 条；全部投递共 {{ applications.length }} 条。</span>
          </p>

          <div class="table-scroll">
            <el-table v-loading="loading.applications" :data="visibleApplications" class="data-table" empty-text="暂无投递记录">
              <el-table-column label="岗位" min-width="220">
                <template #default="{ row }">
                  <span class="table-primary">{{ jobLabel(row.jobId) }}</span>
                  <span class="table-secondary">投递 #{{ row.id }}</span>
                </template>
              </el-table-column>
              <el-table-column label="简历" min-width="160">
                <template #default="{ row }">{{ resumeLabel(row.resumeId) }}</template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="投递时间" min-width="170">
                <template #default="{ row }">{{ formatDate(row.appliedAt) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button
                    size="small"
                    :disabled="row.status === 'WITHDRAWN'"
                    @click="withdraw(row.id)"
                  >
                    撤回
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 匹配" name="matches">
        <section class="workspace-grid narrow-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>生成匹配</h2>
                <p>选择已发布简历和公开岗位，计算岗位要求中可识别技能的覆盖率。</p>
              </div>
            </div>
            <el-form class="action-form" :model="matchForm" label-position="top">
              <el-form-item label="岗位">
                <el-select v-model="matchForm.jobId" placeholder="请选择岗位">
                  <el-option
                    v-for="job in jobs"
                    :key="job.id"
                    :label="`${job.title} / ${job.companyName}`"
                    :value="job.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="已发布简历">
                <el-select v-model="matchForm.resumeId" placeholder="请选择简历">
                  <el-option
                    v-for="resume in publishedResumes"
                    :key="resume.id"
                    :label="resume.title"
                    :value="resume.id"
                  />
                </el-select>
              </el-form-item>
              <el-button type="primary" :icon="MagicStick" :loading="loading.createMatch" @click="generateMatch">
                生成 AI 匹配
              </el-button>
            </el-form>
            <div class="notice info" role="note">
              <el-icon aria-hidden="true"><InfoFilled /></el-icon>
              <span><strong>评分怎么来？</strong>分数 = 简历文本中提及的岗位技能数 ÷ 岗位要求中词典可识别的技能数。由本地规则计算，不调用外部 AI；不代表熟练度或录用概率，最终决定由人工复核。</span>
            </div>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>匹配结果</h2>
                <p>历史结果保留生成时的版本，如需比较请重新生成。</p>
              </div>
              <el-button :icon="Refresh" @click="refreshMatches">刷新</el-button>
            </div>

            <div v-loading="loading.matches" class="stack-list">
              <MatchResultCard
                v-for="match in matches"
                :key="match.id"
                :match="match"
                :job-label="jobLabel(match.jobId)"
                :resume-label="resumeLabel(match.resumeId)"
              />
              <el-empty v-if="!loading.matches && matches.length === 0" description="暂无匹配结果，选择岗位和简历后生成" />
            </div>
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="简历优化" name="resumeOptimization">
        <section class="workspace-grid narrow-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>生成优化建议</h2>
                <p>选择一份简历和目标岗位，使用本地规则生成改进建议。</p>
              </div>
            </div>

            <el-form class="action-form" :model="optimizationForm" label-position="top">
              <el-form-item label="目标岗位">
                <el-select v-model="optimizationForm.jobId" placeholder="请选择目标岗位">
                  <el-option
                    v-for="job in jobs"
                    :key="job.id"
                    :label="`${job.title} / ${job.companyName}`"
                    :value="job.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="简历">
                <el-select v-model="optimizationForm.resumeId" placeholder="请选择简历">
                  <el-option
                    v-for="resume in resumes"
                    :key="resume.id"
                    :label="`${resume.title} / ${statusText(resume.status)}`"
                    :value="resume.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="loading.resumeOptimization"
                @click="generateResumeOptimization"
              >
                生成优化建议
              </el-button>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>建议结果</h2>
                <p>仅补充真实具备的技能和经历，修改后可在“我的简历”中保存。</p>
              </div>
            </div>

            <article v-if="resumeOptimization" class="result-card">
              <div class="result-head">
                <div>
                  <h3>{{ resumeLabel(resumeOptimization.resumeId) }}</h3>
                  <p>目标岗位：{{ jobLabel(resumeOptimization.jobId) }}</p>
                </div>
                <span class="kind-flag rule">本地规则建议</span>
              </div>
              <p class="result-summary">{{ resumeOptimization.overallSummary }}</p>
              <div class="result-grid">
                <div class="result-section positive">
                  <h4><el-icon aria-hidden="true"><CircleCheck /></el-icon>已覆盖关键词</h4>
                  <div v-if="resumeOptimization.matchedKeywords.length" class="chip-row">
                    <span v-for="item in resumeOptimization.matchedKeywords" :key="item" class="chip accent">{{ item }}</span>
                  </div>
                  <p v-else class="form-hint">暂无明显重合关键词</p>
                </div>
                <div class="result-section gap">
                  <h4><el-icon aria-hidden="true"><Warning /></el-icon>待补充关键词</h4>
                  <div v-if="resumeOptimization.missingKeywords.length" class="chip-row">
                    <span v-for="item in resumeOptimization.missingKeywords" :key="item" class="chip">{{ item }}</span>
                  </div>
                  <p v-else class="form-hint">暂无明显关键词缺口</p>
                </div>
              </div>
              <div class="result-section">
                <h4><el-icon aria-hidden="true"><EditPen /></el-icon>内容建议</h4>
                <ul class="suggestion-list">
                  <li v-for="item in resumeOptimization.contentSuggestions" :key="item">{{ item }}</li>
                </ul>
              </div>
              <div class="result-section action">
                <h4><el-icon aria-hidden="true"><Guide /></el-icon>下一步动作</h4>
                <ul class="suggestion-list">
                  <li v-for="item in resumeOptimization.actionPlan" :key="item">{{ item }}</li>
                </ul>
              </div>
              <div class="source-line"><span>来源</span><code>{{ resumeOptimization.modelName }}</code><span>本地规则，不调用外部 AI 服务</span></div>
            </article>
            <el-empty v-else description="选择简历和目标岗位后生成建议" />
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="行动计划" name="actionPlan">
        <section class="workspace-grid narrow-main">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>生成求职行动计划</h2>
                <p>选择一条本人投递，使用本地规则生成下一步准备清单。</p>
              </div>
            </div>

            <el-form class="action-form" :model="actionPlanForm" label-position="top">
              <el-form-item label="投递记录">
                <el-select v-model="actionPlanForm.applicationId" placeholder="请选择投递记录">
                  <el-option
                    v-for="application in applications"
                    :key="application.id"
                    :label="`${jobLabel(application.jobId)} / ${statusText(application.status)}`"
                    :value="application.id"
                  />
                </el-select>
              </el-form-item>
              <el-button
                type="primary"
                :icon="MagicStick"
                :loading="loading.applicationActionPlan"
                @click="generateActionPlan"
              >
                生成行动计划
              </el-button>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>计划结果</h2>
                <p>计划用于准备，不会改变投递状态。</p>
              </div>
            </div>

            <article v-if="applicationActionPlan" class="result-card">
              <div class="result-head">
                <div>
                  <h3>投递 #{{ applicationActionPlan.applicationId }}</h3>
                  <p>当前状态：{{ statusText(applicationActionPlan.applicationStatus) }} · 优先级：{{ applicationActionPlan.priorityLevel }}</p>
                </div>
                <span class="kind-flag rule">本地规则建议</span>
              </div>
              <div class="score-badge">
                <span class="score-label">规则匹配分 · 只读</span>
                <span class="score-value">{{ applicationActionPlan.matchScore ?? '暂无' }}</span>
                <span class="table-secondary">来源：{{ applicationActionPlan.scoreSource }}</span>
              </div>
              <p class="result-summary">{{ applicationActionPlan.statusSummary }}</p>
              <div class="result-section action">
                <h4><el-icon aria-hidden="true"><Collection /></el-icon>准备清单</h4>
                <ul class="suggestion-list">
                  <li v-for="item in applicationActionPlan.preparationChecklist" :key="item">{{ item }}</li>
                </ul>
              </div>
              <div class="result-section risk">
                <h4><el-icon aria-hidden="true"><Warning /></el-icon>风险提醒</h4>
                <ul class="suggestion-list">
                  <li v-for="item in applicationActionPlan.riskReminders" :key="item">{{ item }}</li>
                </ul>
              </div>
              <div class="result-section">
                <h4><el-icon aria-hidden="true"><Guide /></el-icon>下一步动作</h4>
                <ul class="suggestion-list">
                  <li v-for="item in applicationActionPlan.nextActions" :key="item">{{ item }}</li>
                </ul>
              </div>
              <div class="source-line"><span>来源</span><code>{{ applicationActionPlan.modelName }}</code><span>本地规则，不调用外部 AI 服务</span></div>
            </article>
            <el-empty v-else description="选择投递记录后生成计划" />
          </aside>
        </section>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>

<style scoped>
.resume-main { display: grid; gap: var(--space-4); min-width: 0; }
</style>
