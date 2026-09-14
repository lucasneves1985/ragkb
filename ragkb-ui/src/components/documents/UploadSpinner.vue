<template>
  <div class="upload-overlay">
    <div class="upload-spinner">
      <div class="spinner-ring"></div>
      <p class="spinner-text">Processando documento…</p>
      <p class="spinner-hint">Extraindo texto, dividindo em chunks e indexando na base vetorial.</p>
    </div>
  </div>
</template>

<style scoped>
.upload-overlay {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 280px;
}

.upload-spinner {
  text-align: center;
}

.spinner-ring {
  width: 52px;
  height: 52px;
  margin: 0 auto 20px;
  border: 4px solid #e8f5f0;
  border-top-color: #168463;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

.spinner-text {
  color: #172033;
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 6px;
}

.spinner-hint {
  color: #8490a1;
  font-size: 12px;
  margin: 0;
  max-width: 300px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
