<script setup lang="ts">
import { ref } from 'vue'
import { WarningFilled } from '@element-plus/icons-vue'
import type { Conflict, ConflictActionRequest, KnowledgeDocument } from '@/types'

const props = defineProps<{
  conflict: Conflict
  documents: KnowledgeDocument[]
}>()

const emit = defineEmits<{
  (e: 'resolve', payload: { action: ConflictActionRequest['action']; note?: string }): void
}>()

const note = ref('')

function filename(id: string) {
  return props.documents.find((d) => d.id === id)?.filename
}

function update(action: ConflictActionRequest['action']) {
  if (props.conflict.status !== 'OPEN') return
  if (action === 'DISMISSED' && !note.value.trim()) return
  emit('resolve', { action, note: note.value || undefined })
}
</script>

<template>
  <section class="surface review">
    <div class="review-title">
      <div>
        <p class="eyebrow">CONFLITO #{{ conflict.id }}</p>
        <h2>
          Comparação semântica
          <el-tag type="warning" effect="light">
            {{ Math.round((conflict.similarityScore ?? 0) * 100) }}% de similaridade
          </el-tag>
        </h2>
      </div>
      <WarningFilled />
    </div>

    <div class="diff">
      <article>
        <header>DOCUMENTO A <b>{{ filename(conflict.documentIdA) }}</b></header>
        <p>{{ conflict.snippetA }}</p>
      </article>
      <article>
        <header>DOCUMENTO B <b>{{ filename(conflict.documentIdB) }}</b></header>
        <p>{{ conflict.snippetB }}</p>
      </article>
    </div>

    <el-alert
      title="Para resolver, arquive ou substitua um dos documentos envolvidos antes de confirmar."
      type="warning"
      :closable="false"
      show-icon
    />

    <div class="resolution">
      <el-input
        v-model="note"
        type="textarea"
        :rows="2"
        :disabled="conflict.status !== 'OPEN'"
        placeholder="Justificativa obrigatória para descartar…"
      />
      <div>
        <el-button :disabled="conflict.status !== 'OPEN'" @click="update('REVIEWED')">
          Marcar como analisado
        </el-button>
        <el-button type="warning" plain :disabled="conflict.status !== 'OPEN'" @click="update('DISMISSED')">
          Descartar
        </el-button>
        <el-button class="primary-button" :disabled="conflict.status !== 'OPEN'" @click="update('RESOLVED')">
          Resolver após arquivamento
        </el-button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.review {
  padding: 25px;
}

.review-title {
  display: flex;
  justify-content: space-between;
}

.review-title h2 {
  margin: 0;
  color: #202b3c;
  font-size: 19px;
}

.review-title > :deep(svg) {
  width: 26px;
  color: #dc952b;
}

.diff {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin: 23px 0;
}

.diff article {
  border: 1px solid #e8ebf0;
  border-radius: 10px;
  overflow: hidden;
}

.diff header {
  padding: 10px 12px;
  background: #f8fafb;
  color: #78869a;
  font: 10px 'DM Mono';
  letter-spacing: 0.4px;
}

.diff header b {
  display: block;
  margin-top: 4px;
  color: #405069;
  font-family: Manrope;
}

.diff p {
  min-height: 90px;
  margin: 0;
  padding: 15px;
  color: #3d4b60;
  font-size: 13px;
  line-height: 1.65;
}

.resolution {
  margin-top: 15px;
}

.resolution > div {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  margin-top: 12px;
}

@media (max-width: 850px) {
  .diff {
    grid-template-columns: 1fr;
  }

  .resolution > div {
    flex-wrap: wrap;
  }
}
</style>
