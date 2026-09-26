# GAT108 - Sistema ciberfísico para trabalhador isolado

Projeto da disciplina de Automação Avançada - UFLA 2026.

## S1 - domínio, contratos e estruturas

O domínio representa um trabalhador de campo e suas leituras de aceleração, velocidade,
posição e incerteza do sensor. `Track` é um buffer circular de capacidade fixa e valida
instantes estritamente crescentes. Os contratos de aquisição, reconciliação e agente estão
em `app/src/main/java/br/ufla/gat108/contracts` e ainda não possuem implementação.

Para executar a bateria no Android Studio:
1. Abra a aba **Gradle** à direita.
2. Navegue em `GAT108_Sistema_Ciberfisico > app > Tasks > verification > testDebugUnitTest`.
3. Ou execute via terminal: `./gradlew :app:testDebugUnitTest`.

Os testes de invariantes do domínio são verdes. Os três testes de contrato falham
intencionalmente com `UnsupportedOperationException`, documentando o método contrato antes
do código exigido pela S1.
