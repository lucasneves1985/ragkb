<!-- components/cards/CardsAssistantWidget.vue -->
<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { ChatDotRound, Close, Promotion } from '@element-plus/icons-vue'
import { useProjects, useAskCards, extractCardsErrorMessage } from '@/composables'
import { CARD_STATUS_COLORS, CARD_STATUS_LABELS } from '@/types'
import type { CardReference } from '@/types'

/**
 * Widget flutuante de consulta de tarefas (estilo chat de suporte).
 * Vive apenas dentro do módulo de cards. Referências clicáveis emitem
 * open-card — o CardsView abre o detalhe do card no board, sem navegação.
 * O composer segue o mesmo padrão visual do ChatInput da KB (textarea
 * auto-expansível + botão circular de envio sobreposto).
 * Sem persistência: o histórico do widget vive só na sessão da página.
 */
const emit = defineEmits<{
  (e: 'open-card', cardId: string): void
}>()

const open = ref(false)
const { data: projects } = useProjects()
const { mutateAsync: askCards, isPending: loading } = useAskCards()

const projectId = ref<string>()
const question = ref('')
const inputEl = ref<{ focus: () => void }>()
const listEl = ref<HTMLElement>()

interface WidgetMessage {
  from: 'user' | 'assistant'
  text: string
  references?: CardReference[]
}

const messages = ref<WidgetMessage[]>([])

const canSend = computed(() => question.value.trim().length > 0 && !loading.value)

watch(open, (value) => {
  if (value) {
    nextTick(() => inputEl.value?.focus())
  }
})

function toggle() {
  open.value = !open.value
}

function scrollToBottom() {
  nextTick(() => {
    if (listEl.value) {
      listEl.value.scrollTop = listEl.value.scrollHeight
    }
  })
}

async function ask() {
  const text = question.value.trim()
  if (!text || loading.value) return
  messages.value.push({ from: 'user', text })
  question.value = ''
  scrollToBottom()
  try {
    const response = await askCards({ question: text, projectId: projectId.value })
    messages.value.push({
      from: 'assistant',
      text: response.answer,
      references: response.cards ?? [],
    })
  } catch (error) {
    messages.value.push({
      from: 'assistant',
      text: extractCardsErrorMessage(error),
    })
  }
  scrollToBottom()
}

function openCard(reference: CardReference) {
  emit('open-card', reference.cardId)
  open.value = false
}
</script>

<template>
  <!-- Botão flutuante -->
  <button type="button" class="assistant-fab" title="Consultar tarefas" @click="toggle">
    <el-icon v-if="!open">
      <ChatDotRound />
    </el-icon>
    <el-icon v-else>
      <Close />
    </el-icon>
  </button>

  <!-- Painel de chat -->
  <transition name="widget-fade">
    <div v-if="open" class="assistant-widget">
      <header class="assistant-widget-header">
        <el-icon>
          <ChatDotRound />
        </el-icon>
        <span class="title">Consulta de tarefas</span>
        <el-button size="small" text class="widget-close" @click="open = false">
          <el-icon>
            <Close />
          </el-icon>
        </el-button>
      </header>

      <div ref="listEl" class="assistant-widget-body">
        <div v-if="!messages.length" class="widget-empty">
          Pergunte em linguagem natural sobre suas tarefas.<br />
          Ex.: "Como está a tarefa da nota fiscal?"
        </div>

        <div v-for="(message, index) in messages" :key="index" :class="['widget-msg', message.from]">
          <div class="widget-bubble">{{ message.text }}</div>

          <div v-if="message.references?.length" class="widget-references">
            <button v-for="reference in message.references" :key="reference.cardId" type="button"
              class="widget-reference" @click="openCard(reference)">
              <span class="ref-title">{{ reference.cardTitle }}</span>
              <span class="ref-status" :style="{ color: CARD_STATUS_COLORS[reference.status] }">
                {{ CARD_STATUS_LABELS[reference.status] }}
              </span>
            </button>
          </div>
        </div>

        <div v-if="loading" class="widget-msg assistant">
          <div class="widget-bubble widget-typing">Consultando tarefas…</div>
        </div>
      </div>

      <footer class="assistant-widget-footer">
        <el-select v-model="projectId" placeholder="Todos os projetos" clearable popper-class="widget-popper">
          <el-option v-for="project in projects ?? []" :key="project.id" :value="project.id" :label="project.name" />
        </el-select>

        <!-- Composer no padrão do ChatInput da KB -->
        <div class="widget-composer">
          <el-input ref="inputEl" v-model="question" type="textarea" :autosize="{ minRows: 2, maxRows: 5 }"
            resize="none" placeholder="Pergunte sobre suas tarefas..." :disabled="loading"
            @keydown.enter.exact.prevent="ask" />
          <el-button circle class="widget-send" :disabled="!canSend" :icon="Promotion" @click="ask" />
        </div>

        <p class="widget-hint">
          Enter para enviar · As respostas são geradas exclusivamente a partir das suas tarefas.
        </p>
      </footer>
    </div>
  </transition>
</template>
