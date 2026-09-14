<script setup lang="ts">
import { Promotion } from '@element-plus/icons-vue'

defineProps<{
  modelValue: string
  loading: boolean
  canSend: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
}>()
</script>

<template>
  <div class="input-area">
    <div class="composer">
      <el-input
        :model-value="modelValue"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 5 }"
        resize="none"
        placeholder="Pergunte algo a base de conhecimento..."
        :disabled="loading"
        @update:model-value="emit('update:modelValue', $event)"
        @keydown.enter.exact.prevent="emit('send')"
      />
      <el-button circle class="send" :disabled="!canSend" :icon="Promotion" @click="emit('send')" />
    </div>
    <p>Enter para enviar · As respostas sao geradas exclusivamente com documentos autorizados.</p>
  </div>
</template>
