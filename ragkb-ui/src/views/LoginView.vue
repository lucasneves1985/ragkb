<script setup lang="ts">
import { useRouter } from 'vue-router'
import LoginIntro from '@/components/auth/LoginIntro.vue'
import LoginForm from '@/components/auth/LoginForm.vue'

const router = useRouter()

function onLoginSuccess() {
  router.push('/chat')
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
