package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import java.util.Vector;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jogador controlado pelo celular (IA).
 * <p>
 * O bot possui uma Thread própria para processar suas jogadas sem travar
 * a partida principal.
 */
public class JogadorBot extends Jogador implements Runnable {

    private final static Logger LOGGER = Logger.getLogger("JogadorBot");
    public static final String APELIDO_BOT = "bot";

    private boolean fingeQuePensa = true;

    final Thread thread;
    private final Estrategia estrategia;
    final SituacaoJogo situacaoJogo = new SituacaoJogo();

    // Controle de turno
    private boolean minhaVez = false;
    private boolean isFasePalpite = false;

    // Cartas que ainda estão na mão do Bot
    private final Vector<Carta> cartasRestantes = new Vector<>();

    public JogadorBot() {
        this(null, null);
    }

    public JogadorBot(ThreadFactory tf) {
        this(null, tf);
    }

    public JogadorBot(Estrategia e, ThreadFactory tf) {
        // Se não passarem uma estratégia específica, usamos essa IA básica
        if (e == null) {
            estrategia = new Estrategia() {
                @Override
                public int fazPalpite(SituacaoJogo s) {
                    // Bot burro: chuta um número aleatório entre 0 e o total de cartas dele
                    return random.nextInt(s.quantidadeCartasRodada + 1);
                }

                @Override
                public int joga(SituacaoJogo s) {
                    // Bot burro: joga uma carta aleatória da mão
                    return random.nextInt(s.cartasJogador.length);
                }
            };
        } else {
            estrategia = e;
        }

        setNome(APELIDO_BOT);
        
        if (tf == null) {
            thread = new Thread(this);
        } else {
            thread = tf.newThread(this);
        }
        thread.start();
    }

    public void setFingeQuePensa(boolean fingeQuePensa) {
        this.fingeQuePensa = fingeQuePensa;
    }

    @Override
    public void vez(Jogador j, boolean isFasePalpite) {
        if (this.equals(j)) {
            LOGGER.log(Level.INFO, "Bot " + this.getPosicao() + " viu que é a vez dele.");
            this.isFasePalpite = isFasePalpite;
            this.minhaVez = true;
        }
    }

    @Override
    public void run() {
        LOGGER.log(Level.INFO, "Thread do Bot " + this + " iniciada");
        
        while (partida == null || !partida.finalizada) {
            sleep(100);

            if (minhaVez) {
                // Dá um tempinho para a tela não piscar instantaneamente
                if (fingeQuePensa) {
                    sleep(800 + random.nextInt(1000));
                }

                atualizaSituacaoJogo();

                try {
                    if (isFasePalpite) {
                        int palpite = estrategia.fazPalpite(situacaoJogo);
                        LOGGER.log(Level.INFO, "Bot " + getPosicao() + " vai dar palpite: " + palpite);
                        partida.fazPalpite(this, palpite);
                    } else {
                        int posCarta = estrategia.joga(situacaoJogo);
                        
                        // Proteção contra IA devolvendo posição inválida
                        if (posCarta < 0 || posCarta >= cartasRestantes.size()) {
                            posCarta = 0;
                        }

                        Carta c = cartasRestantes.elementAt(posCarta);
                        cartasRestantes.removeElement(c);
                        
                        LOGGER.log(Level.INFO, "Bot " + getPosicao() + " vai jogar a carta: " + c);
                        partida.jogaCarta(this, c);
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Erro na execução da IA do Bot", ex);
                }

                minhaVez = false;
            }
        }
        LOGGER.log(Level.INFO, "Thread do Bot " + this + " finalizada");
    }

    /**
     * Atualiza a "foto" da mesa para a Inteligência Artificial analisar
     */
    private void atualizaSituacaoJogo() {
        partida.atualizaSituacao(situacaoJogo, this);
        
        int numCartas = cartasRestantes.size();
        situacaoJogo.cartasJogador = new Carta[numCartas];
        for (int i = 0; i < numCartas; i++) {
            Carta c = cartasRestantes.elementAt(i);
            situacaoJogo.cartasJogador[i] = new Carta(c.getLetra(), c.getNaipe());
        }
    }

    @Override
    public void inicioMao(Jogador jogadorQueAbre) {
        minhaVez = false; // Cancela qualquer resíduo da mão passada
        cartasRestantes.removeAllElements();
        
        // Pega as cartas recebidas nesta rodada e guarda na "mão" do bot
        if (this.getCartas() != null) {
            for (Carta c : this.getCartas()) {
                if (c != null) {
                    cartasRestantes.addElement(c);
                }
            }
        }
    }

    // --- EVENTOS IGNORADOS PELO BOT ---
    // Como a IA é passiva nesses eventos, não precisamos fazer nada
    
    @Override
    public void cartaJogada(Jogador j, Carta c) {}

    @Override
    public void inicioPartida(int dummy1, int dummy2) {}

    @Override
    public void jogoFechado(int numVencedor, int rndFrase) {}

    @Override
    public void jogoAbortado(int posicao, int rndFrase) {}

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Sleep do bot interrompido: " + e);
        }
    }
}
