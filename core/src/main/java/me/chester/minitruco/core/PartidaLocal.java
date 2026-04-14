package me.chester.minitruco.core;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PartidaLocal extends Partida {

    private final static Logger LOGGER = Logger.getLogger("PartidaLocal");
    private final boolean jogoAutomatico;
    private int posJogadorDaVez;
    private int posJogadorAbriuMao;

    private boolean alguemJogou = false;
    private boolean alguemPalpitou = false;
    private Jogador jogadorQueAgiu;
    private Carta cartaJogada;
    private int palpiteFeito;

    private int faseJogo = 0;
    private int rodadasJogadasNestaMao = 0;
    private Carta[] mesa = new Carta[7];
    private int qtdCartasNaMesa = 0;

    public PartidaLocal(boolean humanoDecide, boolean jogoAutomatico, String modoStr) {
        super(Modo.fromString(modoStr));
        this.jogoAutomatico = jogoAutomatico;
    }

    public void run() {
        LOGGER.log(Level.INFO, "Partida Fodinha iniciada");
        for (Jogador interessado : jogadores) {
            if (interessado != null) interessado.inicioPartida(0, 0);
        }
        iniciaMao(getProximoVivo(0));

        while (getJogadoresVivos() > 1 && !finalizada) {
            while ((!alguemJogou && !alguemPalpitou) && !finalizada) {
                sleep(100);
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

    private void iniciaMao(Jogador jogadorQueAbre) {
        Baralho baralhoDaMao = new Baralho();
        baralhoDaMao.embaralha();
        rodadasJogadasNestaMao = 0;

        for (int i = 1; i <= 6; i++) {
            palpites[i] = -1;
            feitas[i] = 0;
            mesa[i] = null;
        }

        this.cartaDaMesa = baralhoDaMao.sorteiaCarta();
        String ordem = "4567JQKA23";
        int idx = ordem.indexOf(this.cartaDaMesa.getLetra());
        this.manilha = ordem.charAt(idx == 9 ? 0 : idx + 1);

        for (int j = 1; j <= numJogadores; j++) {
            if (!eliminado[j]) {
                Jogador jogador = getJogador(j);
                Carta[] cartas = new Carta[quantidadeCartasRodada];
                for (int i = 0; i < quantidadeCartasRodada; i++) cartas[i] = baralhoDaMao.sorteiaCarta();
                jogador.setCartas(cartas);
            }
        }

        posJogadorAbriuMao = jogadorQueAbre.getPosicao();
        posJogadorDaVez = jogadorQueAbre.getPosicao();
        faseJogo = 1;

        for (Jogador j : jogadores) {
            if (j != null && !eliminado[j.getPosicao()]) j.inicioMao(jogadorQueAbre);
        }
        notificaVez();
    }

    private Jogador getProximoVivo(int posAtual) {
        int iteracoes = 0;
        int proximo = posAtual;
        while (iteracoes < 6) {
            proximo++;
            if (proximo > numJogadores) proximo = 1;
            if (!eliminado[proximo]) return getJogador(proximo);
            iteracoes++;
        }
        return getJogador(1);
    }

    private void processaPalpite() {
        Jogador j = this.jogadorQueAgiu;
        if (j.getPosicao() != posJogadorDaVez || faseJogo != 1) return;

        palpites[j.getPosicao()] = this.palpiteFeito;
        Jogador proximo = getProximoVivo(posJogadorDaVez);
        posJogadorDaVez = proximo.getPosicao();

        if (posJogadorDaVez == posJogadorAbriuMao) faseJogo = 2;
        notificaVez();
    }

    private void processaJogada() {
        Jogador j = this.jogadorQueAgiu;
        Carta c = this.cartaJogada;
        if (j.getPosicao() != posJogadorDaVez || faseJogo != 2) return;

        mesa[j.getPosicao()] = c;
        qtdCartasNaMesa++;

        for (Jogador interessado : jogadores) {
            if (interessado != null) interessado.cartaJogada(j, c);
        }

        if (qtdCartasNaMesa == getJogadoresVivos()) {
            avaliaGanhadorRodada();
        } else {
            posJogadorDaVez = getProximoVivo(posJogadorDaVez).getPosicao();
            notificaVez();
        }
    }

    private int calculaForcaFodinha(Carta c) {
        if (c.getLetra() == this.manilha) return 11 + c.getNaipe();
        String ordem = "4567JQKA23";
        return ordem.indexOf(c.getLetra()) + 1;
    }

    private void avaliaGanhadorRodada() {
        int vencedor = 0;
        int maiorValor = -1;
        boolean empardou = false;

        for (int i = 1; i <= numJogadores; i++) {
            if (mesa[i] != null) {
                int valor = calculaForcaFodinha(mesa[i]);
                if (valor > maiorValor) {
                    maiorValor = valor; vencedor = i; empardou = false;
                } else if (valor == maiorValor) {
                    empardou = true;
                }
            }
        }

        if (empardou) {
            posJogadorDaVez = vencedor;
        } else {
            feitas[vencedor]++;
            posJogadorDaVez = vencedor;
        }

        // --- PAUSA DRAMÁTICA ---
        // Espera 2 segundos com as cartas paradas na mesa para o jogador ver quem ganhou!
        sleep(2000);

        rodadasJogadasNestaMao++;
        qtdCartasNaMesa = 0;
        for (int i = 1; i <= 6; i++) mesa[i] = null;

        if (rodadasJogadasNestaMao == quantidadeCartasRodada) fechaMao();
        else notificaVez();
    }

    private void fechaMao() {
        boolean alguemMorreuNestaMao = false;
        for (int i = 1; i <= numJogadores; i++) {
            if (!eliminado[i]) {
                if (palpites[i] != feitas[i]) {
                    vidas[i]--;
                    if (vidas[i] <= 0) { eliminado[i] = true; alguemMorreuNestaMao = true; }
                }
            }
        }

        if (getJogadoresVivos() <= 1) {
            finalizada = true;
            for (Jogador j : jogadores) { if (j != null) j.jogoFechado(1, 0); }
            return;
        }

        if (alguemMorreuNestaMao) quantidadeCartasRodada = 1;
        else quantidadeCartasRodada++;

        this.maoAtual++;
        Jogador proximoAbre = getProximoVivo(posJogadorAbriuMao);
        iniciaMao(proximoAbre);
    }

    public synchronized void jogaCarta(Jogador j, Carta c) {
        this.jogadorQueAgiu = j; this.cartaJogada = c; this.alguemJogou = true;
    }

    public synchronized void fazPalpite(Jogador j, int palpite) {
        this.jogadorQueAgiu = j; this.palpiteFeito = palpite; this.alguemPalpitou = true;
    }

    private void notificaVez() {
        Jogador j = getJogador(posJogadorDaVez);
        for (Jogador interessado : jogadores) {
            if (interessado != null && !eliminado[interessado.getPosicao()]) interessado.vez(j, (faseJogo == 1));
        }
    }

    public void atualizaSituacao(SituacaoJogo s, Jogador j) {
        s.quantidadeCartasRodada = this.quantidadeCartasRodada;
        s.faseJogo = this.faseJogo;
        s.numeroDaMao = this.maoAtual;

        System.arraycopy(this.vidas, 0, s.vidas, 0, 7);
        System.arraycopy(this.palpites, 0, s.palpites, 0, 7);
        System.arraycopy(this.feitas, 0, s.feitas, 0, 7);
        System.arraycopy(this.eliminado, 0, s.eliminado, 0, 7);
    }

    public void abandona(int posicao) {
        finalizada = true;
        for (Jogador j : jogadores) if (j != null) j.jogoAbortado(posicao, 0);
    }

    @Override
    public boolean isJogoAutomatico() { return jogoAutomatico; }

    private void sleep(int ms) { // Atualizado para aceitar o tempo exato
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
