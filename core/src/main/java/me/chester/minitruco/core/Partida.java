package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

/**
 * Representa o esqueleto de uma partida de Fodinha.
 * <p>
 * Recebe comandos dos jogadores (ex: fazer palpite, jogar carta) e
 * envia notificações (ex: início de mão, carta jogada, atualização de vidas).
 */
public abstract class Partida implements Runnable {

    protected final Modo modo;

    /**
     * Quantidade de cartas distribuídas na rodada atual (1, 2, 3...)
     */
    protected int quantidadeCartasRodada = 1;

    /**
     * Total de jogadores na mesa (pode variar de 2 a 6)
     */
    protected int numJogadores = 0;

    /**
     * Jogadores adicionados a esta partida (índices de 1 a 6 são os válidos, o 0 é ignorado)
     */
    protected final Jogador[] jogadores = new Jogador[7];

    /**
     * Guarda as vidas de cada jogador. Começam com 2 vidas.
     */
    protected int[] vidas = new int[7];

    /**
     * Guarda o palpite (quantas promessas de vitória) cada jogador fez na mão atual.
     */
    protected int[] palpites = new int[7];

    /**
     * Guarda quantas rodadas cada jogador efetivamente ganhou na mão atual.
     */
    protected int[] feitas = new int[7];

    /**
     * Array para verificar se um jogador já foi eliminado (zerou vidas).
     */
    protected boolean[] eliminado = new boolean[7];

    /**
     * Carta temporária apenas para manter compatibilidade de eventos na interface gráfica,
     * caso o frontend original exija saber se há algo na mesa. Não afeta a regra da Fodinha.
     */
    public Carta cartaDaMesa;

    /**
     * Indica se a partida foi finalizada.
     */
    public boolean finalizada = false;

    public Partida(Modo modo) {
        this.modo = modo;
        // Inicializa todos com 2 vidas, do jogador 1 ao 6
        for (int i = 1; i <= 6; i++) {
            vidas[i] = 2;
            eliminado[i] = false;
        }
    }

    public Modo getModo() {
        return modo;
    }

    /**
     * Inicia o jogo. O laço principal rodará na subclasse (ex: PartidaLocal)
     */
    public abstract void run();

    /**
     * Informa à partida qual foi o palpite do jogador (quantas ele promete fazer).
     *
     * @param j Jogador que está palpitando
     * @param palpite Quantidade prometida
     */
    public abstract void fazPalpite(Jogador j, int palpite);

    /**
     * Informa à partida que o jogador quer descartar aquela carta.
     */
    public abstract void jogaCarta(Jogador j, Carta c);

    /**
     * Atualiza um objeto que contém a situação da partida.
     */
    public abstract void atualizaSituacao(SituacaoJogo s, Jogador j);

    /**
     * Adiciona um jogador a esta partida.
     * Na Fodinha, permite até 6 jogadores.
     */
    public synchronized boolean adiciona(Jogador jogador) {
        // Se a mesa lotou, não entra mais ninguém
        if (numJogadores == 6) {
            return false;
        }

        numJogadores++;
        jogadores[numJogadores] = jogador;
        jogador.partida = this;
        jogador.setPosicao(numJogadores);

        for (Jogador j : jogadores) {
            if (j != null) {
                j.entrouNoJogo(jogador, this);
            }
        }
        return true;
    }

    /**
     * Recupera um jogador inscrito
     *
     * @param posicao valor de 1 a 6
     * @return Objeto correspondente àquela posição
     */
    public Jogador getJogador(int posicao) {
        return jogadores[posicao];
    }

    /**
     * @return true se esta partida não envolve nenhuma comunicação remota
     */
    public boolean semJogadoresRemotos() {
        return false;
    }

    /**
     * Aborta a partida por iniciativa daquele jogador
     */
    public abstract void abandona(int posicao);

    public boolean isJogoAutomatico() {
        return false;
    }

    /**
     * Função auxiliar para saber quantos jogadores ainda têm vidas.
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
