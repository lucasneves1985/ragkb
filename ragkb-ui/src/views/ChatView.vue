<script setup lang="ts">
import { onMounted } from 'vue'
import { useChat, useConversations } from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import ChatSidebar from '@/components/chat/ChatSidebar.vue'
import ChatMessageList from '@/components/chat/ChatMessageList.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import { ref } from 'vue'

const showHistory = ref(true)

const {
  auth,
  question,
  loading,
  messages,
  ticket,
  currentConversationId,
  canSend,
  selectConversation,
  startNewConversation,
  ask,
  openTicket,
} = useChat()

const { conversations, loadConversations, deleteConversation } = useConversations()

async function handleAsk() {
  await ask(() => loadConversations())
}

async function removeConversation(id: string, event: Event) {
  event.stopPropagation()
  const success = await deleteConversation(id)
  if (success && currentConversationId.value === id) {
    startNewConversation()
  }
}

onMounted(() => {
  loadConversations()
})

function reset() {
  loadConversations()
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div class="chat-layout">
      <ChatSidebar
        :conversations="conversations"
        :current-conversation-id="currentConversationId"
        :show-history="showHistory"
        @update:show-history="showHistory = $event"
        @select="selectConversation"
        @delete="removeConversation"
        @new="startNewConversation"
      />

      <section class="chat-main">
        <button v-if="!showHistory" class="show-side" @click="showHistory = true">
          &#9776; Historico
        </button>
        <div class="chat-welcome">
          <p class="eyebrow">ASSISTENTE VIRTUAL</p>
          <h1>Como posso ajudar?</h1>
          <p>Faca perguntas sobre normas, politicas e processos internos.</p>
        </div>

        <ChatMessageList
          :messages="messages"
          :loading="loading"
          :initials="auth.initials"
          :ticket="ticket"
          @update:ticket="Object.assign(ticket, $event)"
          @open-ticket="openTicket"
        />

        <ChatInput
          v-model="question"
          :loading="loading"
          :can-send="canSend"
          @send="handleAsk"
        />
      </section>
    </div>
  </ErrorBoundary>
</template>

<style lang="css" src="@/views/styles/chat.view.css"></style>
