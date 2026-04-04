package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

/**
 * Uma <code>Estratégia</code> define como o bot joga na sua vez.
 * Na Fodinha, isso envolve duas ações principais: dar o palpite no início
 * da rodada e escolher qual carta jogar na sua vez.
 */
public interface Estrategia {

    /**
     * Define quantas rodadas o bot promete "fazer" nesta mão.
     * <p>
     * O bot deve analisar suas cartas (dentro de SituacaoJogo) e decidir
     * se tem cartas fortes o suficiente para ganhar rodadas.
     *
     * @param s Situação da partida no momento do palpite
     * @return número de rodadas que o bot aposta que vai ganhar
     */
    int fazPalpite(SituacaoJogo s);

    /**
     * Executa uma jogada.
     * <p>
     * Na Fodinha, o bot simplesmente escolhe qual carta da sua mão vai jogar.
     * Ele deve tentar ganhar se ainda não bateu seu palpite, ou tentar perder
     * se já bateu a meta.
     *
     * @param s Situação da partida no momento
     * @return posição (índice) da carta na mão a jogar (0, 1, 2...)
     */
    int joga(SituacaoJogo s);

}
