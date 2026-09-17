<script setup lang="ts">
import { Edit, UserFilled } from '@element-plus/icons-vue'
import type { User } from '@/types'

defineProps<{
  users: User[]
}>()

const emit = defineEmits<{
  (e: 'edit', user: User): void
  (e: 'toggle', user: User): void
}>()

function onToggle(user: User) {
  // user.enabled ainda contém o valor ATUAL (pré-troca): não houve mutação local.
  // A view decide o novo estado e confirma via API; o switch é refletido pelos dados da query.
  emit('toggle', user)
}
</script>

<template>
  <div class="users-caption">
    <el-icon class="caption-icon" :size="16">
      <UserFilled />
    </el-icon>
    {{ users.length }} operadores cadastrados
  </div>

  <el-table :data="users" style="width: 100%">
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column label="Usuário" min-width="240">
      <template #default="{ row }">
        <div class="user-name">
          <el-tag class="username-chip" size="small" effect="plain">
            {{ row.username }}
          </el-tag>
          <b>{{ row.fullName }}</b>
        </div>
      </template>
    </el-table-column>
    <el-table-column prop="email" label="E-mail" min-width="220" show-overflow-tooltip />
    <el-table-column label="Papéis atribuídos" min-width="200">
      <template #default="{ row }">
        <el-tag v-for="role in row.roles" :key="role" class="role" size="small" effect="plain">
          {{ role }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Setor" min-width="160">
      <template #default="{ row }">
        <el-tag class="role" size="small" type="info" effect="plain">
          {{ row.sectorName }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Ativo" width="110">
      <template #default="{ row }">
        <el-switch :model-value="row.enabled" style="--el-switch-on-color: #168463" @change="onToggle(row)" />
      </template>
    </el-table-column>
    <el-table-column label="Ações" width="110" fixed="right">
      <template #default="{ row }">
        <el-button link type="primary" :icon="Edit" @click="emit('edit', row)">
          Editar
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<style scoped>
.users-caption {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.caption-icon {
  flex-shrink: 0;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.username-chip {
  font-weight: 600;
}

.role {
  margin-right: 4px;
}
</style>
