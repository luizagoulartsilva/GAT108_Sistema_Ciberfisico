# GAT108 - Sistema ciberfísico para trabalhador isolado

Projeto da disciplina de Automação Avançada - UFLA 2026.

## S1 - domínio, contratos e estruturas

O domínio representa um trabalhador de campo e suas leituras de aceleração, velocidade,
posição e incerteza do sensor. `Track` é um buffer circular de capacidade fixa e valida
instantes estritamente crescentes. Os contratos de aquisição, reconciliação e agente estão
em `src/main/java/br/ufla/gat108/contracts` e ainda não possuem implementação.

Para executar a bateria quando o Gradle estiver disponível:

```text
gradle test
```

Os testes de invariantes do domínio são verdes. Os três testes de contrato falham
intencionalmente com `UnsupportedOperationException`, documentando o método contrato antes
do código exigido pela S1.
