<!-- views/ArticlesView.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { DocumentAdd } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  useArticles,
  useSectors,
  useCreateArticle,
  useUpdateArticle,
  usePublishArticle,
  useArchiveArticle,
} from '@/composables'
import { articlesService } from '@/services'
import { useAuthStore } from '@/stores/auth'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import ArticleTable from '@/components/articles/ArticleTable.vue'
import ArticleEditorDialog from '@/components/articles/ArticleEditorDialog.vue'
import type { Article, ArticleDetail } from '@/types'

const auth = useAuthStore()
const { articles, isLoading, isError, refetch } = useArticles()
const { sectors } = useSectors()
const { createArticle, creating } = useCreateArticle()
const { updateArticle, updating } = useUpdateArticle()
const { publishArticle } = usePublishArticle()
const { archiveArticle } = useArchiveArticle()

const dialog = ref(false)
const editing = ref<ArticleDetail | null>(null)
const loadingDetail = ref(false)

const isAdmin = computed(() => auth.hasRole('ADMIN'))
const saving = computed(() => creating.value || updating.value)

function openCreate() {
  editing.value = null
  dialog.value = true
}

async function openEdit(article: Article) {
  loadingDetail.value = true
  try {
    // GET /articles/{id} traz o HTML completo (ArticleDetailDto) —
    // a listagem não carrega content por design
    editing.value = await articlesService.get(article.id)
    dialog.value = true
  } catch {
    ElMessage.error('Não foi possível carregar o artigo.')
  } finally {
    loadingDetail.value = false
  }
}

async function handleSubmit(payload: {
  title: string
  content: string
  sector: string
  allowedSectors: string[]
}) {
  try {
    if (editing.value) {
      await updateArticle({
        id: editing.value.id,
        request: {
          title: payload.title,
          content: payload.content,
          allowedSectors: payload.allowedSectors,
        },
      })
      ElMessage.success('Artigo atualizado.')
    } else {
      await createArticle(payload)
      ElMessage.success('Rascunho criado. Publique para indexar na base.')
    }
    dialog.value = false
    editing.value = null
  } catch {
    ElMessage.error('Não foi possível salvar o artigo.')
  }
}

async function handlePublish(id: string) {
  try {
    await publishArticle(id)
    ElMessage.success('Artigo publicado e indexado na base.')
  } catch {
    ElMessage.error('Não foi possível publicar o artigo.')
  }
}

async function handleArchive(id: string) {
  try {
    await archiveArticle(id)
    ElMessage.success('Artigo arquivado e removido da base.')
  } catch {
    ElMessage.error('Não foi possível arquivar o artigo.')
  }
}

function reset() {
  refetch()
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <PageHeading eyebrow="BASE DE CONHECIMENTO" title="Artigos"
        description="Conteúdo editorial indexado para consultas da IA — somente o texto é ingerido.">
        <template #actions>
          <el-button class="primary-button" :icon="DocumentAdd" :loading="loadingDetail" @click="openCreate">
            Novo artigo
          </el-button>
        </template>
      </PageHeading>

      <section v-if="isLoading" class="surface">
        <el-skeleton :rows="5" animated />
      </section>

      <section v-else-if="isError" class="surface">
        <div class="load-error">
          <p>Não foi possível carregar os artigos.</p>
          <el-button class="primary-button" @click="refetch">Tentar novamente</el-button>
        </div>
      </section>

      <section v-else class="surface">
        <ArticleTable :articles="articles ?? []" :current-username="auth.username" :is-admin="isAdmin" @edit="openEdit"
          @publish="handlePublish" @archive="handleArchive" />
      </section>

      <ArticleEditorDialog v-model="dialog" :sectors="sectors ?? []" :article="editing" :saving="saving"
        @submit="handleSubmit" />
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.load-error {
  padding: 28px;
  text-align: center;
  color: #718096;
  font-size: 13px;
}

.load-error .el-button {
  margin-top: 14px;
}
</style>
