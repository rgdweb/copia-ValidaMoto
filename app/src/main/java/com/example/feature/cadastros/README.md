# Módulo Feature Cadastros (Alunos & Motos)

Este módulo foi migrado para a arquitetura **Feature First** com separação clara de responsabilidades seguindo os princípios de **Clean Architecture** e **Material Design 3**.

## Estrutura de Diretórios

```text
feature/cadastros/
├── presentation/
│   ├── screens/
│   │   └── CadastrosScreen.kt (Tela principal de cadastros de Alunos e Motos)
│   ├── components/
│   │   └── (Componentes de visualização de linha ou modais, se aplicável)
│   ├── ViewModel.kt (Gerenciamento de estados e tratamento de eventos)
│   ├── UiState.kt (Estado imutável da tela de cadastros)
│   └── UiEvent.kt (Eventos disparados pela tela para inclusão/edição/deleção)
├── domain/
│   ├── usecases/
│   │   ├── GetAlunosUseCase.kt (Recuperação do fluxo de Alunos)
│   │   ├── GetMotosUseCase.kt (Recuperação do fluxo de Motos)
│   │   ├── GetAulasWithDetailsUseCase.kt (Histórico de aulas para detalhes)
│   │   ├── AddStudentUseCase.kt (Inclusão de novo aluno)
│   │   ├── UpdateStudentUseCase.kt (Edição de aluno existente)
│   │   ├── DeleteStudentUseCase.kt (Exclusão de aluno)
│   │   ├── AddMotoUseCase.kt (Inclusão de nova motocicleta)
│   │   ├── UpdateMotoUseCase.kt (Edição de motocicleta existente)
│   │   └── DeleteMotoUseCase.kt (Exclusão de motocicleta)
│   └── repository/
│       └── CadastrosRepository.kt (Contrato público da camada de domínio)
├── data/
│   └── repository/
│       └── CadastrosRepositoryImpl.kt (Implementação concreta do contrato)
└── README.md
```

## Arquitetura e Fluxo de Dados

```text
[Compose View]  ──( CadastrosUiEvent )──>  [ViewModel]  ──( Executa )──>  [UseCases]
      ▲                                                                     │
      │                                                                 (Acessa)
      └─────────────────( CadastrosUiState )────────────────────────────────▼
                                                                        [Repository]
```

### 1. Presentation Layer
- **`CadastrosScreen.kt`**: Interface reativa em Jetpack Compose que renderiza as abas de Alunos e Motos, campos de buscas inteligentes, botões de ação e diálogos de criação/edição baseados no estado emitido pelo ViewModel.
- **`CadastrosViewModel.kt`**: Centraliza o estado combinando as listas reativas fornecidas pelo banco de dados Room. Dispensa o uso de dependências globais e centraliza a criação de arquivos de foto por meio do `AppPreferences` local.

### 2. Domain Layer
- **`UseCases`**: Encapsulam regras de negócio de menor nível (ex: regras de data limite fallback no exame de aluno, normalização de placas em caixa alta para as motocicletas).
- **`CadastrosRepository`**: Isola a camada de domínio dos detalhes de infraestrutura (Banco de dados Room).

### 3. Data Layer
- **`CadastrosRepositoryImpl`**: Realiza as operações de consulta e persistência real no Room Database (`AppDatabase`).
