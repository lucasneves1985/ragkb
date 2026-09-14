<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useSectors } from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import SectorAdd from '@/components/sectors/SectorAdd.vue'
import SectorTable from '@/components/sectors/SectorTable.vue'
import type { Sector } from '@/types'

const { sectors, saving, errorMessage, loadSectors, createSector, deleteSector, refetch } =
  useSectors()

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

async function remove(s: Sector) {
  try {
    await ElMessageBox.confirm(
      `Excluir o setor "${s.name}"? Usuários e documentos vinculados serão afetados.`,
      'Excluir setor',
      { type: 'warning', confirmButtonText: 'Excluir', cancelButtonText: 'Cancelar' },
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

function reset() {
  refetch()
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <PageHeading
        eyebrow="GOVERNANÇA DA BASE"
        title="Setores"
        description="Defina os departamentos que controlam o acesso a usuários e documentos."
      />

      <section class="surface">
        <SectorAdd v-model="name" :saving="saving" @submit="create" />

        <SectorTable :sectors="sectors" @remove="remove" />
      </section>
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
