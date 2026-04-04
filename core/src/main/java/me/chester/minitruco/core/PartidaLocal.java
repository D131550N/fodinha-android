package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executa o jogo de Fodinha efetivamente.
 * Mantém o estado da mesa, controla as vidas, palpites e gerencia as rodadas.
 */
public class PartidaLocal extends Partida {

    private final static Logger LOGGER = Logger.getLogger("PartidaLocal");

    private final Baralho baralho;
    private final boolean jogoAutomatico;

    private int posJogadorDaVez;
    private int posJogadorAbriuMao;
    
    // Controle das threads e eventos
    private boolean alguemJogou = false;
    private boolean alguemPalpitou = false;
    private Jogador jogadorQueAgiu;
    private Carta cartaJogada;
    private int palpiteFeito;

    // Controle da rodada atual
    private int faseJogo = 0; // 0 = Distribuição, 1 = Palpites, 2 = Jogo
    private int rodadasJogadasNestaMao = 0;
    private Carta[] mesa = new Carta[7]; // Cartas jogadas na mesa (posições 1 a 6)
    private int qtdCartasNaMesa = 0;

    public PartidaLocal(boolean humanoDecide, boolean jogoAutomatico, String modoStr) {
        super(Modo.fromString(modoStr));
        this.baralho = new Baralho();
        this.jogoAutomatico = jogoAutomatico;
    }

    public void run() {
        LOGGER.log(Level.INFO, "Partida Fodinha iniciada");
        
        // Avisa os jogadores que a partida vai começar
        for (Jogador interessado : jogadores) {
            if (interessado != null) {
                // Passamos 0 pros pontos pois não usamos pontos de equipe
                interessado.inicioPartida(0, 0);
            }
        }

        // O primeiro jogador (posição 1) abre o jogo
        iniciaMao(getProximoVivo(0));

        // Loop principal da máquina de estados
        while (getJogadoresVivos() > 1 && !finalizada) {
            while ((!alguemJogou && !alguemPalpitou) && !finalizada) {
                sleep();
            }
            if (!finalizada) {
                if (alguemPalpitou) {
                    processaPalpite();
                    alguemPalpitou = false;
                } else if (alguemJogou) {
                    processaJogada();
                    alguemJogou = false;
                }
            }
        }
        LOGGER.log(Level.INFO, "Partida Fodinha finalizada");
    }

    /**
     * Inicia uma nova distribuição de cartas
     */
    private void iniciaMao(Jogador jogadorQueAbre) {
        baralho.embaralha();
        rodadasJogadasNestaMao = 0;
        
        // Reseta os arrays de controle da mão
        for (int i = 1; i <= 6; i++) {
            palpites[i] = 0;
            feitas[i] = 0;
            mesa[i] = null;
        }

        // Distribui as cartas de acordo com a quantidade da rodada atual
        for (int j = 1; j <= numJogadores; j++) {
            if (!eliminado[j]) {
                Jogador jogador = getJogador(j);
                Carta[] cartas = new Carta[quantidadeCartasRodada];
                for (int i = 0; i < quantidadeCartasRodada; i++) {
                    cartas[i] = baralho.sorteiaCarta();
                }
                jogador.setCartas(cartas);
            }
        }

        posJogadorAbriuMao = jogadorQueAbre.getPosicao();
        posJogadorDaVez = jogadorQueAbre.getPosicao();
        faseJogo = 1; // Fase de Palpites

        LOGGER.log(Level.INFO, "Abrindo mao com " + quantidadeCartasRodada + " cartas. J" + posJogadorAbriuMao + " começa.");

        // Notifica o início da mão (a interface e os bots precisam saber)
        for (Jogador j : jogadores) {
            if (j != null && !eliminado[j.getPosicao()]) {
                j.inicioMao(jogadorQueAbre);
            }
        }

        notificaVez();
    }

    /**
     * Retorna o próximo jogador vivo na mesa (pula os eliminados)
     */
    private Jogador getProximoVivo(int posAtual) {
        int iteracoes = 0;
        int proximo = posAtual;
        while (iteracoes < 6) {
            proximo++;
            if (proximo > numJogadores) proximo = 1;
            if (!eliminado[proximo]) return getJogador(proximo);
            iteracoes++;
        }
        return getJogador(1); // Fallback
    }

    /**
     * Executa a etapa de palpites
     */
    private void processaPalpite() {
        Jogador j = this.jogadorQueAgiu;
        
        if (j.getPosicao() != posJogadorDaVez || faseJogo != 1) return;

        palpites[j.getPosicao()] = this.palpiteFeito;
        LOGGER.log(Level.INFO, "J" + j.getPosicao() + " prometeu fazer " + palpiteFeito);

        // Passa a vez para o próximo vivo
        Jogador proximo = getProximoVivo(posJogadorDaVez);
        posJogadorDaVez = proximo.getPosicao();

        // Se a volta completou e chegou no que abriu a mão, vamos para a Fase de Jogo
        if (posJogadorDaVez == posJogadorAbriuMao) {
            faseJogo = 2; // Fase de jogar cartas
            LOGGER.log(Level.INFO, "Fase de palpites encerrada. Iniciando jogadas.");
        }
        
        notificaVez();
    }

    /**
     * Executa a jogada da carta e avalia quem ganhou
     */
    private void processaJogada() {
        Jogador j = this.jogadorQueAgiu;
        Carta c = this.cartaJogada;

        if (j.getPosicao() != posJogadorDaVez || faseJogo != 2) return;

        LOGGER.log(Level.INFO, "J" + j.getPosicao() + " jogou " + c);

        // Coloca a carta na mesa e notifica os outros
        mesa[j.getPosicao()] = c;
        qtdCartasNaMesa++;

        for (Jogador interessado : jogadores) {
            if (interessado != null) {
                interessado.cartaJogada(j, c);
            }
        }

        // Verifica se todos os vivos já jogaram nesta rodada
        if (qtdCartasNaMesa == getJogadoresVivos()) {
            avaliaGanhadorRodada();
        } else {
            // Se não, passa a vez pro próximo vivo
            posJogadorDaVez = getProximoVivo(posJogadorDaVez).getPosicao();
            notificaVez();
        }
    }

    /**
     * Calcula quem ganhou a rodada baseada na Fodinha (Força + Desempate por Naipe)
     */
    private void avaliaGanhadorRodada() {
        int vencedor = 0;
        int maiorValor = -1;
        int naipeDesempate = -1;

        for (int i = 1; i <= numJogadores; i++) {
            if (mesa[i] != null) {
                int valor = mesa[i].getValorFodinha();
                int naipe = mesa[i].getValorDesempateNaipe();
                
                if (valor > maiorValor) {
                    maiorValor = valor;
                    naipeDesempate = naipe;
                    vencedor = i;
                } else if (valor == maiorValor) { // Empate de valores, entra a Manilha
                    if (naipe > naipeDesempate) {
                        naipeDesempate = naipe;
                        vencedor = i;
                    }
                }
            }
        }

        LOGGER.log(Level.INFO, "J" + vencedor + " venceu a rodada com a carta " + mesa[vencedor]);

        feitas[vencedor]++;
        rodadasJogadasNestaMao++;
        
        // Limpa a mesa para a próxima vaza
        qtdCartasNaMesa = 0;
        for (int i = 1; i <= 6; i++) {
            mesa[i] = null;
        }

        // Quem ganha a rodada, joga a primeira na próxima
        posJogadorDaVez = vencedor;

        // Verifica se acabaram as cartas da mão
        if (rodadasJogadasNestaMao == quantidadeCartasRodada) {
            fechaMao();
        } else {
            notificaVez(); // Continua a mão
        }
    }

    /**
     * Verifica quem errou o palpite, tira vidas e avalia se o jogo acabou
     */
    private void fechaMao() {
        boolean alguemMorreuNestaMao = false;

        for (int i = 1; i <= numJogadores; i++) {
            if (!eliminado[i]) {
                if (palpites[i] != feitas[i]) {
                    vidas[i]--;
                    LOGGER.log(Level.INFO, "J" + i + " errou o palpite! Vidas restantes: " + vidas[i]);
                    
                    if (vidas[i] <= 0) {
                        eliminado[i] = true;
                        alguemMorreuNestaMao = true;
                        LOGGER.log(Level.INFO, "J" + i + " foi ELIMINADO!");
                    }
                } else {
                    LOGGER.log(Level.INFO, "J" + i + " acertou o palpite.");
                }
            }
        }

        int vivos = getJogadoresVivos();

        // Se sobrou 1 ou nenhum, acabou o jogo
        if (vivos <= 1) {
            finalizada = true;
            LOGGER.log(Level.INFO, "Fim de jogo. Sobrou " + vivos + " jogadores.");
            for (Jogador j : jogadores) {
                if (j != null) {
                    j.jogoFechado(1, 0); // Dispara evento de fim de jogo
                }
            }
            return;
        }

        // Se alguém foi eliminado, as cartas voltam pra 1. Se não, aumentam.
        if (alguemMorreuNestaMao) {
            quantidadeCartasRodada = 1;
        } else {
            quantidadeCartasRodada++;
        }

        // O próximo a dar as cartas (e abrir a mão) é o seguinte de quem abriu a anterior
        Jogador proximoAbre = getProximoVivo(posJogadorAbriuMao);
        iniciaMao(proximoAbre);
    }

    // --- RECEPÇÃO DE EVENTOS DOS JOGADORES ---

    public synchronized void jogaCarta(Jogador j, Carta c) {
        this.jogadorQueAgiu = j;
        this.cartaJogada = c;
        this.alguemJogou = true;
    }

    public synchronized void fazPalpite(Jogador j, int palpite) {
        this.jogadorQueAgiu = j;
        this.palpiteFeito = palpite;
        this.alguemPalpitou = true;
    }

    private void notificaVez() {
        Jogador j = getJogador(posJogadorDaVez);
        for (Jogador interessado : jogadores) {
            if (interessado != null && !eliminado[interessado.getPosicao()]) {
                // Envia as vidas atuais e qual fase o jogo está (Palpite ou Carta)
                interessado.vez(j, (faseJogo == 1)); 
            }
        }
    }

    public void atualizaSituacao(SituacaoJogo s, Jogador j) {
        s.faseJogo = this.faseJogo;
        s.numJogadores = this.numJogadores;
        s.quantidadeCartasRodada = this.quantidadeCartasRodada;
        s.posJogador = j.getPosicao();
        s.vez = this.posJogadorDaVez;

        System.arraycopy(this.vidas, 0, s.vidas, 0, 7);
        System.arraycopy(this.palpites, 0, s.palpites, 0, 7);
        System.arraycopy(this.feitas, 0, s.feitas, 0, 7);
        System.arraycopy(this.eliminado, 0, s.eliminado, 0, 7);
        System.arraycopy(this.mesa, 0, s.cartasJogadas, 0, 7);
    }

    public void abandona(int posicao) {
        finalizada = true;
        for (Jogador j : jogadores) {
            if (j != null) {
                j.jogoAbortado(posicao, 0);
            }
        }
    }

    @Override
    public boolean isJogoAutomatico() {
        return jogoAutomatico;
    }
}
