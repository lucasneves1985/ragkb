<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus, UserFilled } from '@element-plus/icons-vue'
import { useSectors, useUsers } from '@/composables'

const { users, loadUsers, createUser, errorMessage: userError } = useUsers()
const { sectors, loadSectors } = useSectors()

const drawer = ref(false)
const error = ref('')
const form = ref({
  username: '',
  password: '',
  roles: ['USER'],
  sector: null as number | null,
})

onMounted(() => {
  loadUsers()
  loadSectors()
})

function reset() {
  loadUsers()
  loadSectors()
}

async function create() {
  error.value = ''
  if (!form.value.username || form.value.password.length < 3) {
    error.value = 'Informe usuário e uma senha temporária de ao menos 3 caracteres.'
    return
  }
  const sectorId = form.value.sector
  if (!sectorId) {
    error.value = 'Informe o setor do usuário.'
    return
  }
  const created = await createUser(
    form.value.username,
    form.value.password,
    form.value.roles,
    sectorId,
  )
  if (created) {
    drawer.value = false
    form.value = { username: '', password: '', roles: ['USER'], sector: null }
  } else {
    error.value = userError.value || 'Não foi possível cadastrar o usuário.'
  }
}
</script>
<template>
  <ErrorBoundary @retry="reset">
    <div>
      <div class="page-heading">
        <div>
          <p class="eyebrow">CONTROLE DE ACESSO</p>
          <h1>Usuários</h1>
          <p>Defina quem pode consultar e administrar a base de conhecimento.</p>
        </div>
        <el-button class="primary-button" :icon="Plus" @click="drawer = true">Novo usuário</el-button>
      </div>
      <section class="surface">
        <div class="users-caption">
          <UserFilled /> {{ users.length }} operadores cadastrados
        </div>
        <el-table :data="users" style="width: 100%"><el-table-column prop="id" label="ID" width="90" /><el-table-column
            prop="username" label="Usuário" min-width="240"><template #default="{ row }">
              <div class="user-name">
                <span>{{
                  row.username
                    .split(/[._]/)
                    .map((x: string) => x[0])
                    .join('')
                    .toUpperCase()
                }}</span><b>{{ row.username }}</b>
              </div>
            </template></el-table-column><el-table-column label="Papéis atribuídos" min-width="280"><template
              #default="{ row }"><el-tag v-for="role in row.roles" :key="role" class="role" size="small" effect="plain">{{
                role }}</el-tag></template></el-table-column><el-table-column label="Ativo" width="130"><template
              #default="{ row }"><el-switch v-model="row.enabled"
                style="--el-switch-on-color: #168463" /></template></el-table-column></el-table>
      </section>
      <el-drawer v-model="drawer" title="Cadastrar usuário" size="430px">
        <p class="drawer-copy">
          O usuário receberá os acessos definidos abaixo. A senha deve ser alterada no primeiro
          acesso.
        </p>
        <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="drawer-alert" /><el-form
          label-position="top"><el-form-item label="Nome de usuário"><el-input v-model="form.username"
              placeholder="nome.sobrenome" /></el-form-item><el-form-item label="Senha temporária"><el-input
              v-model="form.password" type="password" show-password placeholder="Defina uma senha" />
          </el-form-item><el-form-item label="Setor"><el-select v-model="form.sector" placeholder="Selecione o setor">
              <el-option v-for="sector in sectors" :key="sector.id" :label="sector.name" :value="sector.id" />
            </el-select></el-form-item><el-form-item label="Papéis de acesso"><el-checkbox-group
              v-model="form.roles"><el-checkbox label="USER">Consulta à base</el-checkbox><el-checkbox
                label="EDITOR">Gestão
                de documentos</el-checkbox><el-checkbox label="ADMIN">Administração
                completa</el-checkbox></el-checkbox-group></el-form-item></el-form><template #footer><el-button
            @click="drawer = false">Cancelar</el-button><el-button class="primary-button" @click="create">Cadastrar
            usuário</el-button></template>
      </el-drawer>
    </div>
  </ErrorBoundary>
</template>
<style scoped lang="css" src="@/views/styles/users.view.css"></style>
