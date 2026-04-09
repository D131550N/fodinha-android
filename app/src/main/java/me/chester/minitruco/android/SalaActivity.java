package me.chester.minitruco.android;

import static me.chester.minitruco.core.TrucoUtils.nomeHtmlParaDisplay;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import me.chester.minitruco.R;
import me.chester.minitruco.core.JogadorHumano;
import me.chester.minitruco.core.Partida;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Copyright © 2005-2023 Carlos Duarte do Nascimento "Chester" <cd@pobox.com> */

/**
 * Qualquer activity que permite ao jogador iniciar novas partidas.
 * <p>
 * Descendentes desta classe exibem as opções e/ou nomes dos jogadores (conforme
 * o tipo de jogo e o papel do usuário), criam os objetos Partida (onde a lógica
 * do jogo roda) e chamam a TrucoActivity (que permite ao usuário interagir com
 * essa lógica).
 */
public abstract class SalaActivity extends AppCompatActivity {

    private final static Logger LOGGER = Logger.getLogger("SalaActivity");
    protected Button btnIniciar;
    protected Button btnInverter;
    protected Button btnTrocar;
    protected Button btnNovaSalaPublica;
    protected Button btnNovaSalaPrivada;
    protected Button btnEntrarComCodigo;
    protected View layoutJogadoresEBotoesGerente;
    protected View layoutBotoesGerente;
    protected View layoutBotoesInternet;
    protected TextView textViewStatus;
    protected TextView textViewJogador1;
    protected TextView textViewJogador2;
    protected TextView textViewJogador3;
    protected TextView textViewJogador4;
    protected TextView[] textViewsJogadores;
    protected TextView textViewTituloSala;
    protected TextView textViewInstrucoesSalaPrivada;
    protected int posJogador;
    protected String modo;
    // Removida a variavel PartidaRemota, usando apenas a genérica
    protected Partida partida;
    protected int numJogadores;
    protected boolean isGerente;
    public String tipoSala;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdgeHelper.aplicaSystemBarInsets(this);
    }

    /**
     * Em salas multiplayer (que de fato mostram uma "sala" com os nomes dos
     * jogadores, etc.), ativa e inicializa o layout com esses elementos.
     */
    protected void inicializaLayoutSala() {
        setContentView(R.layout.sala);
        btnIniciar = findViewById(R.id.btnIniciar);
        btnInverter = findViewById(R.id.btnInverter);
        btnTrocar = findViewById(R.id.btnTrocar);
        layoutJogadoresEBotoesGerente = findViewById(R.id.layoutJogadoresEBotoesGerente);
        layoutBotoesGerente = findViewById(R.id.layoutBotoesGerente);
        layoutBotoesInternet = findViewById(R.id.layoutBotoesInternet);
        btnNovaSalaPublica = findViewById(R.id.btnNovaSalaPublica);
        btnNovaSalaPrivada = findViewById(R.id.btnNovaSalaPrivada);
        btnEntrarComCodigo = findViewById(R.id.btnEntrarComCodigo);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewTituloSala = findViewById(R.id.textViewTituloSala);
        textViewInstrucoesSalaPrivada = findViewById(R.id.textViewInstrucoesSalaPrivada);
        textViewJogador1 = findViewById(R.id.textViewJogador1);
        textViewJogador2 = findViewById(R.id.textViewJogador2);
        textViewJogador3 = findViewById(R.id.textViewJogador3);
        textViewJogador4 = findViewById(R.id.textViewJogador4);
        textViewsJogadores = new TextView[] {
            textViewJogador1, textViewJogador2, textViewJogador3, textViewJogador4
        };
        layoutJogadoresEBotoesGerente.setVisibility(View.INVISIBLE);
        layoutBotoesGerente.setVisibility(View.INVISIBLE);
        layoutBotoesInternet.setVisibility(View.GONE);
        textViewInstrucoesSalaPrivada.setVisibility(View.GONE);
        textViewStatus.setText("");
        setMensagem(null);
    }

    /**
     * Atualiza a sala com as informações recebidas do servidor.
     */
    protected void exibeMesaForaDoJogo(String notificacaoI) {
        runOnUiThread(() -> {
            String[] tokens = notificacaoI.split(" ");
            String[] nomes = tokens[1].split(Pattern.quote("|"));
            modo = tokens[2];
            posJogador = Integer.parseInt(tokens[3]);
            isGerente = posJogador == 1;
            tipoSala = tokens[4];
            String codigo = null;
            if (tipoSala.startsWith("PRI-")) {
                codigo = tipoSala.substring(4);
                tipoSala = "PRI";
            }

            encerraTrucoActivity();
            if (partida != null) {
                partida.abandona(0);
                partida = null;
            }

            numJogadores = 4;
            for (String nome : nomes) {
                if (nome.equals("bot")) {
                    numJogadores--;
                }
            }

            for (int i = 1; i <= 4; i++) {
                String nomeHtml = nomeHtmlParaDisplay(notificacaoI, i);
                TextView tv = textViewsJogadores[i - 1];
                tv.setText(Html.fromHtml(nomeHtml));
            }

            layoutJogadoresEBotoesGerente.setVisibility(View.VISIBLE);
            findViewById(R.id.layoutBotoesGerente).setVisibility(
                isGerente && !tipoSala.equals("PUB") ? View.VISIBLE : View.INVISIBLE);
            if (isGerente) {
                btnIniciar.setEnabled(numJogadores > 1);
                btnInverter.setEnabled(numJogadores > 1);
                btnTrocar.setEnabled(numJogadores > 1);
            }
            switch (tipoSala) {
                case "PUB":
                    textViewTituloSala.setText("Sala Pública");
                    layoutBotoesInternet.setVisibility(View.VISIBLE);
                    textViewInstrucoesSalaPrivada.setVisibility(View.GONE);
                    break;
                case "PRI":
                    textViewTituloSala.setText("Sala Privada - CÓDIGO: " + codigo);
                    layoutBotoesInternet.setVisibility(View.GONE);
                    textViewInstrucoesSalaPrivada.setVisibility(View.VISIBLE);
                    break;
                case "BLT":
                    textViewTituloSala.setText("Bluetooth");
                    layoutBotoesInternet.setVisibility(View.GONE);
                    textViewInstrucoesSalaPrivada.setVisibility(View.GONE);
                    break;
            }
            // Forçado para Fodinha já que não existe mais Modo.textoModo()
            textViewStatus.setText("Fodinha");

            setMensagem(null);
            if (tipoSala.equals("PUB")) {
                if (numJogadores < 4) {
                    setMensagem("Aguardando mais pessoas");
                }
            } else if (isGerente) {
                if (numJogadores == 1) {
                    setMensagem("Aguardando pelo menos uma pessoa");
                } else if (numJogadores < 4) {
                    setMensagem("Aguardando mais pessoas");
                } else {
                    setMensagem("Organize as pessoas na mesa e clique em JOGAR!");
                }
            } else {
                if (numJogadores < 4) {
                    setMensagem("Aguardando mais pessoas ou gerente iniciar partida");
                } else {
                    setMensagem("Aguardando gerente organizar a mesa e iniciar partida");
                }
            }
        });
    }

    protected void mostraAlertBox(String titulo, String texto) {
        runOnUiThread(() -> {
            if (isFinishing()) {
                return;
            }
            new AlertDialog.Builder(this).setTitle(titulo)
                .setMessage(Html.fromHtml(texto))
                .setNeutralButton("Ok", (dialog, which) -> {
                }).show();
        });
    }

    protected void msgErroFatal(String texto) {
        msgErroFatal("Aviso", texto);
    }

    protected void msgErroFatal(String titulo, String texto) {
        runOnUiThread(() -> {
            encerraTrucoActivity();
            if (isFinishing()) {
                return;
            }
            if (layoutJogadoresEBotoesGerente != null) {
                layoutJogadoresEBotoesGerente.setVisibility(View.INVISIBLE);
            }
            if (layoutBotoesInternet != null) {
                layoutBotoesInternet.setVisibility(View.GONE);
            }
            setMensagem(null);
            new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setMessage(texto)
                .setNeutralButton("Fechar", (dialog, which) -> finish())
                .setOnCancelListener(v -> finish())
                .show();
        });
    }

    public abstract Partida criaNovaPartida(JogadorHumano jogadorHumano);

    public abstract void enviaLinha(String linha);

    public abstract void enviaLinha(int slot, String linha);

    protected void iniciaTrucoActivitySePreciso() {
        if (!TrucoActivity.isViva()) {
            startActivity(
                new Intent(this, TrucoActivity.class)
                    .putExtra("multiplayer", true));
        }
    }

    public void encerraTrucoActivity() {
        if (TrucoActivity.isViva()) {
            Intent intent = new Intent(TrucoActivity.BROADCAST_IDENTIFIER);
            intent.putExtra("evento", "desconectado");
            sendBroadcast(intent);
        }
    }

    protected void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("sleep interrompido: " + e);
        }
    }

    protected void setMensagem(String mensagem) {
        runOnUiThread(() -> {
            TextView textViewMensagem = findViewById(R.id.textViewMensagem);
            if (textViewMensagem == null) {
                return;
            }
            if (mensagem == null) {
                textViewMensagem.setVisibility(View.GONE);
            } else {
                textViewMensagem.setVisibility(View.VISIBLE);
                textViewMensagem.setText(mensagem);
            }
        });
    }
}
