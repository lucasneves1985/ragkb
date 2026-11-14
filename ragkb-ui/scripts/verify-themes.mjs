import { readFileSync } from 'node:fs'
import { runInNewContext } from 'node:vm'
import ts from 'typescript'

const store = readFileSync(new URL('../src/stores/theme.ts', import.meta.url), 'utf8')
const styles = readFileSync(new URL('../src/styles/theme.css', import.meta.url), 'utf8')
const html = readFileSync(new URL('../index.html', import.meta.url), 'utf8')
const requiredTokens = [
  '--app-bg', '--app-bg-image', '--app-surface', '--app-surface-raised', '--app-border',
  '--app-text', '--app-text-secondary', '--app-muted', '--app-hover', '--app-accent',
  '--app-primary', '--app-primary-hover', '--app-on-primary', '--app-tag-bg',
  '--app-tag-text', '--app-focus',
]
const ids = [...store.matchAll(/id: '(light|dark|graphite|indigo)'/g)].map((match) => match[1])
const inlineIds = html.match(/const themeIds = \[([^\]]+)\]/)?.[1]
  ?.match(/'(light|dark|graphite|indigo)'/g)?.map((id) => id.slice(1, -1)) ?? []

if (ids.join(',') !== 'light,dark,graphite,indigo') throw new Error(`Lista da store inesperada: ${ids}`)
if (inlineIds.join(',') !== ids.join(',')) throw new Error('IDs do index.html diferem da store')
for (const id of ids) {
  const block = styles.match(new RegExp(`html\\[data-theme=['"]${id}['"]\\]\\s*\\{([^}]+)\\}`))?.[1]
  if (!block) throw new Error(`Bloco de tokens ausente: ${id}`)
  for (const token of requiredTokens) {
    if (!block.includes(`${token}:`)) throw new Error(`${id} não define ${token}`)
  }
}
for (const marker of ['isThemeMode(saved)', 'if (!isThemeMode(next)) return', "applyTheme('light')", "mode === 'dark' ? 'dark' : 'light'"]) {
  if (!store.includes(marker)) throw new Error(`Verificação de persistência/store ausente: ${marker}`)
}

const compiled = ts.transpileModule(store, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
}).outputText
const values = new Map()
const root = { dataset: {}, style: {} }
const vue = {
  computed: (getter) => ({ get value() { return getter() } }),
  ref: (value) => ({ value }),
}
const pinia = { defineStore: (_name, setup) => () => setup() }
const module = { exports: {} }
runInNewContext(compiled, {
  exports: module.exports,
  module,
  require: (name) => name === 'vue' ? vue : pinia,
  document: { documentElement: root },
  localStorage: {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
  },
})
const theme = module.exports.useThemeStore()
theme.loadForUser('alice')
if (theme.mode.value !== 'light') throw new Error('Valor ausente não caiu em Claro')
theme.setMode('indigo')
if (values.get('ragkb-theme:alice') !== 'indigo' || root.dataset.theme !== 'indigo' || root.style.colorScheme !== 'light') {
  throw new Error('Persistência/DOM do tema Índigo falhou')
}
theme.setMode('no-such-theme')
if (theme.mode.value !== 'indigo') throw new Error('setMode aceitou ID inválido')
values.set('ragkb-theme:bob', 'not-valid')
theme.loadForUser('bob')
if (theme.mode.value !== 'light') throw new Error('Valor inválido salvo não caiu em Claro')
theme.setMode('dark')
if (root.dataset.theme !== 'dark' || root.style.colorScheme !== 'dark') throw new Error('Tema Escuro não atualizou DOM/colorScheme')
theme.resetForGuest()
if (theme.mode.value !== 'light' || root.dataset.theme !== 'light' || root.style.colorScheme !== 'light') {
  throw new Error('resetForGuest não voltou para Claro')
}
console.log('Temas, tokens obrigatórios, fallback, persistência e lista do index.html verificados.')
