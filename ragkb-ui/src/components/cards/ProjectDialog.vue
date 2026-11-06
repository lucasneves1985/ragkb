<!-- components/cards/ProjectDialog.vue -->
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useQuery } from '@tanstack/vue-query'
import {
  useCreateProject,
  useUpdateProject,
  useShareProject,
  useUnshareProject,
  useMyProfile,
  queryKeys,
  extractCardsErrorMessage,
} from '@/composables'
import { usersService } from '@/services'
import type { Project } from '@/types'

const props = defineProps<{
  open: boolean
  /** null = criação; Project = edição (derivado da lista viva pelo pai). */
  project: Project | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const { mutateAsync: createProject } = useCreateProject()
const { mutateAsync: updateProject } = useUpdateProject()
const { mutateAsync: shareProject } = useShareProject()
const { mutateAsync: unshareProject } = useUnshareProject()
const { profile } = useMyProfile()

// Diretório leve (V20) — acessível a qualquer autenticado; o GET /users
// completo é ADMIN-only e não serve ao supervisor que compartilha.
const directoryQuery = useQuery({
  queryKey: queryKeys.users.directory(),
  queryFn: usersService.directory,
  enabled: computed(() => props.open),
})

const form = reactive({ name: '', description: '', color: '' })
const saving = ref(false)
const shareUsername = ref('')
const sharing = ref(false)
const removingUsername = ref('')

// Seção de compartilhamento: apenas o OWNER editando o próprio projeto
// (V20). O backend garante owner-only (404 para os demais) — o gate na UI
// evita expor controles que sempre falhariam. Projetos legados (sem owner)
// não são compartilháveis (regra registrada).
const isOwner = computed(
  () =>
    props.project?.ownerUsername != null &&
    props.project.ownerUsername === profile.value?.username,
)

const participants = computed(() => props.project?.participantUsernames ?? [])

// Candidatos do picker: usuários ATIVOS que não são o owner nem já participam.
const shareCandidates = computed(() =>
  (directoryQuery.data.value ?? []).filter(
    (u) =>
      u.username !== props.project?.ownerUsername &&
      !participants.value.includes(u.username),
  ),
)

// username → id para o endpoint de remoção (DELETE /shares/{userId}).
// O diretório inclui desativados de propósito — participante desativado
// continua removível.
function userIdOf(username: string): number | undefined {
  return (directoryQuery.data.value ?? []).find((u) => u.username === username)?.id
}

// Reset do formulário APENAS na abertura (open false -> true). O projeto
// chega vivo do cache do pai e troca de identidade a cada refetch (share/
// unshare invalidam projects.lists()) — observar props.project aqui
// resetaria o formulário com edições não salvas em cada refetch.
watch(
  () => props.open,
  (open) => {
    if (open) {
      form.name = props.project?.name ?? ''
      form.description = props.project?.description ?? ''
      form.color = props.project?.color ?? ''
      shareUsername.value = ''
    }
  },
)

async function save() {
  const name = form.name.trim()
  if (!name) {
    ElMessage.warning('Informe o nome do projeto.')
    return
  }
  saving.value = true
  try {
    const payload = {
      name,
      description: form.description.trim() || undefined,
      color: form.color || undefined,
    }
    if (props.project) {
      await updateProject({ id: props.project.id, request: payload })
      ElMessage.success('Projeto atualizado!')
    } else {
      await createProject(payload)
      ElMessage.success('Projeto criado!')
    }
    emit('close')
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    saving.value = false
  }
}

async function share() {
  const username = shareUsername.value
  if (!username || !props.project) return
  sharing.value = true
  try {
    await shareProject({ projectId: props.project.id, username })
    ElMessage.success(`Projeto compartilhado com ${username}.`)
    shareUsername.value = ''
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    sharing.value = false
  }
}

async function unshare(username: string) {
  if (!props.project) return
  const userId = userIdOf(username)
  if (userId == null) {
    ElMessage.error('Usuário não encontrado no cadastro — não é possível remover.')
    return
  }
  removingUsername.value = username
  try {
    await unshareProject({ projectId: props.project.id, userId })
    ElMessage.success(`${username} removido do projeto.`)
  } catch (error) {
    ElMessage.error(extractCardsErrorMessage(error))
  } finally {
    removingUsername.value = ''
  }
}
</script>

<template>
  <el-dialog :model-value="open" :title="project ? 'Editar projeto' : 'Novo projeto'" width="520px"
    @close="emit('close')">
    <el-form label-position="top">
      <el-form-item label="Nome" required>
        <el-input v-model="form.name" maxlength="120" placeholder="Ex.: Reforma do apartamento" />
      </el-form-item>
      <el-form-item label="Descrição">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="Contexto do projeto (opcional)" />
      </el-form-item>
      <el-form-item label="Cor">
        <el-color-picker v-model="form.color" />
      </el-form-item>
    </el-form>

    <!-- Compartilhamento (V20): owner-only, apenas em edição de projeto com owner -->
    <template v-if="isOwner">
      <el-divider content-position="left">Compartilhamento</el-divider>
      <p class="share-hint">
        Participantes veem e operam todos os cards do projeto (criar, editar, mover, comentar, anexar) —
        a exclusão de um card fica com quem o criou ou com você (dono do projeto).
      </p>
      <div v-if="participants.length" class="share-list">
        <el-tag v-for="username in participants" :key="username" closable :disable-transitions="true"
          :class="{ 'is-removing': removingUsername === username }" @close="unshare(username)">
          {{ username }}
        </el-tag>
      </div>
      <p v-else class="share-empty">Nenhum participante ainda — o projeto é visível apenas para você.</p>
      <div class="share-picker">
        <el-select v-model="shareUsername" filterable placeholder="Selecione um usuário" class="share-select"
          :disabled="sharing">
          <el-option v-for="u in shareCandidates" :key="u.id" :label="`${u.fullName} (${u.username})`"
            :value="u.username" />
        </el-select>
        <el-button type="primary" class="primary-button" :loading="sharing" :disabled="!shareUsername" @click="share">
          Compartilhar
        </el-button>
      </div>
    </template>

    <template #footer>
      <el-button @click="emit('close')">Cancelar</el-button>
      <el-button type="primary" class="primary-button" :loading="saving" @click="save">
        Salvar
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.share-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.share-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.share-empty {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-style: italic;
}

.share-picker {
  display: flex;
  gap: 8px;
}

.share-select {
  flex: 1;
}

.is-removing {
  opacity: 0.5;
}
</style>
