<script setup lang="ts">
import { computed, ref } from 'vue'
import { DocumentAdd, Search, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  useDocuments,
  useUploadDocument,
  useArchiveDocument,
  useReactivateDocument,
  useSectors,
} from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import type { DocumentStatus } from '@/types'

// Queries — disparam automaticamente ao montar, sem onMounted
const { documents, isLoading, isError, error, refetch } = useDocuments()
const { sectors } = useSectors()

// Mutations — loading e error já embutidos
const { uploadDocument, uploading } = useUploadDocument()
const { archiveDocument } = useArchiveDocument()
const { reactivateDocument } = useReactivateDocument()

const search = ref('')
const sector = ref('')
const status = ref('')
const dialog = ref(false)

const form = ref({
  filename: '',
  file: null as File | null,
  sector: '',
  allowedSectors: [] as string[],
  roles: ['ROLE_USER'],
  supersedes: '',
})

const filtered = computed(() => {
  if (!documents.value) return []
  return documents.value.filter(
    (d) =>
      (!search.value || d.filename.toLowerCase().includes(search.value.toLowerCase())) &&
      (!sector.value || d.sector === sector.value) &&
      (!status.value || d.status === status.value),
  )
})

// não precisa mais de onMounted — Vue Query busca automaticamente

function tag(s: DocumentStatus) {
  return s === 'ACTIVE'
    ? 'status-active'
    : s === 'ARCHIVED'
      ? 'status-archived'
      : 'status-superseded'
}

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

function selectFile(file: { name: string; raw?: File }) {
  form.value.filename = file.name
  form.value.file = file.raw || null
}

async function upload() {
  if (!form.value.file) return ElMessage.error('Selecione um arquivo.')
  if (!form.value.sector) return ElMessage.error('Informe o setor.')
  if (form.value.allowedSectors.length === 0)
    return ElMessage.error('Informe ao menos um setor com permissão.')

  const data = new FormData()
  data.append('file', form.value.file)
  data.append('sector', form.value.sector)
  form.value.allowedSectors.forEach((s) => data.append('allowedSectors', s))
  form.value.roles.forEach((role) => data.append('allowedRoles', role))
  if (form.value.supersedes) data.append('supersedesDocumentId', form.value.supersedes)

  try {
    await uploadDocument(data)
    ElMessage.success('Documento ingerido com sucesso!')
    dialog.value = false
    // invalidação já acontece no onSuccess da mutation → lista atualiza sozinha
    form.value = {
      filename: '',
      file: null,
      sector: '',
      allowedSectors: [],
      roles: ['ROLE_USER'],
      supersedes: '',
    }
  } catch {
    ElMessage.error('Erro ao processar o documento.')
  }
}

// reset para o ErrorBoundary — re-busca dados
function reset() {
  refetch()
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <div class="page-heading">
        <div>
          <h2>Documentos</h2>
          <p>Base de conhecimento indexada</p>
        </div>
        <el-button type="primary" :icon="DocumentAdd" @click="dialog = true">
          Adicionar documento
        </el-button>
      </div>

      <!-- loading state -->
      <section v-if="isLoading" class="surface">
        <el-skeleton :rows="5" animated />
      </section>

      <!-- error state -->
      <section v-else-if="isError" class="surface">
        <el-alert type="error" :title="error?.message || 'Erro ao carregar documentos.'" show-icon>
          <el-button text @click="reset">Tentar novamente</el-button>
        </el-alert>
      </section>

      <!-- success -->
      <section v-else class="surface">
        <div class="filters">
          <el-input v-model="search" placeholder="Buscar arquivo..." :prefix-icon="Search" clearable />
          <el-select v-model="sector" placeholder="Todos os setores" clearable>
            <el-option v-for="item in sectors" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
          <el-select v-model="status" placeholder="Todos os status" clearable>
            <el-option label="Ativo" value="ACTIVE" />
            <el-option label="Arquivado" value="ARCHIVED" />
            <el-option label="Substituído" value="SUPERSEDED" />
          </el-select>
          {{ filtered.length }} documento(s)
        </div>

        <el-table :data="filtered" style="width: 100%">
          <el-table-column label="Arquivo" min-width="265">
            <template #default="{ row }">
              <b class="filename">{{ row.filename }}</b>
              {{ row.id }}
            </template>
          </el-table-column>
          <el-table-column prop="sector" label="Setor" width="140" />
          <el-table-column label="Status" width="130">
            <template #default="{ row }">
              <el-tag :class="tag(row.status)" size="small" effect="light">
                {{ row.status === 'ACTIVE' ? 'Ativo' : row.status === 'ARCHIVED' ? 'Arquivado' : 'Substituído' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="Chunks" width="90" />
          <el-table-column label="Ações" width="160">
            <template #default="{ row }">
              <el-button v-if="row.status === 'ACTIVE'" text type="warning" @click="archive(row.id)">
                Arquivar
              </el-button>
              <el-button v-else-if="row.status === 'ARCHIVED'" text type="success" @click="reactivate(row.id)">
                Reativar
              </el-button>
              <el-tooltip v-else content="Documentos substituídos não podem ser reativados.">
                <el-button text disabled>Reativar</el-button>
              </el-tooltip>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <!-- dialog de upload -->
      <el-dialog v-model="dialog" title="Adicionar documento" width="540px"
        :close-on-click-modal="!uploading" :close-on-press-escape="!uploading" :show-close="!uploading">
        <p class="dialog-copy">O arquivo será processado e indexado para consultas autorizadas.</p>

        <div v-if="uploading" class="upload-overlay">
          <div class="upload-spinner">
            <div class="spinner-ring"></div>
            <p class="spinner-text">Processando documento…</p>
            <p class="spinner-hint">Extraindo texto, dividindo em chunks e indexando na base vetorial.</p>
          </div>
        </div>

        <el-form v-else label-position="top">
          <el-form-item label="Arquivo (PDF, DOCX ou TXT)">
            <el-upload :auto-upload="false" :show-file-list="false" accept=".pdf,.docx,.txt" @change="selectFile">
              <el-button :icon="UploadFilled">Selecionar arquivo</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="Setor responsável">
            <el-select v-model="form.sector" placeholder="Selecione">
              <el-option v-for="s in sectors" :key="s.id" :label="s.name" :value="s.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="Setores com acesso">
            <el-select v-model="form.allowedSectors" multiple placeholder="Selecione">
              <el-option v-for="s in sectors" :key="s.id" :label="s.name" :value="s.name" />
            </el-select>
          </el-form-item>
        </el-form>

        <template #footer>
          <el-button @click="dialog = false" :disabled="uploading">Cancelar</el-button>
          <el-button class="primary-button" @click="upload" :loading="uploading" :disabled="uploading">
            {{ uploading ? 'Processando…' : 'Ingerir documento' }}
          </el-button>
        </template>
      </el-dialog>
    </div>
  </ErrorBoundary>
</template>

<style scoped lang="css" src="@/views/styles/documents.view.css"></style>
