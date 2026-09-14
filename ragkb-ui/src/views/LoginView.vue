<script setup lang="ts">
import { ref } from 'vue'; import { useRouter } from 'vue-router'; import { Lock, User } from '@element-plus/icons-vue'; import { useAuthStore } from '../stores/auth'
const username = ref('joao.silva'), password = ref('SenhaForte123'), loading = ref(false), error = ref(''); const router = useRouter(), auth = useAuthStore()
async function submit() { error.value = ''; loading.value = true; try { await auth.login(username.value, password.value); router.push('/chat') } catch (e: unknown) { const status = (e as { response?: { status?: number } }).response?.status; error.value = status === 401 ? 'Usuário ou senha inválidos.' : 'Não foi possível conectar ao serviço de autenticação.' } finally { loading.value = false } }
</script>
<template>
  <main class="login">
    <section class="login-intro">
      <div class="login-brand"><i>R</i> rag<span>kb</span></div>
      <div>
        <p class="intro-kicker">CONHECIMENTO, COM CONTEXTO</p>
        <h1>Respostas confiáveis começam com uma base bem governada.</h1>
        <p class="intro-copy">Consulte políticas, processos e documentos corporativos com segurança e rastreabilidade.
        </p>
      </div>
      <div class="intro-footer">© 2026 RAG Knowledge Base<br>Ambiente interno e protegido</div>
    </section>
    <section class="login-panel"><el-card class="login-card" shadow="never">
        <p class="eyebrow">ACESSO SEGURO</p>
        <h2>Boas-vindas de volta</h2>
        <p class="login-subtitle">Entre com suas credenciais corporativas.</p><el-alert v-if="error" :title="error"
          type="error" show-icon :closable="false" class="login-alert" /><el-form
          @submit.prevent="submit"><label>Usuário</label><el-input v-model="username" :prefix-icon="User"
            placeholder="nome.sobrenome" size="large" /><label>Senha</label><el-input v-model="password"
            :prefix-icon="Lock" type="password" show-password placeholder="Sua senha" size="large"
            @keyup.enter="submit" /><el-button class="primary-button login-submit" size="large" :loading="loading"
            @click="submit">Entrar na plataforma</el-button></el-form>
      </el-card></section>
  </main>
</template>
<style scoped lang="css" src="@/views/styles/login.view.css"></style>
