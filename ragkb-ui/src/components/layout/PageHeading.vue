<script setup lang="ts">
import { ref } from 'vue'
import type { Component } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

defineProps<{
  eyebrow?: string
  title: string
  icon?: Component
  description?: string
  onRefresh?: () => unknown
  refreshing?: boolean
}>()

const refreshingNow = ref(false)

async function refreshList(onRefresh?: () => unknown) {
  if (!onRefresh) return
  refreshingNow.value = true
  try {
    await onRefresh()
  } finally {
    refreshingNow.value = false
  }
}
</script>

<template>
  <div class="page-heading">
    <div>
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1>
        <el-icon v-if="icon" class="page-heading-icon"><component :is="icon" /></el-icon>
        {{ title }}
      </h1>
      <p v-if="description">{{ description }}</p>
    </div>
    <div v-if="onRefresh || $slots.actions" class="heading-actions">
      <el-button v-if="onRefresh" text circle :icon="Refresh" :loading="refreshing || refreshingNow"
        title="Atualizar lista" aria-label="Atualizar lista" @click="refreshList(onRefresh)" />
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-heading-icon {
  margin-right: 8px;
  font-size: 0.9em;
  vertical-align: -0.08em;
}

.heading-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
