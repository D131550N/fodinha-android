package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import java.util.Random;
import java.util.Vector;

/**
 * Gerencia as cartas já distribuídas, garantindo que não se sorteie duas vezes
 * a mesma carta.
 */
public class Baralho {

    private final Random random = new Random();

    private Vector<Carta> sorteadas = new Vector<>();

    /**
     * Cria um novo baralho para a Fodinha (sempre 40 cartas).
     */
    public Baralho() {
    }

    /**
     * Sorteia uma carta do baralho.
     * <p>
     * O método garante que a carta não foi sorteada previamente nesta mão.
     *
     * @return carta sorteada
     */
    public Carta sorteiaCarta() {
        Carta c;
        // Na Fodinha, sempre usamos o baralho completo de 40 cartas
        String cartas = "A234567JQK"; 
        do {
            char letra = cartas.charAt(sorteiaDeZeroA(cartas.length() - 1));
            int naipe = Carta.NAIPES[sorteiaDeZeroA(3)];
            c = new Carta(letra, naipe);
        } while (sorteadas.contains(c));
        sorteadas.addElement(c);
        return c;
    }

    /**
     * Recolhe as cartas do baralho, zerando-o para um novo uso na próxima rodada.
     */
    public void embaralha() {
        sorteadas = new Vector<>();
    }

    /**
     * Sorteia números entre 0 e um valor especificado, inclusive
     */
    private int sorteiaDeZeroA(int limiteSuperior) {
        return (random.nextInt(limiteSuperior + 1));
    }

    /**
     * Tira uma carta do baralho, evitando que ela seja sorteada
     *
     * @param c Carta a retirar
     */
    public void tiraDoBaralho(Carta c) {
        sorteadas.addElement(c);
    }
}
