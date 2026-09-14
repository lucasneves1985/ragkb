<script setup lang="ts">
import { reactive, watch } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import UploadSpinner from './UploadSpinner.vue'
import type { DocumentUploadPayload, Sector } from '@/types'

const props = defineProps<{
  modelValue: boolean
  sectors: Sector[]
  uploading: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: DocumentUploadPayload): void
}>()

const form = reactive({
  filename: '',
  file: null as File | null,
  sector: '',
  allowedSectors: [] as string[],
  roles: ['ROLE_USER'],
  supersedes: '',
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) reset()
  },
)

function reset() {
  form.filename = ''
  form.file = null
  form.sector = ''
  form.allowedSectors = []
  form.roles = ['ROLE_USER']
  form.supersedes = ''
}

function close() {
  emit('update:modelValue', false)
}

function selectFile(file: { name: string; raw?: File }) {
  form.filename = file.name
  form.file = file.raw || null
}

function submit() {
  if (!form.file) {
    ElMessage.error('Selecione um arquivo.')
    return
  }
  if (!form.sector) {
    ElMessage.error('Informe o setor.')
    return
  }
  if (form.allowedSectors.length === 0) {
    ElMessage.error('Informe ao menos um setor com permissão.')
    return
  }

  emit('submit', {
    file: form.file,
    sector: form.sector,
    allowedSectors: [...form.allowedSectors],
    roles: [...form.roles],
    supersedes: form.supersedes,
  })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="Adicionar documento"
    width="540px"
    :close-on-click-modal="!uploading"
    :close-on-press-escape="!uploading"
    :show-close="!uploading"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <p class="dialog-copy">O arquivo será processado e indexado para consultas autorizadas.</p>

    <UploadSpinner v-if="uploading" />

    <el-form v-else label-position="top">
      <el-form-item label="Arquivo (PDF, DOCX ou TXT)">
        <el-upload :auto-upload="false" :show-file-list="false" accept=".pdf,.docx,.txt" @change="selectFile">
          <el-button :icon="UploadFilled">Selecionar arquivo</el-button>
        </el-upload>
        <span v-if="form.filename" class="selected-file">{{ form.filename }}</span>
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
      <el-button :disabled="uploading" @click="close">Cancelar</el-button>
      <el-button class="primary-button" :loading="uploading" :disabled="uploading" @click="submit">
        {{ uploading ? 'Processando…' : 'Ingerir documento' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-copy {
  margin: -7px 0 21px;
  color: #748096;
  font-size: 12px;
}

.selected-file {
  margin-left: 10px;
  color: #168463;
  font-size: 12px;
}
</style>
