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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    JogadorHumano jogadorHumano;
    Partida partida;
    private MesaView mesa;
    Button btnNovaPartida;

    // Componentes da Interface de Palpite
    private LinearLayout layoutPalpite;
    private Button btnMenosPalpite;
    private Button btnMaisPalpite;
    private Button btnConfirmaPalpite;
    private TextView tvValorPalpite;
    private int palpiteAtual = 0;
    private int maxPalpite = 0;

    private SharedPreferences preferences;
    private Random random = new Random();

    // Novas Frases de Morte da Fodinha
    private final String[] FRASES_MORTE = {
        "Fui tapeado!",
        "Vodu é pra jacu!",
        "É pra lixo que eu vou!",
        "Eu não vou embora!",
        "Meus olhos!",
        "Venci!", // (Irônico)
        "Estupendo!"
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

        btnNovaPartida = findViewById(R.id.btnNovaPartida);
        if(btnNovaPartida != null) btnNovaPartida.setVisibility(View.GONE);

        // Instancia os botões da Caixa de Palpite
        layoutPalpite = findViewById(R.id.layoutPalpite);
        btnMenosPalpite = findViewById(R.id.btnMenosPalpite);
        btnMaisPalpite = findViewById(R.id.btnMaisPalpite);
        btnConfirmaPalpite = findViewById(R.id.btnConfirmaPalpite);
        tvValorPalpite = findViewById(R.id.tvValorPalpite);

        configuraBotoesPalpite();

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

    private void configuraBotoesPalpite() {
        btnMaisPalpite.setOnClickListener(v -> {
            if (palpiteAtual < maxPalpite) {
                palpiteAtual++;
                atualizaTextoPalpite();
            }
        });

        btnMenosPalpite.setOnClickListener(v -> {
            if (palpiteAtual > 0) {
                palpiteAtual--;
                atualizaTextoPalpite();
            }
        });

        btnConfirmaPalpite.setOnClickListener(v -> {
            if (partida != null && jogadorHumano != null) {
                layoutPalpite.setVisibility(View.GONE); // Esconde a caixa
                partida.fazPalpite(jogadorHumano, palpiteAtual); // Envia para o motor
            }
        });
    }

    private void atualizaTextoPalpite() {
        tvValorPalpite.setText(String.valueOf(palpiteAtual));
    }

    /**
     * Atualiza os nomes e as vidas redesenhando a mesa
     */
    public void atualizaVidasNaTela() {
        if (mesa != null) {
            mesa.postInvalidate();
        }
    }

    public void atualizaPlacar(int pontosNos, int pontosEles) {
        // Ignorado, placar não existe mais.
    }

    public void tiraDestaqueDoPlacar() {
        // Ignorado na Fodinha
    }

    /**
     * Chamado pelo Core quando é a vez do humano fazer o palpite.
     * Nós exibimos a interface flutuante e travamos os botões + e -.
     */
    public void pedePalpite() {
        runOnUiThread(() -> {
            palpiteAtual = 0;
            maxPalpite = partida != null ? partida.getSituacaoJogo().quantidadeCartasRodada : 10;
            atualizaTextoPalpite();

            // Aqui futuramente colocaremos a Regra do Último!

            layoutPalpite.setVisibility(View.VISIBLE);
        });
    }

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
