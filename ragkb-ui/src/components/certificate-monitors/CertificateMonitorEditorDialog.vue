<script setup lang="ts">
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { CertificateChannel, CertificateMonitor, CertificateMonitorRequest } from '@/types'
const props = defineProps<{ modelValue: boolean; monitor: CertificateMonitor | null; saving?: boolean }>()
const emit = defineEmits<{ (e:'update:modelValue',value:boolean):void; (e:'submit',request:CertificateMonitorRequest):void }>()
const weekdays=[['MONDAY','Seg'],['TUESDAY','Ter'],['WEDNESDAY','Qua'],['THURSDAY','Qui'],['FRIDAY','Sex'],['SATURDAY','Sáb'],['SUNDAY','Dom']]
const form=reactive<CertificateMonitorRequest>({url:'',checkTime:'09:00',alertThresholdDays:10,alertWeekdays:['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'],notificationChannel:'EMAIL',notificationTarget:'',active:true})
watch(()=>[props.modelValue,props.monitor] as const,([open,monitor])=>{if(!open)return;Object.assign(form,monitor?{url:monitor.url,checkTime:monitor.checkTime,alertThresholdDays:monitor.alertThresholdDays,alertWeekdays:[...monitor.alertWeekdays],notificationChannel:monitor.notificationChannel,notificationTarget:monitor.notificationTarget,active:monitor.active}:{url:'',checkTime:'09:00',alertThresholdDays:10,alertWeekdays:['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'],notificationChannel:'EMAIL' as CertificateChannel,notificationTarget:'',active:true})},{immediate:true})
function submit(){if(!form.url.trim())return ElMessage.error('Informe a URL.');if(!form.alertWeekdays.length)return ElMessage.error('Selecione pelo menos um dia para alertas.');if(!form.notificationTarget.trim())return ElMessage.error('Informe o destino da notificação.');emit('submit',{...form,url:form.url.trim(),notificationTarget:form.notificationTarget.trim(),alertWeekdays:[...form.alertWeekdays]})}
</script>
<template>
  <el-dialog :model-value="modelValue" :title="monitor?'Editar monitor':'Monitorar certificado'" width="600px" @update:model-value="emit('update:modelValue',$event)">
    <el-form label-position="top">
      <el-form-item label="URL" required><el-input v-model="form.url" placeholder="exemplo.com.br" /></el-form-item>
      <el-row :gutter="12"><el-col :span="12"><el-form-item label="Horário de verificação"><el-time-picker v-model="form.checkTime" format="HH:mm" value-format="HH:mm" /></el-form-item></el-col><el-col :span="12"><el-form-item label="Alertar com antecedência (dias)"><el-input-number v-model="form.alertThresholdDays" :min="0" :max="365" /></el-form-item></el-col></el-row>
      <el-form-item label="Dias do alerta"><el-checkbox-group v-model="form.alertWeekdays"><el-checkbox v-for="day in weekdays" :key="day[0]" :label="day[0]">{{ day[1] }}</el-checkbox></el-checkbox-group></el-form-item>
      <el-row :gutter="12"><el-col :span="8"><el-form-item label="Canal"><el-select v-model="form.notificationChannel"><el-option label="E-mail" value="EMAIL"/><el-option label="WhatsApp" value="WHATSAPP"/></el-select></el-form-item></el-col><el-col :span="16"><el-form-item :label="form.notificationChannel==='EMAIL'?'E-mail de destino':'Telefone WhatsApp'"><el-input v-model="form.notificationTarget" :placeholder="form.notificationChannel==='EMAIL'?'nome@empresa.com':'5511999999999'"/></el-form-item></el-col></el-row>
      <el-form-item v-if="monitor" label="Monitoramento"><el-switch v-model="form.active" active-text="Ativo" inactive-text="Pausado"/></el-form-item>
    </el-form>
    <template #footer><el-button @click="emit('update:modelValue',false)">Cancelar</el-button><el-button type="primary" :loading="saving" :disabled="!form.url||!form.checkTime||!form.alertWeekdays.length||!form.notificationTarget" @click="submit">Salvar</el-button></template>
  </el-dialog>
</template>
