# Fase 0 — Reconhecimento da arquitetura atual

**Branch verificada:** `refactor/modulith` (limpa no início do reconhecimento).  
**Escopo:** inventário estático de `src/main/java/br/lcn/ragkb`; nenhum arquivo-fonte foi movido ou alterado.

## 1. Inventário completo

A árvore completa foi listada com `rg --files ragkb-api/src/main/java/br/lcn/ragkb`. Há **242 arquivos Java** no pacote-raiz e subpacotes, distribuídos assim:

| Pacote atual | Arquivos |
|---|---:|
| `config/` | 5 |
| `controller/` | 22 |
| `dto/` | 67 |
| `entity/` | 37 |
| `eval/` | 4 |
| `exception/` | 25 |
| `gateway/` | 2 |
| `metrics/` | 1 |
| `repository/` | 24 |
| `service/` | 48 |
| `whatsapp/` | 5 |
| Raiz | 1 |

> Os totais acima corrigem as contagens aproximadas do contexto (por exemplo, são 67 DTOs, não 27+). Os controllers incluem endpoints de avatar, configuração, conflito, mídia e anexos que não aparecem na decomposição de módulos de referência.

## Apêndice A — Árvore completa dos arquivos Java

```text
br/lcn/ragkb/
├── config/
│   ├── AiConfig.java
│   ├── FlywayConfig.java
│   ├── SchedulingConfig.java
│   ├── SecurityConfig.java
│   └── WAHAProperties.java
├── controller/
│   ├── AppConfigurationController.java
│   ├── ArticleController.java
│   ├── ArticleImageController.java
│   ├── AuthController.java
│   ├── AvatarMediaController.java
│   ├── BusinessRuleAttachmentController.java
│   ├── BusinessRuleController.java
│   ├── CardAttachmentController.java
│   ├── CardController.java
│   ├── CardNotificationsTestController.java
│   ├── CardQueryController.java
│   ├── CertificateMonitorController.java
│   ├── ConflictController.java
│   ├── ConversationController.java
│   ├── DocumentLifecycleController.java
│   ├── IngestionController.java
│   ├── IntegrationController.java
│   ├── MediaController.java
│   ├── ProjectController.java
│   ├── QueryController.java
│   ├── ReminderController.java
│   ├── SectorController.java
│   ├── TicketController.java
│   ├── UserController.java
│   └── WhatsAppController.java
├── dto/
│   ├── AnswerResponse.java
│   ├── AppConfigurationValueRequest.java
│   ├── ArticleActionRequest.java
│   ├── ArticleDetailDto.java
│   ├── ArticleDto.java
│   ├── ArticleImageUploadResponse.java
│   ├── AskRequest.java
│   ├── BusinessRuleAttachmentDto.java
│   ├── BusinessRuleDto.java
│   ├── CardAnswerResponse.java
│   ├── CardAskRequest.java
│   ├── CardAttachmentDto.java
│   ├── CardCommentDto.java
│   ├── CardDetailDto.java
│   ├── CardDto.java
│   ├── CardReferenceDto.java
│   ├── CardStatusEventDto.java
│   ├── CertificateCheckDto.java
│   ├── CertificateMonitorDto.java
│   ├── CertificateMonitorRequest.java
│   ├── ChangeOwnPasswordRequest.java
│   ├── ChatMessageDto.java
│   ├── ConflictActionRequest.java
│   ├── ConflictCandidateResponse.java
│   ├── ConversationDetailDto.java
│   ├── ConversationSummaryDto.java
│   ├── CreateArticleRequest.java
│   ├── CreateBusinessRuleRequest.java
│   ├── CreateCardRequest.java
│   ├── CreateCommentRequest.java
│   ├── CreateIntegrationRequest.java
│   ├── CreateProjectRequest.java
│   ├── CreateSectorRequest.java
│   ├── CreateTicketRequest.java
│   ├── CreateUserRequest.java
│   ├── DocumentActionRequest.java
│   ├── DocumentDto.java
│   ├── IngestRequest.java
│   ├── IntegrationDto.java
│   ├── IntegrationExecutionDto.java
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── MoveCardRequest.java
│   ├── ProjectDto.java
│   ├── RedisChatMessageDto.java
│   ├── ReminderCreateRequest.java
│   ├── ReminderDto.java
│   ├── RuleActionRequest.java
│   ├── SectorResponse.java
│   ├── SetDueDateRequest.java
│   ├── SourceReferenceDto.java
│   ├── TicketSuggestion.java
│   ├── TicketSuggestionDto.java
│   ├── UpdateArticleRequest.java
│   ├── UpdateBusinessRuleRequest.java
│   ├── UpdateCardRequest.java
│   ├── UpdateCommentRequest.java
│   ├── UpdateDocumentAccessRequest.java
│   ├── UpdateIntegrationActiveRequest.java
│   ├── UpdateIntegrationRequest.java
│   ├── UpdateOwnProfileRequest.java
│   ├── UpdateProjectRequest.java
│   ├── UpdateUserRequest.java
│   ├── UserDirectoryResponse.java
│   ├── UserResponse.java
│   ├── WhatsAppStatusResponse.java
│   └── WhatsAppTestSendRequest.java
├── entity/
│   ├── AppConfiguration.java
│   ├── AppRole.java
│   ├── AppUser.java
│   ├── Article.java
│   ├── ArticleStatus.java
│   ├── BusinessRule.java
│   ├── BusinessRuleAttachment.java
│   ├── BusinessRuleStatus.java
│   ├── Card.java
│   ├── CardAttachment.java
│   ├── CardChunk.java
│   ├── CardChunkType.java
│   ├── CardComment.java
│   ├── CardStatus.java
│   ├── CardStatusEvent.java
│   ├── CertificateCheck.java
│   ├── CertificateMonitor.java
│   ├── ChatMessage.java
│   ├── ConflictCandidate.java
│   ├── ConflictStatus.java
│   ├── Conversation.java
│   ├── DocumentMetadata.java
│   ├── DocumentStatus.java
│   ├── EmbeddingStatus.java
│   ├── Integration.java
│   ├── IntegrationActionType.java
│   ├── IntegrationAuthType.java
│   ├── IntegrationExecution.java
│   ├── IntegrationExecutionStatus.java
│   ├── IntegrationHttpMethod.java
│   ├── IntegrationType.java
│   ├── Project.java
│   ├── ProjectShare.java
│   ├── Reminder.java
│   ├── ReminderStatus.java
│   └── Sector.java
├── eval/
│   ├── EvalDatasetGeneratorRunner.java
│   ├── EvalItem.java
│   ├── QuestionGenerator.java
│   └── ThresholdCalibrationRunner.java
├── exception/
│   ├── ArticleNotFoundException.java
│   ├── BusinessRuleNotFoundException.java
│   ├── CardCommentNotFoundException.java
│   ├── CardNotFoundException.java
│   ├── ConflictCandidateNotFoundException.java
│   ├── DocumentNotFoundException.java
│   ├── DuplicateBusinessRuleTitleException.java
│   ├── DuplicateDocumentException.java
│   ├── DuplicateIntegrationNameException.java
│   ├── DuplicateProjectNameException.java
│   ├── EmailAlreadyInUseException.java
│   ├── GlobalExceptionHandler.java
│   ├── ImageNotFoundException.java
│   ├── IntegrationNotFoundException.java
│   ├── InvalidArticleContentException.java
│   ├── InvalidImageException.java
│   ├── InvalidIntegrationException.java
│   ├── InvalidRoleException.java
│   ├── ProjectNotFoundException.java
│   ├── ReminderNotFoundException.java
│   ├── SectorInUseException.java
│   ├── SectorNameAlreadyExistsException.java
│   ├── SectorNotFoundException.java
│   ├── UserNotFoundException.java
│   └── UsernameAlreadyExistsException.java
├── gateway/
│   ├── EmailTicketGateway.java
│   └── TicketGateway.java
├── metrics/
│   └── RoutingMetrics.java
├── repository/
│   ├── AppConfigurationRepository.java
│   ├── AppUserRepository.java
│   ├── ArticleRepository.java
│   ├── BusinessRuleAttachmentRepository.java
│   ├── BusinessRuleRepository.java
│   ├── CardAttachmentRepository.java
│   ├── CardChunkRepository.java
│   ├── CardChunkScore.java
│   ├── CardCommentRepository.java
│   ├── CardRepository.java
│   ├── CardStatusEventRepository.java
│   ├── CertificateCheckRepository.java
│   ├── CertificateMonitorRepository.java
│   ├── ConflictCandidateRepository.java
│   ├── ConversationRepository.java
│   ├── DocumentMetadataRepository.java
│   ├── IntegrationExecutionRepository.java
│   ├── IntegrationRepository.java
│   ├── KbLexicalSearchRepository.java
│   ├── ProjectRepository.java
│   ├── ProjectShareRepository.java
│   ├── ReminderRepository.java
│   └── SectorRepository.java
├── service/
│   ├── AppConfigurationService.java
│   ├── AppUserDetailsService.java
│   ├── ArticleImageService.java
│   ├── ArticleIngestionService.java
│   ├── ArticleLifecycleService.java
│   ├── ArticleSanitizationService.java
│   ├── AvatarImageService.java
│   ├── BusinessRuleAttachmentService.java
│   ├── BusinessRuleEmbeddingRetryJob.java
│   ├── BusinessRuleEmbeddingService.java
│   ├── BusinessRuleService.java
│   ├── CardAttachmentService.java
│   ├── CardDigestJob.java
│   ├── CardDigestScheduler.java
│   ├── CardDueReminderJob.java
│   ├── CardEmbeddingRetryJob.java
│   ├── CardEmbeddingService.java
│   ├── CardQueryService.java
│   ├── CardReminderScheduler.java
│   ├── CardService.java
│   ├── CertificateMonitorScheduler.java
│   ├── CertificateMonitorService.java
│   ├── ConflictCandidateService.java
│   ├── ConflictDetectionService.java
│   ├── ConversationService.java
│   ├── DocumentLifecycleService.java
│   ├── IngestionService.java
│   ├── IntegrationCryptoService.java
│   ├── IntegrationEmbeddingStore.java
│   ├── IntegrationExecutor.java
│   ├── IntegrationHttpClient.java
│   ├── IntegrationParamService.java
│   ├── IntegrationQueryExecutor.java
│   ├── IntegrationRoutingService.java
│   ├── IntegrationScheduler.java
│   ├── IntegrationService.java
│   ├── JwtService.java
│   ├── ProjectService.java
│   ├── QueryService.java
│   ├── RedisChatHistoryService.java
│   ├── RedisIntegrationParamStateService.java
│   ├── ReminderDispatchService.java
│   ├── ReminderExtractionService.java
│   ├── ReminderService.java
│   ├── SectorClassifier.java
│   ├── SectorService.java
│   ├── TicketSuggestionService.java
│   └── UserService.java
├── whatsapp/
│   ├── WAHARuntimeConfig.java
│   ├── WhatsAppConfigResolver.java
│   ├── WhatsAppNotConnectedException.java
│   ├── WhatsAppSendException.java
│   └── WhatsAppService.java
└── RagkbApplication.java
```

## 2. Dependências injetadas dos 48 services

Dependências abaixo são os campos `final` (injeção gerada por Lombok) e os campos configurados sem `final` em serviços sem `@RequiredArgsConstructor`. `Map`/estado local não é dependência injetada.

| Service | Dependências injetadas |
|---|---|
| AppConfigurationService | AppConfigurationRepository |
| AppUserDetailsService | AppUserRepository |
| ArticleImageService | Path (configuração) |
| ArticleIngestionService | VectorStore, ArticleSanitizationService |
| ArticleLifecycleService | ArticleRepository, ArticleSanitizationService, ArticleIngestionService, UserService |
| ArticleSanitizationService | nenhuma |
| AvatarImageService | Path (configuração) |
| BusinessRuleAttachmentService | BusinessRuleRepository, BusinessRuleAttachmentRepository, Path (configuração) |
| BusinessRuleEmbeddingRetryJob | BusinessRuleRepository, BusinessRuleEmbeddingService |
| BusinessRuleEmbeddingService | EmbeddingModel, BusinessRuleRepository |
| BusinessRuleService | BusinessRuleRepository, BusinessRuleEmbeddingService, UserService |
| CardAttachmentService | CardRepository, CardAttachmentRepository, Path (configuração) |
| CardDigestJob | CardRepository, AppUserRepository, WhatsAppService |
| CardDigestScheduler | TaskScheduler, AppConfigurationService, CardDigestJob |
| CardDueReminderJob | CardRepository, AppUserRepository, AppConfigurationService, WhatsAppService |
| CardEmbeddingRetryJob | CardChunkRepository, CardEmbeddingService |
| CardEmbeddingService | EmbeddingModel, CardChunkRepository, CardCommentRepository |
| CardQueryService | ChatClient, CardEmbeddingService, CardChunkRepository, CardRepository, CardCommentRepository, CardStatusEventRepository, ProjectRepository, AppUserRepository (**8**) |
| CardReminderScheduler | TaskScheduler, AppConfigurationService, CardDueReminderJob |
| CardService | CardRepository, ProjectRepository, AppUserRepository, CardCommentRepository, CardStatusEventRepository, CardAttachmentRepository, CardAttachmentService, CardEmbeddingService (**8**) |
| CertificateMonitorScheduler | CertificateMonitorService |
| CertificateMonitorService | CertificateMonitorRepository, CertificateCheckRepository, WhatsAppService, JavaMailSender |
| ConflictCandidateService | ConflictCandidateRepository, DocumentMetadataRepository |
| ConflictDetectionService | VectorStore, JdbcTemplate, ObjectMapper, ConflictCandidateRepository |
| ConversationService | ConversationRepository, RedisChatHistoryService, ObjectMapper |
| DocumentLifecycleService | VectorStore, DocumentMetadataRepository, ConflictCandidateRepository |
| IngestionService | VectorStore, DocumentMetadataRepository, DocumentLifecycleService |
| IntegrationCryptoService | SecretKey, SecureRandom (o segundo é instanciado localmente) |
| IntegrationEmbeddingStore | NamedParameterJdbcTemplate |
| IntegrationExecutor | IntegrationExecutionRepository, IntegrationHttpClient, WhatsAppService, JavaMailSender, ObjectMapper, RoutingMetrics (**6**) |
| IntegrationHttpClient | IntegrationCryptoService, ObjectMapper, RestClient.Builder, RoutingMetrics (**4**) |
| IntegrationParamService | ObjectMapper, ChatClient |
| IntegrationQueryExecutor | IntegrationHttpClient, RoutingMetrics |
| IntegrationRoutingService | IntegrationRepository, IntegrationEmbeddingStore, EmbeddingModel, ChatClient, RoutingMetrics (**5**) |
| IntegrationScheduler | TaskScheduler, IntegrationRepository, IntegrationExecutor |
| IntegrationService | IntegrationRepository, IntegrationExecutionRepository, IntegrationCryptoService, ObjectMapper, EmbeddingModel, IntegrationEmbeddingStore (**6**) |
| JwtService | JwtEncoder, UserDetailsService, PasswordEncoder |
| ProjectService | ProjectRepository, ProjectShareRepository, AppUserRepository |
| QueryService | VectorStore, ChatClient, TicketSuggestionService, DocumentMetadataRepository, ConversationService, RedisChatHistoryService, UserService, ReminderService, IntegrationRoutingService, IntegrationQueryExecutor, IntegrationRepository, IntegrationParamService, RedisIntegrationParamStateService, KbLexicalSearchRepository (**14**) |
| RedisChatHistoryService | StringRedisTemplate, ObjectMapper |
| RedisIntegrationParamStateService | StringRedisTemplate, ObjectMapper |
| ReminderDispatchService | ReminderRepository, AppUserRepository, WhatsAppService, ChatClient |
| ReminderExtractionService | ChatClient |
| ReminderService | ReminderRepository, AppUserRepository, ReminderExtractionService |
| SectorClassifier | nenhuma |
| SectorService | SectorRepository, AppUserRepository, DocumentMetadataRepository, BusinessRuleRepository (**4**) |
| TicketSuggestionService | SectorClassifier, TicketGateway |
| UserService | AppUserRepository, PasswordEncoder, SectorRepository, AvatarImageService |

## 3. Matriz de acoplamento observada

A matriz é contagem de **classes de serviço de origem com referência direta a serviço/repositório de outro domínio**, agrupadas conforme os domínios candidatos do mapa. Bibliotecas/frameworks e referências entre classes do mesmo domínio foram omitidas. Os números são contagem de classes de origem, não de campos; um service pode depender de mais de um serviço no destino.

| Origem | Destinos observados |
|---|---|
| assistant (atual QueryService) | conversation (2), identity (1), integration (5), knowledge (2), reminder (1), ticket (1) |
| businessrule | identity (1) |
| card | identity (4), project (3), whatsapp (2), businessrule (1 — tipos de chunks/referências) |
| certificate | whatsapp (1) |
| conversation | — por injeção de serviço |
| identity | knowledge (1), businessrule (1), shared (AvatarImageService) |
| integration | whatsapp (1) |
| knowledge | identity (1) |
| project | identity (1) |
| reminder | identity (2), whatsapp (1) |
| ticket | identity (1) |

Referências que atravessam fronteira mas não são injeções também importam para o desenho Modulith: `ConversationService` consome `AnswerResponse` e `TicketSuggestionDto`; `QueryService` consome contratos de conversa/lembrete; controllers e DTOs referenciam tipos de domínio diretamente; `KbLexicalSearchRepository` precisa ser classificado em `knowledge/search`, e não em `shared` (ele consulta dados de KB). A contagem acima, portanto, é um retrato conservador das dependências de implementação, não uma lista de `allowedDependencies` pronta para copiar.

## 4. Ciclos e acoplamentos circulares

- **Ciclo concreto de contratos:** `assistant/QueryService` → `conversation/ConversationService` (orquestração/persistência); `conversation/ConversationService` → `assistant/AnswerResponse` e `assistant/TicketSuggestionDto`. Isso confirma ciclo de domínio por tipos mesmo sem ciclo entre os campos injetados. `conversation/ChatMessage` também guarda tipos de resposta/sugestão do fluxo assistant.
- **Setor/identidade e conteúdo:** `identity/SectorService` consulta repositórios de documentos e regras de negócio. Já `knowledge/ArticleLifecycleService` e `businessrule/BusinessRuleService` consultam `UserService` para setor/proprietário. Há referências cruzadas em ambos os sentidos entre identidade e conhecimento/regras.
- **Card e identidade/projeto:** `CardService` usa `AppUserRepository` e `ProjectRepository` diretamente; `CardQueryService` também usa ambos. Além disso, `ProjectRepository` contém associação com `Card`, portanto o corte por agregado precisará ser verificado ao mover entidades/repositórios.
- **Integração, lembrete e WhatsApp:** `IntegrationRoutingService` usa `ReminderService`; `IntegrationExecutor`, serviços de lembrete, jobs de card e monitor de certificado chamam `WhatsAppService`. Esse é um adaptador/módulo existente que não equivale a dependência de domínio pura.

Não identifiquei um ciclo entre **campos de serviço injetados** nos arquivos inspecionados; os ciclos existentes são por contratos/tipos e acesso direto a repositórios, listados acima. A checagem arquitetural final deverá confirmar isso sobre os pacotes já movidos.

## 5. Infraestrutura transversal hoje dentro de service/

- `JwtService` e `AppUserDetailsService`: segurança/autenticação.
- `IntegrationCryptoService`: criptografia e configuração de chave.
- `IntegrationHttpClient`: cliente HTTP de saída (embora seja infraestrutura interna de integration, depende de métricas e crypto de integração).
- `RedisChatHistoryService`, `RedisIntegrationParamStateService`: adaptadores Redis; pertencem melhor aos módulos conversation e integration, respectivamente, em vez de `shared` global.
- `ArticleImageService`, `AvatarImageService`, `BusinessRuleAttachmentService`, `CardAttachmentService`: armazenamento de mídia/arquivos, transversal em implementação, porém suas regras e persistência têm dono de domínio; mover cegamente para shared criaria dependência reversa.
- `*Scheduler`, `*Job` e `*RetryJob`: infraestrutura de agendamento pertencente ao domínio que executa o trabalho, não serviço de domínio genérico.
- `AppConfigurationService`: configuração da aplicação; é consultado por jobs de card. Pode residir em shared/config, com interface pública pequena.

## 6. Ownership dos DTOs (67)

Atribuição pelo uso nos controllers/services e pelos tipos que representam; DTOs de integração do fluxo de resposta e de persistência foram destacados por cruzarem limites.

- **identity:** `ChangeOwnPasswordRequest`, `CreateUserRequest`, `LoginRequest`, `LoginResponse`, `UpdateOwnProfileRequest`, `UpdateUserRequest`, `UserDirectoryResponse`, `UserResponse`, `CreateSectorRequest`, `SectorResponse`.
- **knowledge:** `ArticleActionRequest`, `ArticleDetailDto`, `ArticleDto`, `ArticleImageUploadResponse`, `CreateArticleRequest`, `UpdateArticleRequest`, `DocumentActionRequest`, `DocumentDto`, `UpdateDocumentAccessRequest`, `IngestRequest`, `ConflictActionRequest`, `ConflictCandidateResponse`.
- **conversation:** `ChatMessageDto`, `ConversationDetailDto`, `ConversationSummaryDto`, `RedisChatMessageDto` (formato Redis interno; QueryService atualmente acessa histórico).
- **assistant:** `AnswerResponse`, `AskRequest`, `SourceReferenceDto`. `ConversationDetailDto` é ainda consumido pelo QueryService e `ReminderDto` é emitido pelo fluxo de resposta; estes usos são sinais para mover a orquestração/contratos de saída, não para fazer conversation depender de assistant.
- **integration:** `CreateIntegrationRequest`, `IntegrationDto`, `IntegrationExecutionDto`, `UpdateIntegrationActiveRequest`, `UpdateIntegrationRequest`.
- **reminder:** `ReminderCreateRequest`, `ReminderDto`.
- **ticket:** `CreateTicketRequest`, `TicketSuggestion`, `TicketSuggestionDto`.
- **card:** `CardAnswerResponse`, `CardAskRequest`, `CardAttachmentDto`, `CardCommentDto`, `CardDetailDto`, `CardDto`, `CardReferenceDto`, `CardStatusEventDto`, `CreateCardRequest`, `CreateCommentRequest`, `MoveCardRequest`, `SetDueDateRequest`, `UpdateCardRequest`, `UpdateCommentRequest`.
- **project:** `CreateProjectRequest`, `ProjectDto`, `UpdateProjectRequest`.
- **businessrule:** `BusinessRuleAttachmentDto`, `BusinessRuleDto`, `CreateBusinessRuleRequest`, `RuleActionRequest`, `UpdateBusinessRuleRequest`.
- **certificate:** `CertificateCheckDto`, `CertificateMonitorDto`, `CertificateMonitorRequest`.
- **whatsapp:** `WhatsAppStatusResponse`, `WhatsAppTestSendRequest`.
- **shared/config:** `AppConfigurationValueRequest`.

Os contratos compartilhados entre assistant/conversation/ticket são a divergência mais relevante; sua forma final depende da quebra do QueryService e do record neutro pedido na Fase 2.

## 7. Divergências no mapa de referência e proposta corrigida

1. O inventário real tem **22 controllers**, **37 entidades**, **24 repositórios** e **67 DTOs**. Os diretórios genéricos atuais são mais abrangentes que os exemplos do mapa.
2. Existem componentes não listados no mapa que precisam de dono explícito: `AppConfigurationService`/`AppConfiguration`, `AvatarImageService` e controllers de avatar/mídia, `ConflictCandidateService`, `ConflictDetectionService`, `ConflictCandidate`/`ConflictStatus`, `EmbeddingStatus`, `CardChunk*`, `ProjectShare`, `CertificateCheck`/`CertificateMonitor` e respectivos repositórios/DTOs.
3. `BusinessRuleAttachmentService` e entidades/repositórios de anexos existem; precisam acompanhar businessrule.
4. `KbLexicalSearchRepository` está fisicamente em `repository/`, mas sua responsabilidade é a busca lexical da KB, logo deve pertencer a `knowledge/search`.
5. Há 5 classes em `whatsapp/` e 2 em `gateway/`, mas `gateway/` é especificamente gateway de ticket (não um domínio autônomo). A proposta é manter o adaptador WhatsApp como está e colocar gateways de ticket sob `ticket`.
6. `eval/` é o nome real do pacote; o nome lógico pode ser `evaluation/` na movimentação, com tipo OPEN, conforme a solicitação.
7. `RedisChatHistoryService` e `RedisIntegrationParamStateService` são adaptadores próprios dos respectivos domínios, não `shared`. `IntegrationHttpClient` também pertence a integration, enquanto crypto/security são candidatos a shared.
8. O mapa propõe `assistant` como último módulo, coerente com o fluxo real; porém o acoplamento atual inclui contratos assistant guardados/usados por conversation. A quebra e neutralização do contrato devem preceder a mudança de dependências Modulith.
9. O mapa omite explicitamente `AppConfiguration` e o domínio de certificados; recomendação: `shared/configuration` somente para configuração global, e `certificate` para monitoramento/certificados.
10. O mapa não cita `CardAnswerResponse`/`CardAskRequest` e tipos de chunk/status do card; estes pertencem a card e devem ser movidos juntos.

**Decomposição candidata corrigida:** manter os módulos propostos; adicionar ownership claro de `conflict` a `knowledge` (ou subpacote interno de knowledge), `avatar` e configuração global a `shared` com API estreita, certificados em `certificate`, anexos de business rule em `businessrule`; manter adaptadores Redis junto dos domínios; colocar `KbLexicalSearchRepository` sob `knowledge/search`; manter controladores e DTOs junto ao domínio a que servem. Não é recomendável criar módulo separado para cada tabela/repositório sem fronteira comportamental.

## 8. Limite desta etapa

Este documento conclui somente a Fase 0. **Nenhum código foi movido, a quebra do QueryService ainda não começou e nenhum teste/build foi executado.** Conforme o requisito da tarefa, aguardo a apresentação/revisão deste relatório antes de iniciar a Fase 1.
