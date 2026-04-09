package me.chester.minitruco.android;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.Vector;

import me.chester.minitruco.core.Carta;
import me.chester.minitruco.core.Jogador;
import me.chester.minitruco.core.SituacaoJogo;

public class MesaView extends View {

    public static final int FPS_ANIMANDO = 60;
    public static final int FPS_PARADO = 4;
    public static final int STATUS_VEZ_HUMANO_OK = 1;
    public static final int STATUS_VEZ_OUTRO = 0;

    public final CartaVisual[] cartas = new CartaVisual[66];
    private final Vector<CartaVisual> cartasJogadas = new Vector<>();
    private int[] ultimosPalpites = new int[7];

    private float density;
    protected int velocidade = 1;
    private int posicaoVez;
    private int corFundoCartaBalao = Color.WHITE;
    private TrucoActivity trucoActivity;
    private float tamanhoFonte;
    private float divisorTamanhoFonte = 20;

    private boolean inicializada = false;
    private long animandoAte = System.currentTimeMillis();
    private int statusVez = 0;

    private int posicaoBalao = 1;
    private long mostraBalaoAte = System.currentTimeMillis();
    String fraseBalao = null;
    private boolean visivel = false;
    private boolean[] jaGritouMorte = new boolean[7];

    final Thread threadAnimacao = new Thread(() -> {
        int tempoEntreFramesAnimando = 1000 / FPS_ANIMANDO;
        int tempoEntreFramesParado = 1000 / FPS_PARADO;
        while (trucoActivity == null || trucoActivity.partida == null) sleep(200);
        while (!trucoActivity.isFinishing()) {
            if (visivel) postInvalidate();
            if (calcTempoAteFimAnimacaoMS() >= 0) sleep(tempoEntreFramesAnimando);
            else sleep(tempoEntreFramesParado);
        }
    });

    private void sleep(int tempoMS) {
        try { Thread.sleep(tempoMS); } catch (InterruptedException ignored) { }
    }

    public MesaView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); init(context); }
    public MesaView(Context context, AttributeSet attrs) { super(context, attrs); init(context); }
    public MesaView(Context context) { super(context); init(context); }

    private void init(Context context) { this.density = context.getResources().getDisplayMetrics().density; }
    public void setTrucoActivity(TrucoActivity trucoActivity) { this.trucoActivity = trucoActivity; }
    public void setIndiceDesenhoCartaFechada(int indice) { CartaVisual.setIndiceDesenhoCartaFechada(indice); }
    public void setEscalaFonte(int escala) { divisorTamanhoFonte = 21f - escala; }

    public void notificaAnimacao(long fim) {
        if (animandoAte < fim) animandoAte = fim;
        threadAnimacao.interrupt();
    }

    public void aguardaFimAnimacoes() {
        long msAteFim;
        while ((msAteFim = animandoAte - System.currentTimeMillis()) > 0) {
            try { Thread.sleep(msAteFim); } catch (InterruptedException e) { return; }
        }
    }

    public boolean isInicializada() { return inicializada; }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        CartaVisual.ajustaTamanho(w, h);
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        tamanhoFonte = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, Math.min(w, h) / divisorTamanhoFonte, displayMetrics);

        if (!inicializada) {
            for (int i = 0; i < cartas.length; i++) {
                cartas[i] = new CartaVisual(this, -200, -200, null, corFundoCartaBalao);
                cartas[i].visible = false;
            }
            threadAnimacao.start();
        }
        inicializada = true;
    }

    public CartaVisual getCartaVisual(Carta c) {
        for (CartaVisual cv : cartas) if (c != null && c.equals(cv)) return cv;
        return null;
    }

    public void diz(String texto, int posicaoTela, int tempoMS) {
        aguardaFimAnimacoes();
        mostraBalaoAte = System.currentTimeMillis() + tempoMS;
        fraseBalao = texto.equals("morte") ? trucoActivity.getFraseMorte() : texto;
        posicaoBalao = posicaoTela;
        notificaAnimacao(mostraBalaoAte);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (event.getAction() == MotionEvent.ACTION_UP) {
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

    private long calcTempoAteFimAnimacaoMS() { return animandoAte - System.currentTimeMillis(); }

    public void vez(boolean humano) {
        aguardaFimAnimacoes();
        statusVez = humano ? STATUS_VEZ_HUMANO_OK : STATUS_VEZ_OUTRO;
    }

    public void distribuiMao() {
        aguardaFimAnimacoes();
        recolheMao();

        if (trucoActivity.partida == null) return;
        for(int i = 0; i < 7; i++) { jaGritouMorte[i] = false; ultimosPalpites[i] = -1; }

        int totalCartas = trucoActivity.partida.getSituacaoJogo().quantidadeCartasRodada;

        for (int i = 1; i <= 6; i++) {
            Jogador j = trucoActivity.partida.getJogador(i);
            if (j == null || trucoActivity.partida.getSituacaoJogo().eliminado[i]) continue;
            int posTela = trucoActivity.jogadorHumano.posicaoNaTela(j);
            Carta[] mao = j.getCartas();
            int offset = (posTela - 1) * 10;

            for (int k = 0; k < totalCartas; k++) {
                if (mao != null && k < mao.length && mao[k] != null) {
                    CartaVisual c = cartas[offset + k];
                    c.visible = true; c.descartada = false;
                    if (posTela == 1) { c.copiaCarta(mao[k]); c.setFechada(false); }
                    else c.setFechada(true);
                    c.movePara(calcPosLeftCarta(posTela, k, totalCartas), calcPosTopCarta(posTela, k), 85);
                }
            }
        }

        Carta vira = trucoActivity.partida.cartaDaMesa;
        if (vira != null) {
            CartaVisual cvVira = cartas[60];
            cvVira.visible = true; cvVira.setFechada(false); cvVira.copiaCarta(vira);
            cvVira.movePara(getWidth() / 2 - (CartaVisual.largura / 2) + 25, (getHeight() / 2) - (CartaVisual.altura / 2), 50);

            CartaVisual cvBaralho = cartas[61];
            cvBaralho.visible = true; cvBaralho.setFechada(true);
            cvBaralho.movePara(getWidth() / 2 - (CartaVisual.largura / 2) - 25, (getHeight() / 2) - (CartaVisual.altura / 2), 50);
        }
    }

    public void recolheMao() {
        aguardaFimAnimacoes();
        cartasJogadas.clear();
        for (CartaVisual c : cartas) { c.visible = false; c.descartada = false; c.escura = false; }
    }

    public void descarta(Carta c, int posTela) {
        aguardaFimAnimacoes();
        int offset = (posTela - 1) * 10;
        CartaVisual cv = null;
        for (int i = 0; i < 10; i++) {
            CartaVisual cand = cartas[offset + i];
            if (cand.visible && !cand.descartada) {
                if (posTela == 1 && c.equals(cand)) { cv = cand; break; }
                else if (posTela != 1) { cv = cand; break; }
            }
        }
        if (cv == null) return;
        cv.copiaCarta(c); cv.setFechada(false);
        cv.movePara(calcPosLeftDescartada(posTela), calcPosTopDescartada(posTela), 200);
        cv.descartada = true;
        cartasJogadas.addElement(cv);
    }

    public void setVisivel(boolean visivel) { this.visivel = visivel; }

    private int calcPosLeftCarta(int posTela, int numCarta, int totalCartas) {
        int gap = CartaVisual.largura / 2;
        int totalWidth = (totalCartas - 1) * gap + CartaVisual.largura;
        int centerW = getWidth() / 2;
        int startX;
        switch (posTela) {
            case 1: startX = centerW - totalWidth / 2; break;
            case 2: startX = getWidth() - CartaVisual.largura - 10; break;
            case 3: startX = getWidth() - CartaVisual.largura - 10; break;
            case 4: startX = centerW - totalWidth / 2; break;
            case 5: startX = 10; break;
            case 6: startX = 10; break;
            default: startX = 0;
        }
        return startX + (numCarta * ((posTela == 1 || posTela == 4) ? gap : gap / 3));
    }

    private int calcPosTopCarta(int posTela, int numCarta) {
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
        return startY + (numCarta * gap);
    }

    private int calcPosLeftDescartada(int posTela) {
        int centerX = getWidth() / 2 - CartaVisual.largura / 2;
        int offset = (int) (CartaVisual.largura * 1.3);
        switch (posTela) {
            case 2: case 3: return centerX + offset;
            case 5: case 6: return centerX - offset;
            default: return centerX;
        }
    }

    private int calcPosTopDescartada(int posTela) {
        int centerY = getHeight() / 2 - CartaVisual.altura / 2;
        int offsetH = (int) (CartaVisual.altura * 1.3);
        int offsetM = (int) (CartaVisual.altura * 0.6);
        switch (posTela) {
            case 1: return centerY + offsetH;
            case 2: return centerY + offsetM;
            case 3: return centerY - offsetM;
            case 4: return centerY - offsetH;
            case 5: return centerY - offsetM;
            case 6: return centerY + offsetM;
        }
        return centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRGB(27, 142, 60);

        if (trucoActivity != null && trucoActivity.partida != null) {
            SituacaoJogo sit = trucoActivity.partida.getSituacaoJogo();
            for (int i = 1; i <= 6; i++) {
                if (sit.eliminado[i] && !jaGritouMorte[i]) {
                    Jogador j = trucoActivity.partida.getJogador(i);
                    if (j != null) { diz("morte", trucoActivity.jogadorHumano.posicaoNaTela(j), 5000); jaGritouMorte[i] = true; }
                }
                if (!sit.eliminado[i] && sit.faseJogo == 1 && sit.palpites[i] != -1 && ultimosPalpites[i] == -1) {
                    ultimosPalpites[i] = sit.palpites[i];
                    Jogador j = trucoActivity.partida.getJogador(i);
                    int posTela = trucoActivity.jogadorHumano.posicaoNaTela(j);
                    if (posTela != 1) diz("Faço " + sit.palpites[i] + "!", posTela, 3000);
                }
            }
        }

        if (cartas[61] != null && cartas[61].visible) cartas[61].draw(canvas);
        if (cartas[60] != null && cartas[60].visible) cartas[60].draw(canvas);

        for (int i = 0; i < 60; i++) if (cartas[i] != null && cartas[i].visible && !cartasJogadas.contains(cartas[i])) cartas[i].draw(canvas);
        for (CartaVisual c : cartasJogadas) if (c.visible) c.draw(canvas);

        desenhaIndicadorDeVez(canvas); // Estrela desenhada antes do texto pra não sobrepor letras
        desenhaNomesEVidas(canvas);
        desenhaBalao(canvas);
    }

    // --- FUNÇÃO AUXILIAR PARA TEXTO BONITO COM BORDA PRETA ---
    private void desenhaTextoComContorno(Canvas canvas, String texto, float x, float y, Paint paint) {
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4f);
        canvas.drawText(texto, x, y, paint);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText(texto, x, y, paint);
    }

    private void desenhaNomesEVidas(Canvas canvas) {
        if (trucoActivity == null || trucoActivity.partida == null) return;
        Paint paint = new Paint(); paint.setAntiAlias(true); paint.setTextSize(tamanhoFonte);
        SituacaoJogo sit = trucoActivity.partida.getSituacaoJogo();

        // 1. DESENHA OS BOTS
        paint.setTextAlign(Align.CENTER);
        for (int i = 1; i <= 6; i++) {
            Jogador j = trucoActivity.partida.getJogador(i);
            if (j == null || sit.eliminado[i]) continue;
            int posTela = trucoActivity.jogadorHumano.posicaoNaTela(j);

            if (posTela == 1) continue; // Pula o humano pra desenhar depois

            String linha1 = j.getNome();
            String linha2 = "💖" + sit.vidas[i];
            String linha3 = sit.palpites[i] != -1 ? "⚔️ " + sit.feitas[i] + "/" + sit.palpites[i] : "⚔️ -/-";

            float x = calcPosLeftCarta(posTela, 0, 1) + (CartaVisual.largura / 2f);
            float y = calcPosTopCarta(posTela, 0);

            if (posTela == 6 || posTela == 2) y += CartaVisual.altura + tamanhoFonte + 10;
            else y -= (tamanhoFonte * 2) + 30; // Distância da carta

            desenhaTextoComContorno(canvas, linha1, x, y, paint);
            desenhaTextoComContorno(canvas, linha2, x, y + tamanhoFonte + 5, paint);
            desenhaTextoComContorno(canvas, linha3, x, y + (tamanhoFonte * 2) + 10, paint);
        }

        // 2. DESENHA O HUMANO (Canto Direito Inferior)
        paint.setTextAlign(Align.RIGHT);
        int eu = trucoActivity.jogadorHumano.getPosicao();
        String linhaVidas = "VIDAS: (💖" + sit.vidas[eu] + ")";
        String linhaPalpite = "FAÇO: ⚔️ " + (sit.palpites[eu] != -1 ? sit.feitas[eu] + "/" + sit.palpites[eu] : "-/-");
        float pxDir = getWidth() - 20;
        float pyDir = getHeight() - (tamanhoFonte * 2) - 20;
        desenhaTextoComContorno(canvas, linhaVidas, pxDir, pyDir, paint);
        desenhaTextoComContorno(canvas, linhaPalpite, pxDir, pyDir + tamanhoFonte + 10, paint);

        // 3. DESENHA A RODADA (Canto Esquerdo Inferior)
        paint.setTextAlign(Align.LEFT);
        paint.setTextSize(tamanhoFonte * 0.9f);
        String linhaRodada1 = "RODADA ATUAL:";
        String linhaRodada2 = String.format("%02d", sit.numeroDaMao);
        float pxEsq = 20;
        float pyEsq = getHeight() - (tamanhoFonte * 2) - 20;
        desenhaTextoComContorno(canvas, linhaRodada1, pxEsq, pyEsq, paint);
        desenhaTextoComContorno(canvas, linhaRodada2, pxEsq, pyEsq + tamanhoFonte + 10, paint);
    }

    private void desenhaIndicadorDeVez(Canvas canvas) {
        if (posicaoVez == 0) return;
        Paint paintSeta = new Paint(); paintSeta.setTextAlign(Align.CENTER); paintSeta.setTextSize(CartaVisual.altura * 0.35f); // MENOR!

        // Agora a estrela crava exatamente no meio da carta!
        float x = calcPosLeftCarta(posicaoVez, 0, 1) + (CartaVisual.largura / 2f);
        float y = calcPosTopCarta(posicaoVez, 0) + (CartaVisual.altura / 2f) + (CartaVisual.altura * 0.12f);

        canvas.drawText("⭐", x, y, paintSeta);
    }

    private void desenhaBalao(Canvas canvas) {
        if (fraseBalao == null || mostraBalaoAte <= System.currentTimeMillis()) return;
        Paint paintFonte = new Paint(); paintFonte.setAntiAlias(true); paintFonte.setTextSize(tamanhoFonte); paintFonte.setColor(Color.BLACK);
        Rect bounds = new Rect(); paintFonte.getTextBounds(fraseBalao, 0, fraseBalao.length(), bounds);

        int largBalao = bounds.width() + 40;
        int altBalao = bounds.height() + 40;

        float x = calcPosLeftCarta(posicaoBalao, 0, 1);
        float y = calcPosTopCarta(posicaoBalao, 0);

        // Ajuste inteligente: o balão nasce DO LADO da carta, não em cima do nome!
        if (posicaoBalao == 2 || posicaoBalao == 3) {
            x -= (largBalao + 10);
            y += CartaVisual.altura / 4f;
        } else if (posicaoBalao == 5 || posicaoBalao == 6) {
            x += CartaVisual.largura + 10;
            y += CartaVisual.altura / 4f;
        } else if (posicaoBalao == 4) {
            x += CartaVisual.largura + 10;
            y += CartaVisual.altura / 4f;
        } else { // Humano
            x += CartaVisual.largura + 10;
            y -= (altBalao - 20);
        }

        if (x < 10) x = 10;
        if (x + largBalao > getWidth() - 10) x = getWidth() - largBalao - 10;

        Paint paintFundo = new Paint(); paintFundo.setAntiAlias(true); paintFundo.setColor(corFundoCartaBalao);
        canvas.drawRoundRect(new RectF(x, y, x + largBalao, y + altBalao), 15, 15, paintFundo);
        canvas.drawText(fraseBalao, x + 20, y + altBalao - 20, paintFonte);
    }

    public void setPosicaoVez(int posicaoVez) { this.posicaoVez = posicaoVez; }
    public void setCorFundoCartaBalao(int corFundoCartaBalao) { this.corFundoCartaBalao = corFundoCartaBalao; }
}
