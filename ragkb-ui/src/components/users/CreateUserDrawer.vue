<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { CreateUserRequest, Sector } from '@/types'

const props = defineProps<{
  modelValue: boolean
  sectors: Sector[]
  backendError?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: CreateUserRequest): void
}>()

const localError = ref('')

const form = reactive({
  username: '',
  password: '',
  roles: ['USER'] as string[],
  sector: null as number | null,
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) reset()
  },
)

function reset() {
  localError.value = ''
  form.username = ''
  form.password = ''
  form.roles = ['USER']
  form.sector = null
}

function close() {
  emit('update:modelValue', false)
}

function submit() {
  localError.value = ''
  if (!form.username || form.password.length < 3) {
    localError.value = 'Informe usuário e uma senha temporária de ao menos 3 caracteres.'
    return
  }
  if (!form.sector) {
    localError.value = 'Informe o setor do usuário.'
    return
  }
  emit('submit', {
    username: form.username,
    password: form.password,
    roles: [...form.roles],
    sectorId: form.sector,
  })
}
</script>

<template>
  <el-drawer :model-value="modelValue" title="Cadastrar usuário" size="430px" @update:model-value="emit('update:modelValue', $event)">
    <p class="drawer-copy">
      O usuário receberá os acessos definidos abaixo. A senha deve ser alterada no primeiro
      acesso.
    </p>

    <el-alert
      v-if="localError || backendError"
      :title="localError || backendError"
      type="error"
      show-icon
      :closable="false"
      class="drawer-alert"
    />

    <el-form label-position="top">
      <el-form-item label="Nome de usuário">
        <el-input v-model="form.username" placeholder="nome.sobrenome" />
      </el-form-item>
      <el-form-item label="Senha temporária">
        <el-input v-model="form.password" type="password" show-password placeholder="Defina uma senha" />
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
      <el-button class="primary-button" @click="submit">Cadastrar usuário</el-button>
    </template>
  </el-drawer>
</template>

<style scoped>
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
