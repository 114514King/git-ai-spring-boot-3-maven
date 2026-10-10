<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue';

// Grouped, wrapping replacement for the el-tabs header. The el-tabs below still owns the panes
// (v-model, lazy rendering); this component only renders an accessible tab list for them.
const props = defineProps({
  modelValue: { type: String, required: true },
  groups: { type: Array, required: true },
  label: { type: String, required: true },
});
const emit = defineEmits(['update:modelValue']);
const list = ref();
const flatItems = computed(() => props.groups.flatMap((group) => group.items));

function select(name) {
  if (name !== props.modelValue) emit('update:modelValue', name);
}

// On phones the tab list is one horizontal row; keep the active tab inside the visible part.
function revealActive() {
  const strip = list.value;
  const item = strip?.querySelector(`[data-tab="${props.modelValue}"]`);
  if (!strip || !item || strip.scrollWidth <= strip.clientWidth) return;
  const left = item.offsetLeft - strip.offsetLeft;
  const right = left + item.offsetWidth;
  if (left < strip.scrollLeft + 24) strip.scrollLeft = Math.max(left - 24, 0);
  else if (right > strip.scrollLeft + strip.clientWidth - 32) strip.scrollLeft = right - strip.clientWidth + 32;
}

watch(() => props.modelValue, () => nextTick(revealActive));
onMounted(revealActive);

async function moveFocus(event, name) {
  const keys = { ArrowRight: 1, ArrowDown: 1, ArrowLeft: -1, ArrowUp: -1 };
  const items = flatItems.value;
  let index = items.findIndex((item) => item.name === name);
  if (event.key in keys) index = (index + keys[event.key] + items.length) % items.length;
  else if (event.key === 'Home') index = 0;
  else if (event.key === 'End') index = items.length - 1;
  else return;
  event.preventDefault();
  select(items[index].name);
  await nextTick();
  list.value?.querySelector(`[data-tab="${items[index].name}"]`)?.focus();
}
</script>

<template>
  <nav class="workspace-nav" :aria-label="label">
    <div ref="list" class="workspace-nav-groups" role="tablist" :aria-label="label">
      <div v-for="group in groups" :key="group.title" class="workspace-nav-group" role="presentation">
        <span class="workspace-nav-title" aria-hidden="true">{{ group.title }}</span>
        <div class="workspace-nav-items" role="presentation">
          <button
            v-for="item in group.items"
            :key="item.name"
            type="button"
            role="tab"
            class="workspace-nav-item"
            :class="{ active: item.name === modelValue }"
            :data-tab="item.name"
            :aria-selected="item.name === modelValue"
            :aria-controls="`pane-${item.name}`"
            :tabindex="item.name === modelValue ? 0 : -1"
            @click="select(item.name)"
            @keydown="moveFocus($event, item.name)"
          >
            <el-icon v-if="item.icon" aria-hidden="true"><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>
    </div>
  </nav>
</template>
