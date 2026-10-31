<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCertificateMonitors } from '@/composables'
import type { CertificateMonitor, CertificateMonitorRequest } from '@/types'
import CertificateMonitorTable from '@/components/certificate-monitors/CertificateMonitorTable.vue'
import CertificateMonitorEditorDialog from '@/components/certificate-monitors/CertificateMonitorEditorDialog.vue'
import CertificateMonitorHistoryDialog from '@/components/certificate-monitors/CertificateMonitorHistoryDialog.vue'

const { monitors, isLoading, isError, refetch, createMonitor, updateMonitor, deleteMonitor, checkNow, isSaving, isChecking } = useCertificateMonitors()
const editorOpen = ref(false), historyOpen = ref(false), selected = ref<CertificateMonitor | null>(null)
const saving = computed(() => isSaving.value)

function openCreate() { selected.value = null; editorOpen.value = true }
function openEdit(monitor: CertificateMonitor) { selected.value = monitor; editorOpen.value = true }
function openHistory(monitor: CertificateMonitor) { selected.value = monitor; historyOpen.value = true }
async function submit(request: CertificateMonitorRequest) { try { if (selected.value) await updateMonitor({ id: selected.value.id, request }); else await createMonitor(request); ElMessage.success('Monitor salvo.'); editorOpen.value = false } catch { ElMessage.error('Não foi possível salvar o monitor.') } }
async function remove(monitor: CertificateMonitor) { try { await ElMessageBox.confirm(`Excluir o monitor de ${monitor.url}?`, 'Confirmação', { type: 'warning' }); await deleteMonitor(monitor.id); ElMessage.success('Monitor excluído.') } catch {/* cancelamento ou erro já apresentado pelo diálogo */ } }
async function runCheck(monitor: CertificateMonitor) { try { await checkNow(monitor.id); ElMessage.success('Checagem concluída.'); await refetch() } catch { ElMessage.error('Não foi possível verificar o certificado.') } }
</script>

<template>
  <div class="view">
    <div class="header">
      <div>
        <h2>Certificados SSL/TLS</h2>
        <p>Acompanhe vencimentos e receba alertas no horário configurado.</p>
      </div><el-button type="primary" class="primary-button" @click="openCreate">Adicionar URL</el-button>
    </div>
    <section v-if="isLoading" class="surface loading">Carregando…</section>
    <section v-else-if="isError" class="surface error"><span>Não foi possível carregar os monitores.</span><el-button
        text @click="refetch()">Tentar novamente</el-button></section>
    <section v-else class="surface">
      <CertificateMonitorTable :monitors="monitors ?? []" :checking="isChecking" @check="runCheck" @history="openHistory"
        @edit="openEdit" @delete="remove" />
    </section>
    <CertificateMonitorEditorDialog v-model="editorOpen" :monitor="selected" :saving="saving" @submit="submit" />
    <CertificateMonitorHistoryDialog v-model="historyOpen" :monitor="selected" />
  </div>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px
}

.header p {
  color: #718096;
  margin: 6px 0 0
}

.surface {
  background: white;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 12px #1b2d4810
}

.loading,
.error {
  padding: 28px;
  text-align: center;
  color: #718096
}

.error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px
}
</style>
