<!-- views/ArticleDetailView.vue -->
<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import DOMPurify from 'dompurify'
import { articlesService } from '@/services'
import { useAuthStore } from '@/stores/auth'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import type { ArticleDetail } from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const article = ref<ArticleDetail | null>(null)
const isLoading = ref(true)
const isNotFound = ref(false)

// Conteúdo é HTML sanitizado no backend — DOMPurify é defesa em
// profundidade na renderização, mesmo padrão do chat
const sanitizedContent = computed(() =>
  article.value ? DOMPurify.sanitize(article.value.content) : '',
)

const canManage = computed(() => {
  const a = article.value
  return !!a && (auth.hasRole('ADMIN') || a.authorUsername === auth.username)
})

function formatDateTime(iso?: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function load() {
  isLoading.value = true
  isNotFound.value = false
  try {
    article.value = await articlesService.get(route.params.id as string)
  } catch {
    isNotFound.value = true
  } finally {
    isLoading.value = false
  }
}

// Regras de navegação explícitas:
// - Aberto pela manutenção (?from=management) → volta para a listagem
// - Aberto pelo assistente ou pelo portal (sem o parâmetro) → portal
function goBack() {
  if (route.query.from === 'management') {
    router.push('/articles')
    return
  }
  router.push('/portal')
}

onMounted(load)
</script>

<template>
  <ErrorBoundary @retry="load">
    <div class="article-detail">
      <!-- Eyebrow à esquerda, Voltar alinhado à direita na mesma linha -->
      <div class="detail-topbar">
        <p class="eyebrow">BASE DE CONHECIMENTO</p>
        <el-button text :icon="ArrowLeft" @click="goBack">Voltar</el-button>
      </div>

      <section v-if="isLoading" class="surface detail-body">
        <el-skeleton :rows="8" animated />
      </section>

      <section v-else-if="isNotFound" class="surface detail-body">
        <el-empty description="Artigo não encontrado ou sem acesso." />
      </section>

      <template v-else-if="article">
        <div class="detail-heading">
          <h1>{{ article.title }}</h1>
          <div class="detail-meta">
            <el-tag :type="article.status === 'PUBLISHED' ? 'success' : 'warning'" size="small" effect="light">
              {{ article.status === 'PUBLISHED' ? 'Publicado' : article.status === 'ARCHIVED' ? 'Arquivado' : 'Rascunho'
              }}
            </el-tag>
            <span>{{ article.authorUsername }}</span>
            <span>·</span>
            <span>{{ article.status === 'PUBLISHED' ? formatDateTime(article.publishedAt) : 'Não publicado' }}</span>
          </div>
          <div class="detail-sectors">
            <el-tag v-for="sector in article.allowedSectors" :key="sector" size="small" effect="plain">
              {{ sector }}
            </el-tag>
          </div>
          <el-alert v-if="article.status !== 'PUBLISHED' && canManage"
            title="Este artigo não está publicado — não é recuperável pelo assistente até ser publicado." type="warning"
            :closable="false" show-icon class="status-alert" />
        </div>

        <section class="surface detail-body">
          <!-- Conteúdo é HTML sanitizado no backend; DOMPurify aqui é redundância de defesa -->
          <div class="article-content" v-html="sanitizedContent"></div>
        </section>
      </template>
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.article-detail {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-width: 860px;
  margin: 0 auto;
  /* Aproxima o conteúdo do cabeçalho da aplicação */
  padding: 8px 32px 24px;
}

/* Eyebrow à esquerda, Voltar à direita — mesma linha */
.detail-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-topbar .eyebrow {
  margin: 0;
}

.detail-heading h1 {
  margin: 6px 0 10px;
  color: #172033;
  font-size: 30px;
  letter-spacing: -1px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #748096;
  font-size: 12px;
}

.detail-sectors {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.status-alert {
  margin-top: 14px;
}

.detail-body {
  padding: 28px 32px;
}

.article-content {
  font-size: 15px;
  line-height: 1.75;
  color: #2a3547;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  margin: 22px 0 10px;
  color: #172033;
}

.article-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.article-content :deep(blockquote) {
  margin: 14px 0;
  padding: 8px 16px;
  border-left: 3px solid #72e2bf;
  color: #526077;
  background: #f6faf9;
}

.article-content :deep(a) {
  color: #168463;
}
</style>
