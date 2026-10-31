<!-- components/articles/ArticleTable.vue -->
<script setup lang="ts">
import { View, Promotion, Edit, Box, RefreshLeft } from '@element-plus/icons-vue'
import type { Article } from '@/types'

defineProps<{
  articles: Article[]
  currentUsername: string
  isAdmin: boolean
}>()

defineEmits<{
  (e: 'view', article: Article): void
  (e: 'edit', article: Article): void
  (e: 'publish', id: string): void
  (e: 'archive', id: string): void
  (e: 'reactivate', id: string): void
}>()

function tag(s: Article['status']) {
  return s === 'PUBLISHED' ? 'status-active' : s === 'ARCHIVED' ? 'status-archived' : 'status-draft'
}

function statusLabel(s: Article['status']) {
  return s === 'PUBLISHED' ? 'Publicado' : s === 'ARCHIVED' ? 'Arquivado' : 'Rascunho'
}
// Regra espelhada do backend (findOwned): autor ou ADMIN.
// Rascunho alheio nem chega na lista, mas a defesa fica aqui também.
function canManage(article: Article, currentUsername: string, isAdmin: boolean) {
  return isAdmin || article.authorUsername === currentUsername
}
</script>

<template>
  <el-table :data="articles" style="width: 100%">
    <el-table-column label="Artigo" min-width="280">
      <template #default="{ row }">
        <b class="title">{{ row.title }}</b>
        <small>{{ row.authorUsername }} · {{ row.chunkCount }} chunks</small>
      </template>
    </el-table-column>
    <el-table-column prop="sector" label="Setor" min-width="110" />
    <el-table-column label="Setores com acesso" min-width="180" class-name="col-optional">
      <template #default="{ row }">
        <el-tag v-for="sector in row.allowedSectors" :key="sector" size="small" effect="plain" class="sector-tag">
          {{ sector }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Status" min-width="110">
      <template #default="{ row }">
        <el-tag :class="tag(row.status)" size="small" effect="light">
          {{ statusLabel(row.status) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Ações" width="180" align="center">
      <template #default="{ row }">
        <!-- Visualizar para todas as linhas: ação de leitura; o gate real
             (autor/setor) é o do GET /articles/{id} -->
        <div class="row-actions">
          <el-tooltip content="Visualizar artigo" placement="top">
            <el-button text circle :icon="View" title="Visualizar artigo" aria-label="Visualizar artigo"
              @click="$emit('view', row)" />
          </el-tooltip>
          <template v-if="canManage(row, currentUsername, isAdmin)">
            <!-- Rascunho: publicar e editar -->
            <template v-if="row.status === 'DRAFT'">
              <el-tooltip content="Publicar artigo" placement="top">
                <el-button text circle type="success" :icon="Promotion" title="Publicar artigo"
                  aria-label="Publicar artigo" @click="$emit('publish', row.id)" />
              </el-tooltip>
            </template>
            <!-- Arquivado: reativar (volta a rascunho) -->
            <template v-else-if="row.status === 'ARCHIVED'">
              <el-tooltip content="Reativar como rascunho" placement="top">
                <el-button text circle type="primary" :icon="RefreshLeft" title="Reativar como rascunho"
                  aria-label="Reativar como rascunho" @click="$emit('reactivate', row.id)" />
              </el-tooltip>
            </template>
            <!-- Publicado: arquivar -->
            <template v-else-if="row.status === 'PUBLISHED'">
              <el-tooltip content="Arquivar artigo" placement="top">
                <el-button text circle type="warning" :icon="Box" title="Arquivar artigo" aria-label="Arquivar artigo"
                  @click="$emit('archive', row.id)" />
              </el-tooltip>
            </template>
            <!-- Editar em qualquer status gerenciável -->
            <el-tooltip content="Editar artigo" placement="top">
              <el-button text circle type="primary" :icon="Edit" title="Editar artigo" aria-label="Editar artigo"
                @click="$emit('edit', row)" />
            </el-tooltip>
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

/* Responsividade: colunas secundárias saem em telas estreitas.
   As células do el-table vivem em subcomponentes, por isso :deep(). */
@media (max-width: 900px) {
  .el-table :deep(.col-optional) {
    display: none;
  }
}
</style>
