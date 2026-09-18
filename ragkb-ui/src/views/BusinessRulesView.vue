<!-- views/BusinessRulesView.vue -->
<script setup lang="ts">
import { computed, ref } from 'vue'
import { DocumentAdd, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  useBusinessRules,
  usePortalBusinessRules,
  useArticles,
  useSectors,
  useCreateBusinessRule,
  useUpdateBusinessRule,
  usePublishBusinessRule,
  useArchiveBusinessRule,
} from '@/composables'
import { businessRulesService } from '@/services'
import { useAuthStore } from '@/stores/auth'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import BusinessRuleTable from '@/components/business-rules/BusinessRuleTable.vue'
import BusinessRuleEditorDialog from '@/components/business-rules/BusinessRuleEditorDialog.vue'
import BusinessRuleDetailDialog from '@/components/business-rules/BusinessRuleDetailDialog.vue'
import type { BusinessRule, BusinessRuleSubmitPayload } from '@/types'

const auth = useAuthStore()

// Padrão wrapped do projeto: { rules, isLoading, isError, refetch }
const { rules: managedRules, isLoading, isError, refetch } = useBusinessRules()
// Portal: publicadas ∩ setor — fonte inicial do ROLE_USER.
const { portalRules, isLoadingPortal } = usePortalBusinessRules()
const { sectors } = useSectors()
const { articles } = useArticles()

const { createBusinessRule, creating } = useCreateBusinessRule()
const { updateBusinessRule, updating } = useUpdateBusinessRule()
const { publishBusinessRule } = usePublishBusinessRule()
const { archiveBusinessRule } = useArchiveBusinessRule()

const dialog = ref(false)
const editing = ref<BusinessRule | null>(null)
const viewDialog = ref(false)
const viewing = ref<BusinessRule | null>(null)
const loadingDetail = ref(false)
const saving = computed(() => creating.value || updating.value)

// ROLE_USER: somente leitura — listagem do portal (publicadas do seu setor),
// busca reutiliza o mesmo endpoint e a table renderiza apenas "Ver".
// ADMIN/EDITOR: gestão completa (lista inclui rascunhos e arquivadas próprias).
const isReadOnly = computed(() => !auth.hasAnyRole(['ADMIN', 'EDITOR']))

// ── Busca inteligente (semântica) na listagem ──────────────
// Disparo EXPLÍCITO: Enter no campo ou clique no botão de busca.
// Sem q: fonte = lista de gestão (ADMIN/EDITOR) ou portal (ROLE_USER).
// Com q (3+ chars): GET /business-rules/portal?q= — busca semântica
// pgvector, SOMENTE publicadas (filtro no SQL, não na UI).
const searchQuery = ref('')
const searchResults = ref<BusinessRule[] | null>(null)
const searching = ref(false)

async function runSearch() {
  const query = searchQuery.value.trim()
  if (query.length < 3) {
    ElMessage.warning('Digite pelo menos 3 caracteres para a busca inteligente.')
    return
  }
  searching.value = true
  try {
    searchResults.value = await businessRulesService.portal(query)
  } catch {
    ElMessage.error('Não foi possível executar a busca.')
  } finally {
    searching.value = false
  }
}

function clearSearch() {
  searchQuery.value = ''
  searchResults.value = null
}

const isSmartSearch = computed(() => searchResults.value !== null)
const displayedRules = computed(() => {
  if (searchResults.value !== null) return searchResults.value
  return isReadOnly.value ? (portalRules.value ?? []) : (managedRules.value ?? [])
})

function openCreate() {
  editing.value = null
  dialog.value = true
}

async function openEdit(rule: BusinessRule) {
  loadingDetail.value = true
  try {
    editing.value = await businessRulesService.get(rule.id)
    dialog.value = true
  } catch {
    ElMessage.error('Não foi possível carregar a regra.')
  } finally {
    loadingDetail.value = false
  }
}

function openView(rule: BusinessRule) {
  viewing.value = rule
  viewDialog.value = true
}

async function handleSubmit(payload: BusinessRuleSubmitPayload) {
  try {
    if (editing.value) {
      await updateBusinessRule({ id: editing.value.id, request: payload })
      ElMessage.success('Regra atualizada.')
    } else {
      await createBusinessRule(payload)
      ElMessage.success('Rascunho criado. Publique para habilitar na busca.')
    }
  } catch (error) {
    const message =
      (error as { response?: { data?: { message?: string } } })?.response?.data?.message
    ElMessage.error(message || 'Não foi possível salvar a regra.')
  } finally {
    dialog.value = false
    editing.value = null
    refetch()
  }
}

async function handlePublish(id: string) {
  try {
    await publishBusinessRule(id)
    ElMessage.success('Regra publicada e habilitada na busca.')
  } catch {
    ElMessage.error('Não foi possível publicar a regra.')
  }
}

async function handleArchive(id: string) {
  try {
    await archiveBusinessRule(id)
    ElMessage.success('Regra arquivada.')
  } catch {
    ElMessage.error('Não foi possível arquivar a regra.')
  }
}
</script>

<template>
  <ErrorBoundary @retry="refetch">
    <div>
      <PageHeading eyebrow="BASE DE CONHECIMENTO" title="Regras de negócio">
        <template #actions>
          <div class="search-group">
            <el-input v-model="searchQuery" placeholder="Descreva o que procura… (busca inteligente, publicadas)"
              clearable style="width: 300px" @keyup.enter="runSearch" @clear="clearSearch" />
            <el-button type="primary" plain :icon="Search" :loading="searching" @click="runSearch">
              Buscar
            </el-button>
          </div>
          <el-button v-if="!isReadOnly" class="primary-button" type="primary" :icon="DocumentAdd" @click="openCreate">
            Nova regra
          </el-button>
        </template>
      </PageHeading>

      <el-tag v-if="isSmartSearch" class="smart-badge" size="small" effect="plain" closable @close="clearSearch">
        Resultados inteligentes (publicadas)
      </el-tag>

      <section v-if="isLoading || isLoadingPortal || searching || loadingDetail" class="surface loading">
        Carregando…
      </section>
      <section v-else-if="isError && !isReadOnly" class="surface">Erro ao carregar.</section>
      <section v-else class="surface">
        <BusinessRuleTable :rules="displayedRules" :current-username="auth.username" :is-admin="auth.hasRole('ADMIN')"
          :read-only="isReadOnly" @view="openView" @edit="openEdit" @publish="handlePublish" @archive="handleArchive" />
      </section>

      <BusinessRuleDetailDialog v-model="viewDialog" :rule="viewing" />

      <BusinessRuleEditorDialog v-model="dialog" :sectors="sectors ?? []" :articles="articles ?? []" :rule="editing"
        :saving="saving" @submit="handleSubmit" />
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.search-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.smart-badge {
  margin-bottom: 12px;
}

.loading {
  padding: 28px;
  text-align: center;
  color: #718096;
  font-size: 13px;
}
</style>
