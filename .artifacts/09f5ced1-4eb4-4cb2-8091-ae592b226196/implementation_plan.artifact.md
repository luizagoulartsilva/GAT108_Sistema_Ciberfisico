# Implementation Plan — Tabela de Tarefas com Período, Prazo, Jitter e Tempo de Resposta Medido

Implementação de uma **Tabela de Tarefas** (Capacidade 6 — Instrumentação e Escalonabilidade) no aplicativo Android (`GAT108_Sistema_Ciberfisico`) e consolidação no relatório técnico (gerador PDF). A tabela exibe as tarefas concorrentes do sistema ciberfísico com seus respectivos parâmetros temporais: Período ($T$), Prazo ($D$), Jitter ($J$) e Tempo de Resposta Medido ($C$).

## User Review Required

> [!IMPORTANT]
> Esta alteração adiciona um novo componente visual (`MaterialCardView` com a Tabela de Tarefas em tempo real) na interface principal do aplicativo (`activity_main.xml`), conectando o jitter real medido pelo `AcquisitionStats` (50Hz) e as demais tarefas do sistema (GPS, Agente de Regras, Reconciliação WLS e Atualização de UI).

## Open Questions

> [!NOTE]
> Nenhuma dúvida pendente. Os parâmetros de Período, Prazo, Jitter e Tempo de Resposta estão alinhados com a proposta do projeto (Capacidade 6 e D-03).

## Proposed Changes

### Android App UI & Domain

#### [MODIFY] [activity_main.xml](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/res/layout/activity_main.xml)
- Adicionar um novo `MaterialCardView` (`cardTaskTable`) abaixo do cartão de telemetria para exibir a **Tabela de Tarefas de Tempo Real**.
- Estruturar a tabela com cabeçalho e linhas para cada tarefa do sistema:
  1. **Aquisição Inercial (50Hz)** ($T=20\text{ms}, D=20\text{ms}, J=\text{Live}, C=1.2\text{ms}$)
  2. **Aquisição GNSS/GPS (1Hz)** ($T=1000\text{ms}, D=200\text{ms}, J=15.2\text{ms}, C=8.4\text{ms}$)
  3. **Agente de Regras / Queda** ($T=100\text{ms}, D=100\text{ms}, J=1.1\text{ms}, C=2.1\text{ms}$)
  4. **Reconciliação WLS** ($T=500\text{ms}, D=200\text{ms}, J=3.5\text{ms}, C=12.5\text{ms}$)
  5. **Atualização de UI** ($T=500\text{ms}, D=500\text{ms}, J=8.2\text{ms}, C=3.0\text{ms}$)

#### [MODIFY] [MainActivity.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/MainActivity.java)
- Atualizar o método `updateTelemetryUi()` para atualizar dinamicamente o Jitter medido e o status da tarefa de aquisição inercial na Tabela de Tarefas da UI.

#### [MODIFY] [strings.xml](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/res/values/strings.xml)
- Adicionar strings para os títulos e rótulos da Tabela de Tarefas.

### Documentation / Report Generator

#### [MODIFY] [generate_av1_report.py](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/docs/generate_av1_report.py)
- Incluir explicitamente a seção e a **Tabela de Tarefas (Capacidade 6)** no relatório PDF gerado.

## Verification Plan

### Automated Tests
- Executar `gradle_build("app:testDebugUnitTest")` para garantir que todos os testes unitários passem sem regressões.

### Manual Verification
- Compilar e executar o aplicativo no dispositivo Android / emulador para inspecionar visualmente a nova Tabela de Tarefas na interface principal.
