<script setup>
import { computed } from 'vue';
import { CircleCheck, Warning, Guide, InfoFilled } from '@element-plus/icons-vue';

// Read-only presentation of a saved rule match. The score is the skill coverage rate computed
// by the backend; it is never a hiring probability and is not editable here.
const props = defineProps({
  match: { type: Object, required: true },
  jobLabel: { type: String, default: '' },
  resumeLabel: { type: String, default: '' },
});

const score = computed(() => {
  const value = Number(props.match.score);
  return Number.isFinite(value) ? Math.min(Math.max(value, 0), 100) : null;
});
const level = computed(() => (score.value == null ? 'low' : score.value >= 70 ? 'high' : score.value >= 40 ? 'mid' : 'low'));
const isCoverageModel = computed(() => props.match.modelName === 'local-skill-coverage-v1');

function formatDate(value) {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleString('zh-CN', { hour12: false });
}
</script>

<template>
  <article class="result-card match-result">
    <div class="result-head">
      <div>
        <h3>{{ jobLabel || `岗位 #${match.jobId}` }}</h3>
        <p>{{ resumeLabel || `简历 #${match.resumeId}` }}<template v-if="formatDate(match.createdAt)"> · 生成于 {{ formatDate(match.createdAt) }}</template></p>
      </div>
      <span class="kind-flag rule">规则匹配分 · 只读</span>
    </div>

    <div class="score-badge">
      <span class="score-label">{{ isCoverageModel ? '技能覆盖率' : '匹配分（历史版本）' }}</span>
      <span class="score-value">{{ match.score ?? '—' }}<small v-if="match.score != null">{{ isCoverageModel ? '%' : '分' }}</small></span>
    </div>
    <div v-if="score != null" class="coverage-meter" :class="level" role="img"
      :aria-label="`${isCoverageModel ? '技能覆盖率' : '匹配分'} ${match.score}，满分 100`">
      <div class="coverage-track"><div class="coverage-fill" :style="{ width: `${score}%` }" /></div>
      <div class="coverage-caption">
        <span>衡量简历文本对岗位所需技能的提及程度</span>
        <span>不代表熟练度或录用概率</span>
      </div>
    </div>

    <dl class="result-sections">
      <div class="result-section positive">
        <dt><el-icon aria-hidden="true"><CircleCheck /></el-icon>匹配技能</dt>
        <dd>{{ match.strengthSummary || '暂无匹配技能摘要' }}</dd>
      </div>
      <div class="result-section gap">
        <dt><el-icon aria-hidden="true"><Warning /></el-icon>技能缺口</dt>
        <dd>{{ match.gapSummary || '暂无缺口摘要' }}</dd>
      </div>
      <div class="result-section action">
        <dt><el-icon aria-hidden="true"><Guide /></el-icon>建议动作</dt>
        <dd>{{ match.actionSuggestions || '暂无建议动作' }}</dd>
      </div>
      <div v-if="match.analysis" class="result-section">
        <dt><el-icon aria-hidden="true"><InfoFilled /></el-icon>计算说明</dt>
        <dd>{{ match.analysis }}</dd>
      </div>
    </dl>

    <div class="source-line">
      <span>评分来源</span><code>{{ match.modelName || '未记录' }}</code>
      <span v-if="match.modelName?.startsWith('local-')">本地规则计算，未调用外部模型</span>
      <span>最终决定由人工复核</span>
    </div>
  </article>
</template>
