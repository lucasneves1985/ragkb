<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { useSectors, useUsers } from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import UserTable from '@/components/users/UserTable.vue'
import CreateUserDrawer from '@/components/users/CreateUserDrawer.vue'
import type { CreateUserRequest } from '@/types'

const { users, loadUsers, createUser, errorMessage: userError } = useUsers()
const { sectors, loadSectors } = useSectors()

const drawer = ref(false)
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
    drawer.value = false
  } else {
    backendError.value = userError.value || 'Não foi possível cadastrar o usuário.'
  }
}
</script>

<template>
  <ErrorBoundary @retry="reset">
    <div>
      <PageHeading
        eyebrow="CONTROLE DE ACESSO"
        title="Usuários"
        description="Defina quem pode consultar e administrar a base de conhecimento."
      >
        <template #actions>
          <el-button class="primary-button" :icon="Plus" @click="drawer = true">Novo usuário</el-button>
        </template>
      </PageHeading>

      <section class="surface">
        <UserTable :users="users" />
      </section>

      <CreateUserDrawer
        v-model="drawer"
        :sectors="sectors"
        :backend-error="backendError"
        @submit="create"
      />
    </div>
  </ErrorBoundary>
</template>
