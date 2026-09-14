<template>
  <section class="login-intro">
    <div class="login-brand"><i>R</i> rag<span>kb</span></div>

    <div>
      <p class="intro-kicker">CONHECIMENTO, COM CONTEXTO</p>
      <h1>Respostas confiáveis começam com uma base bem governada.</h1>
      <p class="intro-copy">
        Consulte políticas, processos e documentos corporativos com segurança e rastreabilidade.
      </p>
    </div>

    <div class="intro-footer">
      © 2026 RAG Knowledge Base<br />
      Ambiente interno e protegido
    </div>
  </section>
</template>

<style scoped>
.login-intro {
  min-height: 100vh;
  padding: 54px 12%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  background:
    radial-gradient(circle at 18% 25%, #245b58, transparent 35%),
    linear-gradient(140deg, #101a2a, #162d35);
}

.login-brand {
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -1px;
}

.login-brand i {
  display: inline-grid;
  place-items: center;
  width: 31px;
  height: 31px;
  margin-right: 9px;
  border-radius: 9px;
  background: #72e2bf;
  color: #102219;
  font: 700 17px 'DM Mono';
  font-style: normal;
}

.login-brand span {
  color: #72e2bf;
}

.intro-kicker {
  margin: 0 0 18px;
  color: #89e6c9;
  font: 500 11px 'DM Mono';
  letter-spacing: 1px;
}

.login-intro h1 {
  max-width: 570px;
  margin: 0;
  font-size: 45px;
  line-height: 1.15;
  letter-spacing: -2px;
}

.intro-copy {
  max-width: 470px;
  color: #b8c7d0;
  line-height: 1.7;
  font-size: 15px;
}

.intro-footer {
  color: #8294a3;
  font-size: 11px;
  line-height: 1.7;
}

@media (max-width: 800px) {
  .login-intro {
    min-height: 260px;
    padding: 34px;
  }

  .login-intro h1 {
    font-size: 28px;
  }

  .intro-footer {
    display: none;
  }
}
</style>
