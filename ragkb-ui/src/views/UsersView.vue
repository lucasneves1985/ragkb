<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { useSectors, useUsers } from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import UserTable from '@/components/users/UserTable.vue'
import CreateUserDrawer from '@/components/users/CreateUserDrawer.vue'
import UserEditDrawer from '@/components/users/UserEditDrawer.vue'
import type { CreateUserRequest, UpdateUserRequest, User } from '@/types'

const {
  users,
  loadUsers,
  createUser,
  updateUser,
  errorMessage: userError,
  submitting,
} = useUsers()
const { sectors, loadSectors } = useSectors()

const createDrawer = ref(false)
const editDrawer = ref(false)
const editingUser = ref<User | null>(null)
const backendError = ref('')

onMounted(() => {
  loadUsers()
  loadSectors()
})

function reset() {
  loadUsers()
  loadSectors()
}

async function create(payload: CreateUserRequest) {
  backendError.value = ''
  const created = await createUser(payload)
  if (created) {
    createDrawer.value = false
  } else {
    backendError.value = userError.value || 'Não foi possível cadastrar o usuário.'
  }
}

function openEdit(user: User) {
  backendError.value = ''
  editingUser.value = user
  editDrawer.value = true
}

async function update(payload: UpdateUserRequest) {
  backendError.value = ''
  if (!editingUser.value) return
  const updated = await updateUser(editingUser.value.id, payload)
  if (updated) {
    editDrawer.value = false
  } else {
    backendError.value = userError.value || 'Não foi possível salvar as alterações.'
  }
}

async function toggleEnabled(user: User) {
  backendError.value = ''
  const updated = await updateUser(user.id, {
    fullName: user.fullName,
    email: user.email,
    phone: user.phone ?? undefined,
    roles: user.roles,
    sectorId: user.sectorId,
    enabled: !user.enabled,
  })
  if (!updated) {
    backendError.value = userError.value || 'Não foi possível alterar o status do usuário.'
  }
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <PageHeading eyebrow="CONTROLE DE ACESSO" title="Usuários"
        description="Defina quem pode consultar e administrar a base de conhecimento.">
        <template #actions>
          <el-button class="primary-button" :icon="Plus" @click="createDrawer = true">
            Novo usuário
          </el-button>
        </template>
      </PageHeading>

      <section class="surface">
        <UserTable :users="users" @edit="openEdit" @toggle="toggleEnabled" />
      </section>

      <CreateUserDrawer v-model="createDrawer" :sectors="sectors" :saving="submitting" :backend-error="backendError"
        @submit="create" />

      <UserEditDrawer v-model="editDrawer" :user="editingUser" :sectors="sectors" :saving="submitting"
        :backend-error="backendError" @submit="update" />
    </div>
  </ErrorBoundary>
</template>
