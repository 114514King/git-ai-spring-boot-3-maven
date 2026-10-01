<script setup>
import { computed, ref } from 'vue';
import { ElMessage } from 'element-plus';
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
const busy = computed(() => extracting.value || saving.value);

function clearPreview(resetFile = true) {
  preview.value = null;
  title.value = '';
  text.value = '';
  error.value = '';
  if (resetFile && fileInput.value) fileInput.value.value = '';
}

async function selectFile(event) {
  const file = event.target.files?.[0];
  clearPreview(false);
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
    <h2 id="resume-import-heading">从文件导入简历</h2>
    <p>支持文本型 PDF、UTF-8 TXT，最多 2 MiB、20 页、20,000 字符。扫描件请先转为文字。</p>
    <p>上传后先预览，确认才创建新草稿，不覆盖已有简历。原文件不保存，也不发送给外部 AI。</p>
    <label class="file-label" for="resume-import-file">选择简历文件</label>
    <input id="resume-import-file" ref="fileInput" type="file" accept=".pdf,.txt" :disabled="busy" @change="selectFile" />
    <p v-if="extracting" role="status">正在提取文本，请稍候…</p>
    <p v-if="error" class="import-error" role="alert">{{ error }}</p>
    <el-form v-if="preview" label-position="top" @submit.prevent="confirmImport">
      <p>{{ preview.filename }} · {{ preview.format }}<span v-if="preview.pageCount"> · {{ preview.pageCount }} 页</span></p>
      <p v-for="warning in preview.warnings" :key="warning">{{ warning }}</p>
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
.import-panel { margin-bottom: 20px; }
.import-panel p { line-height: 1.7; }
.file-label { display: block; margin: 12px 0 8px; }
input[type='file'] { max-width: 100%; margin-bottom: 16px; }
.import-error { color: #b42318; }
</style>
