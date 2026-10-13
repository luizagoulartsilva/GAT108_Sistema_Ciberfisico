# Registro de decisões

## D-01 - Linguagem e escopo da S1
- **Data:** 15/09/2026
- **Decisão:** Java 17 com testes unitários JUnit 5; a S1 entrega somente domínio, contratos e testes de aceitação.
- **Alternativas consideradas:** Kotlin/Android desde a primeira semana; implementar as camadas junto com os contratos.
- **Motivo:** Java deixa explícitos os tipos e contratos avaliados em concorrência e tempo real. A implementação das camadas fica para as semanas previstas no calendário.
- **Consequências:** Os testes de domínio devem passar; os testes de aquisição, reconciliação e agente permanecem vermelhos até a implementação das semanas seguintes.
- **Decidido por:** Luiza Goulart Silva, em 15/09/2026.

## D-02 - Interface do aplicativo
- **Data:** 15/09/2026
- **Decisão:** Views com ViewBinding.
- **Alternativas consideradas:** Jetpack Compose.
- **Motivo:** A interface precisa exibir operacao, eventos, aprendizado, desempenho e frota; Views atende esse escopo sem introduzir Kotlin, Compose e dependencias adicionais.
- **Consequencias:** A interface sera implementada com layouts XML e atualizacoes controladas fora da thread de aquisicao.
- **Decidido por:** Luiza Goulart Silva, em 15/09/2026.

## D-03 - Concorrencia e tarefas periodicas
- **Data:** 15/09/2026
- **Decisao:** `ScheduledExecutorService` para aquisicao periodica e `ExecutorService` para tarefas de melhor esforco.
- **Alternativas consideradas:** Corrotinas; um executor compartilhado para todas as tarefas.
- **Motivo:** Threads e executores tornam visiveis o periodo, o prazo, a carga e a interferencia exigidos pela analise de escalonabilidade.
- **Consequencias:** A regiao critica do `Track` sera protegida explicitamente; os tempos de execucao serao medidos por tarefa na AV2.
- **Decidido por:** Luiza Goulart Silva, em 15/09/2026.

## D-04 - Persistencia dos eventos
- **Data:** 15/09/2026
- **Decisao:** `SQLiteOpenHelper`, com os campos sensiveis cifrados individualmente por AES-GCM.
- **Alternativas consideradas:** Room; arquivo cifrado em anexacao; biblioteca que cifra o banco inteiro.
- **Motivo:** A solucao evita dependencia de processador de anotacoes, permite consultas locais e preserva a implementacao de AES-GCM exigida pela rubrica.
- **Consequencias:** A persistencia sera implementada na fase de seguranca; nenhum evento pessoal sera gravado em claro.
- **Decidido por:** Luiza Goulart Silva, em 15/09/2026.

## D-05 - Serializacao
- **Data:** 15/09/2026
- **Decisao:** `org.json` para chamadas de ferramenta e mensagens pequenas de telemetria entre nos.
- **Alternativas consideradas:** Gson; kotlinx.serialization.
- **Motivo:** `org.json` ja esta disponivel no Android, funciona com Java e e suficiente para objetos pequenos sem nova dependencia.
- **Consequencias:** Toda saida do agente sera validada antes de influenciar o sistema; o formato de telemetria permanecera minimo para reduzir consumo de radio.
- **Decidido por:** Luiza Goulart Silva, em 15/09/2026.

## D-09 - Migração para estrutura de módulos Android
- **Data:** 16/09/2026
- **Decisão:** Mover o código de domínio e contratos para o módulo `:app`.
- **Alternativas consideradas:** Manter estrutura de biblioteca Java pura; usar módulos separados para cada camada.
- **Motivo:** O Android Studio e as APIs de sensores/serviços de localização funcionam de forma mais integrada dentro de um módulo de aplicação. Facilita o uso de recursos (XML) e ViewBinding.
- **Consequências:** Os caminhos de source e test foram atualizados para `app/src/main` e `app/src/test`.
- **Decidido por:** Luiza Goulart Silva, em 16/09/2026.

## D-06 - Motor de inferencia local
- **Data:** 15/09/2026
- **Decisao:** O `RuleEngine` sera a linha de base obrigatoria. O `LocalEngine` sera avaliado somente se o levantamento do aparelho confirmar arquitetura e memoria compativeis.
- **Alternativas consideradas:** Usar somente o modelo local; comprometer a entrega com um modelo especifico antes do levantamento.
- **Motivo:** O sistema deve continuar funcionando sem modelo de linguagem, e a proposta exige degradacao graciosa. O hardware ainda precisa ser medido antes da decisao sobre o motor opcional.
- **Consequencias:** O arquivo do modelo nunca sera versionado; a AV2 comparara o motor deterministico com o motor local quando este for suportado.
- **Decidido por:** Luiza Goulart Silva, em 15/09/2026.

## D-07 - Nivel minimo de API
- **Data:** 15/09/2026
- **Decisao:** `minSdk 26`.
- **Alternativas consideradas:** `minSdk 24`; `minSdk 29` ou superior.
- **Motivo:** O nivel 26 preserva ampla compatibilidade e disponibiliza a acuracia de velocidade necessaria para a incerteza das leituras GNSS.
- **Consequencias:** O servico em primeiro plano e as permissoes de localizacao terao verificacoes condicionais quando exigidas pela versao do Android.
- **Decidido por:** Luiza Goulart Silva, em 15/09/2026.

## D-08 - Organizacao do trabalho individual
- **Data:** 15/09/2026
- **Decisao:** Desenvolvimento individual por branches de fase, com uma Pull Request para a linha principal e revisao propria registrada antes do merge; solicitacao de revisao ao docente quando disponivel.
- **Alternativas consideradas:** Trabalho direto na linha principal; dividir o projeto por arquivos como se fosse uma dupla.
- **Motivo:** Branches e Pull Requests preservam o historico das decisoes e permitem revisar cada entrega sem criar uma divisao artificial de componentes.
- **Consequencias:** Cada fase tera commits atribuiveis e concentrados no escopo da semana; mudancas de decisao serao registradas neste arquivo antes ou junto da alteracao.
- **Decidido por:** Luiza Goulart Silva, em 15/09/2026.
