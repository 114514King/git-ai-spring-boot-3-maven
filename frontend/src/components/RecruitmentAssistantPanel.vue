<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { ChatDotRound, Plus, Promotion, View } from '@element-plus/icons-vue';
import { sendAssistantMessage } from '../api/assistant';
import { extractApiError } from '../api/http';

const props = defineProps({
  role: { type: String, required: true, validator: (value) => ['student', 'hr'].includes(value) },
  jobs: { type: Array, default: () => [] },
  resumes: { type: Array, default: () => [] },
  applications: { type: Array, default: () => [] },
});
const emit = defineEmits(['review']);
const contextMode = ref('general');
const jobId = ref(null);
const resumeId = ref(null);
const applicationId = ref(null);
const conversationId = ref(null);
const messages = ref([]);
const draft = ref('');
const pending = ref(false);
const errorMessage = ref('');
const expired = ref(false);
const latestResponse = ref(null);
const transcript = ref(null);
const isHr = computed(() => props.role === 'hr');
const selectedApplication = computed(() => props.applications.find((item) => item.id === applicationId.value));
const contextReady = computed(() => {
  if (contextMode.value === 'application') return Boolean(selectedApplication.value);
  if (contextMode.value === 'job') {
    return props.jobs.some((item) => item.id === jobId.value)
      && (isHr.value || props.resumes.some((item) => item.id === resumeId.value));
  }
  return true;
});
const canSend = computed(() => !pending.value && !expired.value && contextReady.value
  && draft.value.trim().length > 0 && draft.value.length <= 1000);

const modelStatusText = {
  GENERATED: '已生成',
  DISABLED: '模型未启用',
  MISSING_API_KEY: '模型未配置',
  TIMEOUT: '模型响应超时',
  UPSTREAM_ERROR: '模型服务异常',
  NETWORK_ERROR: '模型网络异常',
  INVALID_RESPONSE: '模型响应无效',
  INTERRUPTED: '模型请求中断',
};

function newConversation() {
  conversationId.value = null;
  messages.value = [];
  draft.value = '';
  latestResponse.value = null;
  errorMessage.value = '';
  expired.value = false;
}

watch([contextMode, jobId, resumeId, applicationId, () => props.role], () => {
  newConversation();
  draft.value = '';
}, { flush: 'sync' });

function requestContext() {
  if (contextMode.value === 'application') return { applicationId: applicationId.value };
  if (contextMode.value === 'job') {
    return isHr.value ? { jobId: jobId.value } : { jobId: jobId.value, resumeId: resumeId.value };
  }
  return {};
}

async function send() {
  if (!canSend.value) return;
  const message = draft.value.trim();
  pending.value = true;
  errorMessage.value = '';
  try {
    const response = await sendAssistantMessage(props.role, {
      message,
      ...(conversationId.value ? { conversationId: conversationId.value } : {}),
      ...requestContext(),
    });
    conversationId.value = response.conversationId;
    latestResponse.value = response;
    messages.value = [...messages.value, { kind: 'user', text: message }, { kind: 'assistant', ...response }].slice(-24);
    draft.value = '';
    await nextTick();
    transcript.value?.scrollTo({ top: transcript.value.scrollHeight, behavior: 'smooth' });
  } catch (error) {
    expired.value = Boolean(conversationId.value && [403, 404, 409].includes(error.response?.status));
    errorMessage.value = expired.value ? '当前会话已不可用，请开启新对话。' : extractApiError(error);
  } finally {
    pending.value = false;
  }
}
</script>

<template>
  <section class="assistant-surface" aria-label="招聘助手">
    <header class="assistant-heading">
      <h2><el-icon><ChatDotRound /></el-icon>{{ isHr ? '招聘复核助手' : '求职助手' }}</h2>
      <el-button :icon="Plus" :disabled="pending" @click="newConversation">新对话</el-button>
    </header>

    <el-form class="assistant-context" label-position="top">
      <el-form-item label="招聘上下文">
        <el-radio-group v-model="contextMode" :disabled="pending" aria-label="招聘上下文">
          <el-radio-button value="general">通用</el-radio-button>
          <el-radio-button value="job">{{ isHr ? '岗位' : '岗位与简历' }}</el-radio-button>
          <el-radio-button value="application">投递</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <template v-if="contextMode === 'job'">
        <el-form-item label="岗位">
          <el-select v-model="jobId" :disabled="pending" placeholder="选择岗位" aria-label="助手岗位">
            <el-option v-for="job in jobs" :key="job.id" :value="job.id" :label="job.title" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isHr" label="本人简历">
          <el-select v-model="resumeId" :disabled="pending" placeholder="选择简历" aria-label="助手简历">
            <el-option v-for="resume in resumes" :key="resume.id" :value="resume.id" :label="resume.title" />
          </el-select>
        </el-form-item>
      </template>
      <el-form-item v-if="contextMode === 'application'" label="投递记录">
        <el-select v-model="applicationId" :disabled="pending" placeholder="选择投递" aria-label="助手投递">
          <el-option v-for="item in applications" :key="item.id" :value="item.id"
            :label="`投递 ${item.id} / 岗位 ${item.jobId} / 简历 ${item.resumeId}`" />
        </el-select>
      </el-form-item>
    </el-form>

    <div v-if="latestResponse" class="assistant-reference" aria-label="规则匹配参考">
      <div><span>规则匹配分 · 只读</span><strong>{{ latestResponse.readOnlyMatchScore ?? '暂无' }}</strong></div>
      <p>{{ latestResponse.contextSummary }}</p>
      <small v-if="latestResponse.matchScoreSource">来源：{{ latestResponse.matchScoreSource }}</small>
    </div>

    <div ref="transcript" class="assistant-transcript" role="log" aria-label="当前对话" aria-live="polite" :aria-busy="pending">
      <el-empty v-if="messages.length === 0 && !pending" description="暂无会话消息" :image-size="64" />
      <article v-for="(message, index) in messages" :key="index" class="assistant-message" :class="message.kind">
        <header>
          <strong>{{ message.kind === 'user' ? '我' : message.generated ? '模型辅助建议' : '降级说明' }}</strong>
          <el-tag v-if="message.kind === 'assistant'" :type="message.generated ? 'success' : 'warning'" size="small">
            {{ message.generated ? message.modelName || '外部模型' : modelStatusText[message.modelStatus] || message.modelStatus }}
          </el-tag>
        </header>
        <p>{{ message.kind === 'user' ? message.text : message.assistantMessage }}</p>
        <ul v-if="message.warnings?.length" class="assistant-warnings">
          <li v-for="warning in message.warnings" :key="warning">{{ warning }}</li>
        </ul>
      </article>
      <p v-if="pending" class="assistant-waiting" role="status">正在等待助手回复…</p>
    </div>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />
    <form class="assistant-composer" @submit.prevent="send">
      <el-input v-model="draft" type="textarea" :rows="3" maxlength="1000" show-word-limit
        :disabled="pending" placeholder="输入求职或招聘问题" aria-label="助手消息" />
      <div class="assistant-actions">
        <small>生成建议须人工核对，招聘决定由 HR 确认。</small>
        <el-button type="primary" native-type="submit" :icon="Promotion" :loading="pending" :disabled="!canSend">发送</el-button>
      </div>
    </form>
    <footer class="assistant-footer">
      <span v-if="latestResponse">本次显示 {{ messages.length }} 条 / 服务端上下文最多 {{ latestResponse.maxHistoryMessages }} 条历史</span>
      <el-button v-if="isHr && selectedApplication && contextMode === 'application'" :icon="View" :disabled="pending"
        @click="emit('review', selectedApplication)">人工复核投递 {{ selectedApplication.id }}</el-button>
    </footer>
  </section>
</template>

<style scoped>
.assistant-surface { min-width: 0; background: #fff; padding: 24px; }
.assistant-heading, .assistant-actions, .assistant-footer { display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap; }
.assistant-heading h2 { display: flex; align-items: center; gap: 8px; margin: 0; font-size: 20px; }
.assistant-context { display: flex; flex-wrap: wrap; gap: 16px; margin-top: 20px; border-bottom: 1px solid #e1e7ea; }
.assistant-context .el-form-item { flex: 1 1 220px; min-width: 0; }
.assistant-context .el-select { width: 100%; }
.assistant-reference { padding: 14px 0; border-bottom: 1px solid #e1e7ea; }
.assistant-reference div { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; }
.assistant-reference strong { font-size: 24px; color: #0f766e; }
.assistant-reference p, .assistant-message p { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.7; }
.assistant-reference p { margin: 8px 0; }
.assistant-transcript { height: 360px; overflow-y: auto; margin: 12px 0; }
.assistant-message { padding: 16px 0; border-bottom: 1px solid #e1e7ea; }
.assistant-message.user { color: #475569; }
.assistant-message header { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.assistant-message p { margin: 10px 0 0; }
.assistant-warnings { padding-left: 20px; color: #876522; font-size: 13px; line-height: 1.6; }
.assistant-composer { margin-top: 16px; }
.assistant-actions { margin-top: 10px; }
.assistant-actions small, .assistant-footer, .assistant-reference small, .assistant-waiting { color: #667480; overflow-wrap: anywhere; font-size: 13px; }
.assistant-footer { margin-top: 16px; min-height: 32px; }
@media (max-width: 640px) {
  .assistant-surface { padding: 16px; }
  .assistant-context { gap: 0; }
  .assistant-context .el-form-item { flex-basis: 100%; }
  .assistant-transcript { height: 320px; }
}
</style>
