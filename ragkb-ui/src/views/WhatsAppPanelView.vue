<!-- views/WhatsAppPanelView.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useStartWhatsAppSession, useTestWhatsAppSend, useWhatsAppStatus } from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const { status, isLoadingStatus, isErrorStatus } = useWhatsAppStatus()
const { startSession, starting, errorMessage: startError } = useStartWhatsAppSession()
const { testSend, sending, errorMessage: sendError } = useTestWhatsAppSend()

const chatId = ref('')
const message = ref('')

async function handleStart() {
  await startSession()
  if (!startError.value) {
    ElMessage.info('Sessão iniciada. Escaneie o QR Code no Swagger do WAHA (porta 3000).')
  } else {
    ElMessage.error(startError.value)
  }
}

async function handleTestSend() {
  if (!chatId.value.trim() || !message.value.trim()) {
    return ElMessage.error('Informe chatId e mensagem.')
  }
  await testSend({ chatId: chatId.value.trim(), message: message.value.trim() })
  if (!sendError.value) {
    ElMessage.success('Mensagem enviada.')
    message.value = ''
  } else {
    ElMessage.error(sendError.value)
  }
}
</script>

<template>
  <ErrorBoundary>
    <div class="view">
      <div class="header">
        <h2><el-icon class="title-icon">
            <ChatDotRound />
          </el-icon> WhatsApp</h2>
      </div>

      <section class="surface">
        <div v-if="isLoadingStatus" class="loading">Verificando sessão…</div>
        <template v-else-if="isErrorStatus || !status">
          <el-alert type="warning" :closable="false" title="Sessão não encontrada ou não emparelhada"
            description="Inicie a sessão e escaneie o QR Code no Swagger do WAHA." />
        </template>
        <template v-else>
          <el-alert :type="status.working ? 'success' : 'error'" :closable="false"
            :title="status.working ? 'Conectado' : 'Não conectado'"
            :description="`Status da sessão: ${status.status}`" />
        </template>
        <el-button class="start-button" type="primary" plain :loading="starting" @click="handleStart">
          Iniciar / reemparelhar sessão
        </el-button>
      </section>

      <section class="surface">
        <h4>Envio de teste (ADMIN)</h4>
        <el-input v-model="chatId" placeholder="55DDNNNNNNNNN@c.us" class="field" />
        <el-input v-model="message" type="textarea" :rows="2" placeholder="Mensagem de teste" class="field" />
        <el-button type="primary" :loading="sending" @click="handleTestSend">Enviar teste</el-button>
      </section>
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.header {
  margin-bottom: 12px;
}

.title-icon {
  margin-right: 6px;
  vertical-align: -2px;
}

.loading {
  padding: 12px;
  color: #718096;
}

.start-button {
  margin-top: 12px;
}

.field {
  margin-bottom: 12px;
}
</style>
