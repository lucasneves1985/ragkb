# Arquitetura modular do ragkb-api

O backend está organizado como monólito modular dentro do pacote `br.lcn.ragkb`. Cada domínio possui um pacote raiz reconhecido pelo Spring Modulith. Os contratos consumidos entre módulos ficam em `api/`, marcado como interface nomeada; entidades, repositórios e serviços de implementação permanecem internos ao módulo.

## Módulos

- `assistant`: orquestra a escolha entre conhecimento, integrações e lembretes. É módulo folha e nenhum outro domínio depende dele.
- `knowledge`: ingestão, ciclo de vida de artigos/documentos e busca híbrida.
- `conversation`: persistência e histórico das conversas; usa `InteractionRecord` neutro na fronteira.
- `integration`: roteamento, parametrização e execução de integrações.
- `reminder`: criação, consulta e despacho de lembretes.
- `ticket`: sugestão e abertura de chamados.
- `identity`: usuários, autenticação, papéis e setores; publica APIs de consulta específicas.
- `project`: projetos e compartilhamento, com API limitada de acesso e consulta de nome.
- `card`: cards, pesquisa, anexos, embeddings e lembretes/digest.
- `businessrule`: regras de negócio e embeddings.
- `certificate`: monitoramento de certificados.
- `whatsapp`: integração com WAHA e contrato de envio.
- `shared`: configuração e capacidades transversais abertas, além de exceções e métricas compartilhadas.
- `evaluation`: runners e dados de avaliação, módulo aberto para execução de ferramentas.

## Fluxo do assistente

`QueryService` foi substituído por `AssistantOrchestrator`. Ele prepara o contexto e escolhe um dos handlers. `KnowledgeAnswerHandler` mantém prompt, constantes, busca híbrida, resposta LLM e fontes; a busca foi isolada em `KnowledgeSearchService`. `IntegrationAnswerHandler` chama o contrato de integração, enquanto a implementação de roteamento e execução para o assistente fica em `IntegrationAssistantService`. `ReminderAnswerHandler` preserva a detecção e a criação de lembretes. A gravação usa `ConversationApi` e o registro neutro `InteractionRecord`, evitando que conversation conheça os DTOs do assistant.

## Contratos e decisões

- `identity.api.UserLookupApi` e `SectorLookupApi` substituem acessos de outros módulos a repositórios e ao service de identidade. O contrato de contato devolve apenas id, username, telefone e nome necessários aos fluxos de cards e lembretes.
- `project.api.ProjectLookupApi` expõe verificação de acesso, participação, id do owner e nome do projeto. Isso mantém o card independente do repositório e da entidade `Project`.
- `knowledge.api.KnowledgeSearchApi`, `integration.api.IntegrationAssistantApi`, `reminder.api.ReminderAssistantApi`, `ticket.api.TicketSuggestionApi` e `whatsapp.api.WhatsAppMessagingApi` expõem operações estreitas, sem publicar services completos.
- `shared.api.SectorUsageCheck` remove o ciclo de identidade com knowledge e businessrule.
- `TicketSuggestionApi` converte o agregado interno em DTO de fronteira. `ConversationApi` aceita `InteractionRecord`, sem importar contratos do assistant.
- `shared` e `evaluation` são `OPEN`; os módulos de domínio declaram `allowedDependencies` em `package-info.java` com base nos usos observados.

## Limites e dívidas conscientes

A camada HTTP continua nos pacotes raiz dos domínios em vez de adotar um subpacote `web/`, evitando movimentações que não agregam fronteira arquitetural. `shared` agrega configuração, métricas, criptografia, exceções genéricas, configuração de aplicação e o enum `EmbeddingStatus`; por ser aberto, isso é um ponto de evolução para dividir caso cresça. O acesso ao nome do projeto pelo card permanece uma dependência explícita do contrato de leitura `ProjectLookupApi`, pois a resposta do card precisa exibir o nome sem expor a entidade.

A arquitetura é verificada por `ModularityTest` e `ArchitectureTest`. `ModularityTest` também grava os diagramas/documentos no diretório padrão `target/spring-modulith-docs/` durante os testes.
