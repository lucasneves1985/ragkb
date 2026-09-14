<script setup lang="ts">
import type { Conflict, KnowledgeDocument } from '@/types'

const props = defineProps<{
  conflicts: Conflict[]
  selectedId?: number
  documents: KnowledgeDocument[]
}>()

defineEmits<{
  (e: 'select', id: number): void
}>()

function tagType(status: string) {
  switch (status) {
    case 'OPEN':
      return 'danger'
    case 'RESOLVED':
      return 'success'
    case 'REVIEWED':
      return 'primary'
    default:
      return 'info'
  }
}

function filename(id: string) {
  return props.documents.find((d) => d.id === id)?.filename
}
</script>

<template>
  <section class="surface queue">
    <div class="queue-head">
      <b>Fila de análise</b>
      <span>{{ conflicts.length }} encontrados</span>
    </div>

    <button
      v-for="item in conflicts"
      :key="item.id"
      class="conflict-row"
      :class="{ selected: item.id === selectedId }"
      @click="$emit('select', item.id)"
    >
      <span class="score">{{ Math.round((item.similarityScore ?? 0) * 100) }}%</span>
      <div>
        <b>{{ filename(item.documentIdA) }}</b>
        <small>vs. {{ filename(item.documentIdB) }}</small>
      </div>
      <el-tag size="small" :type="tagType(item.status)">{{ item.status }}</el-tag>
    </button>
  </section>
</template>

<style scoped>
.queue {
  overflow: hidden;
}

.queue-head {
  display: flex;
  justify-content: space-between;
  padding: 18px;
  border-bottom: 1px solid #edf0f4;
  color: #263248;
  font-size: 13px;
}

.queue-head span {
  color: #8793a4;
  font-size: 11px;
}

.conflict-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 15px;
  border: 0;
  border-bottom: 1px solid #edf0f4;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.conflict-row.selected {
  background: #f0faf6;
}

.score {
  display: grid;
  place-items: center;
  width: 38px;
  height: 30px;
  border-radius: 6px;
  background: #fff4e6;
  color: #c27a17;
  font: 500 11px 'DM Mono';
}

.conflict-row b,
.conflict-row small {
  display: block;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conflict-row b {
  color: #405068;
  font-size: 11px;
}

.conflict-row small {
  margin-top: 3px;
  color: #8d99aa;
  font-size: 10px;
}

.conflict-row :deep(.el-tag) {
  margin-left: auto;
}
</style>
