package me.chester.minitruco.android;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.Vector;

import me.chester.minitruco.core.Carta;
import me.chester.minitruco.core.Jogador;
import me.chester.minitruco.core.SituacaoJogo;

public class MesaView extends View {

    public static final int FPS_ANIMANDO = 60;
    public static final int FPS_PARADO = 4;

    public static final int STATUS_VEZ_HUMANO_OK = 1;
    public static final int STATUS_VEZ_OUTRO = 0;

    // Na Fodinha, podemos ter até 6 jogadores com até 10 cartas na mão (60 cartas)
    // + 6 cartas no centro da mesa (Total = 66 objetos visuais)
    public final CartaVisual[] cartas = new CartaVisual[66];
    private final Vector<CartaVisual> cartasJogadas = new Vector<>();

    private float density;
    public boolean vaiJogarFechada;
    protected int velocidade = 1;
    private int posicaoVez;
    private int corFundoCartaBalao = Color.WHITE;
    private TrucoActivity trucoActivity;
    private float tamanhoFonte;
    private float divisorTamanhoFonte = 20;

    private boolean inicializada = false;
    private final HashMap<String, Integer> ultimaFrase = new HashMap<>();

    private long animandoAte = System.currentTimeMillis();
    private int statusVez = 0;

    private CartaVisual cartaQueFez;
    private boolean isRodadaPiscando;
    private long rodadaPiscaAte = System.currentTimeMillis();

    private int posicaoBalao = 1;
    private long mostraBalaoAte = System.currentTimeMillis();
    String fraseBalao = null;
    private boolean visivel = false;

    // Array para controlar quem já gritou a frase de morte pra não flodar a tela
    private boolean[] jaGritouMorte = new boolean[7];

    final Thread threadAnimacao = new Thread(() -> {
        int tempoEntreFramesAnimando = 1000 / FPS_ANIMANDO;
        int tempoEntreFramesParado = 1000 / FPS_PARADO;
        while (trucoActivity == null || trucoActivity.partida == null) {
            sleep(200);
        }
        while (!trucoActivity.isFinishing()) {
            if (visivel) {
                postInvalidate();
            }
            if (calcTempoAteFimAnimacaoMS() >= 0) {
                sleep(tempoEntreFramesAnimando);
            } else {
                sleep(tempoEntreFramesParado);
            }
        }
    });

    private void sleep(int tempoMS) {
        try { Thread.sleep(tempoMS); } catch (InterruptedException ignored) { }
    }

    public MesaView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init(context);
    }

    public MesaView(Context context, AttributeSet attrs) {
        super(context, attrs); init(context);
    }

    public MesaView(Context context) {
        super(context); init(context);
    }

    private void init(Context context) {
        this.density = context.getResources().getDisplayMetrics().density;
    }

    public void setTrucoActivity(TrucoActivity trucoActivity) {
        this.trucoActivity = trucoActivity;
    }

    public void setIndiceDesenhoCartaFechada(int indice) {
        CartaVisual.setIndiceDesenhoCartaFechada(indice);
    }

    public void setEscalaFonte(int escala) {
        divisorTamanhoFonte = 21f - escala;
    }

    public void notificaAnimacao(long fim) {
        if (animandoAte < fim) {
            animandoAte = fim;
        }
        threadAnimacao.interrupt();
    }

    public void aguardaFimAnimacoes() {
        long msAteFim;
        while ((msAteFim = animandoAte - System.currentTimeMillis()) > 0) {
            try { Thread.sleep(msAteFim); } catch (InterruptedException e) { return; }
        }
    }

    public boolean isInicializada() {
        return inicializada;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        CartaVisual.ajustaTamanho(w, h);
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        tamanhoFonte = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_PX,
            Math.min(w, h) / divisorTamanhoFonte,
            displayMetrics
        );

        if (!inicializada) {
            for (int i = 0; i < cartas.length; i++) {
                // Instancia as cartas fora da tela inicialmente
                cartas[i] = new CartaVisual(this, -200, -200, null, corFundoCartaBalao);
                cartas[i].visible = false;
            }
            threadAnimacao.start();
        }

        inicializada = true;
    }

    public CartaVisual getCartaVisual(Carta c) {
        for (CartaVisual cv : cartas) {
            if (c != null && c.equals(cv)) return cv;
        }
        return null;
    }

    public void atualizaResultadoRodada(int numRodada, int resultado, Jogador jogadorQueTorna) {
        // Ignorado visualmente para não piscar, a Fodinha é muito rápida
    }

    public void diz(String chave, int posicaoTela, int tempoMS, int rndFrase) {
        aguardaFimAnimacoes();
        mostraBalaoAte = System.currentTimeMillis() + tempoMS / Math.min(velocidade, 2);
        
        // Se a chave for "morte", usamos a frase dinâmica do Pica-Pau
        if (chave.equals("morte")) {
            fraseBalao = trucoActivity.getFraseMorte();
        } else {
            fraseBalao = "Falou!"; // Fallback genérico para outros eventos
        }
        
        posicaoBalao = posicaoTela;
        notificaAnimacao(mostraBalaoAte);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // Verifica se tocou em alguma carta do Jogador 1 (posições 0 a 9)
            for (int i = 9; i >= 0; i--) {
                if (cartas[i].visible && cartas[i].isDentro(x, y)) {
                    jogaCarta(i);
                    return true;
                }
            }
        }
        return true;
    }

    public void jogaCarta(int indexMao) {
        CartaVisual carta = cartas[indexMao];
        if (carta.descartada || statusVez != STATUS_VEZ_HUMANO_OK) return;

        statusVez = STATUS_VEZ_OUTRO;
        trucoActivity.partida.jogaCarta(trucoActivity.jogadorHumano, carta);
    }

    private long calcTempoAteFimAnimacaoMS() {
        return animandoAte - System.currentTimeMillis();
    }

    public void vez(boolean humano) {
        aguardaFimAnimacoes();
        statusVez = humano ? STATUS_VEZ_HUMANO_OK : STATUS_VEZ_OUTRO;
    }

    public void distribuiMao() {
        aguardaFimAnimacoes();
        recolheMao();

        if (trucoActivity.partida == null) return;
        
        // Zera gritos de morte para a nova mão
        for(int i=0; i<7; i++) jaGritouMorte[i] = false;

        int totalCartasRodada = trucoActivity.partida.getSituacaoJogo().quantidadeCartasRodada;

        for (int i = 1; i <= 6; i++) {
            Jogador j = trucoActivity.partida.getJogador(i);
            if (j == null || trucoActivity.partida.getSituacaoJogo().eliminado[i]) continue;

            int posTela = trucoActivity.jogadorHumano.posicaoNaTela(j);
            Carta[] mao = j.getCartas();

            // Mapeia o array geral de 60 cartas (10 por jogador na tela)
            int offset = (posTela - 1) * 10; 

            for (int k = 0; k < totalCartasRodada; k++) {
                if (mao != null && k < mao.length && mao[k] != null) {
                    CartaVisual c = cartas[offset + k];
                    c.visible = true;
                    c.descartada = false;
                    
                    if (posTela == 1) { // Nós (Humano)
                        c.copiaCarta(mao[k]);
                        c.setFechada(false);
                    } else { // Adversários/Bots (Apenas o fundo)
                        c.setFechada(true);
                    }
                    entregaCarta(c, posTela, k, totalCartasRodada);
                }
            }
        }
    }

    public void recolheMao() {
        aguardaFimAnimacoes();
        cartasJogadas.clear();
        for (CartaVisual c : cartas) {
            c.visible = false;
            c.descartada = false;
            c.escura = false;
        }
    }

    public void descarta(Carta c, int posTela) {
        aguardaFimAnimacoes();
        int topFinal = calcPosTopDescartada(posTela);
        int leftFinal = calcPosLeftDescartada(posTela);

        int offset = (posTela - 1) * 10;
        CartaVisual cv = null;
        
        for (int i = 0; i < 10; i++) {
            CartaVisual cvCandidata = cartas[offset + i];
            if (cvCandidata.visible && !cvCandidata.descartada) {
                if (posTela == 1 && c.equals(cvCandidata)) {
                    cv = cvCandidata; break;
                } else if (posTela != 1) {
                    cv = cvCandidata; break; // Pega a primeira carta fechada livre do bot
                }
            }
        }

        if (cv == null) return;

        cv.copiaCarta(c); // Revela a carta do bot
        cv.setFechada(false);
        cv.movePara(leftFinal, topFinal, 200);
        cv.descartada = true;
        cartasJogadas.addElement(cv);
    }

    public void setVisivel(boolean visivel) {
        this.visivel = visivel;
    }

    private void entregaCarta(CartaVisual carta, int posTela, int numCartaNaMao, int totalCartasNaMao) {
        carta.movePara(calcPosLeftCarta(posTela, numCartaNaMao, totalCartasNaMao), calcPosTopCarta(posTela, numCartaNaMao), 85);
    }

    // --- MATEMÁTICA HEXAGONAL ---

    private int calcPosLeftCarta(int posTela, int numCartaNaMao, int totalCartas) {
        int gap = CartaVisual.largura / 2;
        int totalWidth = (totalCartas - 1) * gap + CartaVisual.largura;
        int centerW = getWidth() / 2;
        int startX;
        
        switch (posTela) {
            case 1: startX = centerW - totalWidth / 2; break; // Baixo Centro
            case 2: startX = getWidth() - CartaVisual.largura - 10; break; // Baixo Dir
            case 3: startX = getWidth() - CartaVisual.largura - 10; break; // Cima Dir
            case 4: startX = centerW - totalWidth / 2; break; // Topo Centro
            case 5: startX = 10; break; // Cima Esq
            case 6: startX = 10; break; // Baixo Esq
            default: startX = 0;
        }

        if (posTela == 1 || posTela == 4) return startX + (numCartaNaMao * gap);
        else return startX + (numCartaNaMao * (gap / 3)); // Pilha mais apertada nas laterais
    }

    private int calcPosTopCarta(int posTela, int numCartaNaMao) {
        int gap = CartaVisual.altura / 5;
        int startY;
        
        switch (posTela) {
            case 1: return getHeight() - CartaVisual.altura - 10;
            case 2: startY = (getHeight() * 3 / 4) - CartaVisual.altura; break;
            case 3: startY = getHeight() / 4; break;
            case 4: return 20 + (int) tamanhoFonte * 2;
            case 5: startY = getHeight() / 4; break;
            case 6: startY = (getHeight() * 3 / 4) - CartaVisual.altura; break;
            default: return 0;
        }
        return startY + (numCartaNaMao * gap);
    }

    private int calcPosLeftDescartada(int posTela) {
        int left = getWidth() / 2 - CartaVisual.largura / 2;
        switch (posTela) {
            case 2: case 3: left += CartaVisual.largura; break;
            case 5: case 6: left -= CartaVisual.largura; break;
        }
        return left + (int)(System.currentTimeMillis() % 5 - 2);
    }

    private int calcPosTopDescartada(int posTela) {
        int top = getHeight() / 2 - CartaVisual.altura / 2;
        switch (posTela) {
            case 1: case 2: case 6: top += CartaVisual.altura / 2; break;
            case 3: case 4: case 5: top -= CartaVisual.altura / 2; break;
        }
        return top + (int)(System.currentTimeMillis() % 5 - 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRGB(27, 142, 60); // Fundo verde mesa

        // Checagem de Morte (Pica-Pau)
        if (trucoActivity != null && trucoActivity.partida != null) {
            SituacaoJogo sit = trucoActivity.partida.getSituacaoJogo();
            for (int i = 1; i <= 6; i++) {
                if (sit.eliminado[i] && !jaGritouMorte[i]) {
                    Jogador j = trucoActivity.partida.getJogador(i);
                    if (j != null) {
                        diz("morte", trucoActivity.jogadorHumano.posicaoNaTela(j), 3000, 0);
                        jaGritouMorte[i] = true;
                    }
                }
            }
        }

        // Desenha Cartas
        for (CartaVisual c : cartasJogadas) {
            if (c.visible) c.draw(canvas);
        }
        for (CartaVisual carta : cartas) {
            if (carta != null && carta.visible && !cartasJogadas.contains(carta)) {
                carta.draw(canvas);
            }
        }

        desenhaNomesEVidas(canvas);
        desenhaBalao(canvas);
        desenhaIndicadorDeVez(canvas);
    }

    private void desenhaNomesEVidas(Canvas canvas) {
        if (trucoActivity == null || trucoActivity.partida == null) return;
        
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextSize(tamanhoFonte);
        paint.setTextAlign(Align.CENTER);
        
        SituacaoJogo sit = trucoActivity.partida.getSituacaoJogo();

        for (int i = 1; i <= 6; i++) {
            Jogador j = trucoActivity.partida.getJogador(i);
            if (j == null || sit.eliminado[i]) continue;

            int vidas = sit.vidas[i];
            int palpites = sit.palpites[i];
            int feitas = sit.feitas[i];

            // Formato: "Nome (💖 2) [1/2]"
            String texto = j.getNome() + " (💖" + vidas + ")";
            if (sit.faseJogo == 2) { // Fase de jogo (mostra palpites vs feitas)
                texto += " [" + feitas + "/" + palpites + "]";
            }

            int posTela = trucoActivity.jogadorHumano.posicaoNaTela(j);
            float x = 0, y = 0;

            switch (posTela) {
                case 1: x = getWidth() / 2f; y = getHeight() - 5; break;
                case 2: x = getWidth() - (CartaVisual.largura * 1.5f); y = (getHeight() * 3 / 4f) + (CartaVisual.altura * 0.8f); break;
                case 3: x = getWidth() - (CartaVisual.largura * 1.5f); y = (getHeight() / 4f) - 10; break;
                case 4: x = getWidth() / 2f; y = tamanhoFonte + 10; break;
                case 5: x = (CartaVisual.largura * 1.5f); y = (getHeight() / 4f) - 10; break;
                case 6: x = (CartaVisual.largura * 1.5f); y = (getHeight() * 3 / 4f) + (CartaVisual.altura * 0.8f); break;
            }

            // Sombra
            paint.setColor(Color.BLACK);
            canvas.drawText(texto, x + 2, y + 2, paint);
            // Texto principal
            paint.setColor(Color.WHITE);
            canvas.drawText(texto, x, y, paint);
        }
    }

    private void desenhaIndicadorDeVez(Canvas canvas) {
        if (posicaoVez == 0) return;
        
        Paint paintSetaVez = new Paint();
        paintSetaVez.setColor(Color.YELLOW);
        paintSetaVez.setTextAlign(Align.CENTER);
        paintSetaVez.setTextSize(CartaVisual.altura / 2f);
        
        float x = 0, y = 0;
        String seta = "⭐"; // Marcador visual mais genérico que seta fixa

        switch (posicaoVez) {
            case 1: x = getWidth() / 2f; y = getHeight() - CartaVisual.altura - 20; break;
            case 2: x = getWidth() - CartaVisual.largura - 30; y = getHeight() * 3/4f; break;
            case 3: x = getWidth() - CartaVisual.largura - 30; y = getHeight() / 4f; break;
            case 4: x = getWidth() / 2f; y = CartaVisual.altura + 40; break;
            case 5: x = CartaVisual.largura + 30; y = getHeight() / 4f; break;
            case 6: x = CartaVisual.largura + 30; y = getHeight() * 3/4f; break;
        }
        
        canvas.drawText(seta, x, y, paintSetaVez);
    }

    private void desenhaBalao(Canvas canvas) {
        if (fraseBalao == null || mostraBalaoAte <= System.currentTimeMillis()) return;

        Paint paintFonte = new Paint();
        paintFonte.setAntiAlias(true);
        paintFonte.setTextSize(tamanhoFonte);
        paintFonte.setColor(Color.BLACK);
        
        Rect bounds = new Rect();
        paintFonte.getTextBounds(fraseBalao, 0, fraseBalao.length(), bounds);

        int largBalao = bounds.width() + 40;
        int altBalao = bounds.height() + 40;
        
        // Posição centralizada para a fala
        int x = (getWidth() - largBalao) / 2;
        int y = (getHeight() - altBalao) / 2;

        Paint paintFundo = new Paint();
        paintFundo.setAntiAlias(true);
        paintFundo.setColor(corFundoCartaBalao);
        
        canvas.drawRoundRect(new RectF(x, y, x + largBalao, y + altBalao), 15, 15, paintFundo);
        canvas.drawText(fraseBalao, x + 20, y + altBalao - 20, paintFonte);
    }

    public void setPosicaoVez(int posicaoVez) {
        this.posicaoVez = posicaoVez;
    }

    public void setCorFundoCartaBalao(int corFundoCartaBalao) {
        this.corFundoCartaBalao = corFundoCartaBalao;
    }
}
