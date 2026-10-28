<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage, ElMessageBox } from 'element-plus'
import { certificateMonitorsService as api } from '@/services/certificateMonitors.service'
import type { CertificateCheck, CertificateMonitor, CertificateMonitorRequest } from '@/types/certificateMonitor'
const key=['certificate-monitors'];const client=useQueryClient()
const query=useQuery({queryKey:key,queryFn:api.list});const monitors=computed(()=>query.data.value??[])
const dialog=ref(false),editing=ref<CertificateMonitor|null>(null),historyOpen=ref(false),selected=ref<CertificateMonitor|null>(null)
const history=useQuery({queryKey:['certificate-history',selected],queryFn:()=>api.history(selected.value!.id),enabled:computed(()=>historyOpen.value&&!!selected.value)})
const weekdays=[['MONDAY','Seg'],['TUESDAY','Ter'],['WEDNESDAY','Qua'],['THURSDAY','Qui'],['FRIDAY','Sex'],['SATURDAY','Sáb'],['SUNDAY','Dom']]
const form=reactive<CertificateMonitorRequest>({url:'',checkTime:'09:00',alertThresholdDays:10,alertWeekdays:['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'],notificationChannel:'EMAIL',notificationTarget:'',active:true})
const save=useMutation({mutationFn:(p:CertificateMonitorRequest)=>editing.value?api.update(editing.value.id,p):api.create(p),onSuccess:()=>{client.invalidateQueries({queryKey:key});dialog.value=false;ElMessage.success('Monitor salvo.')}})
const remove=useMutation({mutationFn:api.remove,onSuccess:()=>client.invalidateQueries({queryKey:key})})
const check=useMutation({mutationFn:api.check,onSuccess:()=>{ElMessage.success('Checagem concluída.');client.invalidateQueries({queryKey:['certificate-history']});client.invalidateQueries({queryKey:key})}})
function open(m?:CertificateMonitor){editing.value=m??null;Object.assign(form,m?{url:m.url,checkTime:m.checkTime,alertThresholdDays:m.alertThresholdDays,alertWeekdays:[...m.alertWeekdays],notificationChannel:m.notificationChannel,notificationTarget:m.notificationTarget,active:m.active}:{url:'',checkTime:'09:00',alertThresholdDays:10,alertWeekdays:['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY'],notificationChannel:'EMAIL',notificationTarget:'',active:true});dialog.value=true}
function showHistory(m:CertificateMonitor){selected.value=m;historyOpen.value=true}
async function deleteMonitor(m:CertificateMonitor){try{await ElMessageBox.confirm(`Excluir o monitor de ${m.url}?`,'Confirmação',{type:'warning'});await remove.mutateAsync(m.id);ElMessage.success('Monitor excluído.')}catch{/* cancelado */}}
function statusLabel(c:CertificateCheck){return ({VALID:'Válido',EXPIRING:'Próximo do vencimento',EXPIRED:'Vencido',INVALID_CERTIFICATE:'Certificado inválido',CONNECTION_ERROR:'Falha de conexão'})[c.status]}
function date(v:string|null){return v?new Date(v).toLocaleString('pt-BR'):'—'}
</script>
<template>
 <div class="view">
  <div class="header"><div><h2>Certificados SSL/TLS</h2><p>Acompanhe vencimentos e receba alertas no horário configurado.</p></div><el-button type="primary" @click="open()">Adicionar URL</el-button></div>
  <section class="surface" v-loading="query.isLoading.value">
   <el-table :data="monitors" empty-text="Nenhuma URL monitorada ainda.">
    <el-table-column label="URL" min-width="220"><template #default="s">{{s.row.url}}</template></el-table-column>
    <el-table-column label="Verificação" width="115"><template #default="s">{{s.row.checkTime}}</template></el-table-column>
    <el-table-column label="Alerta antes" width="130"><template #default="s">{{s.row.alertThresholdDays}} dias</template></el-table-column>
    <el-table-column label="Canal" width="110"><template #default="s">{{s.row.notificationChannel==='EMAIL'?'E-mail':'WhatsApp'}}</template></el-table-column>
    <el-table-column label="Última checagem" min-width="160"><template #default="s">{{date(s.row.lastCheckedAt)}}</template></el-table-column>
    <el-table-column label="Ações" width="250"><template #default="s"><el-button link @click="check.mutate(s.row.id)">Checar agora</el-button><el-button link @click="showHistory(s.row)">Histórico</el-button><el-button link @click="open(s.row)">Editar</el-button><el-button link type="danger" @click="deleteMonitor(s.row)">Excluir</el-button></template></el-table-column>
   </el-table>
  </section>
  <el-dialog v-model="dialog" :title="editing?'Editar monitor':'Monitorar certificado'" width="600px">
   <el-form label-position="top">
    <el-form-item label="URL" required><el-input v-model="form.url" placeholder="exemplo.com.br" /></el-form-item>
    <el-row :gutter="12"><el-col :span="12"><el-form-item label="Horário de verificação"><el-time-picker v-model="form.checkTime" format="HH:mm" value-format="HH:mm" /></el-form-item></el-col><el-col :span="12"><el-form-item label="Alertar com antecedência (dias)"><el-input-number v-model="form.alertThresholdDays" :min="0" :max="365" /></el-form-item></el-col></el-row>
    <el-form-item label="Dias do alerta"><el-checkbox-group v-model="form.alertWeekdays"><el-checkbox v-for="d in weekdays" :key="d[0]" :label="d[0]">{{d[1]}}</el-checkbox></el-checkbox-group></el-form-item>
    <el-row :gutter="12"><el-col :span="8"><el-form-item label="Canal"><el-select v-model="form.notificationChannel"><el-option label="E-mail" value="EMAIL"/><el-option label="WhatsApp" value="WHATSAPP"/></el-select></el-form-item></el-col><el-col :span="16"><el-form-item :label="form.notificationChannel==='EMAIL'?'E-mail de destino':'Telefone WhatsApp'"><el-input v-model="form.notificationTarget" :placeholder="form.notificationChannel==='EMAIL'?'nome@empresa.com':'5511999999999'"/></el-form-item></el-col></el-row>
    <el-form-item v-if="editing" label="Monitoramento"><el-switch v-model="form.active" active-text="Ativo" inactive-text="Pausado"/></el-form-item>
   </el-form>
   <template #footer><el-button @click="dialog=false">Cancelar</el-button><el-button type="primary" :loading="save.isPending.value" :disabled="!form.url||!form.checkTime||!form.alertWeekdays.length||!form.notificationTarget" @click="save.mutate({...form})">Salvar</el-button></template>
  </el-dialog>
  <el-dialog v-model="historyOpen" :title="`Histórico — ${selected?.url??''}`" width="850px"><el-table :data="history.data.value??[]" empty-text="Nenhuma checagem registrada."><el-table-column label="Data" min-width="170"><template #default="s">{{date(s.row.checkedAt)}}</template></el-table-column><el-table-column label="Resultado" min-width="180"><template #default="s">{{statusLabel(s.row)}}</template></el-table-column><el-table-column label="Dias restantes" width="130"><template #default="s">{{s.row.daysRemaining??'—'}}</template></el-table-column><el-table-column label="Detalhe" min-width="220"><template #default="s">{{s.row.errorMessage??(s.row.expiresAt?`Vence em ${date(s.row.expiresAt)}`:'—')}}</template></el-table-column></el-table></el-dialog>
 </div>
</template>
<style scoped>.header{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px}.header p{color:#718096;margin:6px 0 0}.surface{background:white;border-radius:12px;padding:12px;box-shadow:0 2px 12px #1b2d4810}</style>
