<script setup>
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { CircleClose, InfoFilled, Loading, Upload, Warning } from '@element-plus/icons-vue';
import { createResume, previewResumeImport } from '../api/student';
import { extractApiError } from '../api/http';

const emit = defineEmits(['imported']);
const fileInput = ref();
const extracting = ref(false);
const saving = ref(false);
const preview = ref(null);
const title = ref('');
const text = ref('');
const error = ref('');
const chosenFileName = ref('');
const busy = computed(() => extracting.value || saving.value);

function clearPreview(resetFile = true) {
  preview.value = null;
  title.value = '';
  text.value = '';
  error.value = '';
  if (resetFile && fileInput.value) {
    fileInput.value.value = '';
    chosenFileName.value = '';
  }
}

async function selectFile(event) {
  const file = event.target.files?.[0];
  clearPreview(false);
  chosenFileName.value = file?.name || '';
  if (!file) return;
  if (!/\.(pdf|txt)$/i.test(file.name)) {
    error.value = '仅支持文本型 PDF 或 UTF-8 TXT 文件';
    event.target.value = '';
    return;
  }
  if (file.size === 0 || file.size > 2 * 1024 * 1024) {
    error.value = '请选择非空且不超过 2 MiB 的文件';
    event.target.value = '';
    return;
  }
  extracting.value = true;
  try {
    preview.value = await previewResumeImport(file);
    text.value = preview.value.text;
    title.value = file.name.replace(/\.(pdf|txt)$/i, '').slice(0, 100);
  } catch (failure) {
    error.value = extractApiError(failure);
    event.target.value = '';
  } finally {
    extracting.value = false;
  }
}

async function confirmImport() {
  if (busy.value || !preview.value) return;
  error.value = '';
  if (!title.value.trim() || !text.value.trim() || text.value.length > 20000) {
    error.value = '请填写标题并核对非空文本，文本最多 20,000 字符';
    return;
  }
  saving.value = true;
  try {
    const saved = await createResume({ title: title.value.trim(), sourceText: text.value.trim() });
    clearPreview();
    ElMessage.success('已创建本人简历草稿，发布前仍可编辑');
    emit('imported', saved);
  } catch (failure) {
    error.value = extractApiError(failure);
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <section class="work-panel import-panel" aria-labelledby="resume-import-heading">
    <div class="section-heading">
      <div>
        <h2 id="resume-import-heading">从文件导入简历</h2>
        <p>支持文本型 PDF、UTF-8 TXT，最多 2 MiB、20 页、20,000 字符；扫描件请先转为文字。</p>
      </div>
    </div>

    <ol class="import-steps" aria-label="导入步骤">
      <li :class="{ current: !preview && !extracting, done: Boolean(preview) }"><span>1</span>选择文件并提取</li>
      <li :class="{ current: Boolean(preview) && !saving }"><span>2</span>预览并修改文本</li>
      <li :class="{ current: saving }"><span>3</span>确认后创建新草稿</li>
    </ol>
    <p class="notice info">
      <el-icon aria-hidden="true"><InfoFilled /></el-icon>
      <span>确认后才会创建新的简历草稿，<strong>不会覆盖已有简历</strong>。原文件不保存，也不发送给外部 AI。</span>
    </p>

    <div class="file-picker">
      <span class="file-label" id="resume-import-file-label">选择简历文件</span>
      <div class="file-row">
        <input id="resume-import-file" ref="fileInput" class="file-input" type="file" accept=".pdf,.txt"
          aria-labelledby="resume-import-file-label" :disabled="busy" @change="selectFile" />
        <label class="file-button" :class="{ disabled: busy }" for="resume-import-file">
          <el-icon aria-hidden="true"><Upload /></el-icon>选择 PDF / TXT 文件
        </label>
        <span class="file-name">{{ chosenFileName || '尚未选择文件' }}</span>
      </div>
    </div>
    <p v-if="extracting" class="import-status" role="status"><el-icon class="is-loading" aria-hidden="true"><Loading /></el-icon>正在提取文本，请稍候…</p>
    <p v-if="error" class="notice error import-error" role="alert"><el-icon aria-hidden="true"><CircleClose /></el-icon><span>{{ error }}</span></p>

    <el-form v-if="preview" class="import-preview" label-position="top" @submit.prevent="confirmImport">
      <div class="import-file-meta">
        <span class="chip accent">{{ preview.format }}</span>
        <strong>{{ preview.filename }}</strong>
        <span v-if="preview.pageCount">{{ preview.pageCount }} 页</span>
      </div>
      <p v-for="warning in preview.warnings" :key="warning" class="notice warning">
        <el-icon aria-hidden="true"><Warning /></el-icon><span>{{ warning }}</span>
      </p>
      <el-form-item label="导入草稿标题">
        <el-input v-model="title" maxlength="100" :disabled="saving" />
      </el-form-item>
      <el-form-item label="提取文本预览（可修改）">
        <el-input v-model="text" type="textarea" :rows="9" maxlength="20000" show-word-limit :disabled="saving" />
      </el-form-item>
      <div class="button-row">
        <el-button type="primary" native-type="submit" :loading="saving" :disabled="busy">确认创建草稿</el-button>
        <el-button :disabled="busy" @click="clearPreview">取消导入</el-button>
      </div>
    </el-form>
  </section>
</template>

<style scoped>
.import-panel { display: grid; gap: var(--space-3); }
.import-panel .section-heading { margin-bottom: 0; }
.import-steps { display: flex; flex-wrap: wrap; gap: var(--space-2) var(--space-4); margin: 0; padding: 0; list-style: none; color: var(--color-muted); font-size: var(--text-sm); }
.import-steps li { display: inline-flex; align-items: center; gap: 6px; }
.import-steps span { display: inline-grid; place-items: center; width: 20px; height: 20px; font-size: 12px; font-weight: 600; border: 1px solid var(--color-line-strong); border-radius: 50%; }
.import-steps li.current { color: var(--color-ink); font-weight: 600; }
.import-steps li.current span { color: #fff; background: var(--color-accent); border-color: var(--color-accent); }
.import-steps li.done span { color: var(--color-accent-strong); background: var(--color-accent-soft); border-color: var(--color-accent-line); }
.file-picker { position: relative; display: grid; gap: 6px; padding: var(--space-4); background: var(--color-surface-muted); border: 1px dashed var(--color-line-strong); border-radius: var(--radius-md); }
.file-label { color: var(--color-ink); font-weight: 500; }
.file-row { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2) var(--space-3); }
/* Native input stays focusable for keyboard and screen readers; the label is the visible control. */
.file-input { position: absolute; width: 1px; height: 1px; opacity: 0; overflow: hidden; }
.file-button { display: inline-flex; align-items: center; gap: 6px; min-height: 36px; padding: 0 var(--space-4); color: var(--color-accent-strong); font-weight: 500; background: var(--color-surface); border: 1px solid var(--color-accent-line); border-radius: var(--radius-md); cursor: pointer; }
.file-button:hover { background: var(--color-accent-soft); }
.file-input:focus-visible + .file-button { outline: 2px solid var(--color-focus); outline-offset: 2px; }
.file-button.disabled { color: var(--color-subtle); border-color: var(--color-line); cursor: not-allowed; }
.file-name { min-width: 0; color: var(--color-muted); font-size: var(--text-sm); overflow-wrap: anywhere; }
.import-status { display: flex; align-items: center; gap: 6px; color: var(--color-muted); }
.import-preview { display: grid; gap: var(--space-3); padding-top: var(--space-3); border-top: 1px solid var(--color-line); }
.import-preview .el-form-item { margin-bottom: 0; }
.import-file-meta { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); color: var(--color-muted); font-size: var(--text-sm); }
.import-file-meta strong { color: var(--color-ink); overflow-wrap: anywhere; }
</style>
