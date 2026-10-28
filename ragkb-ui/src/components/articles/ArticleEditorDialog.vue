<!-- components/articles/ArticleEditorDialog.vue -->
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import { BackgroundColor, FontFamily, FontSize, TextStyle } from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import Highlight from '@tiptap/extension-highlight'
import TextAlign from '@tiptap/extension-text-align'
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
const toolbarVersion = ref(0)
const fontColor = ref('#172033')
const backgroundColor = ref('#fff2a8')
const fontSizeOptions = ['8px', '10px', '12px', '14px', '16px', '18px', '24px', '32px', '48px']
const fontFamilies = [
  { label: 'Padrão', value: '' },
  { label: 'Arial', value: 'Arial, sans-serif' },
  { label: 'Georgia', value: 'Georgia, serif' },
  { label: 'Times New Roman', value: '"Times New Roman", serif' },
  { label: 'Courier New', value: '"Courier New", monospace' },
]

const MEDIA_PREFIX = '/api/media/articles/'

const editor = useEditor({
  content: '',
  extensions: [
    StarterKit.configure({ link: { openOnClick: false } }),
    Image.configure({ inline: false, allowBase64: false }),
    TextStyle,
    FontSize,
    FontFamily,
    Color,
    BackgroundColor,
    Highlight.configure({ multicolor: true }),
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
  ],
  onSelectionUpdate: () => { toolbarVersion.value++ },
  onTransaction: () => { toolbarVersion.value++ },
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
    // Colar HTML de página web traz <img src="https://..."> — o backend
    // rejeita qualquer src fora do endpoint de mídia (InvalidArticleContentException).
    // Mesma política: remove as imagens externas do HTML colado.
    transformPastedHTML(html: string) {
      const doc = new DOMParser().parseFromString(html, 'text/html')
      doc.querySelectorAll('img').forEach((img) => {
        if (!img.getAttribute('src')?.startsWith(MEDIA_PREFIX)) {
          img.remove()
        }
      })
      return doc.body.innerHTML
    },
  },
})

const currentFontSize = computed(() => {
  toolbarVersion.value
  return editor.value?.getAttributes('textStyle').fontSize ?? '16px'
})
const currentFontFamily = computed(() => {
  toolbarVersion.value
  return editor.value?.getAttributes('textStyle').fontFamily ?? ''
})

function isActive(name: string, attributes?: Record<string, unknown>) {
  toolbarVersion.value
  return editor.value?.isActive(name, attributes) ?? false
}

function setBlockStyle(value: string) {
  if (!editor.value) return
  const chain = editor.value.chain().focus()
  if (value === 'paragraph') chain.setParagraph().run()
  else chain.toggleHeading({ level: Number(value) as 1 | 2 | 3 }).run()
}

function setFontSize(size: string) {
  editor.value?.chain().focus().setFontSize(size).run()
}

function setFontFamily(family: string) {
  const chain = editor.value?.chain().focus()
  if (!chain) return
  if (family) chain.setFontFamily(family).run()
  else chain.unsetFontFamily().run()
}

function setLink() {
  const href = window.prompt('Informe o endereço do link (https:// ou mailto:)')?.trim()
  if (!href || !editor.value) return
  if (!/^(https?:\/\/|mailto:)/i.test(href)) {
    ElMessage.warning('Use um endereço começando com https://, http:// ou mailto:.')
    return
  }
  editor.value.chain().focus().setLink({ href, target: '_blank', rel: 'noopener noreferrer' }).run()
}

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
        <div v-if="editor" class="editor-toolbar">
          <el-select :model-value="isActive('heading', { level: 1 }) ? '1' : isActive('heading', { level: 2 }) ? '2' : isActive('heading', { level: 3 }) ? '3' : 'paragraph'"
            size="small" class="block-select" aria-label="Estilo do parágrafo" @change="setBlockStyle">
            <el-option label="Parágrafo" value="paragraph" />
            <el-option label="Título 1" value="1" />
            <el-option label="Título 2" value="2" />
            <el-option label="Título 3" value="3" />
          </el-select>
          <el-select :model-value="currentFontSize" size="small" class="size-select" aria-label="Tamanho da fonte" @change="setFontSize">
            <el-option v-for="size in fontSizeOptions" :key="size" :label="size" :value="size" />
          </el-select>
          <el-select :model-value="currentFontFamily" size="small" class="font-select" aria-label="Fonte" @change="setFontFamily">
            <el-option v-for="family in fontFamilies" :key="family.value" :label="family.label" :value="family.value" />
          </el-select>
          <span class="toolbar-divider" />
          <el-button size="small" title="Negrito" :type="isActive('bold') ? 'primary' : ''" @mousedown.prevent
            @click="editor.chain().focus().toggleBold().run()"><b>N</b></el-button>
          <el-button size="small" title="Itálico" :type="isActive('italic') ? 'primary' : ''" @mousedown.prevent
            @click="editor.chain().focus().toggleItalic().run()"><i>I</i></el-button>
          <el-button size="small" title="Sublinhado" :type="isActive('underline') ? 'primary' : ''" @mousedown.prevent
            @click="editor.chain().focus().toggleUnderline().run()"><u>S</u></el-button>
          <el-button size="small" title="Tachado" :type="isActive('strike') ? 'primary' : ''" @mousedown.prevent
            @click="editor.chain().focus().toggleStrike().run()"><s>T</s></el-button>
          <label class="color-control" title="Cor da fonte">
            <span>A</span><input v-model="fontColor" type="color" aria-label="Cor da fonte"
              @input="editor.chain().focus().setColor(fontColor).run()" />
          </label>
          <label class="color-control highlight-control" title="Cor de fundo do texto">
            <span>▰</span><input v-model="backgroundColor" type="color" aria-label="Cor de fundo do texto"
              @input="editor.chain().focus().setBackgroundColor(backgroundColor).run()" />
          </label>
          <span class="toolbar-divider" />
          <el-button size="small" title="Alinhar à esquerda" @mousedown.prevent
            @click="editor.chain().focus().setTextAlign('left').run()">≡</el-button>
          <el-button size="small" title="Centralizar" @mousedown.prevent
            @click="editor.chain().focus().setTextAlign('center').run()">≣</el-button>
          <el-button size="small" title="Alinhar à direita" @mousedown.prevent
            @click="editor.chain().focus().setTextAlign('right').run()">☰</el-button>
          <el-button size="small" title="Justificar" @mousedown.prevent
            @click="editor.chain().focus().setTextAlign('justify').run()">▤</el-button>
          <el-button size="small" title="Lista com marcadores" :type="isActive('bulletList') ? 'primary' : ''" @mousedown.prevent
            @click="editor.chain().focus().toggleBulletList().run()">• Lista</el-button>
          <el-button size="small" title="Lista numerada" :type="isActive('orderedList') ? 'primary' : ''" @mousedown.prevent
            @click="editor.chain().focus().toggleOrderedList().run()">1. Lista</el-button>
          <el-button size="small" title="Citação" :type="isActive('blockquote') ? 'primary' : ''" @mousedown.prevent
            @click="editor.chain().focus().toggleBlockquote().run()">Citação</el-button>
          <el-button size="small" title="Inserir link" :type="isActive('link') ? 'primary' : ''" @mousedown.prevent @click="setLink">Link</el-button>
          <el-button size="small" title="Remover link" :disabled="!isActive('link')" @mousedown.prevent
            @click="editor.chain().focus().unsetLink().run()">Desvincular</el-button>
          <el-button size="small" :icon="Picture" title="Inserir imagem" :loading="uploading" @mousedown.prevent @click="openFilePicker" />
          <input ref="fileInput" type="file" accept=".jpg,.jpeg,.png,.webp" hidden @change="handleImageChange" />
          <el-button size="small" title="Linha horizontal" @mousedown.prevent
            @click="editor.chain().focus().setHorizontalRule().run()">Linha</el-button>
          <el-button size="small" title="Desfazer" :disabled="!editor.can().undo()" @mousedown.prevent
            @click="editor.chain().focus().undo().run()">↶</el-button>
          <el-button size="small" title="Refazer" :disabled="!editor.can().redo()" @mousedown.prevent
            @click="editor.chain().focus().redo().run()">↷</el-button>
          <el-button size="small" title="Limpar formatação" @mousedown.prevent
            @click="editor.chain().focus().unsetAllMarks().clearNodes().run()">Limpar</el-button>
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
.editor-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
  padding: 10px;
  border: 1px solid #e8ebf0;
  border-radius: 8px;
  background: #f8fafc;
}

.block-select { width: 125px; }
.size-select { width: 78px; }
.font-select { width: 145px; }
.toolbar-divider { height: 24px; border-left: 1px solid #dce2e9; }

.color-control {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 6px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
  color: #344054;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.color-control input {
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
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

.editor-body :deep(.tiptap h1) { font-size: 2em; }
.editor-body :deep(.tiptap h2) { font-size: 1.6em; }
.editor-body :deep(.tiptap h3) { font-size: 1.3em; }
.editor-body :deep(.tiptap a) { color: #168463; text-decoration: underline; }
.editor-body :deep(.tiptap blockquote) {
  border-left: 3px solid #72e2bf;
  margin: 12px 0;
  padding-left: 14px;
  color: #667085;
}

.editor-body :deep(img) {
  max-width: 100%;
  border-radius: 6px;
}
</style>
