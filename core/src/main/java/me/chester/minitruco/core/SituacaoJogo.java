package me.chester.minitruco.core;

public class SituacaoJogo {
    public int quantidadeCartasRodada = 1;
    public int faseJogo = 1;
    public int numeroDaMao = 1; // <-- Nova variável para contar as rodadas!

    public int[] vidas = new int[7];
    public int[] palpites = new int[7];
    public int[] feitas = new int[7];
    public boolean[] eliminado = new boolean[7];
}
