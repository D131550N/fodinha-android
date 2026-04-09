package me.chester.minitruco.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import me.chester.minitruco.BuildConfig;
import me.chester.minitruco.R;
import me.chester.minitruco.core.JogadorHumano;
import me.chester.minitruco.core.JogadorBot;
import me.chester.minitruco.core.Partida;
import me.chester.minitruco.core.PartidaLocal;

public class TituloActivity extends SalaActivity {

    SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.titulo);

        ((TextView) findViewById(R.id.versao_app)).setText("Fodinha v" + BuildConfig.VERSION_NAME);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        findViewById(R.id.btnAjuda).setOnClickListener(v -> {
            mostraAlertBox("Regras da Fodinha", "A primeira rodada começa com 1 carta. Você deve prometer quantas rodadas vai ganhar. Quem errar o palpite perde 1 vida.");
        });

        findViewById(R.id.btnSobre).setOnClickListener(v -> {
            mostraAlertBox("Sobre", "Modificação de Fodinha baseada no Minitruco de Chester.");
        });
    }

    public void jogarClickHandler(View v) {
        String[] opcoes = {"2 Jogadores (1x1)", "3 Jogadores", "4 Jogadores", "5 Jogadores", "6 Jogadores"};

        new AlertDialog.Builder(this)
            .setTitle("Tamanho da Mesa")
            .setItems(opcoes, (dialog, qual) -> {
                int totalJogadores = qual + 2;
                preferences.edit().putInt("qtd_jogadores_local", totalJogadores).apply();

                CriadorDePartida.setActivitySala(this);
                Intent intent = new Intent(TituloActivity.this, TrucoActivity.class);
                startActivity(intent);
            })
            .show();
    }

    public void opcoesButtonClickHandler(View v) {
        startActivity(new Intent(getBaseContext(), OpcoesActivity.class));
    }

    public void modoButtonClickHandler(View view) {
        Toast.makeText(this, "A Fodinha possui apenas uma regra oficial!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public Partida criaNovaPartida(JogadorHumano jogadorHumano) {
        Partida novaPartida = new PartidaLocal(true, false, "F");
        novaPartida.adiciona(jogadorHumano);

        int totalJogadores = preferences.getInt("qtd_jogadores_local", 6);
        for (int i = 1; i < totalJogadores; i++) {
            novaPartida.adiciona(new JogadorBot());
        }
        return novaPartida;
    }

    @Override
    public void enviaLinha(String linha) { }

    @Override
    public void enviaLinha(int slot, String linha) { }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.finishAndRemoveTask();
        }
    }
}
