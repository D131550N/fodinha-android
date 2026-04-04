package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

/**
 * Define as configurações base do jogo.
 * Como o projeto agora é exclusivo para Fodinha, temos apenas uma regra.
 */
public class Modo {

    /**
     * Retorna a instância do modo Fodinha, ignorando o que vier do Bluetooth antigo.
     */
    public static Modo fromString(String modoStr) {
        return new Modo();
    }

    public static boolean isModoValido(String modoStr) {
        // Aceitamos qualquer conexão, mas sempre forçamos as regras da Fodinha
        return true;
    }

    public static String[] getModosValidos() {
        return new String[] { "F" }; // "F" de Fodinha
    }

    /**
     * @return false, pois a Fodinha sempre joga com baralho sujo (40 cartas)
     */
    public boolean isBaralhoLimpo() {
        return false;
    }

    /**
     * @return true, pois na Fodinha as manilhas são fixas (sem o "vira")
     */
    public boolean isManilhaVelha() {
        return true;
    }
}
