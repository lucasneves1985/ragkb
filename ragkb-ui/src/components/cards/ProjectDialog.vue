<!-- components/cards/ProjectDialog.vue -->
<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useCreateProject, useUpdateProject, extractCardsErrorMessage } from '@/composables'
import type { Project } from '@/types'

const props = defineProps<{
  open: boolean
  /** null = criação; Project = edição. */
  project: Project | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const { mutateAsync: createProject } = useCreateProject()
const { mutateAsync: updateProject } = useUpdateProject()

const form = reactive({ name: '', description: '', color: '' })
const saving = ref(false)

watch(
  () => [props.open, props.project] as const,
  ([open]) => {
    if (open) {
      form.name = props.project?.name ?? ''
      form.description = props.project?.description ?? ''
      form.color = props.project?.color ?? ''
    }
  },
)

async function save() {
  const name = form.name.trim()
  if (!name) {
    ElMessage.warning('Informe o nome do projeto.')
    return
  }
  saving.value = true
  try {
    const payload = {
      name,
      description: form.description.trim() || undefined,
      color: form.color || undefined,
    }
    if (props.project) {
      await updateProject({ id: props.project.id, request: payload })
      ElMessage.success('Projeto atualizado!')
    } else {
      await createProject(payload)
      ElMessage.success('Projeto criado!')
    }
    emit('close')
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog :model-value="open" :title="project ? 'Editar projeto' : 'Novo projeto'" width="460px"
    @close="emit('close')">
    <el-form label-position="top">
      <el-form-item label="Nome" required>
        <el-input v-model="form.name" maxlength="120" placeholder="Ex.: Reforma do apartamento" />
      </el-form-item>
      <el-form-item label="Descrição">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="Contexto do projeto (opcional)" />
      </el-form-item>
      <el-form-item label="Cor">
        <el-color-picker v-model="form.color" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('close')">Cancelar</el-button>
      <el-button type="primary" class="primary-button" :loading="saving" @click="save">
        Salvar
      </el-button>
    </template>
  </el-dialog>
</template>
