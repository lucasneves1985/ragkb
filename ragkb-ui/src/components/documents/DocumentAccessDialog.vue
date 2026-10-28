<script setup lang="ts">
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { KnowledgeDocument, Sector } from '@/types'

const props = defineProps<{
  modelValue: boolean
  document: KnowledgeDocument | null
  sectors: Sector[]
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', allowedSectors: string[]): void
}>()

const form = reactive({ allowedSectors: [] as string[] })

watch(() => [props.modelValue, props.document] as const, ([open, document]) => {
  if (open && document) form.allowedSectors = [...(document.allowedSectors ?? [])]
})

function submit() {
  if (form.allowedSectors.length === 0) {
    ElMessage.error('Selecione ao menos um setor com acesso.')
    return
  }
  emit('submit', [...form.allowedSectors])
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="Setores com acesso ao documento"
    width="520px"
    :close-on-click-modal="!saving"
    :close-on-press-escape="!saving"
    :show-close="!saving"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <p class="document-name">{{ document?.filename }}</p>
    <el-form label-position="top">
      <el-form-item label="Setores autorizados">
        <el-select v-model="form.allowedSectors" multiple filterable placeholder="Selecione os setores">
          <el-option v-for="sector in sectors" :key="sector.id" :label="sector.name" :value="sector.name" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="saving" @click="emit('update:modelValue', false)">Cancelar</el-button>
      <el-button class="primary-button" :loading="saving" :disabled="saving" @click="submit">
        Salvar acessos
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.document-name { margin: 0 0 16px; color: #667085; font-size: 13px; }
</style>
