<!-- components/chat/ChatMessageList.vue -->
<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { Document, Link, Tickets } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import type { Message, SourceReference } from '@/types'

const props = defineProps<{
  messages: Message[]
  loading: boolean
  initials: string
  ticket: { subject: string; description: string }
}>()

const emit = defineEmits<{
  (e: 'open-ticket', message: Message): void
  (e: 'update:ticket', ticket: { subject: string; description: string }): void
}>()

const container = ref<HTMLElement | null>(null)

const md = new MarkdownIt({ breaks: true, html: false })
function renderMarkdown(content?: string) {
  if (!content) return ''
  return DOMPurify.sanitize(md.render(content))
}

function isArticleSource(source: string | SourceReference): source is SourceReference {
  return typeof source !== 'string' && source.type === 'ARTICLE'
}

function sourceLabel(source: string | SourceReference): string {
  return typeof source === 'string' ? source : source.label
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
</script>

<template>
  <div ref="container" class="messages">
    <article v-for="message in messages" :key="message.id" class="message" :class="message.from">
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
          <el-alert v-if="message.ticketSent"
            title="Chamado aberto com sucesso. Voce recebera atualizacoes pelo canal corporativo." type="success"
            :closable="false" show-icon />
          <el-form v-else label-position="top" class="ticket-form">
            <div class="ticket-fields">
              <el-form-item label="Assunto">
                <el-input :model-value="ticket.subject"
                  @update:model-value="emit('update:ticket', { ...ticket, subject: $event })" />
              </el-form-item>
            </div>
            <el-form-item label="Descricao">
              <el-input :model-value="ticket.description" type="textarea" :rows="3"
                @update:model-value="emit('update:ticket', { ...ticket, description: $event })" />
            </el-form-item>
            <el-button class="primary-button" @click="emit('open-ticket', message)">
              Abrir chamado tecnico
            </el-button>
          </el-form>
        </template>

        <div v-if="message.sources?.length" class="sources">
          <span>FONTES CONSULTADAS</span>
          <el-tag v-for="(source, index) in message.sources" :key="index" size="small" effect="plain">
            <template v-if="isArticleSource(source)">
              <Link />
              <a v-if="source.url" :href="source.url" target="_blank" class="source-link">
                {{ source.label }}
              </a>
              <span v-else>{{ source.label }}</span>
            </template>
            <template v-else>
              <Document /> {{ sourceLabel(source) }}
            </template>
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

<style scoped>
.source-link {
  color: #168463;
  text-decoration: none;
}

/* Mata o roxo de link visitado — sem isso o browser vence a cor do el-tag */
.source-link:visited {
  color: #168463;
  text-decoration: none;
}

.source-link:hover,
.source-link:focus {
  color: #168463;
  text-decoration: underline;
}

/* Links markdown no corpo da mensagem também ficam roxos quando visitados */
.bubble :deep(a),
.bubble :deep(a:visited) {
  color: #168463;
}
</style>
