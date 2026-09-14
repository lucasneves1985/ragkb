<!-- components/ErrorBoundary.vue -->
<script setup lang="ts">
import { ref, onErrorCaptured } from 'vue'
import { ElButton } from 'element-plus'

const error = ref<Error | null>(null)

onErrorCaptured((err) => {
  error.value = err as Error
  // Retorna false para parar a propagação
  return false
})

function retry() {
  error.value = null
  // Força remount do slot emitindo uma key
  emit('retry')
}

const emit = defineEmits<{ retry: [] }>()
</script>

<template>
  <div v-if="error" class="error-boundary">
    <div class="error-content">
      <h2>Algo deu errado nesta seção</h2>
      <p class="error-message">{{ error.message }}</p>
      <ElButton type="primary" @click="retry">Tentar novamente</ElButton>
    </div>
  </div>
  <slot v-else />
</template>

<style scoped>
.error-boundary {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  padding: 24px;
}
.error-content {
  text-align: center;
  max-width: 400px;
}
.error-message {
  color: #718096;
  margin: 12px 0 20px;
  font-family: 'DM Mono', monospace;
  font-size: 13px;
}
</style>
