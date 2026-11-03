export const queryKeys = {
  documents: {
    all: ['documents'] as const,
    lists: () => [...queryKeys.documents.all, 'list'] as const,
    detail: (id: string) => [...queryKeys.documents.all, 'detail', id] as const,
  },
  sectors: {
    all: ['sectors'] as const,
    lists: () => [...queryKeys.sectors.all, 'list'] as const,
  },
  conflicts: {
    all: ['conflicts'] as const,
    lists: () => [...queryKeys.conflicts.all, 'list'] as const,
  },
  conversations: {
    all: ['conversations'] as const,
    lists: () => [...queryKeys.conversations.all, 'list'] as const,
  },
  users: {
    all: ['users'] as const,
    lists: () => [...queryKeys.users.all, 'list'] as const,
  },
  tickets: {
    all: ['tickets'] as const,
    lists: () => [...queryKeys.tickets.all, 'list'] as const,
  },
  articles: {
    all: ['articles'] as const,
    lists: () => [...queryKeys.articles.all, 'list'] as const,
  },
  projects: {
    all: ['projects'] as const,
    lists: () => [...queryKeys.projects.all, 'list'] as const,
  },
  cards: {
    all: ['cards'] as const,
    board: (projectId: string, includeClosed: boolean) =>
      [...queryKeys.cards.all, 'board', projectId, includeClosed] as const,
    detail: (id: string) => [...queryKeys.cards.all, 'detail', id] as const,
  },
} as const
