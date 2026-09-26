# Walkthrough - Capacidade 2 e 4: Reconciliação e Aquisição

Implementação completa do fluxo de dados: Aquisição ruidosa -> Reconciliação cinemática -> Decisão do Agente.

## Mudanças Realizadas

### 1. Aquisição em Tempo Real (Capacidade 4)
- [AndroidSensorAcquisition.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/acquisition/AndroidSensorAcquisition.java): Ponte entre as APIs de hardware do Android (`SensorManager`, `LocationManager`) e o modelo de domínio. Mapeia a acurácia qualitativa para valores quantitativos de desvio padrão.
- [SensorService.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/acquisition/SensorService.java): **Foreground Service** que executa o loop de aquisição a **50Hz** (20ms) utilizando um `ScheduledExecutorService`. Garante que o monitoramento continue mesmo com a tela bloqueada.
- [AndroidManifest.xml](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/AndroidManifest.xml): Registradas as permissões de localização e o serviço de monitoramento.

### 2. Incerteza e Reconciliação (Capacidade 2)
- [UncertaintyModel.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/domain/UncertaintyModel.java): Transforma acurácias em pesos estatísticos ($1/\sigma^2$).
- [KinematicReconciler.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/reconciliation/KinematicReconciler.java): Implementa o ajuste de Mínimos Quadrados Ponderados (WLS) fundindo GPS e Acelerômetro.
- [GrossErrorDetector.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/reconciliation/GrossErrorDetector.java): Filtro de resíduo que descarta amostras anômalas antes do ajuste.

### 3. Agente de Regras (Capacidade 8 - Baseline)
- [RuleAgent.java](file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/agent/RuleAgent.java): Implementação básica do agente que detecta quedas através da magnitude do vetor de aceleração.

## Validação e Testes
Todos os testes de contrato foram atualizados para usar as implementações reais.
- `8 tests completed, 0 failed`

> [!IMPORTANT]
> O sistema agora possui um loop completo de monitoramento. As exceções `UnsupportedOperationException` da fase inicial foram todas substituídas por lógica funcional.

render_diffs(file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/main/java/br/ufla/gat108/acquisition/SensorService.java)
render_diffs(file:///C:/Users/luiza/StudioProjects/GAT108_Sistema_Ciberfisico/app/src/test/java/br/ufla/gat108/AcquisitionContractTest.java)
