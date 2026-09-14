<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Refresh, WarningFilled } from '@element-plus/icons-vue'
import { useConflicts, useDocuments } from '@/composables'

const { conflicts, scanning, loadConflicts, scanConflicts, updateConflict } = useConflicts()
const { documents, loadDocuments } = useDocuments()

const selected = ref<number | undefined>()
const note = ref('')

const current = computed(() => conflicts.value.find((c) => c.id === selected.value) || conflicts.value[0])
watch(
  current,
  (newVal) => {
    note.value = newVal?.note || ''
  },
  { immediate: true },
)

function name(id: string) {
  return documents.value.find((d) => d.id === id)?.filename || id
}

function reset() {
  selected.value = undefined
  note.value = ''
  load()
}

function tagType(status: string) {
  switch (status) {
    case 'OPEN':
      return 'warning'
    case 'RESOLVED':
      return 'success'
    case 'REVIEWED':
      return 'primary'
    default:
      return 'info'
  }
}

async function load() {
  await Promise.all([loadConflicts(), loadDocuments()])
  selected.value ??= conflicts.value[0]?.id
}

onMounted(load)

async function scan() {
  await scanConflicts()
  if (!conflicts.value.some((c) => c.id === selected.value)) {
    selected.value = conflicts.value[0]?.id
  }
}

async function update(action: 'DISMISSED' | 'REVIEWED' | 'RESOLVED') {
  if (!current.value || current.value.status !== 'OPEN' || (action === 'DISMISSED' && !note.value.trim())) return
  await updateConflict(current.value.id, action, note.value || undefined)
}
</script>
<template>
  <ErrorBoundary @retry="reset">
    <div>
      <div class="page-heading">
        <div>
          <p class="eyebrow">QUALIDADE DA BASE</p>
          <h1>Triagem de conflitos</h1>
          <p>Analise potenciais divergências encontradas entre documentos ativos.</p>
        </div><el-button class="primary-button" :loading="scanning" :icon="Refresh" @click="scan">Executar nova
          varredura</el-button>
      </div>
      <div class="conflicts">
        <section class="surface queue">
          <div class="queue-head"><b>Fila de análise</b><span>{{ conflicts.length }} encontrados</span></div>
          <button v-for="item in conflicts" :key="item.id" class="conflict-row"
            :class="{ selected: item.id === selected }" @click="selected = item.id"><span class="score">{{
              Math.round(item.score * 100) }}%</span>
            <div><b>{{ name(item.documentIdA) }}</b><small>vs. {{ name(item.documentIdB) }}</small></div><el-tag
              size="small" :type="tagType(item.status)">{{ item.status }}</el-tag>
          </button>
        </section>
        <section v-if="current" class="surface review">
          <div class="review-title">
            <div>
              <p class="eyebrow">CONFLITO #{{ current.id }}</p>
              <h2>Comparação semântica <el-tag type="warning" effect="light">{{ Math.round(current.score *
                100) }}% de
                  similaridade</el-tag></h2>
            </div>
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
          </div><el-alert title="Para resolver, arquive ou substitua um dos documentos envolvidos antes de confirmar."
            type="warning" :closable="false" show-icon />
          <div class="resolution"><el-input v-model="note" type="textarea" :rows="2" :disabled="current.status !== 'OPEN'"
              placeholder="Justificativa obrigatória para descartar…" />
            <div><el-button :disabled="current.status !== 'OPEN'" @click="update('REVIEWED')">Marcar como
                analisado</el-button><el-button type="warning" plain :disabled="current.status !== 'OPEN'"
                @click="update('DISMISSED')">Descartar</el-button><el-button class="primary-button"
                :disabled="current.status !== 'OPEN'" @click="update('RESOLVED')">Resolver após
                arquivamento</el-button>
            </div>
          </div>
        </section>
      </div>
    </div>
  </ErrorBoundary>
</template>
<style scoped lang="css" src="@/views/styles/conflicts.view.css"></style>
