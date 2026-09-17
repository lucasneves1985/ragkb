<!-- views/LoginView.vue -->
<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import LoginIntro from '@/components/auth/LoginIntro.vue'
import LoginForm from '@/components/auth/LoginForm.vue'

const route = useRoute()
const router = useRouter()

function onLoginSuccess() {
  // Volta para o destino original (ex.: artigo citado no chat)
  const redirect = route.query.redirect
  router.push(typeof redirect === 'string' && redirect.startsWith('/') ? redirect : '/chat')
}
</script>

<template>
  <main class="login">
    <LoginIntro />
    <section class="login-panel">
      <LoginForm @success="onLoginSuccess" />
    </section>
  </main>
</template>

<style scoped>
.login {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  background: #fff;
}

.login-panel {
  display: grid;
  place-items: center;
  padding: 32px;
}

@media (max-width: 800px) {
  .login {
    grid-template-columns: 1fr;
  }

  .login-panel {
    padding: 20px;
  }
}
</style>
