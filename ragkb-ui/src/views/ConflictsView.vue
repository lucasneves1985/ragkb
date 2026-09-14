<script setup lang="ts">
import { computed, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  useConflicts,
  useScanConflicts,
  useUpdateConflict,
  useDocuments,
} from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import ConflictsQueue from '@/components/conflicts/ConflictsQueue.vue'
import ConflictReview from '@/components/conflicts/ConflictReview.vue'
import type { ConflictActionRequest } from '@/types'

const { conflicts, isLoading, isError, error, refetch } = useConflicts()
const { documents } = useDocuments()
const { scanConflicts, scanning } = useScanConflicts()
const { updateConflict } = useUpdateConflict()

const selected = ref<number | undefined>()

const current = computed(() => conflicts.value?.find((c) => c.id === selected.value))

async function scan() {
  await scanConflicts()
}

async function handleResolve(payload: {
  action: ConflictActionRequest['action']
  note?: string
}) {
  if (!current.value) return
  try {
    await updateConflict({
      id: current.value.id,
      action: payload.action,
      note: payload.note,
    })
  } catch {
    ElMessage.error('Não foi possível atualizar o conflito.')
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
        eyebrow="QUALIDADE DA BASE"
        title="Triagem de conflitos"
        description="Analise potenciais divergências encontradas entre documentos ativos."
      >
        <template #actions>
          <el-button class="primary-button" :icon="Refresh" :loading="scanning" @click="scan">
            Escanear conflitos
          </el-button>
        </template>
      </PageHeading>

      <section v-if="isLoading" class="surface">
        <el-skeleton :rows="5" animated />
      </section>

      <section v-else-if="isError" class="surface error-state">
        <el-alert type="error" :title="error?.message || 'Erro ao carregar conflitos.'" show-icon />
      </section>

      <div v-else-if="conflicts && conflicts.length > 0" class="conflicts">
        <ConflictsQueue
          :conflicts="conflicts"
          :selected-id="selected"
          :documents="documents ?? []"
          @select="selected = $event"
        />

        <ConflictReview
          v-if="current"
          :conflict="current"
          :documents="documents ?? []"
          @resolve="handleResolve"
        />
      </div>

      <section v-else class="surface">
        <p class="empty-state">Nenhum conflito encontrado. Execute um scan.</p>
      </section>
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.conflicts {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
}

.error-state {
  padding: 18px;
}

.empty-state {
  padding: 28px;
  color: #718096;
  font-size: 13px;
  text-align: center;
}

@media (max-width: 850px) {
  .conflicts {
    grid-template-columns: 1fr;
  }
}
</style>
