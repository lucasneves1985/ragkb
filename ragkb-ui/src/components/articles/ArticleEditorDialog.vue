<!-- components/articles/ArticleEditorDialog.vue -->
<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import { Picture } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUploadArticleImage } from '@/composables'
import type { ArticleDetail, Sector } from '@/types'

export interface ArticleSubmitPayload {
  title: string
  content: string
  sector: string
  allowedSectors: string[]
}

const props = defineProps<{
  modelValue: boolean
  sectors: Sector[]
  article?: ArticleDetail | null
  saving?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: ArticleSubmitPayload): void
}>()

const { uploadArticleImage, uploading } = useUploadArticleImage()

const form = reactive({
  title: '',
  sector: '',
  allowedSectors: [] as string[],
})

const fileInput = ref<HTMLInputElement | null>(null)

const editor = useEditor({
  content: '',
  extensions: [
    StarterKit,
    Image.configure({ inline: false, allowBase64: false }),
  ],
  editorProps: {
    // Bloqueia colar imagem como base64 — o backend rejeitaria no save (400)
    handlePaste: (_view, event) => {
      const items = event.clipboardData?.items
      if (items && Array.from(items).some((i) => i.type.startsWith('image/'))) {
        ElMessage.warning('Use o botão de imagem para enviar pelo endpoint de mídia.')
        return true
      }
      return false
    },
  },
})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    form.title = props.article?.title ?? ''
    form.sector = props.article?.sector ?? ''
    form.allowedSectors = props.article ? [...props.article.allowedSectors] : []
    editor.value?.commands.setContent(props.article?.content ?? '')
  },
)

function close() {
  emit('update:modelValue', false)
}

function openFilePicker() {
  fileInput.value?.click()
}

async function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  try {
    const { url } = await uploadArticleImage(file)
    editor.value?.chain().focus().setImage({ src: url }).run()
  } catch {
    ElMessage.error('Não foi possível enviar a imagem. Verifique formato (JPG, PNG, WebP) e tamanho (máx. 5 MB).')
  }
}

function submit() {
  const content = editor.value?.getHTML() ?? ''

  if (!form.title.trim()) return ElMessage.error('Informe o título.')
  if (!content.trim() || content === '<p></p>') return ElMessage.error('Informe o conteúdo do artigo.')
  if (!props.article && !form.sector) return ElMessage.error('Informe o setor.')
  if (form.allowedSectors.length === 0) return ElMessage.error('Informe ao menos um setor com permissão.')

  emit('submit', {
    title: form.title,
    content,
    sector: form.sector,
    allowedSectors: [...form.allowedSectors],
  })
}
</script>

<template>
  <el-dialog :model-value="modelValue" :title="article ? 'Editar artigo' : 'Novo artigo'" width="760px" top="4vh"
    :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving"
    @update:model-value="emit('update:modelValue', $event)">
    <el-form label-position="top">
      <el-form-item label="Título">
        <el-input v-model="form.title" placeholder="Título do artigo" maxlength="200" />
      </el-form-item>

      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="Setor responsável">
            <el-select v-model="form.sector" placeholder="Selecione" :disabled="!!article">
              <el-option v-for="s in sectors" :key="s.id" :label="s.name" :value="s.name" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Setores com acesso">
            <el-select v-model="form.allowedSectors" multiple placeholder="Selecione">
              <el-option v-for="s in sectors" :key="s.id" :label="s.name" :value="s.name" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="Conteúdo">
        <div class="editor-toolbar">
          <el-button size="small" :icon="Picture" :loading="uploading" @click="openFilePicker">
            Imagem (JPG, PNG, WebP — máx. 5 MB)
          </el-button>
          <input ref="fileInput" type="file" accept=".jpg,.jpeg,.png,.webp" hidden @change="handleImageChange" />
        </div>
        <div v-if="editor" class="editor-formats">
          <el-button size="small" :type="editor.isActive('bold') ? 'primary' : ''"
            @click="editor.chain().focus().toggleBold().run()">B</el-button>
          <el-button size="small" :type="editor.isActive('italic') ? 'primary' : ''"
            @click="editor.chain().focus().toggleItalic().run()">I</el-button>
          <el-button size="small" :type="editor.isActive('heading', { level: 2 }) ? 'primary' : ''"
            @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">H2</el-button>
          <el-button size="small" :type="editor.isActive('heading', { level: 3 }) ? 'primary' : ''"
            @click="editor.chain().focus().toggleHeading({ level: 3 }).run()">H3</el-button>
          <el-button size="small" :type="editor.isActive('bulletList') ? 'primary' : ''"
            @click="editor.chain().focus().toggleBulletList().run()">Lista</el-button>
          <el-button size="small" :type="editor.isActive('blockquote') ? 'primary' : ''"
            @click="editor.chain().focus().toggleBlockquote().run()">Citação</el-button>
        </div>
        <div class="editor-body">
          <editor-content :editor="editor" />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="close">Cancelar</el-button>
      <el-button class="primary-button" :loading="saving" :disabled="saving" @click="submit">
        {{ article ? 'Salvar alterações' : 'Criar rascunho' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.editor-toolbar,
.editor-formats {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.editor-body {
  width: 100%;
  border: 1px solid #e8ebf0;
  border-radius: 8px;
  padding: 12px;
  min-height: 260px;
}

.editor-body :deep(.tiptap) {
  outline: none;
  min-height: 240px;
  font-size: 14px;
  line-height: 1.7;
}

.editor-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
}
</style>
