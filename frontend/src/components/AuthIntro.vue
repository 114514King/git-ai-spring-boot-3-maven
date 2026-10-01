<script setup>
import { ArrowRight, Briefcase, Document, Connection } from '@element-plus/icons-vue';
defineProps({ register: Boolean });
</script>

<template>
  <section class="auth-hero" aria-labelledby="auth-title">
    <h1 id="auth-title">{{ register ? '新的机会，\n从这里开始。' : '让人才与机会，\n更好地相遇。' }}</h1>
    <p>{{ register ? '选择学生或 HR 身份，开启你的求职与招聘之旅。' : '从发现岗位到完善简历，在一个工作台里，走好职业发展的每一步。' }}</p>
    <ul class="auth-features">
      <li><el-icon><Briefcase /></el-icon><div><strong>发现合适的岗位</strong><span>按关键词、城市和用工类型，找到新的机会。</span></div></li>
      <li><el-icon><Document /></el-icon><div><strong>呈现你的优势</strong><span>导入简历、整理经历，让你的能力清晰可见。</span></div></li>
      <li><el-icon><Connection /></el-icon><div><strong>看清技能匹配</strong><span>了解已具备的技能与差距，为下一步做好准备。</span></div></li>
    </ul>
    <div class="auth-journey"><span>发现机会</span><el-icon><ArrowRight /></el-icon><span>完善简历</span><el-icon><ArrowRight /></el-icon><span>开启下一站</span></div>
  </section>
</template>
