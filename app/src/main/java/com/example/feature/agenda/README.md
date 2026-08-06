# Módulo Feature Agenda (Mapeamento de Aulas)

Este módulo foi migrado para a arquitetura **Feature First** com separação clara de responsabilidades seguindo os princípios de **Clean Architecture** e **Material Design 3**.

## Estrutura de Diretórios

```text
feature/agenda/
├── presentation/
│   ├── screens/
│   │   └── AgendaScreen.kt (Tela principal da agenda)
│   ├── components/
│   │   └── (Componentes reutilizáveis da agenda, se aplicável)
│   ├── AgendaViewModel.kt (Gerenciamento de estados e tratamento de eventos)
│   ├── UiState.kt (Estado imutável da tela da agenda)
│   └── UiEvent.kt (Eventos disparados pela tela)
├── domain/
│   ├── models/ (Entidades exclusivas da camada de domínio da agenda)
│   ├── usecases/
│   │   ├── GetAgendamentosWithDetailsUseCase.kt
│   │   ├── GetAlunosUseCase.kt
│   │   ├── GetMotosUseCase.kt
│   │   ├── ScheduleClassUseCase.kt
│   │   ├── UpdateScheduleStatusUseCase.kt
│   │   └── DeleteScheduleUseCase.kt
│   └── repository/
│       └── AgendaRepository.kt (Contrato público da camada de domínio)
├── data/
│   └── repository/
│       └── AgendaRepositoryImpl.kt (Implementação concreta do contrato)
└── README.md
```

## Arquitetura e Fluxo de Dados

```text
[Compose View]  ──( AgendaUiEvent )──>  [ViewModel]  ──( Executa )──>  [UseCases]
      ▲                                                                     │
      │                                                                 (Acessa)
      └─────────────────( AgendaUiState )───────────────────────────────────▼
                                                                        [Repository]
```

### 1. Presentation Layer
- **`AgendaScreen.kt`**: UI construída em Jetpack Compose que renderiza o estado atualizado emitido pelo ViewModel. Os componentes de interação disparam eventos estruturados do tipo `AgendaUiEvent`.
- **`AgendaViewModel.kt`**: Expõe um único fluxo imutável de estado `uiState` (`AgendaUiState`), resultante da reatividade contínua gerada pela composição dos fluxos de dados do Room Database. Trata eventos via `onEvent(AgendaUiEvent)`.

### 2. Domain Layer
- **`UseCases`**: Classes focadas com única responsabilidade (Single Responsibility Principle) encapsulando regras de negócio, como agendar uma nova aula, atualizar status de agendamentos e registrar auditorias automáticas.
- **`AgendaRepository`**: Interface abstrata para isolar completamente a camada de domínio dos detalhes de infraestrutura.

### 3. Data Layer
- **`AgendaRepositoryImpl`**: Realiza o acesso real aos dados via `AppDatabase` (core-database).

## Comunicação Inter-Módulos
De acordo com os limites definidos pelo projeto, a comunicação deste módulo com outras partes da aplicação ocorre exclusivamente através do banco de dados compartilhado (`Room + Flow`), observando os dados de forma totalmente reativa e desacoplada.
