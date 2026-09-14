<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import type { Sector } from '@/types'

defineProps<{
  sectors: Sector[]
  count: number
}>()

const emit = defineEmits<{
  'update:search': [v: string]
  'update:sector': [v: string]
  'update:status': [v: string]
}>()
</script>

<template>
  <div class="filters">
    <el-input
      placeholder="Buscar arquivo..."
      :prefix-icon="Search"
      clearable
      @update:model-value="emit('update:search', $event)"
    />
    <el-select placeholder="Todos os setores" clearable @update:model-value="emit('update:sector', $event)">
      <el-option v-for="item in sectors" :key="item.id" :label="item.name" :value="item.name" />
    </el-select>
    <el-select placeholder="Todos os status" clearable @update:model-value="emit('update:status', $event)">
      <el-option label="Ativo" value="ACTIVE" />
      <el-option label="Arquivado" value="ARCHIVED" />
      <el-option label="Substituido" value="SUPERSEDED" />
    </el-select>
    <span>{{ count }} documento(s)</span>
  </div>
</template>
