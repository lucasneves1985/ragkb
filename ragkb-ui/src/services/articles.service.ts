import { http } from './http'
import type {
  Article,
  ArticleActionRequest,
  ArticleDetail,
  ArticleImageUploadResponse,
  CreateArticleRequest,
  UpdateArticleRequest,
} from '@/types'

export const articlesService = {
  list: () => http.get<Article[]>('/articles').then((r) => r.data),

  create: (request: CreateArticleRequest) =>
    http.post<Article>('/articles', request).then((r) => r.data),

  update: (id: string, request: UpdateArticleRequest) =>
    http.put<Article>(`/articles/${id}`, request).then((r) => r.data),

  changeStatus: (id: string, request: ArticleActionRequest) =>
    http.patch<Article>(`/articles/${id}`, request).then((r) => r.data),

  uploadImage: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return http
      .post<ArticleImageUploadResponse>('/articles/images', formData)
      .then((r) => r.data)
  },
  get: (id: string) => http.get<ArticleDetail>(`/articles/${id}`).then((r) => r.data),
}
