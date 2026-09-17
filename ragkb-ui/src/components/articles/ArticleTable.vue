<!-- components/articles/ArticleTable.vue -->
<script setup lang="ts">
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
    <el-table-column prop="sector" label="Setor" width="140" />
    <el-table-column label="Setores com acesso" min-width="180">
      <template #default="{ row }">
        <el-tag v-for="sector in row.allowedSectors" :key="sector" size="small" effect="plain" class="sector-tag">
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
    <el-table-column label="Ações" width="320">
      <template #default="{ row }">
        <!-- Visualizar para todas as linhas: ação de leitura; o gate real
             (autor/setor) é o do GET /articles/{id} -->
        <el-button text @click="$emit('view', row)">Visualizar</el-button>
        <template v-if="canManage(row, currentUsername, isAdmin)">
          <el-button v-if="row.status === 'DRAFT'" text type="success" @click="$emit('publish', row.id)">
            Publicar
          </el-button>
          <el-button text type="primary" @click="$emit('edit', row)">Editar</el-button>
          <el-button text type="warning" @click="$emit('archive', row.id)">Arquivar</el-button>
        </template>
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
