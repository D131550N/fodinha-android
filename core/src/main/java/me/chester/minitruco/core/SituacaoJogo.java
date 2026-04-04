package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

/**
 * Representa o estado da partida em um determinado momento.
 * Na Fodinha, isso inclui saber a quantidade de cartas da rodada,
 * em qual fase estamos (palpites ou cartas), as vidas e quantas
 * rodadas cada jogador prometeu fazer.
 */
public class SituacaoJogo {

    /**
     * Fase atual do jogo.
     * 0 = Aguardando / Distribuindo cartas
     * 1 = Fase de Palpites (dizendo quantas vai fazer)
     * 2 = Fase de Jogo (jogando as cartas na mesa)
     */
    public int faseJogo;

    /**
     * Quantidade total de jogadores na mesa (de 2 a 6)
     */
    public int numJogadores;

    /**
     * Posição do jogador que está recebendo esta situação (1 a numJogadores).
     */
    public int posJogador;

    /**
     * Posição do jogador cuja vez é a atual.
     */
    public int vez;

    /**
     * Quantas cartas cada jogador recebeu nesta rodada (1, 2, 3...)
     */
    public int quantidadeCartasRodada;

    /**
     * Array com as vidas de cada jogador.
     * O índice 0 é ignorado para facilitar o mapeamento das posições (1 a 6).
     * Ex: vidas[1] = vidas do jogador 1.
     */
    public int[] vidas = new int[7];

    /**
     * Array com os palpites (quantas rodadas o jogador disse que faria).
     */
    public int[] palpites = new int[7];

    /**
     * Array com a quantidade de rodadas que o jogador efetivamente já ganhou nesta mão.
     */
    public int[] feitas = new int[7];

    /**
     * Array com as cartas que estão atualmente na mesa.
     */
    public Carta[] cartasJogadas = new Carta[7];

    /**
     * Indica se um jogador específico já foi eliminado (zerou as vidas).
     */
    public boolean[] eliminado = new boolean[7];

    /**
     * Retorna a quantidade de jogadores que ainda estão vivos no jogo.
     */
    public int getJogadoresVivos() {
        int vivos = 0;
        for (int i = 1; i <= numJogadores; i++) {
            if (!eliminado[i]) {
                vivos++;
            }
        }
        return vivos;
    }
}
