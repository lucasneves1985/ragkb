// ragkb-ui/src/composables/usePortalArticles.ts
import { computed, ref } from 'vue'
import { useQuery } from '@tanstack/vue-query'
import { articlesService } from '@/services'
import { queryKeys } from './queryKeys'

// Feed do portal — query reativa aos filtros: mudar q/sector refaz a busca.
// O input de busca aplica em "aplicarSearch" (enter/botão) para não
// disparar requisição por tecla; o filtro de setor aplica imediato.
export function usePortalArticles() {
  const searchInput = ref('')
  const appliedSearch = ref('')
  const sector = ref('')

  const queryKey = computed(() => [
    ...queryKeys.articles.all,
    'portal',
    { q: appliedSearch.value, sector: sector.value },
  ])

  const query = useQuery({
    queryKey,
    queryFn: () =>
      articlesService.portal({
        q: appliedSearch.value || undefined,
        sector: sector.value || undefined,
      }),
  })

  function applySearch() {
    appliedSearch.value = searchInput.value.trim()
  }

  function clearSearch() {
    searchInput.value = ''
    appliedSearch.value = ''
  }

  return {
    searchInput,
    appliedSearch,
    sector,
    applySearch,
    clearSearch,
    articles: query.data,
    isLoading: query.isLoading,
    isError: query.isError,
    refetch: query.refetch,
  }
}
