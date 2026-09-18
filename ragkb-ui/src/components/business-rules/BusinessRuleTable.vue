<!-- components/business-rules/BusinessRuleTable.vue -->
<script setup lang="ts">
import type { BusinessRule } from '@/types'

defineProps<{
  rules: BusinessRule[]
  currentUsername: string
  isAdmin: boolean
}>()

defineEmits<{
  (e: 'view', rule: BusinessRule): void
  (e: 'edit', rule: BusinessRule): void
  (e: 'publish', id: string): void
  (e: 'archive', id: string): void
}>()

function tag(s: BusinessRule['status']) {
  return s === 'PUBLISHED'
    ? 'status-active'
    : s === 'ARCHIVED'
      ? 'status-archived'
      : 'status-draft'
}

function statusLabel(s: BusinessRule['status']) {
  return s === 'PUBLISHED' ? 'Publicada' : s === 'ARCHIVED' ? 'Arquivada' : 'Rascunho'
}

// Regra espelhada do backend (findOwned): autor ou ADMIN.
function canManage(rule: BusinessRule, currentUsername: string, isAdmin: boolean) {
  return isAdmin || rule.authorUsername === currentUsername
}
</script>

<template>
  <el-table :data="rules" style="width: 100%">
    <el-table-column label="Regra" min-width="280">
      <template #default="{ row }">
        <b class="title">{{ row.title }}</b>
        <small>{{ row.authorUsername }} · {{ row.articleIds.length }} artigo(s)</small>
      </template>
    </el-table-column>
    <el-table-column label="Setores com acesso" min-width="180">
      <template #default="{ row }">
        <el-tag v-for="sector in row.sectorNames" :key="sector" size="small" effect="plain" class="sector-tag">
          {{ sector }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Status" width="130">
      <template #default="{ row }">
        <el-tag :class="tag(row.status)" size="small" effect="light">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Ações" width="260">
      <template #default="{ row }">
        <el-button text type="primary" @click="$emit('view', row)">Ver</el-button>
        <template v-if="canManage(row, currentUsername, isAdmin)">
          <!-- Rascunho e arquivada: publica/republica -->
          <el-button v-if="row.status === 'DRAFT' || row.status === 'ARCHIVED'" text type="success"
            @click="$emit('publish', row.id)">
            {{ row.status === 'ARCHIVED' ? 'Republicar' : 'Publicar' }}
          </el-button>
          <el-button text type="primary" @click="$emit('edit', row)">Editar</el-button>
          <!-- Só publicada pode ser arquivada -->
          <el-button v-if="row.status === 'PUBLISHED'" text type="warning" @click="$emit('archive', row.id)">
            Arquivar
          </el-button>
        </template>
        <el-tooltip v-else content="Somente o autor ou um admin pode gerenciar esta regra.">
          <el-button text disabled>Sem permissão</el-button>
        </el-tooltip>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.title {
  display: block;
  font-size: 13px;
}

.title+small {
  display: block;
  margin-top: 3px;
  color: #9aa4b1;
  font: 10px 'DM Mono';
}

.sector-tag {
  margin-right: 4px;
}

.status-draft {
  color: #7a8699;
}
</style>
