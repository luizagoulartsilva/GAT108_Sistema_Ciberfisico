# Plano de Implementação: Incerteza e Reconciliação (Capacidade 2)

Este plano detalha a implementação da lógica de reconciliação cinemática utilizando Mínimos Quadrados Ponderados (WLS) e a modelagem de incerteza baseada na acurácia dos sensores, conforme a Proposta e a Capacidade 2.

## User Review Required

> [!IMPORTANT]
> A implementação da reconciliação focará no modelo cinemático $v_k = v_{k-1} + a_k \cdot \Delta t$. Utilizaremos uma abordagem escalar para cada eixo ($X, Y, Z$) para manter a simplicidade e performance em dispositivos de borda, evitando dependências pesadas de álgebra linear.

> [!NOTE]
> O "buffer de leituras" solicitado será implementado como um método utilitário no `Track` ou uma classe específica para extrair janelas temporais, garantindo que o `Reconciler` receba o contexto necessário.

## Propostas de Mudanças

### Domínio e Incerteza

#### [NEW] [UncertaintyModel.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/domain/UncertaintyModel.java) (Já criado)
Responsável por converter a acurácia (1-sigma) informada pela API em variância ($\sigma^2$) e pesos de ajuste ($1/\sigma^2$).

#### [MODIFY] [Track.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/domain/Track.java)
Adicionar método `getSlidingWindow(int size)` para facilitar a extração de janelas para a reconciliação.

### Camada de Contratos (Implementação)

#### [NEW] [KinematicReconciler.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/reconciliation/KinematicReconciler.java)
Implementação do `ReconciliationContract`.
- Cálculo do resíduo cinemático antes do ajuste.
- Aplicação de WLS para ajustar velocidade e aceleração.
- [OPCIONAL] `GrossErrorDetector` para rejeitar picos anômalos.

### Testes

#### [MODIFY] [ReconciliationContractTest.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/test/java/br/ufla/gat108/ReconciliationContractTest.java)
Substituir o teste que falha por uma bateria de testes reais com dados simulados, validando a redução do resíduo após a reconciliação.

## Plano de Verificação

### Testes Automatizados
1. Executar `./gradlew :app:testDebugUnitTest --tests ReconciliationContractTest`.
2. Validar que o `residualAfter` é sempre menor ou igual ao `residualBefore`.
3. Teste de Injeção de Falha: Validar que erros grosseiros artificiais são detectados ou minimizados no ajuste.

### Verificação Manual
1. Inspecionar os logs de teste para verificar os valores de covariância gerados a partir de diferentes níveis de acurácia (ex: GPS com sinal fraco vs. forte).
