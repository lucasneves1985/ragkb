<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { DocumentAdd, Search, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useDocuments, useSectors } from '@/composables'
import type { DocumentStatus } from '@/types'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const {
  documents,
  uploading,
  loadDocuments,
  uploadDocument,
  archiveDocument,
  reactivateDocument,
  errorMessage: docError,
} = useDocuments()
const { sectors, loadSectors } = useSectors()

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

const filtered = computed(() =>
  documents.value.filter(
    (d) =>
      (!search.value || d.filename.toLowerCase().includes(search.value.toLowerCase())) &&
      (!sector.value || d.sector === sector.value) &&
      (!status.value || d.status === status.value),
  ),
)

onMounted(() => {
  loadDocuments()
  loadSectors()
})

function tag(s: DocumentStatus) {
  return s === 'ACTIVE'
    ? 'status-active'
    : s === 'ARCHIVED'
      ? 'status-archived'
      : 'status-superseded'
}

async function archive(id: string) {
  await archiveDocument(id)
}

async function reactivate(id: string) {
  await reactivateDocument(id)
}

function reset() {
  loadDocuments()
  loadSectors()
}

function selectFile(file: { name: string; raw?: File }) {
  form.value.filename = file.name
  form.value.file = file.raw || null
}

async function upload() {
  if (!form.value.file) {
    ElMessage.error('Selecione um arquivo para ingerir.')
    return
  }
  if (!form.value.sector) {
    ElMessage.error('Informe o setor responsável pelo documento.')
    return
  }
  if (form.value.allowedSectors.length === 0) {
    ElMessage.error('Informe ao menos um setor com permissão de acesso.')
    return
  }

  const data = new FormData()
  data.append('file', form.value.file)
  data.append('sector', form.value.sector)
  form.value.allowedSectors.forEach((s) => data.append('allowedSectors', s))
  form.value.roles.forEach((role) => data.append('allowedRoles', role))
  if (form.value.supersedes) data.append('supersedesDocumentId', form.value.supersedes)

  const result = await uploadDocument(data)
  if (result) {
    dialog.value = false
    form.value = {
      filename: '',
      file: null,
      sector: '',
      allowedSectors: [],
      roles: ['ROLE_USER'],
      supersedes: '',
    }
    ElMessage.success('Documento ingerido com sucesso!')
  } else {
    ElMessage.error(docError.value || 'Erro ao processar o documento. Tente novamente.')
  }
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <div class="page-heading">
        <div>
          <p class="eyebrow">GOVERNANÇA DA BASE</p>
          <h1>Documentos</h1>
          <p>Gerencie as fontes que alimentam as respostas do assistente.</p>
        </div>
        <el-button class="primary-button" :icon="DocumentAdd" @click="dialog = true">Adicionar documento</el-button>
      </div>

      <section class="surface">
        <div class="filters">
          <el-input v-model="search" :prefix-icon="Search" placeholder="Buscar por nome…" clearable />
          <el-select v-model="sector" placeholder="Todos os setores" clearable>
            <el-option v-for="item in sectors" :key="item.id" :label="item.name" :value="item.name" />
          </el-select>
          <el-select v-model="status" placeholder="Todos os status" clearable>
            <el-option label="Ativo" value="ACTIVE" />
            <el-option label="Arquivado" value="ARCHIVED" />
            <el-option label="Substituído" value="SUPERSEDED" />
          </el-select>
          <span>{{ filtered.length }} documento(s)</span>
        </div>

        <el-table :data="filtered" style="width: 100%">
          <el-table-column label="Arquivo" min-width="265">
            <template #default="{ row }">
              <b class="filename">{{ row.filename }}</b>
              <small>{{ row.id }}</small>
            </template>
          </el-table-column>
          <el-table-column prop="sector" label="Setor" min-width="170" />
          <el-table-column label="Setores com acesso" min-width="220">
            <template #default="{ row }">
              <el-tag v-for="s in row.allowedSectors" :key="s" size="small" effect="plain" class="access-tag">
                {{ s }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Status" min-width="125">
            <template #default="{ row }">
              <el-tag :class="tag(row.status)" size="small" effect="light">
                {{
                  row.status === 'ACTIVE'
                    ? 'Ativo'
                    : row.status === 'ARCHIVED'
                      ? 'Arquivado'
                      : 'Substituído'
                }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="chunkCount" label="Chunks" width="90" />
          <el-table-column prop="ingestedAt" label="Ingestão" min-width="145" />
          <el-table-column label="Ações" width="145">
            <template #default="{ row }">
              <el-button v-if="row.status === 'ACTIVE'" text type="warning" @click="archive(row.id)">Arquivar</el-button>
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

      <el-dialog v-model="dialog" title="Adicionar documento" width="540px" :close-on-click-modal="!uploading"
        :close-on-press-escape="!uploading" :show-close="!uploading">
        <p class="dialog-copy">O arquivo será processado e indexado para consultas autorizadas.</p>

        <div v-if="uploading" class="upload-overlay">
          <div class="upload-spinner">
            <div class="spinner-ring"></div>
            <p class="spinner-text">Processando documento…</p>
            <p class="spinner-hint">
              Extraindo texto, dividindo em chunks e indexando na base vetorial.
            </p>
          </div>
        </div>

        <el-form v-else label-position="top">
          <el-form-item label="Arquivo (PDF, DOCX ou TXT)">
            <el-upload :auto-upload="false" :show-file-list="false" accept=".pdf,.docx,.txt" @change="selectFile">
              <el-button :icon="UploadFilled">Selecionar arquivo</el-button>
            </el-upload>
            <span v-if="form.filename" class="selected-file">{{ form.filename }}</span>
          </el-form-item>

          <el-form-item label="Setor responsável">
            <el-select v-model="form.sector" placeholder="Selecione o setor" style="width: 100%">
              <el-option v-for="item in sectors" :key="item.id" :label="item.name" :value="item.name" />
            </el-select>
          </el-form-item>

          <el-form-item label="Setores que podem acessar (obrigatório)">
            <el-select v-model="form.allowedSectors" multiple placeholder="Selecione um ou mais setores"
              style="width: 100%">
              <el-option v-for="item in sectors" :key="item.id" :label="item.name" :value="item.name" />
            </el-select>
          </el-form-item>

          <el-form-item label="Papéis permitidos">
            <el-checkbox-group v-model="form.roles">
              <el-checkbox label="ROLE_USER">Usuário</el-checkbox>
              <el-checkbox label="ROLE_EDITOR">Editor</el-checkbox>
              <el-checkbox label="ROLE_ADMIN">Administrador</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item label="Este documento substitui uma versão anterior?">
            <el-select v-model="form.supersedes" placeholder="Não substitui nenhum documento" clearable
              style="width: 100%">
              <el-option v-for="doc in documents.filter((d) => d.status === 'ACTIVE')" :key="doc.id" :label="doc.filename"
                :value="doc.id" />
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
