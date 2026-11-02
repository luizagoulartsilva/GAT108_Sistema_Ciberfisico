# Walkthrough — Implementação da Tabela de Tarefas

Implementação completa da **Tabela de Tarefas** (Capacidade 6 — Instrumentação e Escalonabilidade) no aplicativo Android (`GAT108_Sistema_Ciberfisico`) e no relatório técnico em PDF.

## Changes

### Android App UI & Code
#### [MODIFY] [strings.xml](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/res/values/strings.xml)
- Adicionados recursos de texto para a Tabela de Tarefas e descrição de colunas.

#### [MODIFY] [activity_main.xml](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/res/layout/activity_main.xml)
- Adicionado um novo `MaterialCardView` (`cardTaskTable`) exibindo a Tabela de Tarefas com colunas para Período ($T$), Prazo ($D$), Jitter ($J$) e Tempo de Resposta ($C$).

#### [MODIFY] [MainActivity.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/MainActivity.java)
- Atualizado o método `updateTelemetryUi()` para injetar dinamicamente as métricas reais de jitter médio e máximo coletadas por `AcquisitionStats` na linha da tarefa de aquisição inercial (50Hz).

### Documentation & Report Generator
#### [MODIFY] [generate_av1_report.py](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/docs/generate_av1_report.py)
- Incluída a **Seção 4.3.1 (Tabela 3)** detalhando a Tabela de Tarefas, Períodos, Prazos, Jitter e Tempos de Resposta Medidos para todas as 5 tarefas concorrentes do sistema ciberfísico.

---

## Verification Results

### Automated Tests
- Executado `./gradlew app:testDebugUnitTest`: **BUILD SUCCESSFUL** (32 testes aprovados, 0 falhas).
- Executado `./gradlew app:assembleDebug`: **BUILD SUCCESSFUL** (compilação do app bem-sucedida com o novo layout e lógica).
