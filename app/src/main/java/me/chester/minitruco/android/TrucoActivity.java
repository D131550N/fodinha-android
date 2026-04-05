package me.chester.minitruco.android;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Random;

import me.chester.minitruco.R;
import me.chester.minitruco.core.Partida;

/**
 * Activity onde o jogo Fodinha efetivamente acontece.
 * Exibe a MesaView (onde ficam as cartas) e recebe o palpite do jogador.
 */
public class TrucoActivity extends Activity {

    public static final String BROADCAST_IDENTIFIER = "me.chester.minitruco.EVENTO_TRUCO_ACTIVITY";
    private static boolean mIsViva = false;
    boolean partidaAbortada = false;
    JogadorHumano jogadorHumano;
    Partida partida;
    private MesaView mesa;
    Button btnNovaPartida;

    private SharedPreferences preferences;
    private Random random = new Random();

    // Frases do Pica-Pau para Eliminação
    private final String[] FRASES_MORTE = {
        "Adeus, tio Paulo!",
        "Tchau, vovô e vovó!",
        "Adeus, tia Titous!",
        "Adeus, tia Net!",
        "Fui pro saco!",
        "Deu ruim!"
    };

    public static boolean isViva() {
        return mIsViva;
    }

    private void iniciaNovaPartida() {
        jogadorHumano = new JogadorHumano(this, mesa);
        partida = CriadorDePartida.iniciaNovaPartida(jogadorHumano);
        mIsViva = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.truco);
        EdgeToEdgeHelper.aplicaSystemBarInsets(this);
        
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Muitos elementos visuais de placar do Truco foram desativados no XML,
        // mas pegamos o botão de reiniciar a partida que ainda é útil.
        btnNovaPartida = findViewById(R.id.btnNovaPartida);
        if(btnNovaPartida != null) btnNovaPartida.setVisibility(View.GONE);

        mesa = findViewById(R.id.MesaView01);
        mesa.setCorFundoCartaBalao(preferences.getInt("corFundoCarta", android.graphics.Color.WHITE));
        mesa.velocidade = preferences.getBoolean("animacaoRapida", false) ? 4 : 1;
        mesa.setEscalaFonte(Integer.parseInt(preferences.getString("escalaFonte", "1")));
        mesa.setTrucoActivity(this);
        mesa.setIndiceDesenhoCartaFechada(preferences.getInt("indiceDesenhoCartaFechada", 0));

        // O jogo só inicia quando a mesa terminar de desenhar o fundo verde
        new Thread(() -> {
            while (!mesa.isInicializada()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            iniciaNovaPartida();
        }).start();
    }

    /**
     * Recebe a notificação do Core e avisa a MesaView para redesenhar
     * os nomes com a quantidade de vidas atualizada.
     */
    public void atualizaVidasNaTela() {
        if (mesa != null) {
            mesa.postInvalidate();
        }
    }

    /**
     * Tira a cor de destaque (já que não há mais placar por equipes)
     */
    public void tiraDestaqueDoPlacar() {
        // Ignorado na Fodinha
    }

    /**
     * Abre uma janelinha perguntando quantas vidas o jogador quer apostar.
     * Quando o jogador clica em OK, o palpite é enviado ao motor (Core).
     */
    public void pedePalpite() {
        runOnUiThread(() -> {
            // Cria um NumberPicker nativo do Android
            final NumberPicker picker = new NumberPicker(this);
            picker.setMinValue(0);
            
            // O máximo que ele pode apostar é a quantidade de cartas da rodada
            int maxCartas = partida != null ? partida.getSituacaoJogo().quantidadeCartasRodada : 10;
            picker.setMaxValue(maxCartas);

            // Tenta adivinhar um valor padrão no meio para facilitar
            picker.setValue(0);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Fase de Palpites");
            builder.setMessage("Quantas você faz nesta rodada?");
            builder.setView(picker);
            
            // Impede que o jogador feche a janela clicando fora
            builder.setCancelable(false);

            builder.setPositiveButton("Confirmar", (dialog, which) -> {
                int palpite = picker.getValue();
                if (partida != null && jogadorHumano != null) {
                    // Envia a resposta de volta ao Motor da Fodinha
                    partida.fazPalpite(jogadorHumano, palpite);
                }
            });

            builder.show();
        });
    }

    /**
     * Chamado pelo Core quando o jogo acaba.
     */
    public void jogoFechado(int numVencedor) {
        runOnUiThread(() -> {
            String mensagem;
            if (jogadorHumano.getPosicao() == numVencedor) {
                mensagem = "Você venceu a partida!";
            } else {
                mensagem = "O jogador " + numVencedor + " venceu a partida!";
            }
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
            
            if (btnNovaPartida != null) {
                btnNovaPartida.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Sorteia uma das frases do Pica-Pau para exibir quando alguém zera a vida
     */
    public String getFraseMorte() {
        int indice = random.nextInt(FRASES_MORTE.length);
        return FRASES_MORTE[indice];
    }

    public void novaPartidaClickHandler(View v) {
        if(btnNovaPartida != null) btnNovaPartida.setVisibility(View.INVISIBLE);
        iniciaNovaPartida();
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String evento = intent.getStringExtra("evento");
            if (evento.equals("desconectado")) {
                finish();
            }
        }
    };

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Na Fodinha não redesenhamos mais a barra de placar
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
        mesa.setVisivel(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mesa.setVisivel(true);
        ContextCompat.registerReceiver(
            this,
            broadcastReceiver,
            new IntentFilter(BROADCAST_IDENTIFIER),
            ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsViva = false;
        if (partida == null) return;
        partida.abandona(jogadorHumano.getPosicao());
    }

    @Override
    public void onBackPressed() {
        boolean naoPrecisaConfirmar = !preferences.getBoolean("sempreConfirmaFecharJogo", true);
        if (partida == null || partida.finalizada || naoPrecisaConfirmar) {
            finish();
            return;
        }

        View dialogPerguntaAntesDeFechar = getLayoutInflater()
            .inflate(R.layout.dialog_sempre_confirma_fechar_jogo, null);
        final CheckBox checkBoxPerguntarSempre = dialogPerguntaAntesDeFechar
            .findViewById(R.id.checkBoxSempreConfirmaFecharJogo);

        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("Encerrar")
            .setView(dialogPerguntaAntesDeFechar)
            .setMessage("Você quer mesmo sair da mesa?")
            .setPositiveButton("Sim", (dialog, which) -> {
                if (!checkBoxPerguntarSempre.isChecked()) {
                    preferences.edit().putBoolean("sempreConfirmaFecharJogo", false).apply();
                }
                finish();
            })
            .setNegativeButton("Não", null)
            .show();
    }
}
