package me.chester.minitruco.android;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import java.util.logging.Level;
import java.util.logging.Logger;

import me.chester.minitruco.core.Carta;
import me.chester.minitruco.core.Jogador;
import me.chester.minitruco.core.Partida;

/**
 * Jogador que controla o celular na camada Android.
 * Trabalha em conjunto com TrucoActivity e MesaView para exibir o jogo
 * e receber toques na tela e inputs do usuário.
 */
public class JogadorHumano extends me.chester.minitruco.core.JogadorHumano {

    private final static Logger LOGGER = Logger.getLogger("JogadorHumano");

    private final TrucoActivity activity;
    private final MesaView mesa;

    public JogadorHumano(TrucoActivity activity, MesaView mesa) {
        this.activity = activity;
        this.mesa = mesa;
    }

    @Override
    public void cartaJogada(Jogador j, Carta c) {
        mesa.setPosicaoVez(0);
        mesa.descarta(c, posicaoNaTela(j));
        LOGGER.log(Level.INFO, "Jogador na posicao de tela " + posicaoNaTela(j) + " jogou " + c);
    }

    @Override
    public void entrouNoJogo(Jogador j, Partida p) {
        // Sem ação necessária por enquanto
    }

    @Override
    public void inicioMao(Jogador jogadorQueAbre) {
        LOGGER.log(Level.INFO, "Distribuindo a mão");
        mesa.distribuiMao();
        mesa.setPosicaoVez(posicaoNaTela(jogadorQueAbre));
        activity.tiraDestaqueDoPlacar();
    }

    @Override
    public void inicioPartida(int dummy1, int dummy2) {
        // A Fodinha usa as "Vidas" guardadas no SituacaoJogo e não placar de equipes
        activity.atualizaVidasNaTela(); 
    }

    @Override
    public void jogoAbortado(int posicao, int rndFrase) {
        if (posicao != 0 && mesa != null) {
            mesa.diz("abortou", convertePosicaoJogadorParaPosicaoTela(posicao), 1000, rndFrase);
            mesa.aguardaFimAnimacoes();
        }
        if (activity != null) {
            activity.partidaAbortada = true;
            activity.finish();
        }
    }

    @Override
    public void jogoFechado(int numVencedor, int rndFrase) {
        // A Fodinha é free-for-all. Ganhei se o numVencedor for a minha posição no Core
        boolean ganhei = (numVencedor == this.getPosicao());
        mesa.diz(ganhei ? "vitoria" : "derrota", 1, 1000, rndFrase);
        mesa.aguardaFimAnimacoes();
        activity.jogoFechado(numVencedor);
    }

    @Override
    public void vez(Jogador j, boolean isFasePalpite) {
        LOGGER.log(Level.INFO, "Vez do jogador tela: " + posicaoNaTela(j));
        
        // Se for a vez do jogador humano do celular...
        if (j.equals(this)) {
            if (isFasePalpite) {
                // Se a mesa pede palpite, mandamos a Activity abrir o pop-up
                activity.pedePalpite();
            } else {
                // Se a mesa pede carta, destrava o toque na tela
                mesa.vez(true);
            }
        } else {
            // Se for vez de outro, trava o toque para o humano
            mesa.vez(false);
        }
        
        mesa.setPosicaoVez(posicaoNaTela(j));
    }

    /**
     * Retorna a posição do jogador na tela (convertendo de 1-6 para o formato circular).
     * O Jogador humano do celular (este objeto) sempre senta na posição 1 da TELA (embaixo).
     *
     * @return 1 (Inferior), 2 (Dir Baixo), 3 (Dir Cima), 4 (Topo), 5 (Esq Cima), 6 (Esq Baixo)
     */
    public int posicaoNaTela(Jogador j) {
        return convertePosicaoJogadorParaPosicaoTela(j.getPosicao());
    }

    private int convertePosicaoJogadorParaPosicaoTela(int posicaoJogadorCore) {
        // Pegamos o total de jogadores direto da Partida
        int totalMesa = partida != null ? partida.getJogadoresVivos() : 6; 
        
        int pos = posicaoJogadorCore - this.getPosicao() + 1;
        if (pos < 1) {
            pos = pos + 6; // Ajustado para mesa de até 6
        }
        return pos;
    }
}
