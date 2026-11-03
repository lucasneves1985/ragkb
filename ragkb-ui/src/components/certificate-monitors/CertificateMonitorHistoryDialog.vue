<script setup lang="ts">
import { computed, watch } from 'vue'
import { useCertificateMonitorHistory } from '@/composables'
import type { CertificateCheck, CertificateMonitor } from '@/types'
const props=defineProps<{modelValue:boolean;monitor:CertificateMonitor|null}>()
const emit=defineEmits<{(e:'update:modelValue',value:boolean):void}>()
const monitorId=computed(()=>props.monitor?.id??'')
const {history,isLoading,isError,refetch}=useCertificateMonitorHistory(monitorId,()=>props.modelValue)
watch(()=>props.modelValue,open=>{if(open&&monitorId.value)refetch()})
function date(value:string|null){return value?new Date(value).toLocaleString('pt-BR'):'—'}
function statusLabel(check:CertificateCheck){return ({VALID:'Válido',EXPIRING:'Próximo do vencimento',EXPIRED:'Vencido',INVALID_CERTIFICATE:'Certificado inválido',CONNECTION_ERROR:'Falha de conexão'})[check.status]}
</script>
<template>
  <el-dialog :model-value="modelValue" :title="`Histórico — ${monitor?.url??''}`" width="850px" @update:model-value="emit('update:modelValue',$event)">
    <div v-if="isLoading" class="loading">Carregando…</div><el-alert v-else-if="isError" type="error" title="Não foi possível carregar o histórico." :closable="false" />
    <el-table v-else :data="history??[]" empty-text="Nenhuma checagem registrada.">
      <el-table-column label="Data" min-width="170" class-name="mobile-label-checked-at"><template #default="{row}">{{date(row.checkedAt)}}</template></el-table-column>
      <el-table-column label="Resultado" min-width="180" class-name="mobile-label-result"><template #default="{row}">{{statusLabel(row)}}</template></el-table-column>
      <el-table-column label="Dias restantes" width="130" class-name="mobile-label-days-left"><template #default="{row}">{{row.daysRemaining??'—'}}</template></el-table-column>
      <el-table-column label="Detalhe" min-width="220" class-name="mobile-label-detail"><template #default="{row}">{{row.errorMessage??(row.expiresAt?`Vence em ${date(row.expiresAt)}`:'—')}}</template></el-table-column>
    </el-table>
  </el-dialog>
</template>
<style scoped>.loading{padding:28px;text-align:center;color:#718096}</style>
