<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { UpdateUserRequest, User, Sector } from '@/types'

const props = defineProps<{
  modelValue: boolean
  user: User | null
  sectors: Sector[]
  saving?: boolean
  backendError?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: UpdateUserRequest): void
}>()

const localError = ref('')

const form = reactive({
  fullName: '',
  email: '',
  phone: '',
  password: '',
  roles: [] as string[],
  sector: null as number | null,
})

watch(
  () => [props.modelValue, props.user] as const,
  ([open, user]) => {
    if (open && user) {
      localError.value = ''
      form.fullName = user.fullName
      form.email = user.email
      form.phone = user.phone ?? ''
      form.password = ''
      form.roles = [...user.roles]
      form.sector = user.sectorId
    }
  },
)

function close() {
  emit('update:modelValue', false)
}

function submit() {
  localError.value = ''
  if (!form.fullName.trim()) {
    localError.value = 'Informe o nome completo.'
    return
  }
  if (!form.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    localError.value = 'Informe um e-mail válido.'
    return
  }
  if (form.password && form.password.length < 8) {
    localError.value = 'A senha temporária deve ter ao menos 8 caracteres.'
    return
  }
  if (!form.sector) {
    localError.value = 'Informe o setor do usuário.'
    return
  }
  if (!form.roles.length) {
    localError.value = 'Selecione ao menos um papel de acesso.'
    return
  }
  emit('submit', {
    fullName: form.fullName.trim(),
    email: form.email.trim().toLowerCase(),
    phone: form.phone.trim() || undefined,
    roles: [...form.roles],
    sectorId: form.sector,
    ...(form.password ? { password: form.password } : {}),
  })
}
</script>

<template>
  <el-drawer :model-value="modelValue" title="Editar usuário" size="430px"
    @update:model-value="emit('update:modelValue', $event)">
    <el-alert v-if="localError || backendError" :title="localError || backendError" type="error" show-icon
      :closable="false" class="drawer-alert" />

    <el-form label-position="top" @submit.prevent>
      <el-form-item label="Nome de usuário">
        <el-input :model-value="user?.username" disabled />
        <span class="field-hint">O nome de usuário não pode ser alterado.</span>
      </el-form-item>
      <el-form-item label="Nome completo" required>
        <el-input v-model="form.fullName" placeholder="Nome e sobrenome" />
      </el-form-item>
      <el-form-item label="E-mail" required>
        <el-input v-model="form.email" placeholder="email@empresa.com" />
      </el-form-item>
      <el-form-item label="Telefone (opcional)">
        <el-input v-model="form.phone" placeholder="(00) 00000-0000" />
      </el-form-item>
      <el-form-item label="Nova senha temporária">
        <el-input v-model="form.password" type="password" show-password
          placeholder="Deixe em branco para manter a atual" />
      </el-form-item>
      <el-form-item label="Setor">
        <el-select v-model="form.sector" placeholder="Selecione o setor">
          <el-option v-for="sector in sectors" :key="sector.id" :label="sector.name" :value="sector.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="Papéis de acesso">
        <el-checkbox-group v-model="form.roles">
          <el-checkbox label="USER">Consulta à base</el-checkbox>
          <el-checkbox label="EDITOR">Gestão de documentos</el-checkbox>
          <el-checkbox label="ADMIN">Administração completa</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">Cancelar</el-button>
      <el-button class="primary-button" :disabled="saving" :loading="saving" @click="submit">
        Salvar alterações
      </el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
.field-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
