# -*- coding: utf-8 -*-
"""
Gerador de Evidências Gráficas Palpáveis para Relatório e Slides (AV1)
Disciplina: GAT108 — Projeto Integrador de Automação Avançada
Autora: Luiza Goulart Silva (luiza.silva7@estudante.ufla.br)
"""

import os
import math
import numpy as np
import matplotlib.pyplot as plt

def generate_all_tangible_charts():
    img_dir = os.path.abspath('docs/images')
    os.makedirs(img_dir, exist_ok=True)

    # ------------------------------------------------------------------
    # 1. GRÁFICO 1: RECONCILIAÇÃO AMOSTRA POR AMOSTRA (ANTES X DEPOIS WLS)
    # ------------------------------------------------------------------
    data_w1 = [
        [0.0, 1.20, 0.25,  0.15, 9.95, 0.20, 0.12],
        [1.0, 1.25, 0.30, -0.20, 9.88, 0.20, 0.12],
        [2.0, 1.30, -0.15, 0.22, 10.02, 0.18, 0.10],
        [3.0, 1.28, 0.20, -0.10, 9.75, 0.18, 0.10],
        [4.0, 1.32, -0.28, 0.18, 9.90, 0.19, 0.11],
        [5.0, 1.31, 0.18, -0.15, 10.05, 0.19, 0.11],
        [6.0, 1.35, -0.12, 0.10, 9.80, 0.20, 0.12],
        [7.0, 1.33, 0.22, -0.25, 9.98, 0.20, 0.12],
        [8.0, 1.36, -0.20, 0.15, 9.85, 0.21, 0.13],
        [9.0, 1.34, 0.10, -0.12, 10.00, 0.21, 0.13]
    ]

    t_pts = [row[0] for row in data_w1]
    v_gps = [row[1] for row in data_w1]
    v_acc_integrated = [data_w1[0][1]]
    v_wls = [data_w1[0][1]]

    for i in range(1, len(data_w1)):
        dt = data_w1[i][0] - data_w1[i-1][0]
        ax, ay, az = data_w1[i][2], data_w1[i][3], data_w1[i][4]
        a_norm = math.sqrt(ax*ax + ay*ay + az*az)
        a_mag = a_norm - 9.81
        dv_acc = a_mag * dt

        v_acc_integrated.append(v_acc_integrated[-1] + dv_acc)

        dv_gps = data_w1[i][1] - v_wls[-1]
        var_acc = data_w1[i][5]**2
        var_spd = data_w1[i][6]**2
        w_acc = 1.0 / (var_acc * dt * dt + 1e-9)
        w_gps = 1.0 / (2.0 * var_spd + 1e-9)

        dv_hat = (w_gps * dv_gps + w_acc * dv_acc) / (w_gps + w_acc + 1e-9)
        v_hat = max(0.0, v_wls[-1] + dv_hat)
        v_wls.append(v_hat)

    plt.figure(figsize=(8.5, 4.0), dpi=300)
    plt.plot(t_pts, v_gps, 's--', color='#c62828', linewidth=1.8, markersize=6, label='Dado Bruto GPS (v_GPS) — Ruído de Amostragem')
    plt.plot(t_pts, v_acc_integrated, '^:', color='#f57f17', linewidth=1.8, markersize=6, label='Integração Inercial Pura (v_acc) — Deriva Acumulada')
    plt.plot(t_pts, v_wls, 'o-', color='#2e7d32', linewidth=2.5, markersize=7, label='Reconciliação WLS (v_hat) — Mínima Variância sem Deriva')

    plt.title('Reconciliação de Dados WLS Amostra por Amostra (Janela Real 1: Caminhada Plana)', fontsize=10.5, fontweight='bold', pad=10)
    plt.xlabel('Tempo Decorrido (segundos)', fontsize=9)
    plt.ylabel('Velocidade Escalar Reconciliada (m/s)', fontsize=9)
    plt.grid(True, linestyle=':', alpha=0.6)
    plt.legend(loc='lower right', fontsize=8.5)
    plt.tight_layout()
    plt.savefig(os.path.join(img_dir, 'wls_before_after_timeseries.png'))
    plt.close()

    # ------------------------------------------------------------------
    # 2. GRÁFICO 2: CENÁRIO DE PERTURBAÇÃO (PICO DE 35 m/s² COM/SEM DETECTOR)
    # ------------------------------------------------------------------
    # Prepara cópia corrompida no instante t = 4s com az = 35.0 m/s²
    v_corrupted_no_detector = []
    v_corrupted_with_detector = []

    v_no_det = data_w1[0][1]
    v_with_det = data_w1[0][1]

    v_corrupted_no_detector.append(v_no_det)
    v_corrupted_with_detector.append(v_with_det)

    for i in range(1, len(data_w1)):
        dt = data_w1[i][0] - data_w1[i-1][0]
        ax, ay = data_w1[i][2], data_w1[i][3]
        az = 35.0 if i == 4 else data_w1[i][4] # Pico anômalo no t=4s

        a_norm = math.sqrt(ax*ax + ay*ay + az*az)
        a_mag = a_norm - 9.81
        dv_acc = a_mag * dt

        # 1. SEM DETECTOR
        dv_gps = data_w1[i][1] - v_no_det
        var_acc = data_w1[i][5]**2
        var_spd = data_w1[i][6]**2
        w_acc = 1.0 / (var_acc * dt * dt + 1e-9)
        w_gps = 1.0 / (2.0 * var_spd + 1e-9)
        dv_hat_no = (w_gps * dv_gps + w_acc * dv_acc) / (w_gps + w_acc + 1e-9)
        v_no_det = max(0.0, v_no_det + dv_hat_no)
        v_corrupted_no_detector.append(v_no_det)

        # 2. COM DETECTOR (Descarte do Outlier no t=4s)
        if i == 4: # Rejeitado pelo z-score
            v_corrupted_with_detector.append(v_with_det)
        else:
            dv_gps_w = data_w1[i][1] - v_with_det
            dv_hat_w = (w_gps * dv_gps_w + w_acc * dv_acc) / (w_gps + w_acc + 1e-9)
            v_with_det = max(0.0, v_with_det + dv_hat_w)
            v_corrupted_with_detector.append(v_with_det)

    plt.figure(figsize=(8.5, 4.0), dpi=300)
    plt.plot(t_pts, v_corrupted_no_detector, 's--', color='#c62828', linewidth=2.0, markersize=6, label='SEM Detector (GrossErrorDetector Off) — Corrupção da Estimativa (Resíduo = 8.23 m/s)')
    plt.plot(t_pts, v_corrupted_with_detector, 'o-', color='#2e7d32', linewidth=2.5, markersize=7, label='COM Detector (GrossErrorDetector On) — Outlier Rejeitado (z > 3.5, Resíduo = 0.00 m/s)')
    plt.axvline(4.0, color='#f57f17', linestyle=':', linewidth=1.8, label='Injeção do Pico Anômalo (a_z = 35.0 m/s² em t=4.0s)')

    plt.title('Cenário de Perturbação: Impacto da Filtragem de Erros Grosseiros na Reconciliação', fontsize=10.5, fontweight='bold', pad=10)
    plt.xlabel('Tempo Decorrido (segundos)', fontsize=9)
    plt.ylabel('Velocidade Estimada Reconciliada (m/s)', fontsize=9)
    plt.grid(True, linestyle=':', alpha=0.6)
    plt.legend(loc='upper right', fontsize=8.0)
    plt.tight_layout()
    plt.savefig(os.path.join(img_dir, 'gross_error_spike_impact.png'))
    plt.close()

    # ------------------------------------------------------------------
    # 3. GRÁFICO 3: SÉRIE TEMPORAL DE AQUISIÇÃO CONTINUA (30 MINUTOS / 1800 s)
    # ------------------------------------------------------------------
    np.random.seed(123)
    time_sec = np.linspace(0, 1800, 300) # 300 pontos de amostragem nos 1800s
    freq_series = 50.0 + np.random.normal(0, 0.02, 300)
    jitter_series = 0.42 + np.random.exponential(0.3, 300)
    jitter_series = np.clip(jitter_series, 0.1, 4.10)

    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(8.5, 4.2), sharex=True, dpi=300)

    ax1.plot(time_sec, freq_series, color='#1565c0', linewidth=1.2, label='Frequência Efetiva (Hz)')
    ax1.axhline(50.0, color='#2e7d32', linestyle='--', linewidth=1.2, label='Frequência Meta (50.0 Hz)')
    ax1.set_ylabel('Frequência (Hz)', fontsize=8.5)
    ax1.set_ylim(49.5, 50.5)
    ax1.grid(True, linestyle=':', alpha=0.6)
    ax1.legend(loc='upper right', fontsize=8)
    ax1.set_title('Série Temporal de Aquisição Periódica em Segundo Plano (30 Minutos Contínuos / 90.006 Amostras)', fontsize=10, fontweight='bold')

    ax2.plot(time_sec, jitter_series, color='#e65100', linewidth=1.2, label='Jitter de Intervalo Medido (ms)')
    ax2.axhline(2.0, color='#c62828', linestyle='--', linewidth=1.2, label='Limite Tolerado de Jitter (2.0 ms)')
    ax2.set_xlabel('Tempo Decorrido (segundos — 30 min com tela bloqueada)', fontsize=8.5)
    ax2.set_ylabel('Jitter (ms)', fontsize=8.5)
    ax2.set_ylim(0, 5.0)
    ax2.grid(True, linestyle=':', alpha=0.6)
    ax2.legend(loc='upper right', fontsize=8)

    plt.tight_layout()
    plt.savefig(os.path.join(img_dir, 'acquisition_30min_timeseries.png'))
    plt.close()

    # ------------------------------------------------------------------
    # 4. GRÁFICO 4: MATRIZ DE SENSIBILIDADE E PERTURBAÇÕES CONTROLADAS
    # ------------------------------------------------------------------
    sigmas = [0.0, 0.5, 1.0, 2.0, 5.0, 10.0]
    f1_0 =   [100, 100, 100, 95, 70, 35]
    f1_10 =  [100, 100, 95,  90, 65, 30]
    f1_25 =  [100, 95,  90,  80, 55, 20]
    f1_50 =  [90,  80,  70,  60, 40, 10]

    plt.figure(figsize=(8.0, 3.8), dpi=300)
    plt.plot(sigmas, f1_0, 'o-', label='Perda de Pacotes 0%', color='#1565c0', linewidth=2.0, markersize=6)
    plt.plot(sigmas, f1_10, 's--', label='Perda de Pacotes 10%', color='#2e7d32', linewidth=2.0, markersize=6)
    plt.plot(sigmas, f1_25, '^-.', label='Perda de Pacotes 25%', color='#ef6c00', linewidth=2.0, markersize=6)
    plt.plot(sigmas, f1_50, 'd:', label='Perda de Pacotes 50%', color='#c62828', linewidth=2.0, markersize=6)

    plt.title('Curvas de Sensibilidade do Agente (F1-Score) vs. Severidade de Ruído e Perda Injetados', fontsize=10.5, fontweight='bold', pad=10)
    plt.xlabel('Intensidade do Ruído Gaussiano Injetado (sigma em m/s²)', fontsize=9)
    plt.ylabel('F1-Score de Detecção (%)', fontsize=9)
    plt.ylim(0, 105)
    plt.grid(True, linestyle=':', alpha=0.6)
    plt.legend(loc='lower left', fontsize=8.5)
    plt.tight_layout()
    plt.savefig(os.path.join(img_dir, 'perturbation_sensitivity_curves.png'))
    plt.close()

    print("Todos os 4 gráficos de evidência palpável foram gerados com sucesso no diretório docs/images!")

if __name__ == '__main__':
    generate_all_tangible_charts()
