<script setup lang="ts">
import { UserFilled } from '@element-plus/icons-vue'
import type { User } from '@/types'

defineProps<{
  users: User[]
}>()
</script>

<template>
  <div class="users-caption">
    <UserFilled /> {{ users.length }} operadores cadastrados
  </div>

  <el-table :data="users" style="width: 100%">
    <el-table-column prop="id" label="ID" width="90" />
    <el-table-column prop="username" label="Usuário" min-width="240">
      <template #default="{ row }">
        <div class="user-name">
          <span>{{
            row.username
              .split(/[._]/)
              .map((x: string) => x[0])
              .join('')
              .toUpperCase()
          }}</span>
          <b>{{ row.username }}</b>
        </div>
      </template>
    </el-table-column>
    <el-table-column label="Papéis atribuídos" min-width="280">
      <template #default="{ row }">
        <el-tag v-for="role in row.roles" :key="role" class="role" size="small" effect="plain">
          {{ role }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="Ativo" width="130">
      <template #default="{ row }">
        <el-switch v-model="row.enabled" style="--el-switch-on-color: #168463" />
      </template>
    </el-table-column>
  </el-table>
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

.users-caption :deep(svg) {
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
</style>
