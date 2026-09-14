<script setup lang="ts">
import { ref, computed } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import { ElButton, ElTag, ElInput } from 'element-plus'
import {
  useConflicts,
  useScanConflicts,
  useUpdateConflict,
  useDocuments,
} from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const { conflicts, isLoading, isError, error, refetch } = useConflicts()
const { documents } = useDocuments()
const { scanConflicts, scanning } = useScanConflicts()
const { updateConflict } = useUpdateConflict()

const selected = ref<number | undefined>()
const note = ref('')

const current = computed(() =>
  conflicts.value?.find((c) => c.id === selected.value),
)

function name(id: string) {
  return documents.value?.find((d) => d.id === id)?.filename
}

function tagType(status: string) {
  switch (status) {
    case 'OPEN': return 'danger'
    case 'RESOLVED': return 'success'
    case 'REVIEWED': return 'primary'
    default: return 'info'
  }
}

async function update(action: 'DISMISSED' | 'REVIEWED' | 'RESOLVED') {
  if (!current.value || current.value.status !== 'OPEN') return
  if (action === 'DISMISSED' && !note.value.trim()) return
  await updateConflict({
    id: current.value.id,
    action,
    note: note.value || undefined,
  })
}

async function scan() {
  await scanConflicts()
}

function reset() {
  refetch()
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <div class="page-heading">
        <div>
          <h2>Conflitos</h2>
          <p>Documentos com sobreposição semântica</p>
        </div>
        <ElButton type="primary" :loading="scanning" @click="scan">
          {{ scanning ? 'Escaneando…' : 'Escanear conflitos' }}
        </ElButton>
      </div>

      <!-- loading -->
      <section v-if="isLoading" class="surface">
        <el-skeleton :rows="5" animated />
      </section>

      <!-- error -->
      <section v-else-if="isError" class="surface">
        <el-alert type="error" :title="error?.message || 'Erro ao carregar conflitos.'" show-icon>
          <ElButton text @click="reset">Tentar novamente</ElButton>
        </el-alert>
      </section>

      <!-- sucesso -->
      <div v-else-if="conflicts?.length" class="conflicts-layout">
        <section class="surface conflict-list">
          <button
            v-for="item in conflicts"
            :key="item.id"
            class="conflict-row"
            :class="{ selected: item.id === selected }"
            @click="selected = item.id"
          >
            <span class="score">{{ (item.similarityScore ?? 0).toFixed(2) }}</span>
            <div>
              <b>{{ name(item.documentIdA) }}</b>
              <small>vs. {{ name(item.documentIdB) }}</small>
            </div>
            <ElTag size="small" :type="tagType(item.status)">{{ item.status }}</ElTag>
          </button>
        </section>

        <section v-if="current" class="surface review">
          <div>
            <p class="eyebrow">CONFLITO #{{ current.id }}</p>
            <WarningFilled />
          </div>
          <div class="diff">
            <article>
              <header>DOCUMENTO A <b>{{ name(current.documentIdA) }}</b></header>
              <p>{{ current.snippetA }}</p>
            </article>
            <article>
              <header>DOCUMENTO B <b>{{ name(current.documentIdB) }}</b></header>
              <p>{{ current.snippetB }}</p>
            </article>
          </div>
          <el-alert type="warning" :closable="false" show-icon />
          <ElInput v-model="note" placeholder="Nota (opcional para DISMISSED)" />
          <div>
            <ElButton :disabled="current.status !== 'OPEN'" @click="update('REVIEWED')">
              Marcar como revisado
            </ElButton>
            <ElButton :disabled="current.status !== 'OPEN'" @click="update('DISMISSED')">
              Descartar
            </ElButton>
          </div>
        </section>
      </div>

      <!-- empty state -->
      <section v-else class="surface">
        <p>Nenhum conflito encontrado. Execute um scan.</p>
      </section>
    </div>
  </ErrorBoundary>
</template>

<style scoped lang="css" src="@/views/styles/conflicts.view.css"></style>
