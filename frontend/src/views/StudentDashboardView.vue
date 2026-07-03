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
  Search,
  SwitchButton,
} from '@element-plus/icons-vue';
import { extractApiError } from '../api/http';
import {
  createMatch,
  createResume,
  getJob,
  listApplications,
  listJobs,
  listMatches,
  listResumes,
  submitApplication,
  updateResume,
  updateResumeStatus,
  withdrawApplication,
} from '../api/student';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const activeTab = ref('jobs');
const loading = reactive({
  jobs: false,
  resumes: false,
  applications: false,
  matches: false,
  saveResume: false,
  submitApplication: false,
  createMatch: false,
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

const dashboardStats = computed(() => [
  { label: '公开岗位', value: jobs.value.length },
  { label: '我的简历', value: resumes.value.length },
  { label: '投递记录', value: applications.value.length },
  { label: 'AI 匹配', value: matches.value.length },
]);

function emptyResumeForm() {
  return {
    title: '',
    education: '',
    workExperience: '',
    projectExperience: '',
    skills: '',
    selfEvaluation: '',
  };
}

function normalizeParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== ''));
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

async function selectJob(id) {
  selectedJobId.value = id;
  try {
    selectedJob.value = await getJob(id);
    applicationForm.jobId = id;
    matchForm.jobId = id;
  } catch (error) {
    ElMessage.error(extractApiError(error));
  }
}

async function refreshResumes() {
  loading.resumes = true;
  try {
    resumes.value = await listResumes();
    if (!applicationForm.resumeId && publishedResumes.value.length > 0) {
      applicationForm.resumeId = publishedResumes.value[0].id;
      matchForm.resumeId = publishedResumes.value[0].id;
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
  });
  activeTab.value = 'resumes';
}

function resetResumeForm() {
  editingResumeId.value = null;
  Object.assign(resumeForm, emptyResumeForm());
  resumeFormRef.value?.clearValidate();
}

async function saveResume() {
  await resumeFormRef.value.validate();
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
        <p class="eyebrow">学生端</p>
        <h1>求职工作台</h1>
        <p>浏览公开岗位、维护个人简历、发起投递并查看 AI 匹配结果。</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" :icon="SwitchButton" @click="logout">退出</el-button>
      </div>
    </header>

    <section class="stat-strip" aria-label="学生端数据概览">
      <article v-for="stat in dashboardStats" :key="stat.label" class="stat-item">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </article>
    </section>

    <el-tabs v-model="activeTab" class="student-tabs">
      <el-tab-pane label="岗位浏览" name="jobs">
        <section class="workspace-grid">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>公开岗位</h2>
                <p>仅展示后端已发布岗位，可按关键词、城市和类型筛选。</p>
              </div>
            </div>

            <el-form class="filter-grid" :model="jobFilters" @submit.prevent="refreshJobs">
              <el-input v-model.trim="jobFilters.keyword" :prefix-icon="Search" placeholder="岗位或公司关键词" />
              <el-input v-model.trim="jobFilters.city" placeholder="城市" />
              <el-select v-model="jobFilters.employmentType" clearable placeholder="用工类型">
                <el-option label="全职" value="FULL_TIME" />
                <el-option label="兼职" value="PART_TIME" />
                <el-option label="实习" value="INTERNSHIP" />
              </el-select>
              <el-button type="primary" native-type="submit" :loading="loading.jobs">筛选</el-button>
            </el-form>

            <div v-loading="loading.jobs" class="job-list">
              <button
                v-for="job in jobs"
                :key="job.id"
                class="job-row"
                :class="{ active: selectedJobId === job.id }"
                type="button"
                @click="selectJob(job.id)"
              >
                <span>
                  <strong>{{ job.title }}</strong>
                  <small>{{ job.companyName }} · {{ job.city }} · {{ salaryText(job) }}</small>
                </span>
                <el-tag size="small">{{ job.employmentType }}</el-tag>
              </button>
              <el-empty v-if="!loading.jobs && jobs.length === 0" description="暂无公开岗位" />
            </div>
          </div>

          <aside class="work-panel detail-panel">
            <template v-if="selectedJob">
              <div class="section-heading">
                <div>
                  <h2>{{ selectedJob.title }}</h2>
                  <p>{{ selectedJob.companyName }} · {{ selectedJob.city }} · {{ salaryText(selectedJob) }}</p>
                </div>
              </div>

              <el-descriptions :column="1" border>
                <el-descriptions-item label="用工类型">{{ selectedJob.employmentType }}</el-descriptions-item>
                <el-descriptions-item label="发布时间">{{ formatDate(selectedJob.publishedAt) }}</el-descriptions-item>
              </el-descriptions>

              <section class="detail-copy">
                <h3>岗位描述</h3>
                <p>{{ selectedJob.description }}</p>
                <h3>岗位要求</h3>
                <p>{{ selectedJob.requirements }}</p>
              </section>

              <el-form class="action-form" :model="applicationForm" label-position="top">
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
                <p v-if="!hasPublishedResume" class="form-hint">请先发布至少一份简历，才能投递或生成匹配。</p>
              </el-form>
            </template>
            <el-empty v-else description="请选择岗位查看详情" />
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="我的简历" name="resumes">
        <section class="workspace-grid">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>简历维护</h2>
                <p>创建草稿、编辑内容，并将可投递版本发布。</p>
              </div>
              <el-button @click="resetResumeForm">新建草稿</el-button>
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
                <el-input v-model.trim="resumeForm.title" placeholder="例如：Java 后端实习简历" />
              </el-form-item>
              <el-form-item label="教育经历">
                <el-input v-model.trim="resumeForm.education" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item label="工作经历">
                <el-input v-model.trim="resumeForm.workExperience" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item label="项目经历">
                <el-input v-model.trim="resumeForm.projectExperience" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item label="技能关键词">
                <el-input v-model.trim="resumeForm.skills" placeholder="Java, Spring Boot, MySQL" />
              </el-form-item>
              <el-form-item label="自我评价">
                <el-input v-model.trim="resumeForm.selfEvaluation" type="textarea" :rows="3" />
              </el-form-item>
              <el-button type="primary" native-type="submit" :loading="loading.saveResume">
                {{ editingResumeId ? '保存修改' : '创建草稿' }}
              </el-button>
            </el-form>
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>简历列表</h2>
                <p>只有已发布简历可用于投递和 AI 匹配。</p>
              </div>
            </div>

            <div v-loading="loading.resumes" class="stack-list">
              <article v-for="resume in resumes" :key="resume.id" class="stack-card">
                <div>
                  <h3>{{ resume.title }}</h3>
                  <p>{{ resume.skills || '未填写技能关键词' }}</p>
                </div>
                <el-tag :type="statusType(resume.status)">{{ statusText(resume.status) }}</el-tag>
                <div class="button-row">
                  <el-button size="small" :icon="Document" @click="editResume(resume)">编辑</el-button>
                  <el-button
                    v-if="resume.status === 'DRAFT'"
                    size="small"
                    type="success"
                    :icon="CircleCheck"
                    @click="changeResumeStatus(resume, 'PUBLISHED')"
                  >
                    发布
                  </el-button>
                  <el-button v-else size="small" @click="changeResumeStatus(resume, 'DRAFT')">设为草稿</el-button>
                </div>
              </article>
              <el-empty v-if="!loading.resumes && resumes.length === 0" description="暂无简历" />
            </div>
          </aside>
        </section>
      </el-tab-pane>

      <el-tab-pane label="投递记录" name="applications">
        <section class="work-panel">
          <div class="section-heading">
            <div>
              <h2>我的投递</h2>
              <p>查看投递状态变化，并撤回仍属于自己的投递记录。</p>
            </div>
            <el-button :icon="Refresh" @click="refreshApplications">刷新投递</el-button>
          </div>

          <el-table v-loading="loading.applications" :data="selectedJobApplications" class="data-table">
            <el-table-column prop="id" label="投递 ID" width="100" />
            <el-table-column prop="jobId" label="岗位 ID" width="100" />
            <el-table-column prop="resumeId" label="简历 ID" width="100" />
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="投递时间">
              <template #default="{ row }">{{ formatDate(row.appliedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120">
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
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 匹配" name="matches">
        <section class="workspace-grid">
          <div class="work-panel">
            <div class="section-heading">
              <div>
                <h2>生成匹配</h2>
                <p>选择已发布简历和公开岗位，调用本地关键词规则生成分数。</p>
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
          </div>

          <aside class="work-panel">
            <div class="section-heading">
              <div>
                <h2>匹配结果</h2>
                <p>结果来自后端 `local-keyword-match-v1`，不调用外部 AI 服务。</p>
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
    </el-tabs>
  </main>
</template>
