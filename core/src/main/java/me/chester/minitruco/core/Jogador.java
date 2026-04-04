package me.chester.minitruco.core;

import java.util.Random;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

/**
 * Base para os diversos tipos de jogador que podem participar de uma partida.
 * <p>
 * Independente de ser o usuário do celular (JogadorHumano), um jogador virtual
 * (JogadorBot) ou via Bluetooth, o jogador recebe notificações da Partida
 * e envia comandos a ela.
 */
public abstract class Jogador {

    protected static final Random random = new Random();

    private int posicao = 0;
    private Carta[] cartas;
    private String nome = "unnamed";

    /**
     * Partida da qual este jogador está participando no momento
     */
    public Partida partida;

    public static String sanitizaNome(String nome) {
        return (nome == null ? "" : nome)
            .replaceAll("^bot$", "")
            .replaceAll("[-_ \r\n]"," ")
            .trim()
            .replaceAll("[^a-zA-Z0-9À-ÿ ]", "")
            .trim()
            .replaceAll(" +","_")
            .replaceAll("^(.{0,25}).*$", "$1")
            .replaceAll("_$","")
            .replaceAll("^[-_ ]*$", "sem_nome_"+(1 + random.nextInt(999)));
    }

    public void entrouNoJogo(Jogador j, Partida p) {
        if (j.equals(this)) {
            this.partida = p;
        }
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    @Override
    public String toString() {
        return super.toString()+"["+nome+"]";
    }

    /**
     * Recupera a posição do jogador na partida
     * @return número de 1 a 6
     */
    public int getPosicao() {
        return posicao;
    }

    public void setPosicao(int posicao) {
        this.posicao = posicao;
    }

    public void setCartas(Carta[] cartas) {
        this.cartas = cartas;
    }

    public Carta[] getCartas() {
        return cartas;
    }

    // --- NOTIFICAÇÕES RECEBIDAS DA PARTIDA ---

    /**
     * Informa que uma carta foi jogada na mesa.
     */
    public abstract void cartaJogada(Jogador j, Carta c);

    /**
     * Informa ao jogador que uma nova mão está iniciando e as cartas já foram distribuídas.
     */
    public abstract void inicioMao(Jogador jogadorQueAbre);

    /**
     * Informa que a partida começou.
     */
    public abstract void inicioPartida(int dummy1, int dummy2);

    /**
     * Informa que é a vez de um jogador agir.
     *
     * @param j Jogador cuja vez chegou
     * @param isFasePalpite true se o jogador deve fazer um palpite, false se deve jogar uma carta
     */
    public abstract void vez(Jogador j, boolean isFasePalpite);

    /**
     * Informa que a partida foi concluída.
     */
    public abstract void jogoFechado(int numVencedor, int rndFrase);

    /**
     * Informa que a partida foi abandonada por alguma causa externa.
     */
    public abstract void jogoAbortado(int posicao, int rndFrase);

}
