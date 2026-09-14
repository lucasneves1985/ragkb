<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Document, Tickets } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import type { Message } from '@/types'

const props = defineProps<{
  messages: Message[]
  loading: boolean
  initials: string
  ticket: { subject: string; description: string }
}>()

const emit = defineEmits<{
  'open-ticket': [message: Message]
  'update:ticket': [ticket: { subject: string; description: string }]
}>()

const container = ref<HTMLElement | null>(null)

const md = new MarkdownIt({ breaks: true, html: false })
function renderMarkdown(content?: string) {
  if (!content) return ''
  return DOMPurify.sanitize(md.render(content))
}

function scrollToBottom() {
  nextTick(() => {
    if (container.value) {
      container.value.scrollTop = container.value.scrollHeight
    }
  })
}

watch(() => props.messages.length, scrollToBottom)
watch(() => props.loading, scrollToBottom)

defineExpose({ scrollToBottom })

const ticketSubject = {
  get: () => props.ticket.subject,
  set: (v: string) => emit('update:ticket', { ...props.ticket, subject: v }),
}
const ticketDescription = {
  get: () => props.ticket.description,
  set: (v: string) => emit('update:ticket', { ...props.ticket, description: v }),
}
</script>

<template>
  <div ref="container" class="messages">
    <article
      v-for="message in messages"
      :key="message.id"
      class="message"
      :class="message.from"
    >
      <div class="avatar">{{ message.from === 'user' ? initials : 'R' }}</div>
      <div class="bubble">
        <div v-if="message.text" class="message-text" v-html="renderMarkdown(message.text)"></div>

        <template v-if="message.ticket">
          <div class="fallback-head">
            <Tickets />
            <div>
              <b>Conhecimento nao localizado</b>
              <p>Posso encaminhar esta solicitacao ao suporte tecnico.</p>
            </div>
          </div>
          <el-alert
            v-if="message.ticketSent"
            title="Chamado aberto com sucesso. Voce recebera atualizacoes pelo canal corporativo."
            type="success"
            :closable="false"
            show-icon
          />
          <el-form v-else label-position="top" class="ticket-form">
            <div class="ticket-fields">
              <el-form-item label="Assunto">
                <el-input
                  :model-value="ticket.subject"
                  @update:model-value="emit('update:ticket', { ...ticket, subject: $event })"
                />
              </el-form-item>
            </div>
            <el-form-item label="Descricao">
              <el-input
                :model-value="ticket.description"
                type="textarea"
                :rows="3"
                @update:model-value="emit('update:ticket', { ...ticket, description: $event })"
              />
            </el-form-item>
            <el-button class="primary-button" @click="emit('open-ticket', message)">
              Abrir chamado tecnico
            </el-button>
          </el-form>
        </template>

        <div v-if="message.sources?.length" class="sources">
          <span>FONTES CONSULTADAS</span>
          <el-tag v-for="source in message.sources" :key="source" size="small" effect="plain">
            <Document /> {{ source }}
          </el-tag>
        </div>
      </div>
    </article>

    <div v-if="loading" class="message assistant">
      <div class="avatar">R</div>
      <div class="bubble typing"><i></i><i></i><i></i></div>
    </div>
  </div>
</template>
