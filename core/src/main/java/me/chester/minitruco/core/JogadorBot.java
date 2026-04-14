package me.chester.minitruco.core;
import java.util.Random;

public class JogadorBot extends Jogador {
    private boolean[] cartasJogadas;
    private final Random rand = new Random();

    public JogadorBot() {
        setNome("Bot");
    }

    @Override
    public void entrouNoJogo(Jogador j, Partida p) {
        if (this == j) setNome("Bot " + getPosicao());
    }

    @Override
    public void inicioMao(Jogador jogadorQueAbre) {
        if (getCartas() != null) cartasJogadas = new boolean[getCartas().length];
    }

    private int calculaForca(Carta c, char manilha) {
        if (c.getLetra() == manilha) return 11 + c.getNaipe();
        String ordem = "4567JQKA23";
        return ordem.indexOf(c.getLetra()) + 1;
    }

    @Override
    public void vez(Jogador j, boolean isFasePalpite) {
        if (j != this) return;

        new Thread(() -> {
            if (isFasePalpite) {
                // FASE DE PALPITE: BEM MAIS RÁPIDO AGORA! (0.3 segundos)
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}

                int palpite = 0;
                Carta[] mao = getCartas();
                if (mao != null) {
                    for (Carta c : mao) {
                        int forca = calculaForca(c, partida.manilha);
                        if (forca >= 8) palpite++;
                    }
                    if (rand.nextInt(100) < 25) palpite += (rand.nextBoolean() ? 1 : -1);
                    if (palpite < 0) palpite = 0;
                    if (palpite > mao.length) palpite = mao.length;
                }
                partida.fazPalpite(this, palpite);
            } else {
                // FASE DE JOGO: Pensa um pouco (1 segundo) para jogar a carta
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

                int meuPalpite = partida.getSituacaoJogo().palpites[getPosicao()];
                int minhasFeitas = partida.getSituacaoJogo().feitas[getPosicao()];
                boolean querGanhar = minhasFeitas < meuPalpite;

                Carta escolhida = null;
                int indiceEscolhida = -1;
                int melhorForca = querGanhar ? -1 : 999;

                Carta[] mao = getCartas();
                if (mao != null) {
                    for (int i = 0; i < mao.length; i++) {
                        if (!cartasJogadas[i]) {
                            int forca = calculaForca(mao[i], partida.manilha);
                            if (querGanhar && forca > melhorForca) {
                                melhorForca = forca; escolhida = mao[i]; indiceEscolhida = i;
                            } else if (!querGanhar && forca < melhorForca) {
                                melhorForca = forca; escolhida = mao[i]; indiceEscolhida = i;
                            }
                        }
                    }
                    if (escolhida != null) {
                        cartasJogadas[indiceEscolhida] = true;
                        partida.jogaCarta(this, escolhida);
                    }
                }
            }
        }).start();
    }

    @Override public void cartaJogada(Jogador j, Carta c) {}
    @Override public void inicioPartida(int p1, int p2) {}
    @Override public void jogoAbortado(int posicao, int rnd) {}
    @Override public void jogoFechado(int num, int rnd) {}
}
