<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import type { Sector } from '@/types'

defineProps<{
  search: string
  sector: string
  status: string
  sectors: Sector[]
  count: number
}>()

defineEmits<{
  (e: 'update:search', value: string): void
  (e: 'update:sector', value: string): void
  (e: 'update:status', value: string): void
}>()
</script>

<template>
  <div class="filters">
    <el-input
      :model-value="search"
      placeholder="Buscar arquivo..."
      :prefix-icon="Search"
      clearable
      @update:model-value="$emit('update:search', $event)"
    />
    <el-select
      :model-value="sector"
      placeholder="Todos os setores"
      clearable
      @update:model-value="$emit('update:sector', $event)"
    >
      <el-option v-for="item in sectors" :key="item.id" :label="item.name" :value="item.name" />
    </el-select>
    <el-select
      :model-value="status"
      placeholder="Todos os status"
      clearable
      @update:model-value="$emit('update:status', $event)"
    >
      <el-option label="Ativo" value="ACTIVE" />
      <el-option label="Arquivado" value="ARCHIVED" />
      <el-option label="Substituído" value="SUPERSEDED" />
    </el-select>
    <span>{{ count }} documento(s)</span>
  </div>
</template>

<style scoped>
.filters {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px;
}

.filters .el-input {
  max-width: 280px;
}

.filters .el-select {
  width: 180px;
}

.filters > span {
  margin-left: auto;
  color: #8793a4;
  font-size: 12px;
}

@media (max-width: 780px) {
  .filters {
    align-items: stretch;
    flex-direction: column;
  }

  .filters .el-input,
  .filters .el-select {
    max-width: none;
    width: 100%;
  }
}
</style>
