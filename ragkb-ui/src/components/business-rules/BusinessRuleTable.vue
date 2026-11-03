<!-- components/business-rules/BusinessRuleTable.vue -->
<script setup lang="ts">
import { View, Promotion, Edit, Box } from '@element-plus/icons-vue'
import type { BusinessRule } from '@/types'

defineProps<{
  rules: BusinessRule[]
  currentUsername: string
  isAdmin: boolean
  /** ROLE_USER: somente visualização — nenhuma ação de gestão. */
  readOnly?: boolean
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
// Regra arquivada = inativa no momento → linha inteira em cinza.
function rowClass({ row }: { row: BusinessRule }) {
  return row.status === 'ARCHIVED' ? 'archived-row' : ''
}
</script>

<template>
  <el-table :data="rules" style="width: 100%" :row-class-name="rowClass">
    <el-table-column label="Regra" min-width="280" class-name="mobile-label-rule">
      <template #default="{ row }">
        <b class="title">{{ row.title }}</b>
        <small>{{ row.authorUsername }} · {{ row.articleIds.length }} artigo(s)</small>
      </template>
    </el-table-column>
    <el-table-column label="Setores com acesso" min-width="180" class-name="mobile-label-access">
      <template #default="{ row }">
        <el-tag v-for="sector in row.sectorNames" :key="sector" size="small" effect="plain" class="sector-tag">
          {{ sector }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Status" min-width="110" class-name="mobile-label-status">
      <template #default="{ row }">
        <el-tag :class="tag(row.status)" size="small" effect="light">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Ações" width="180" align="center" class-name="mobile-label-actions">
      <template #default="{ row }">
        <div class="row-actions">
          <el-tooltip content="Visualizar regra" placement="top">
            <el-button text circle type="primary" class="table-action-neutral" :icon="View" title="Visualizar regra" aria-label="Visualizar regra"
              @click="$emit('view', row)" />
          </el-tooltip>
          <template v-if="!readOnly && canManage(row, currentUsername, isAdmin)">
            <!-- Rascunho e arquivada: publica/republica -->
            <template v-if="row.status === 'DRAFT' || row.status === 'ARCHIVED'">
              <el-tooltip :content="row.status === 'ARCHIVED' ? 'Republicar regra' : 'Publicar regra'" placement="top">
                <el-button text circle type="success" :icon="Promotion"
                  :title="row.status === 'ARCHIVED' ? 'Republicar regra' : 'Publicar regra'"
                  :aria-label="row.status === 'ARCHIVED' ? 'Republicar regra' : 'Publicar regra'"
                  @click="$emit('publish', row.id)" />
              </el-tooltip>
            </template>
            <!-- Arquivada é imutável: edição só após republicar (guard no backend também) -->
            <template v-if="row.status !== 'ARCHIVED'">
              <el-tooltip content="Editar regra" placement="top">
                <el-button text circle type="primary" class="table-action-neutral" :icon="Edit" title="Editar regra" aria-label="Editar regra"
                  @click="$emit('edit', row)" />
              </el-tooltip>
            </template>
            <!-- Só publicada pode ser arquivada -->
            <template v-if="row.status === 'PUBLISHED'">
              <el-tooltip content="Arquivar regra" placement="top">
                <el-button text circle type="warning" :icon="Box" title="Arquivar regra" aria-label="Arquivar regra"
                  @click="$emit('archive', row.id)" />
              </el-tooltip>
            </template>
          </template>
        </div>
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

/* Regra arquivada: linha inteira em cinza (fonte e herdeiros de texto) */
:deep(tr.archived-row td) {
  color: #9aa4b1;
}

.row-actions {
  display: inline-flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

/* O Element Plus aplica margin-left de 12px entre botões adjacentes;
   aqui o espaçamento é controlado pelo gap do container. */
.row-actions .el-button+.el-button {
  margin-left: 0;
}

/* Ícones levemente maiores que o padrão (14px) do Element Plus. */
.row-actions .el-button .el-icon {
  font-size: 18px;
}

</style>
