package me.chester.minitruco.android;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import java.util.logging.Level;
import java.util.logging.Logger;

import me.chester.minitruco.core.Carta;
import me.chester.minitruco.core.Jogador;
import me.chester.minitruco.core.Partida;

public class JogadorHumano extends me.chester.minitruco.core.JogadorHumano {

    private final static Logger LOGGER = Logger.getLogger("JogadorHumano");
    private final TrucoActivity activity;
    private final MesaView mesa;

    public JogadorHumano(TrucoActivity activity, MesaView mesa) {
        this.activity = activity;
        this.mesa = mesa;
        this.setNome("Você"); // O fim do "unnamed"!
    }

    @Override
    public void cartaJogada(Jogador j, Carta c) {
        mesa.setPosicaoVez(0);
        mesa.descarta(c, posicaoNaTela(j));
        LOGGER.log(Level.INFO, "Jogador na posicao de tela " + posicaoNaTela(j) + " jogou " + c);
    }

    @Override
    public void entrouNoJogo(Jogador j, Partida p) {}

    @Override
    public void inicioMao(Jogador jogadorQueAbre) {
        mesa.distribuiMao();
        mesa.setPosicaoVez(posicaoNaTela(jogadorQueAbre));
        activity.tiraDestaqueDoPlacar();
    }

    @Override
    public void inicioPartida(int dummy1, int dummy2) {
        activity.atualizaVidasNaTela();
    }

    @Override
    public void jogoAbortado(int posicao, int rndFrase) {
        // Se alguém abandonou (posicao > 0), o motor core avisa
        if (posicao > 0 && mesa != null) {
            // Converte a posição core para a posição visual de tela
            int posTela = convertePosicaoJogadorParaPosicaoTela(posicao);
            mesa.diz("morte", posTela, 3000); // Grita a frase de morte NAQUELE LUGAR!
            mesa.aguardaFimAnimacoes(); // Espera acabar a frase
        }
    }

    @Override
    public void jogoFechado(int numVencedor, int rndFrase) {
        boolean ganhei = (numVencedor == this.getPosicao());
        mesa.diz(ganhei ? "vitoria" : "derrota", 1, 1000);
        mesa.aguardaFimAnimacoes();
        activity.jogoFechado(numVencedor);
    }

    @Override
    public void vez(Jogador j, boolean isFasePalpite) {
        if (j.equals(this)) {
            if (isFasePalpite) activity.pedePalpite();
            else mesa.vez(true);
        } else {
            mesa.vez(false);
        }
        mesa.setPosicaoVez(posicaoNaTela(j));
    }

    public int posicaoNaTela(Jogador j) {
        return convertePosicaoJogadorParaPosicaoTela(j.getPosicao());
    }

    private int convertePosicaoJogadorParaPosicaoTela(int posicaoJogadorCore) {
        int pos = posicaoJogadorCore - this.getPosicao() + 1;
        if (pos < 1) pos += partida.numJogadores;

        // --- SISTEMA DE ASSENTOS INTELIGENTE ---
        if (partida.numJogadores == 2 && pos == 2) return 4; // 1x1: Frente a frente

        if (partida.numJogadores == 3) { // Triângulo
            if (pos == 2) return 3;
            if (pos == 3) return 5;
        }

        if (partida.numJogadores == 4) { // Cruz
            if (pos == 2) return 3;
            if (pos == 3) return 4;
            if (pos == 4) return 6;
        }

        if (partida.numJogadores == 5) { // Pentágono
            if (pos == 5) return 6;
        }

        return pos; // 6 Jogadores usa o círculo completo
    }
}
