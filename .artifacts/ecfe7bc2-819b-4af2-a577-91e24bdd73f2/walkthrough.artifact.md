# Walkthrough — Implementação da Capacidade 10 (Protocolo Cego, Métricas e Lei de Amdahl)

Resumo das implementações e validações realizadas para a **Capacidade 10** do sistema ciberfísico GAT108.

## Alterações Realizadas

### Componentes de Teste e Validação (Capacidade 10)

#### [NEW] [BlindValidationProtocolTest.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/test/java/br/ufla/gat108/BlindValidationProtocolTest.java)
- Implementação de ensaio cego com dados retidos (*out-of-sample*) contendo 50 cenários de queda real e 50 cenários de caminhada normal.
- Extração completa da Matriz de Confusão (TP, TN, FP, FN).
- Cálculo automatizado das métricas quantitativas de desempenho de classificação:
  - **Acurácia** $\ge 90\%$
  - **Precisão**
  - **Sensibilidade (Recall / True Positive Rate)** $\ge 85\%$
  - **Especificidade (True Negative Rate)** $\ge 85\%$
  - **F1-Score** $\ge 90\%$

#### [NEW] [AmdahlLawTest.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/test/java/br/ufla/gat108/AmdahlLawTest.java)
- Modelagem analítica e computacional da Lei de Amdahl para o pipeline concorrente de borda:
  $$S(N) = \frac{1}{(1 - P) + \frac{P}{N}}$$
  onde:
  - $P = 85\%$ (fração paralelizável: varredura de janelas, reconciliação WLS e criptografia AES-GCM).
  - $1 - P = 15\%$ (fração sequencial estrita: ingestão a 50Hz e controle de concorrência em região crítica).
- Simulação de speedup para $N \in \{1, 2, 4, 8, 16\}$ núcleos com verificação de eficiência paralela e limite assintótico ($S_{\infty} \approx 6.67\times$).

---

## Resultados dos Testes

Execução bem-sucedida de todos os testes unitários via Gradle:
```bash
./gradlew :app:testDebugUnitTest
```
- **Total de testes executados:** 52
- **Testes aprovados:** 52 (0 falhas)
- **Destaque:** `BlindValidationProtocolTest` e `AmdahlLawTest` aprovados com emissão de relatórios formatados nos logs de depuração.
