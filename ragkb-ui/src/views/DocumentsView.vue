<script setup lang="ts">
import { computed, ref } from 'vue'
import { DocumentAdd } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  useDocuments,
  useSectors,
  useUploadDocument,
  useArchiveDocument,
  useReactivateDocument,
} from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import DocumentFilters from '@/components/documents/DocumentFilters.vue'
import DocumentTable from '@/components/documents/DocumentTable.vue'
import DocumentUploadDialog from '@/components/documents/DocumentUploadDialog.vue'
import type { DocumentUploadPayload } from '@/types'

const { documents, isLoading, isError, refetch } = useDocuments()
const { sectors } = useSectors()

const { uploadDocument, uploading } = useUploadDocument()
const { archiveDocument } = useArchiveDocument()
const { reactivateDocument } = useReactivateDocument()

const search = ref('')
const sector = ref('')
const status = ref('')
const dialog = ref(false)

const filtered = computed(() => {
  if (!documents.value) return []
  return documents.value.filter(
    (d) =>
      (!search.value || d.filename.toLowerCase().includes(search.value.toLowerCase())) &&
      (!sector.value || d.sector === sector.value) &&
      (!status.value || d.status === status.value),
  )
})

async function archive(id: string) {
  try {
    await archiveDocument(id)
    ElMessage.success('Documento arquivado.')
  } catch {
    ElMessage.error('Erro ao arquivar documento.')
  }
}

async function reactivate(id: string) {
  try {
    await reactivateDocument(id)
    ElMessage.success('Documento reativado.')
  } catch {
    ElMessage.error('Erro ao reativar documento.')
  }
}

async function handleUpload(payload: DocumentUploadPayload) {
  const data = new FormData()
  data.append('file', payload.file)
  data.append('sector', payload.sector)
  payload.allowedSectors.forEach((s) => data.append('allowedSectors', s))
  payload.roles.forEach((role) => data.append('allowedRoles', role))
  if (payload.supersedes) data.append('supersedesDocumentId', payload.supersedes)

  try {
    await uploadDocument(data)
    ElMessage.success('Documento ingerido com sucesso!')
    dialog.value = false
  } catch {
    ElMessage.error('Erro ao processar o documento.')
  }
}

function reset() {
  refetch()
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <PageHeading
        eyebrow="BASE DE CONHECIMENTO"
        title="Documentos"
        description="Base de conhecimento indexada"
      >
        <template #actions>
          <el-button class="primary-button" :icon="DocumentAdd" @click="dialog = true">
            Adicionar documento
          </el-button>
        </template>
      </PageHeading>

      <section v-if="isLoading" class="surface">
        <el-skeleton :rows="5" animated />
      </section>

      <section v-else-if="isError" class="surface">
        <div class="load-error">
          <p>Não foi possível carregar os documentos.</p>
          <el-button class="primary-button" @click="refetch">Tentar novamente</el-button>
        </div>
      </section>

      <section v-else class="surface">
        <DocumentFilters
          v-model:search="search"
          v-model:sector="sector"
          v-model:status="status"
          :sectors="sectors ?? []"
          :count="filtered.length"
        />

        <DocumentTable :documents="filtered" @archive="archive" @reactivate="reactivate" />
      </section>

      <DocumentUploadDialog
        v-model="dialog"
        :sectors="sectors ?? []"
        :uploading="uploading"
        @submit="handleUpload"
      />
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
