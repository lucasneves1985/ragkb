<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { useSectors } from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import type { Sector } from '@/types'

const { sectors, saving, errorMessage, loadSectors, createSector, deleteSector } = useSectors()
const name = ref('')

onMounted(loadSectors)

async function create() {
  const trimmed = name.value.trim()
  if (!trimmed) {
    ElMessage.warning('Informe o nome do setor.')
    return
  }
  const result = await createSector(trimmed)
  if (result) {
    name.value = ''
    ElMessage.success('Setor cadastrado!')
  } else if (errorMessage.value) {
    ElMessage.error(errorMessage.value)
  }
}
function reset() {
  loadSectors()
}

async function remove(s: Sector) {
  try {
    await ElMessageBox.confirm(
      `Excluir o setor "${s.name}"? Usuários e documentos vinculados serão afetados.`,
      'Excluir setor',
      { type: 'warning', confirmButtonText: 'Excluir', cancelButtonText: 'Cancelar' }
    )
  } catch {
    return
  }
  const success = await deleteSector(s.id)
  if (success) {
    ElMessage.success('Setor excluído!')
  } else if (errorMessage.value) {
    ElMessage.error(errorMessage.value)
  }
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <div class="page-heading">
        <div>
          <p class="eyebrow">GOVERNANÇA DA BASE</p>
          <h1>Setores</h1>
          <p>Defina os departamentos que controlam o acesso a usuários e documentos.</p>
        </div>
      </div>

      <section class="surface">
        <div class="sector-add">
          <el-input v-model="name" placeholder="Digite o nome do novo setor…" clearable @keyup.enter="create" />
          <el-button class="primary-button" :icon="Plus" :loading="saving" @click="create">
            Incluir
          </el-button>
        </div>

        <el-table :data="sectors" style="width: 100%" empty-text="Nenhum setor cadastrado ainda.">
          <el-table-column prop="name" label="Nome do setor" min-width="200" />
          <el-table-column label="Ações" width="120" align="right">
            <template #default="{ row }">
              <el-button text type="danger" :icon="Delete" @click="remove(row)">Remover</el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </ErrorBoundary>
</template>

<style scoped lang="css" src="@/views/styles/sector.view.css"></style>
