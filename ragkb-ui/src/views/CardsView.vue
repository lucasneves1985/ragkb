<!-- views/CardsView.vue -->
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  useProjects,
  useBoard,
  useCreateCard,
  useMoveCard,
  useDeleteCard,
  useDeleteProject,
  extractCardsErrorMessage,
} from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import ProjectDialog from '@/components/cards/ProjectDialog.vue'
import CardDetailDialog from '@/components/cards/CardDetailDialog.vue'
import { CARD_STATUSES, CARD_STATUS_COLORS, CARD_STATUS_LABELS, isClosedStatus } from '@/types'
import type { Card, CardStatus, Project } from '@/types'

const { data: projects, refetch: refetchProjects } = useProjects()
const selectedProjectId = ref<string>()
const includeClosed = ref(false)

const { data: boardCards, isLoading, refetch: refetchBoard } = useBoard(selectedProjectId, includeClosed)

// Auto-seleciona o primeiro projeto quando a lista chega e nada está selecionado.
watch(projects, (list) => {
  const first = list?.[0]
  if (!selectedProjectId.value && first) {
    selectedProjectId.value = first.id
  }
})

const selectedProject = computed(() =>
  projects.value?.find((p) => p.id === selectedProjectId.value) ?? null,
)

const cardsByStatus = computed<Record<CardStatus, Card[]>>(() => {
  const map = Object.fromEntries(CARD_STATUSES.map((s) => [s, [] as Card[]])) as Record<CardStatus, Card[]>
  for (const card of boardCards.value ?? []) {
    map[card.status]?.push(card)
  }
  for (const status of CARD_STATUSES) {
    map[status].sort((a, b) => a.position - b.position)
  }
  return map
})

// ── Drag-and-drop nativo ──────────────────────────────────
const draggedCard = ref<Card | null>(null)
const dragOverStatus = ref<CardStatus | null>(null)

function onDragStart(card: Card) {
  draggedCard.value = card
}

function onDragEnd() {
  draggedCard.value = null
  dragOverStatus.value = null
}

function onDropToColumn(status: CardStatus) {
  if (!draggedCard.value) return
  move(draggedCard.value, status, null)
}

function onDropToCard(status: CardStatus, target: Card) {
  if (!draggedCard.value) return
  const column = cardsByStatus.value[status] ?? []
  const withoutDragged = column.filter((c) => c.id !== draggedCard.value!.id)
  const index = withoutDragged.findIndex((c) => c.id === target.id)
  move(draggedCard.value, status, index >= 0 ? index : withoutDragged.length)
}

const { mutateAsync: moveCard } = useMoveCard()

async function move(card: Card, status: CardStatus, position: number | null) {
  if (card.status === status && position === null) return
  try {
    await moveCard({ id: card.id, request: { status, position } })
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    onDragEnd()
  }
}

// ── CRUD de projetos ──────────────────────────────────────
const projectDialogOpen = ref(false)
const projectBeingEdited = ref<Project | null>(null)
const { mutateAsync: deleteProject } = useDeleteProject()

function openNewProject() {
  projectBeingEdited.value = null
  projectDialogOpen.value = true
}

function openEditProject() {
  if (!selectedProject.value) return
  projectBeingEdited.value = selectedProject.value
  projectDialogOpen.value = true
}

async function removeProject() {
  if (!selectedProject.value) return
  try {
    await ElMessageBox.confirm(
      `Excluir o projeto "${selectedProject.value.name}"? Todos os cards, comentários e anexos serão removidos.`,
      'Excluir projeto',
      { type: 'warning', confirmButtonText: 'Excluir', cancelButtonText: 'Cancelar' },
    )
  } catch {
    return
  }
  try {
    await deleteProject(selectedProject.value.id)
    selectedProjectId.value = undefined
    ElMessage.success('Projeto excluído!')
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  }
}

// ── Criação de card ───────────────────────────────────────
const newCardOpen = ref(false)
const newCardTitle = ref('')
const newCardDescription = ref('')
const creatingCard = ref(false)
const { mutateAsync: createCard } = useCreateCard()

function openNewCard() {
  newCardTitle.value = ''
  newCardDescription.value = ''
  newCardOpen.value = true
}

async function submitNewCard() {
  if (!selectedProjectId.value) return
  const trimmed = newCardTitle.value.trim()
  if (!trimmed) {
    ElMessage.warning('Informe o título da tarefa.')
    return
  }
  creatingCard.value = true
  try {
    await createCard({
      projectId: selectedProjectId.value,
      request: {
        title: trimmed,
        description: newCardDescription.value.trim() || undefined,
      },
    })
    newCardOpen.value = false
    ElMessage.success('Tarefa criada em Pendente!')
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    creatingCard.value = false
  }
}

// ── Exclusão de card ──────────────────────────────────────
const { mutateAsync: deleteCard } = useDeleteCard()

async function removeCard(card: Card) {
  try {
    await ElMessageBox.confirm(`Excluir a tarefa "${card.title}"?`, 'Excluir tarefa', {
      type: 'warning',
      confirmButtonText: 'Excluir',
      cancelButtonText: 'Cancelar',
    })
  } catch {
    return
  }
  try {
    await deleteCard(card.id)
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  }
}

// ── Detalhe ───────────────────────────────────────────────
const detailCardId = ref<string | null>(null)

function openDetail(card: Card) {
  detailCardId.value = card.id
}

function refresh() {
  refetchProjects()
  refetchBoard()
}
</script>

<template>
  <ErrorBoundary @retry="refresh">
    <div>
      <PageHeading eyebrow="GESTÃO DE TAREFAS" title="Cards"
        description="Projetos e tarefas em formato Kanban, com assistente de consulta." :on-refresh="refresh" />

      <section class="surface board-toolbar">
        <el-select v-model="selectedProjectId" placeholder="Selecione um projeto" style="width: 260px">
          <el-option v-for="project in projects ?? []" :key="project.id" :value="project.id" :label="project.name" />
        </el-select>

        <el-button size="small" @click="openNewProject">
          <el-icon>
            <Plus />
          </el-icon>
          Novo projeto
        </el-button>

        <el-button v-if="selectedProject" size="small" text type="primary" @click="openEditProject">
          Editar projeto
        </el-button>

        <el-button v-if="selectedProject" size="small" text type="danger" @click="removeProject">
          Excluir projeto
        </el-button>

        <div class="spacer" />

        <el-switch v-model="includeClosed" active-text="Mostrar encerradas" />

        <el-button type="primary" class="primary-button" :disabled="!selectedProjectId" @click="openNewCard">
          <el-icon>
            <Plus />
          </el-icon>
          Nova tarefa
        </el-button>
      </section>

      <div v-if="!projects?.length" class="column-hint">
        Nenhum projeto ainda. Crie o primeiro com "Novo projeto".
      </div>

      <div v-else-if="!selectedProjectId" class="column-hint">Selecione um projeto para ver o board.</div>

      <div v-else class="board" v-loading="isLoading">
        <div v-for="status in CARD_STATUSES" :key="status" :class="[
          'board-column',
          { 'is-closed-column': isClosedStatus(status) },
          { 'drag-over': dragOverStatus === status },
        ]" @dragover.prevent="dragOverStatus = status"
          @dragleave="dragOverStatus === status && (dragOverStatus = null)" @drop.prevent="onDropToColumn(status)">
          <header class="column-header">
            <span class="column-title" :style="{ color: CARD_STATUS_COLORS[status] }">
              {{ CARD_STATUS_LABELS[status] }}
            </span>
            <span class="column-count">{{ cardsByStatus[status].length }}</span>
          </header>

          <div class="column-body">
            <article v-for="card in cardsByStatus[status]" :key="card.id" class="task-card"
              :class="{ 'is-dragging': draggedCard?.id === card.id }" draggable="true" @dragstart="onDragStart(card)"
              @dragend="onDragEnd" @dragover.prevent.stop @drop.prevent.stop="onDropToCard(status, card)"
              @click="openDetail(card)">
              <p class="task-card-title">{{ card.title }}</p>
              <div class="task-card-meta">
                <span>{{ new Date(card.createdAt).toLocaleDateString('pt-BR') }}</span>
                <el-button size="small" text type="danger" @click.stop="removeCard(card)">
                  Excluir
                </el-button>
              </div>
            </article>

            <div v-if="!cardsByStatus[status].length && isClosedStatus(status) && !includeClosed" class="column-hint">
              Ative "Mostrar encerradas" para ver estas tarefas.
            </div>
            <div v-else-if="!cardsByStatus[status].length" class="column-empty">—</div>
          </div>
        </div>
      </div>

      <ProjectDialog :open="projectDialogOpen" :project="projectBeingEdited" @close="projectDialogOpen = false" />

      <el-dialog v-model="newCardOpen" title="Nova tarefa" width="460px">
        <el-form label-position="top">
          <el-form-item label="Título" required>
            <el-input v-model="newCardTitle" maxlength="200" placeholder="Ex.: Enviar nota fiscal ao fornecedor" />
          </el-form-item>
          <el-form-item label="Descrição">
            <el-input v-model="newCardDescription" type="textarea" :rows="3"
              placeholder="Contexto da tarefa (opcional)" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="newCardOpen = false">Cancelar</el-button>
          <el-button type="primary" class="primary-button" :loading="creatingCard" @click="submitNewCard">
            Criar
          </el-button>
        </template>
      </el-dialog>

      <CardDetailDialog :card-id="detailCardId" @close="detailCardId = null" />
    </div>
  </ErrorBoundary>
</template>

<style lang="css" src="@/views/styles/cards.view.css"></style>
