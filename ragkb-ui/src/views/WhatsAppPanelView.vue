<!-- views/WhatsAppPanelView.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  useStartWhatsAppSession,
  useTestWhatsAppSend,
  useWhatsAppQr,
  useWhatsAppStatus,
} from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'

const { status, isLoadingStatus, isErrorStatus, refetchStatus } = useWhatsAppStatus()
const { startSession, starting, errorMessage: startError } = useStartWhatsAppSession()
const { testSend, sending, errorMessage: sendError } = useTestWhatsAppSend()

// QR só é buscado quando a sessão NÃO está conectada; renova a cada 25s
// (o QR da WAHA expira: 60s o primeiro, 20s os subsequentes).
// A renovação é 100% do composable (refetchInterval + enabled) — sem refetch manual.
const needsQr = computed(() => !(status.value?.working ?? false))
const { qr } = useWhatsAppQr(needsQr)

const qrSrc = computed(() =>
  qr.value ? `data:${qr.value.mimetype};base64,${qr.value.data}` : '',
)

const chatId = ref('')
const message = ref('')

async function handleStart() {
  await startSession()
  if (!startError.value) {
    ElMessage.info('Sessão iniciada. Escaneie o QR Code abaixo.')
    await refetchStatus()
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

      <section class="surface card">
        <h3 class="card-title">Status da sessão</h3>
        <div v-if="isLoadingStatus" class="loading">Verificando sessão…</div>
        <template v-else-if="isErrorStatus || !status">
          <el-alert type="warning" :closable="false" class="card-alert" title="Sessão não encontrada ou não emparelhada"
            description="Clique em iniciar e escaneie o QR Code abaixo." />
        </template>
        <template v-else>
          <el-alert :type="status.working ? 'success' : 'error'" :closable="false" class="card-alert"
            :title="status.working ? 'Conectado' : 'Não conectado'"
            :description="`Status da sessão: ${status.status}`" />
        </template>
        <el-button class="start-button" type="primary" plain :loading="starting" @click="handleStart">
          Iniciar / reemparelhar sessão
        </el-button>
      </section>

      <!-- QR de emparelhamento: visível apenas quando a sessão não está conectada -->
      <section v-if="needsQr" class="surface card qr-card">
        <h3 class="card-title">Escaneie para conectar</h3>
        <div class="qr-wrapper">
          <img v-if="qrSrc" :src="qrSrc" alt="QR Code de emparelhamento WhatsApp" class="qr-image" />
          <div v-else class="qr-placeholder">Gerando QR Code…</div>
        </div>
        <p class="qr-hint">
          O QR Code expira em ~60s e é renovado automaticamente a cada 25s.
          Abra o WhatsApp no celular &gt; Aparelhos conectados &gt; Conectar aparelho.
        </p>
      </section>

      <section class="surface card">
        <h3 class="card-title">Envio de teste</h3>
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

.card {
  padding: 20px 24px;
  margin-bottom: 12px;
}

.card-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.card-alert {
  width: 100%;
}

.loading {
  padding: 12px 0;
  color: #718096;
}

.start-button {
  margin-top: 14px;
}

.qr-card {
  text-align: center;
}

.qr-wrapper {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.qr-image {
  width: 240px;
  height: 240px;
  border-radius: 8px;
  background: #fff;
  padding: 8px;
}

.qr-placeholder {
  width: 240px;
  height: 240px;
  display: grid;
  place-items: center;
  border: 1px dashed #c0c8d4;
  border-radius: 8px;
  color: #718096;
  font-size: 13px;
}

.qr-hint {
  margin: 10px auto 0;
  max-width: 420px;
  font-size: 12px;
  color: #718096;
}

.field {
  margin-bottom: 12px;
}
</style>
