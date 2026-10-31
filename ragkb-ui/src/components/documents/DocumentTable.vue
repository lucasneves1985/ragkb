<script setup lang="ts">
import { Edit } from '@element-plus/icons-vue'
import type { DocumentStatus, KnowledgeDocument } from '@/types'

defineProps<{
  documents: KnowledgeDocument[]
}>()

const emit = defineEmits<{
  (e: 'archive', id: string): void
  (e: 'reactivate', id: string): void
  (e: 'edit-access', document: KnowledgeDocument): void
}>()

function tag(s: DocumentStatus) {
  return s === 'ACTIVE'
    ? 'status-active'
    : s === 'ARCHIVED'
      ? 'status-archived'
      : 'status-superseded'
}

function statusLabel(s: DocumentStatus) {
  return s === 'ACTIVE' ? 'Ativo' : s === 'ARCHIVED' ? 'Arquivado' : 'Substituído'
}
</script>

<template>
  <el-table :data="documents" style="width: 100%">
    <el-table-column label="Arquivo" min-width="265" class-name="mobile-label-file">
      <template #default="{ row }">
        <b class="filename">{{ row.filename }}</b>
        <small>{{ row.id }}</small>
      </template>
    </el-table-column>
    <el-table-column prop="sector" label="Setor" width="140" class-name="mobile-label-sector" />
    <el-table-column label="Status" width="130" class-name="mobile-label-status">
      <template #default="{ row }">
        <el-tag :class="tag(row.status)" size="small" effect="light">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="chunkCount" label="Chunks" width="90" class-name="mobile-label-chunks" />
    <el-table-column label="Setores com acesso" min-width="170" class-name="mobile-label-access">
      <template #default="{ row }">
        <el-tag v-for="sector in row.allowedSectors" :key="sector" size="small" effect="plain" class="sector-tag">
          {{ sector }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Ações" width="190" class-name="mobile-label-actions">
      <template #default="{ row }">
        <el-button text class="table-action-neutral" :icon="Edit" @click="emit('edit-access', row)">Acessos</el-button>
        <el-button v-if="row.status === 'ACTIVE'" text type="warning" @click="emit('archive', row.id)">
          Arquivar
        </el-button>
        <el-button
          v-else-if="row.status === 'ARCHIVED'"
          text
          type="success"
          @click="emit('reactivate', row.id)"
        >
          Reativar
        </el-button>
        <el-tooltip v-else content="Documentos substituídos não podem ser reativados.">
          <el-button text disabled>Reativar</el-button>
        </el-tooltip>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.filename {
  display: block;
  font-size: 13px;
}

.filename + small {
  display: block;
  margin-top: 3px;
  color: #9aa4b1;
  font: 10px 'DM Mono';
}

.sector-tag { margin: 2px 4px 2px 0; }
</style>
