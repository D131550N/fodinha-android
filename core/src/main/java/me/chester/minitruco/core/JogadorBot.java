package me.chester.minitruco.core;
import java.util.Random;

public class JogadorBot extends Jogador {
    private boolean[] cartasJogadas; // Memória de quais cartas já foram
    private final Random rand = new Random();

    @Override
    public void inicioMao(Jogador jogadorQueAbre) {
        if (getCartas() != null) {
            // Ajusta a memória para o tamanho exato da mão atual (1, 2, 5, 10 cartas...)
            cartasJogadas = new boolean[getCartas().length];
        }
    }

    @Override
    public void vez(Jogador j, boolean isFasePalpite) {
        if (j != this) return;

        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

            if (isFasePalpite) {
                // Palpite aleatório baseado no número de cartas que ele tem
                int max = getCartas() != null ? getCartas().length : 1;
                partida.fazPalpite(this, rand.nextInt(max + 1));
            } else {
                // Joga a primeira carta que ainda não foi usada
                Carta[] minhasCartas = getCartas();
                if (minhasCartas != null) {
                    for (int i = 0; i < minhasCartas.length; i++) {
                        if (!cartasJogadas[i]) {
                            cartasJogadas[i] = true;
                            partida.jogaCarta(this, minhasCartas[i]);
                            return;
                        }
                    }
                }
            }
        }).start();
    }

    // Métodos vazios para manter compatibilidade
    @Override public void cartaJogada(Jogador j, Carta c) {}
    @Override public void inicioPartida(int p1, int p2) {}
    @Override public void jogoAbortado(int posicao, int rnd) {}
    @Override public void jogoFechado(int num, int rnd) {}
    @Override public void entrouNoJogo(Jogador j, Partida p) {}
}
