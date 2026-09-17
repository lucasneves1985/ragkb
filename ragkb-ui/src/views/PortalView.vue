<!-- views/PortalView.vue -->
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { usePortalArticles, useSectors } from '@/composables'
import { useAuthStore } from '@/stores/auth'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import type { ArticleDetail } from '@/types'

const router = useRouter()
const auth = useAuthStore()
const { sectors } = useSectors()

const {
  searchInput,
  appliedSearch,
  sector,
  applySearch,
  clearSearch,
  articles,
  isLoading,
  isError,
  refetch,
} = usePortalArticles()

// Filtro de setor só faz sentido para ADMIN — os demais estão presos
// ao próprio setor no backend
const isAdmin = computed(() => auth.hasRole('ADMIN'))

function excerpt(html: string, max = 180): string {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  const text = (doc.body.textContent || '').replace(/\s+/g, ' ').trim()
  return text.length > max ? text.slice(0, max) + '…' : text
}

function formatDate(iso?: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  })
}

function openArticle(article: ArticleDetail) {
  router.push(`/articles/${article.id}`)
}

function reset() {
  refetch()
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <PageHeading eyebrow="PORTAL DE CONHECIMENTO" title="Artigos publicados"
        description="Navegue pelos artigos da base — visíveis conforme os setores com acesso." />

      <div class="portal-filters">
        <el-input v-model="searchInput" placeholder="Buscar por título…" :prefix-icon="Search" clearable
          class="portal-search" @keyup.enter="applySearch" @clear="clearSearch" />
        <el-button class="primary-button" @click="applySearch">Buscar</el-button>
        <el-select v-if="isAdmin" v-model="sector" placeholder="Todos os setores" clearable class="portal-sector">
          <el-option v-for="s in sectors ?? []" :key="s.id" :label="s.name" :value="s.name" />
        </el-select>
      </div>

      <section v-if="isLoading" class="portal-grid">
        <el-card v-for="n in 6" :key="n" class="portal-card" shadow="never">
          <el-skeleton :rows="3" animated />
        </el-card>
      </section>

      <section v-else-if="isError" class="surface portal-empty">
        <p>Não foi possível carregar os artigos.</p>
        <el-button class="primary-button" @click="refetch">Tentar novamente</el-button>
      </section>

      <section v-else-if="!articles?.length" class="surface portal-empty">
        <el-empty :description="appliedSearch
          ? `Nenhum artigo encontrado para “${appliedSearch}”.`
          : 'Nenhum artigo publicado para o seu setor ainda.'" />
      </section>

      <section v-else class="portal-grid">
        <el-card v-for="article in articles" :key="article.id" class="portal-card" shadow="hover"
          @click="openArticle(article)">
          <div class="card-meta">
            <span class="card-author">{{ article.authorUsername }}</span>
            <span>·</span>
            <span>{{ formatDate(article.publishedAt) }}</span>
          </div>
          <h3 class="card-title">{{ article.title }}</h3>
          <p class="card-excerpt">{{ excerpt(article.content) }}</p>
          <div class="card-footer">
            <el-tag v-for="s in article.allowedSectors" :key="s" size="small" effect="plain">
              {{ s }}
            </el-tag>
            <span class="card-read">Ler artigo →</span>
          </div>
        </el-card>
      </section>
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.portal-filters {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.portal-search {
  max-width: 320px;
}

.portal-sector {
  width: 190px;
}

.portal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.portal-card {
  cursor: pointer;
  border: 1px solid #e8ebf0;
  border-radius: 10px;
  transition: border-color 0.15s;
}

.portal-card:hover {
  border-color: #72e2bf;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #748096;
  font-size: 11px;
}

.card-author {
  font-weight: 700;
  color: #526077;
}

.card-title {
  margin: 8px 0 6px;
  color: #172033;
  font-size: 16px;
  line-height: 1.35;
}

.card-excerpt {
  margin: 0;
  color: #6b7889;
  font-size: 13px;
  line-height: 1.6;
}

.card-footer {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #eef1f4;
}

.card-read {
  margin-left: auto;
  color: #168463;
  font-size: 12px;
  font-weight: 700;
}

.portal-empty {
  padding: 48px;
  text-align: center;
  color: #718096;
  font-size: 13px;
}

.portal-empty .el-button {
  margin-top: 14px;
}

@media (max-width: 760px) {
  .portal-filters {
    flex-wrap: wrap;
  }

  .portal-search,
  .portal-sector {
    max-width: none;
    width: 100%;
  }
}
</style>
