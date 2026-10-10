<script setup>
import { User } from '@element-plus/icons-vue';

defineProps({ context: { type: String, default: '' } });
</script>

<template>
  <header class="platform-bar">
    <div class="platform-bar-inner">
      <div class="platform-brand">
        <svg class="brand-symbol" viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <rect x="4" y="9" width="24" height="19" rx="2" />
          <path d="M11 9V5a1 1 0 0 1 1-1h8a1 1 0 0 1 1 1v4M4 17h24M14 15v4h4v-4" />
        </svg>
        <span>AI 智能求职招聘平台</span>
      </div>
      <div v-if="context" class="platform-context" :aria-label="`当前位置：${context}`">
        <el-icon class="context-symbol" aria-hidden="true"><User /></el-icon>
        <span>{{ context }}</span>
      </div>
    </div>
  </header>
</template>
