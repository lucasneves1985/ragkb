<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import type { Sector } from '@/types'

defineProps<{
  sectors: Sector[]
}>()

defineEmits<{
  (e: 'remove', sector: Sector): void
}>()
</script>

<template>
  <el-table :data="sectors" style="width: 100%" empty-text="Nenhum setor cadastrado ainda.">
    <el-table-column prop="name" label="Nome do setor" min-width="200" />
    <el-table-column label="Ações" width="120" align="right">
      <template #default="{ row }">
        <el-button text type="danger" :icon="Delete" @click="$emit('remove', row)">Remover</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
