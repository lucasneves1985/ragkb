import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus, { ElNotification } from 'element-plus'
import 'element-plus/dist/index.css'
import { VueQueryPlugin, type VueQueryPluginOptions } from '@tanstack/vue-query'
import App from './App.vue'
import router from './router'

const app = createApp(App)

const vueQueryOptions: VueQueryPluginOptions = {
  queryClientConfig: {
    defaultOptions: {
      queries: {
        staleTime: 1000 * 60 * 3,        // 3 min — dados frescos
        gcTime: 1000 * 60 * 10,           // 10 min — cache após desmontar
        retry: 2,
        refetchOnWindowFocus: true,
      },
      mutations: {
        retry: 0,                          // mutations não retry (upload de arquivo, etc.)
      },
    },
  },
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(VueQueryPlugin, vueQueryOptions)

app.config.errorHandler = (err, instance, info) => {
  console.error('[ragkb-ui] Erro não capturado:', err, info)
  ElNotification.error({ title: 'Erro inesperado', message: 'Recarregue a página.' })
}

app.mount('#app')
