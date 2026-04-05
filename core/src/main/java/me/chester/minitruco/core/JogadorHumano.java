package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

/**
 * Esta superclasse só existe para que o core possa identificar instâncias do jogador humano.
 * <p>
 * A implementação real (exibição das cartas, botões de palpite, animações, etc.)
 * ocorre na camada de Interface Gráfica (Android).
 */
public abstract class JogadorHumano extends Jogador {
    
    // A implementação dos métodos herdados de Jogador.java (vez, inicioMao, etc.)
    // será feita na classe que herdar desta lá na pasta do Android.
    
}
