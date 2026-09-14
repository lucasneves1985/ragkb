<script setup lang="ts">
import { ref } from 'vue'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const emit = defineEmits<{
  (e: 'success'): void
}>()

const auth = useAuthStore()
const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    emit('success')
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } }).response?.status
    error.value =
      status === 401
        ? 'Usuário ou senha inválidos.'
        : 'Não foi possível conectar ao serviço de autenticação.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card class="login-card" shadow="never">
    <p class="eyebrow">ACESSO SEGURO</p>
    <h2>Boas-vindas de volta</h2>
    <p class="login-subtitle">Entre com suas credenciais corporativas.</p>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="login-alert" />

    <el-form @submit.prevent="submit">
      <label>Usuário</label>
      <el-input v-model="username" :prefix-icon="User" placeholder="Usuário" size="large" />
      <label>Senha</label>
      <el-input
        v-model="password"
        :prefix-icon="Lock"
        type="password"
        show-password
        placeholder="Senha"
        size="large"
        @keyup.enter="submit"
      />
      <el-button class="primary-button login-submit" size="large" :loading="loading" @click="submit">
        Entrar na plataforma
      </el-button>
    </el-form>
  </el-card>
</template>

<style scoped>
.login-card {
  width: min(420px, 100%);
  padding: 22px;
  border: 0 !important;
}

.login-card h2 {
  margin: 0;
  color: #172033;
  font-size: 27px;
  letter-spacing: -1px;
}

.login-subtitle {
  margin: 8px 0 29px;
  color: #778499;
  font-size: 13px;
}

.login-card label {
  display: block;
  margin: 18px 0 7px;
  color: #46536a;
  font-size: 12px;
  font-weight: 700;
}

.login-submit {
  width: 100%;
  margin-top: 27px;
}

.login-alert {
  margin-bottom: 18px;
}

.hint {
  margin-top: 23px;
  color: #98a2b3;
  font-size: 11px;
  line-height: 1.6;
}
</style>
