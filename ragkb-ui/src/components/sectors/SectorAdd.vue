<script setup lang="ts">
import { Plus } from '@element-plus/icons-vue'

defineProps<{
  modelValue: string
  saving: boolean
}>()

defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'submit'): void
}>()
</script>

<template>
  <div class="sector-add">
    <el-input
      :model-value="modelValue"
      placeholder="Digite o nome do novo setor…"
      clearable
      @update:model-value="$emit('update:modelValue', $event)"
      @keyup.enter="$emit('submit')"
    />
    <el-button class="primary-button" :icon="Plus" :loading="saving" @click="$emit('submit')">
      Incluir
    </el-button>
  </div>
</template>

<style scoped>
.sector-add {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px;
  border-bottom: 1px solid #edf0f4;
}

.sector-add .el-input {
  flex: 1;
  min-width: 0;
}

.sector-add .primary-button {
  flex: none;
  margin-left: 0;
  min-width: 96px;
}

@media (max-width: 480px) {
  .sector-add {
    flex-direction: column;
    align-items: stretch;
  }

  .sector-add .primary-button {
    width: 100%;
  }
}
</style>
