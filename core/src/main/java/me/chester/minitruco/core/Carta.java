package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

/**
 * Representa uma carta do jogo.
 * <p>
 * Mantém a estrutura de comunicação, mas com a lógica de força 
 * reescrita para as regras da Fodinha.
 */
public class Carta {

    public Carta(char letra, int naipe) {
        setLetra(letra);
        setNaipe(naipe);
    }

    public Carta(String sCarta) {
        this(sCarta.charAt(0), "coepx".indexOf(sCarta.charAt(1)));
    }

    public static final int NAIPE_COPAS = 0;
    public static final int NAIPE_OUROS = 1;
    public static final int NAIPE_ESPADAS = 2;
    public static final int NAIPE_PAUS = 3;

    public static final int[] NAIPES = { NAIPE_COPAS, NAIPE_ESPADAS,
            NAIPE_OUROS, NAIPE_PAUS };

    public static final int NAIPE_NENHUM = 4;
    public static final char LETRA_NENHUMA = 'X';

    // Removidos o 8 e 9, pois o baralho da Fodinha (espanhol sujo) tem 40 cartas
    private static final String LETRAS_VALIDAS = "A234567JQK";

    private char letra = LETRA_NENHUMA;
    private int naipe = NAIPE_NENHUM;
    private boolean fechada = false;

    public void setLetra(char letra) {
        if (LETRAS_VALIDAS.indexOf(letra) != -1 || letra == LETRA_NENHUMA) {
            this.letra = letra;
        }
    }

    public char getLetra() {
        return letra;
    }

    public void setNaipe(int naipe) {
        if (naipe == NAIPE_COPAS || naipe == NAIPE_OUROS || naipe == NAIPE_PAUS
                || naipe == NAIPE_ESPADAS || naipe == NAIPE_NENHUM) {
            this.naipe = naipe;
        }
    }

    public int getNaipe() {
        return naipe;
    }

    public void setFechada(boolean fechada) {
        this.fechada = fechada;
    }

    public boolean isFechada() {
        return fechada;
    }

    public boolean equals(Object outroObjeto) {
        if ((outroObjeto instanceof Carta)) {
            Carta outraCarta = (Carta) outroObjeto;
            return outraCarta.getNaipe() == this.getNaipe()
                    && outraCarta.getLetra() == this.getLetra();
        }
        return false;
    }

    public int hashCode() {
        return getLetra() * 256 + getNaipe();
    }

    /**
     * LÓGICA DA FODINHA: Retorna a força base da carta para comparação.
     * Ordem de força: 4, 5, 6, 7, 10(J), 11(Q), 12(K), 1(A), 2, 3
     */
    public int getValorFodinha() {
        switch (this.letra) {
            case '4': return 1;
            case '5': return 2;
            case '6': return 3;
            case '7': return 4;
            case 'J': return 5; // Representa o 10 (Sota)
            case 'Q': return 6; // Representa o 11 (Cavalo)
            case 'K': return 7; // Representa o 12 (Rei)
            case 'A': return 8; // Representa o 1 (Ás)
            case '2': return 9;
            case '3': return 10;
            default: return 0;
        }
    }

    /**
     * LÓGICA DA FODINHA: Força do Naipe para critério de desempate.
     * Ordem de força: Paus (4) > Copas (3) > Espadas (2) > Ouros (1)
     */
    public int getValorDesempateNaipe() {
        switch (this.naipe) {
            case NAIPE_PAUS:    return 4; // Gato / Zap
            case NAIPE_COPAS:   return 3;
            case NAIPE_ESPADAS: return 2;
            case NAIPE_OUROS:   return 1;
            default: return 0;
        }
    }

    public String toString() {
        return letra + "" + ("coepx").charAt(naipe);
    }
}
