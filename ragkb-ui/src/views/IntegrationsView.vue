<!-- views/IntegrationsView.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { Connection, DocumentAdd } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  useCreateIntegration,
  useDeleteIntegration,
  useIntegrations,
  useSetActiveIntegration,
  useUpdateIntegration,
} from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import IntegrationTable from '@/components/integrations/IntegrationTable.vue'
import IntegrationEditorDialog from '@/components/integrations/IntegrationEditorDialog.vue'
import IntegrationExecutionsDialog from '@/components/integrations/IntegrationExecutionsDialog.vue'
import type { Integration, UpdateIntegrationRequest } from '@/types'

const { integrations, isLoading, isError } = useIntegrations()
const { createIntegration, creating } = useCreateIntegration()
const { updateIntegration, updating } = useUpdateIntegration()
const { setActive, toggling } = useSetActiveIntegration()
const { deleteIntegration } = useDeleteIntegration()

const dialog = ref(false)
const editing = ref<Integration | null>(null)
const executionsDialog = ref(false)
const executionTarget = ref<Integration | null>(null)
const saving = computed(() => creating.value || updating.value || toggling.value)

function openCreate() {
  editing.value = null
  dialog.value = true
}

function openEdit(integration: Integration) {
  editing.value = integration
  dialog.value = true
}

function openExecutions(integration: Integration) {
  executionTarget.value = integration
  executionsDialog.value = true
}

async function handleSubmit(payload: UpdateIntegrationRequest) {
  try {
    if (editing.value) {
      await updateIntegration({ id: editing.value.id, request: payload })
      ElMessage.success('Integração atualizada. O agendamento vale em até 30s.')
    } else {
      await createIntegration(payload)
      ElMessage.success('Integração criada.')
    }
    dialog.value = false
  } catch {
    // erro já tratado no composable (errorMessage)
  }
}

async function handleToggle(integration: Integration) {
  // PATCH dedicado: só inverte o boolean — sem reenviar payload completo,
  // sem revalidar, sem risco de sobrescrever edição concorrente
  await setActive({ id: integration.id, active: !integration.active })
}

async function handleDelete(integration: Integration) {
  try {
    await ElMessageBox.confirm(`Excluir a integração "${integration.name}"?`, 'Confirmação', {
      type: 'warning',
    })
  } catch {
    return
  }
  await deleteIntegration(integration.id)
  ElMessage.success('Integração excluída.')
}
</script>

<template>
  <ErrorBoundary>
    <div class="view">
      <div class="header">
        <h2><el-icon class="title-icon">
            <Connection />
          </el-icon> Integrações</h2>
        <el-button class="primary-button" type="primary" :icon="DocumentAdd" @click="openCreate">
          Nova integração
        </el-button>
      </div>

      <section v-if="isLoading || saving" class="surface loading">Carregando…</section>
      <section v-else-if="isError" class="surface">Erro ao carregar.</section>
      <section v-else class="surface">
        <IntegrationTable :integrations="integrations ?? []" @edit="openEdit" @executions="openExecutions"
          @toggle="handleToggle" @delete="handleDelete" />
      </section>

      <IntegrationEditorDialog v-model="dialog" :integration="editing" :saving="saving" @submit="handleSubmit" />
      <IntegrationExecutionsDialog v-model="executionsDialog" :integration="executionTarget" />
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.title-icon {
  margin-right: 6px;
  vertical-align: -2px;
}

.loading {
  padding: 28px;
  text-align: center;
  color: #718096;
  font-size: 13px;
}
</style>
