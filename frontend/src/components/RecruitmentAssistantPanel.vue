<script setup>
import { computed, nextTick, ref, watch } from 'vue';
import { ChatDotRound, Plus, Promotion, View, Warning } from '@element-plus/icons-vue';
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
    <aside class="assistant-side">
      <header class="assistant-heading">
        <h2><el-icon aria-hidden="true"><ChatDotRound /></el-icon>{{ isHr ? '招聘复核助手' : '求职助手' }}</h2>
        <el-button :icon="Plus" :disabled="pending" @click="newConversation">新对话</el-button>
      </header>
      <p class="assistant-intro">先选择上下文，助手会结合对应的岗位、简历或投递回答。切换上下文会开启新对话。</p>

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
        <p v-if="!contextReady" class="assistant-hint">请先选择{{ contextMode === 'application' ? '投递记录' : isHr ? '岗位' : '岗位和简历' }}，再发送问题。</p>
      </el-form>

      <div v-if="latestResponse" class="assistant-reference" aria-label="规则匹配参考">
        <div class="reference-score">
          <span class="readonly-flag">规则匹配分 · 只读</span>
          <strong>{{ latestResponse.readOnlyMatchScore ?? '暂无' }}</strong>
        </div>
        <small v-if="latestResponse.matchScoreSource">来源：{{ latestResponse.matchScoreSource }}</small>
        <p>{{ latestResponse.contextSummary }}</p>
        <small>该分数由规则服务计算，助手不能修改分数或招聘状态。</small>
      </div>
      <div v-else class="assistant-reference empty">
        <span class="readonly-flag">规则匹配分 · 只读</span>
        <small>发送问题后，这里显示当前上下文的规则匹配分与上下文摘要。</small>
      </div>

      <ul class="assistant-legend" aria-label="消息类型说明">
        <li><span class="kind-flag model">模型辅助建议</span>外部模型生成，需人工核对</li>
        <li><span class="kind-flag human">降级说明</span>模型不可用时的规则说明</li>
      </ul>
    </aside>

    <div class="assistant-main">
      <div ref="transcript" class="assistant-transcript" role="log" aria-label="当前对话" aria-live="polite" :aria-busy="pending">
        <div v-if="messages.length === 0 && !pending" class="assistant-empty">
          <el-icon aria-hidden="true"><ChatDotRound /></el-icon>
          <strong>暂无会话消息</strong>
          <span>{{ isHr ? '例如：这位候选人的技能缺口需要在面试中如何核实？' : '例如：针对这个岗位，我的简历还缺哪些技能证据？' }}</span>
        </div>
        <article v-for="(message, index) in messages" :key="index" class="assistant-message" :class="[message.kind, message.kind === 'assistant' && !message.generated ? 'fallback' : '']">
          <header>
            <strong>{{ message.kind === 'user' ? '我' : message.generated ? '模型辅助建议' : '降级说明' }}</strong>
            <el-tag v-if="message.kind === 'assistant'" :type="message.generated ? 'success' : 'warning'" size="small">
              {{ message.generated ? message.modelName || '外部模型' : modelStatusText[message.modelStatus] || message.modelStatus }}
            </el-tag>
          </header>
          <p>{{ message.kind === 'user' ? message.text : message.assistantMessage }}</p>
          <div v-if="message.warnings?.length" class="assistant-warnings">
            <span><el-icon aria-hidden="true"><Warning /></el-icon>注意</span>
            <ul>
              <li v-for="warning in message.warnings" :key="warning">{{ warning }}</li>
            </ul>
          </div>
        </article>
        <p v-if="pending" class="assistant-waiting" role="status"><span class="dots" aria-hidden="true"><i /><i /><i /></span>正在等待助手回复…</p>
      </div>

      <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />
      <form class="assistant-composer" @submit.prevent="send">
        <el-input v-model="draft" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" maxlength="1000" show-word-limit
          :disabled="pending" placeholder="输入求职或招聘问题，Ctrl + Enter 发送" aria-label="助手消息"
          @keydown.ctrl.enter.prevent="send" @keydown.meta.enter.prevent="send" />
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
    </div>
  </section>
</template>

<style scoped>
.assistant-surface { display: grid; grid-template-columns: minmax(260px, 340px) minmax(0, 1fr); gap: var(--space-4); align-items: start; min-width: 0; }
.assistant-side, .assistant-main { min-width: 0; padding: var(--space-5); background: var(--color-surface); border: 1px solid var(--color-line); border-radius: var(--radius-lg); }
.assistant-side { display: grid; gap: var(--space-4); position: sticky; top: calc(var(--header-height) + var(--space-4)); }
.assistant-main { display: grid; gap: var(--space-3); }
.assistant-heading, .assistant-actions, .assistant-footer { display: flex; justify-content: space-between; align-items: center; gap: var(--space-3); flex-wrap: wrap; }
.assistant-heading h2 { display: flex; align-items: center; gap: var(--space-2); font-size: var(--text-xl); }
.assistant-heading h2 .el-icon { color: var(--color-accent); }
.assistant-intro, .assistant-hint { color: var(--color-muted); font-size: var(--text-sm); line-height: var(--leading-normal); }
.assistant-hint { margin-top: calc(var(--space-2) * -1); color: var(--color-warning-text); }
.assistant-context .el-form-item { margin-bottom: var(--space-3); }
.assistant-context .el-radio-group { display: flex; flex-wrap: wrap; }
.assistant-reference { display: grid; gap: 6px; padding: var(--space-3) var(--space-4); background: var(--color-surface-muted); border: 1px solid var(--color-line); border-radius: var(--radius-md); }
.reference-score { display: flex; align-items: center; justify-content: space-between; gap: var(--space-2); }
.reference-score strong { color: var(--color-ink); font-size: var(--text-3xl); line-height: 1.1; font-variant-numeric: tabular-nums; }
.assistant-reference p { color: var(--color-text); font-size: var(--text-sm); white-space: pre-wrap; overflow-wrap: anywhere; line-height: var(--leading-normal); }
.assistant-reference small { color: var(--color-muted); font-size: var(--text-xs); overflow-wrap: anywhere; line-height: 1.5; }
.assistant-reference.empty { justify-items: start; }
.assistant-legend { display: grid; gap: 6px; margin: 0; padding: 0; list-style: none; color: var(--color-muted); font-size: var(--text-xs); }
.assistant-legend li { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); }

.assistant-transcript { display: flex; flex-direction: column; gap: var(--space-3); height: min(56vh, 520px); min-height: 320px; overflow-y: auto; padding: var(--space-1) var(--space-1) var(--space-2); }
.assistant-empty { display: grid; justify-items: center; gap: 6px; margin: auto; max-width: 360px; color: var(--color-muted); text-align: center; font-size: var(--text-sm); }
.assistant-empty .el-icon { font-size: 28px; color: var(--color-accent-line); }
.assistant-empty strong { color: var(--color-text); font-size: var(--text-md); }
.assistant-message { max-width: min(760px, 92%); padding: var(--space-3) var(--space-4); border: 1px solid var(--color-line); border-radius: var(--radius-lg); background: var(--color-surface); }
.assistant-message.user { align-self: flex-end; background: var(--color-accent-soft); border-color: var(--color-accent-line); border-bottom-right-radius: var(--radius-sm); }
.assistant-message.assistant { align-self: flex-start; border-bottom-left-radius: var(--radius-sm); border-left: 3px solid var(--color-model-mark); }
.assistant-message.assistant.fallback { background: var(--color-warning-bg); border-color: var(--color-warning-line); border-left-color: var(--color-gap-mark); }
.assistant-message header { display: flex; gap: var(--space-2); align-items: center; flex-wrap: wrap; font-size: var(--text-sm); }
.assistant-message header strong { color: var(--color-ink); }
.assistant-message p { margin-top: 6px; color: var(--color-text); font-size: var(--text-md); line-height: var(--leading-relaxed); white-space: pre-wrap; overflow-wrap: anywhere; }
.assistant-warnings { margin-top: var(--space-2); padding: var(--space-2) var(--space-3); color: var(--color-warning-text); font-size: var(--text-sm); background: rgba(253, 245, 230, 0.7); border: 1px solid var(--color-warning-line); border-radius: var(--radius-sm); }
.assistant-warnings > span { display: inline-flex; align-items: center; gap: 4px; font-weight: 600; }
.assistant-warnings ul { margin: 4px 0 0; padding-left: 20px; line-height: var(--leading-normal); }
.assistant-waiting { display: inline-flex; align-self: flex-start; align-items: center; gap: var(--space-2); padding: var(--space-2) var(--space-3); color: var(--color-muted); font-size: var(--text-sm); background: var(--color-surface-muted); border: 1px solid var(--color-line); border-radius: var(--radius-lg); }
.dots { display: inline-flex; gap: 3px; }
.dots i { width: 5px; height: 5px; background: var(--color-accent); border-radius: 50%; animation: assistant-dot 1s infinite ease-in-out; }
.dots i:nth-child(2) { animation-delay: 0.15s; }
.dots i:nth-child(3) { animation-delay: 0.3s; }
@keyframes assistant-dot { 0%, 80%, 100% { opacity: 0.3; } 40% { opacity: 1; } }
.assistant-composer { display: grid; gap: var(--space-2); padding-top: var(--space-3); border-top: 1px solid var(--color-line); }
.assistant-actions small, .assistant-footer { color: var(--color-muted); overflow-wrap: anywhere; font-size: var(--text-sm); }
.assistant-actions .el-button { min-width: 96px; }
.assistant-footer:empty { display: none; }
@media (max-width: 960px) {
  .assistant-surface { grid-template-columns: 1fr; }
  .assistant-side { position: static; }
}
@media (max-width: 640px) {
  .assistant-side, .assistant-main { padding: var(--space-4); }
  .assistant-transcript { height: 52vh; min-height: 280px; }
  .assistant-message { max-width: 100%; }
  .assistant-actions { display: grid; }
  .assistant-actions .el-button { width: 100%; }
}
</style>
