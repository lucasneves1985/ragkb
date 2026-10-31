<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Lock, Moon, Sunny, User } from '@element-plus/icons-vue'
import { useMyProfile } from '@/composables'
import { resolveAvatarUrl } from '@/services/users.service'
import ErrorBoundary from '@/components/ErrorBoundary.vue'
import PageHeading from '@/components/layout/PageHeading.vue'
import { useThemeStore } from '@/stores/theme'

const { profile, isLoading, isError, refetch, updateProfile, changePassword, uploadAvatar, removeAvatar } =
  useMyProfile()
const theme = useThemeStore()
const darkMode = computed({
  get: () => theme.isDark,
  set: (enabled: boolean) => theme.setMode(enabled ? 'dark' : 'light'),
})

const profileForm = reactive({ fullName: '', email: '' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const selectedFile = ref<File | null>(null)
const previewUrl = ref('')
const removePhoto = ref(false)
const savingProfile = ref(false)
const savingPassword = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

watch(profile, (value) => {
  if (!value) return
  profileForm.fullName = value.fullName
  profileForm.email = value.email
}, { immediate: true })

const avatarPreview = computed(() => {
  if (previewUrl.value) return previewUrl.value
  if (removePhoto.value) return ''
  return resolveAvatarUrl(profile.value?.avatarUrl)
})
const initials = computed(() => profileForm.fullName.trim().split(/\s+/).filter(Boolean)
  .slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'U')

function errorMessage(error: unknown, fallback: string) {
  return (error as { response?: { data?: { message?: string } } })?.response?.data?.message || fallback
}

function choosePhoto(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('A foto deve ter no máximo 5 MB.')
    input.value = ''
    return
  }
  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.error('Use uma foto JPG, PNG ou WebP.')
    input.value = ''
    return
  }
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  selectedFile.value = file
  previewUrl.value = URL.createObjectURL(file)
  removePhoto.value = false
}

function clearPhoto() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
  selectedFile.value = null
  removePhoto.value = !!profile.value?.avatarUrl
  if (fileInput.value) fileInput.value.value = ''
}

async function saveProfile() {
  if (profileForm.fullName.trim().length < 3) {
    ElMessage.warning('Informe seu nome completo.')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(profileForm.email.trim())) {
    ElMessage.warning('Informe um e-mail válido.')
    return
  }

  savingProfile.value = true
  try {
    await updateProfile({ fullName: profileForm.fullName.trim(), email: profileForm.email.trim().toLowerCase() })
    if (selectedFile.value) await uploadAvatar(selectedFile.value)
    else if (removePhoto.value) await removeAvatar()
    ElMessage.success('Preferências atualizadas.')
    selectedFile.value = null
    removePhoto.value = false
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = ''
    if (fileInput.value) fileInput.value.value = ''
  } catch (error) {
    ElMessage.error(errorMessage(error, 'Não foi possível salvar suas preferências.'))
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  if (passwordForm.newPassword.length < 8) {
    ElMessage.warning('A nova senha deve ter pelo menos 8 caracteres.')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('A confirmação da nova senha não confere.')
    return
  }

  savingPassword.value = true
  try {
    await changePassword({ ...passwordForm })
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('Senha alterada com sucesso.')
  } catch (error) {
    ElMessage.error(errorMessage(error, 'Não foi possível alterar sua senha.'))
  } finally {
    savingPassword.value = false
  }
}

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <ErrorBoundary @retry="refetch">
    <div>
      <PageHeading eyebrow="SUA CONTA" title="Preferências" description="Atualize seus dados pessoais e as opções de acesso." />

      <section v-if="isLoading" class="surface loading-state">
        <el-skeleton :rows="5" animated />
      </section>
      <section v-else-if="isError || !profile" class="surface error-state">
        <p>Não foi possível carregar suas preferências.</p>
        <el-button class="primary-button" @click="refetch">Tentar novamente</el-button>
      </section>
      <div v-else class="preferences-grid">
        <section class="surface preference-card">
          <div class="section-heading">
            <el-icon><User /></el-icon>
            <div>
              <h2>Dados pessoais</h2>
              <p>Essas informações aparecem no seu perfil.</p>
            </div>
          </div>

          <div class="avatar-editor">
            <div class="avatar-preview">
              <img v-if="avatarPreview" :src="avatarPreview" alt="Prévia da foto do perfil" />
              <span v-else>{{ initials }}</span>
            </div>
            <div class="avatar-actions">
              <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="choosePhoto" />
              <el-button :icon="Camera" @click="fileInput?.click()">Escolher foto</el-button>
              <el-button v-if="profile.avatarUrl || selectedFile" text type="danger" @click="clearPhoto">Remover foto</el-button>
              <small>JPG, PNG ou WebP · até 5 MB</small>
            </div>
          </div>

          <el-form label-position="top" @submit.prevent="saveProfile">
            <el-form-item label="Nome completo" required>
              <el-input v-model="profileForm.fullName" autocomplete="name" />
            </el-form-item>
            <el-form-item label="E-mail" required>
              <el-input v-model="profileForm.email" type="email" autocomplete="email" />
            </el-form-item>
            <el-form-item label="Nome de usuário">
              <el-input :model-value="profile.username" disabled />
            </el-form-item>
            <el-button class="primary-button" :loading="savingProfile" @click="saveProfile">
              Salvar alterações
            </el-button>
          </el-form>
        </section>

        <section class="surface preference-card">
          <div class="section-heading">
            <el-icon><Lock /></el-icon>
            <div>
              <h2>Alterar senha</h2>
              <p>Confirme sua senha atual para definir uma nova.</p>
            </div>
          </div>

          <el-form label-position="top" @submit.prevent="savePassword">
            <el-form-item label="Senha atual" required>
              <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-form-item label="Nova senha" required>
              <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password"
                placeholder="Pelo menos 8 caracteres" />
            </el-form-item>
            <el-form-item label="Confirmar nova senha" required>
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-button class="primary-button" :loading="savingPassword" @click="savePassword">
              Atualizar senha
            </el-button>
          </el-form>
        </section>

        <section class="surface preference-card appearance-card">
          <div class="section-heading">
            <el-icon><Moon /></el-icon>
            <div>
              <h2>Aparência</h2>
              <p>Escolha o tema usado neste dispositivo. Sua preferência será mantida quando você voltar.</p>
            </div>
          </div>
          <div class="appearance-controls">
            <div class="appearance-choice">
              <el-icon><Sunny /></el-icon>
              <span>Claro</span>
            </div>
            <el-switch v-model="darkMode" aria-label="Alternar entre tema claro e escuro"
              style="--el-switch-on-color: #168463" />
            <div class="appearance-choice">
              <el-icon><Moon /></el-icon>
              <span>Escuro</span>
            </div>
          </div>
        </section>
      </div>
    </div>
  </ErrorBoundary>
</template>

<style scoped>
.preferences-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.preference-card {
  padding: 24px;
}

.appearance-card {
  grid-column: 1 / -1;
}

.appearance-controls {
  display: flex;
  align-items: center;
  gap: 14px;
  width: fit-content;
  padding: 12px 14px;
  border: 1px solid #e8ebf1;
  border-radius: 10px;
  background: #f9fafc;
}

.appearance-choice {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #526077;
  font-size: 13px;
}

.section-heading {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 24px;
  color: #168463;
}

.section-heading > .el-icon {
  margin-top: 3px;
  font-size: 20px;
}

.section-heading h2 {
  margin: 0;
  color: #172033;
  font-size: 17px;
}

.section-heading p {
  margin: 5px 0 0;
  color: #718096;
  font-size: 12px;
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 22px;
}

.avatar-preview {
  display: grid;
  flex: 0 0 76px;
  place-items: center;
  width: 76px;
  height: 76px;
  overflow: hidden;
  border-radius: 50%;
  background: #dcefe9;
  color: #168463;
  font: 600 20px 'DM Mono';
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.avatar-actions small {
  width: 100%;
  color: #8490a1;
  font-size: 11px;
}

.loading-state,
.error-state {
  padding: 28px;
}

.error-state {
  text-align: center;
  color: #718096;
}

@media (max-width: 900px) {
  .preferences-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .preference-card {
    padding: 18px;
  }

  .appearance-controls {
    width: 100%;
    justify-content: center;
  }

  .avatar-editor {
    align-items: flex-start;
  }
}
</style>
