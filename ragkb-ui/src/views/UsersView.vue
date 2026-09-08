<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Plus, UserFilled } from '@element-plus/icons-vue'
import { backend, type SectorItem, type UserItem } from '../services/api'
const users = ref<UserItem[]>([]),
  drawer = ref(false),
  error = ref(''),
  sectors = ref<SectorItem[]>([]),
  form = ref({ username: '', password: '', roles: ['USER'], sector: '' })

async function loadSectors() {
  try {
    sectors.value = (await backend.listSectors()).data
  } catch {
    sectors.value = []
  }
}

async function load() {
  try {
    users.value = (await backend.listUsers()).data
  } catch {
    users.value = []
  }
}

onMounted(() => {
  load()
  loadSectors()
})
async function create() {
  error.value = ''
  if (!form.value.username || form.value.password.length < 3) {
    error.value = 'Informe usuário e uma senha temporária de ao menos 3 caracteres.'
    return
  }
  if (!form.value.sector) {
    error.value = 'Informe o setor do usuário.'
    return
  }
  try {
    await backend.createUser(
      form.value.username,
      form.value.password,
      form.value.roles,
      form.value.sector,
    )
    drawer.value = false
    form.value = { username: '', password: '', roles: ['USER'], sector: '' }
    await load()
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } }).response?.status
    error.value =
      status === 409
        ? 'Este nome de usuário já está em uso.'
        : 'Não foi possível cadastrar o usuário.'
  }
}
</script>
<template>
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
      <div class="users-caption"><UserFilled /> {{ users.length }} operadores cadastrados</div>
      <el-table :data="users" style="width: 100%"
        ><el-table-column prop="id" label="ID" width="90" /><el-table-column
          prop="username"
          label="Usuário"
          min-width="240"
          ><template #default="{ row }"
            ><div class="user-name">
              <span>{{
                row.username
                  .split(/[._]/)
                  .map((x: string) => x[0])
                  .join('')
                  .toUpperCase()
              }}</span
              ><b>{{ row.username }}</b>
            </div></template
          ></el-table-column
        ><el-table-column label="Papéis atribuídos" min-width="280"
          ><template #default="{ row }"
            ><el-tag
              v-for="role in row.roles"
              :key="role"
              class="role"
              size="small"
              effect="plain"
              >{{ role }}</el-tag
            ></template
          ></el-table-column
        ><el-table-column label="Ativo" width="130"
          ><template #default="{ row }"
            ><el-switch
              v-model="row.enabled"
              style="--el-switch-on-color: #168463" /></template></el-table-column
      ></el-table>
    </section>
    <el-drawer v-model="drawer" title="Cadastrar usuário" size="430px"
      ><p class="drawer-copy">
        O usuário receberá os acessos definidos abaixo. A senha deve ser alterada no primeiro
        acesso.
      </p>
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        :closable="false"
        class="drawer-alert"
      /><el-form label-position="top"
        ><el-form-item label="Nome de usuário"
          ><el-input v-model="form.username" placeholder="nome.sobrenome" /></el-form-item
        ><el-form-item label="Senha temporária"
          ><el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="Defina uma senha"
          /> </el-form-item
        ><el-form-item label="Setor"
          ><el-select v-model="form.sector" placeholder="Selecione o setor">
            <el-option
              v-for="sector in sectors"
              :key="sector.id"
              :label="sector.name"
              :value="sector.id"
            /> </el-select></el-form-item
        ><el-form-item label="Papéis de acesso"
          ><el-checkbox-group v-model="form.roles"
            ><el-checkbox label="USER">Consulta à base</el-checkbox
            ><el-checkbox label="EDITOR">Gestão de documentos</el-checkbox
            ><el-checkbox label="ADMIN">Administração completa</el-checkbox></el-checkbox-group
          ></el-form-item
        ></el-form
      ><template #footer
        ><el-button @click="drawer = false">Cancelar</el-button
        ><el-button class="primary-button" @click="create">Cadastrar usuário</el-button></template
      ></el-drawer
    >
  </div>
</template>
<style scoped>
.users-caption {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 17px 19px;
  border-bottom: 1px solid #edf0f4;
  color: #7d899b;
  font-size: 12px;
}
.users-caption svg {
  width: 15px;
  color: #168463;
}
.user-name {
  display: flex;
  align-items: center;
  gap: 9px;
}
.user-name span {
  display: grid;
  place-items: center;
  width: 27px;
  height: 27px;
  border-radius: 50%;
  background: #e8f5f0;
  color: #168463;
  font: 10px 'DM Mono';
}
.user-name b {
  font-size: 13px;
}
.role {
  margin-right: 5px;
  color: #526077;
}
.drawer-copy {
  margin: -5px 0 22px;
  color: #728095;
  font-size: 12px;
  line-height: 1.65;
}
.drawer-alert {
  margin-bottom: 18px;
}
.el-checkbox {
  display: flex;
  margin: 0 0 12px;
}
.el-checkbox-group {
  display: flex;
  flex-direction: column;
}
</style>
