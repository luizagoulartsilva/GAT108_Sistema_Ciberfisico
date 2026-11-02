# Implementação da Capacidade 10: Protocolo Cego, Métricas e Lei de Amdahl

Este plano detalha a implementação e formalização da **Capacidade 10** do sistema ciberfísico de segurança para trabalhador isolado (GAT108):
1. **Protocolo Cego de Validação (*Blind Validation Protocol*):** Validação rigorosa em massa com conjunto de dados retidos (*out-of-sample*) sem nenhum vazamento de treino, comparando o Motor Híbrido vs. Motor Determinístico.
2. **Métricas Quantitativas de Classificação e Desempenho:** Acurácia, Precisão, Sensibilidade (Recall), Especificidade e F1-Score, além de métricas de latência e resíduos cinemáticos.
3. **Lei de Amdahl:** Modelagem matemática e ensaio automatizado do speedup computacional da inferência e processamento concorrente em borda para múltiplos núcleos ($N$ cores).

---

## User Review Required

> [!IMPORTANT]
> Os novos testes automatizados (`BlindValidationProtocolTest.java` e `AmdahlLawTest.java`) serão adicionados ao pacote de testes unitários do app (`app/src/test/java/br/ufla/gat108/`), garantindo execução limpa via `./gradlew :app:testDebugUnitTest`.

---

## Proposed Changes

### Componente de Testes e Validação (Capacidade 10)

#### [NEW] [BlindValidationProtocolTest.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/test/java/br/ufla/gat108/BlindValidationProtocolTest.java)
- Implementa o protocolo de validação cega com dados retidos (70% treino / 30% teste cego congelado).
- Executa a avaliação comparativa entre o Motor Híbrido (Reconciliação WLS + Regras + IA Local) e o Motor Determinístico Puro.
- Extrai e valida a Matriz de Confusão e Métricas (Acurácia $\ge 90\%$, Precisão, Sensibilidade, Especificidade e F1-Score $\ge 90\%$).

#### [NEW] [AmdahlLawTest.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/test/java/br/ufla/gat108/AmdahlLawTest.java)
- Implementa a modelagem da Lei de Amdahl $S(N) = \frac{1}{(1 - P) + \frac{P}{N}}$ para o pipeline concorrente de borda (onde a fração paralelizável $P = 0.85$ e a fração sequencial $1 - P = 0.15$ engloba o buffer protegido e ingestão a 50Hz).
- Executa simulação de benchmarks para $N \in \{1, 2, 4, 8, 16\}$ núcleos.
- Valida o speedup obtido e gera tabela formatada com o rendimento de paralelização e eficiência em hardware multicore.

---

## Verification Plan

### Automated Tests
- Executar `./gradlew :app:testDebugUnitTest --tests BlindValidationProtocolTest`
- Executar `./gradlew :app:testDebugUnitTest --tests AmdahlLawTest`
- Executar todos os testes do projeto para garantir integridade contínua.
