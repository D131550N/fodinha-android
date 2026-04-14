package me.chester.minitruco.android;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
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

        // --- PUXANDO AS REGRAS DO STRINGS.XML COM FORMATAÇÃO HTML ---
        findViewById(R.id.btnAjuda).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_instrucoes)
                .setMessage(Html.fromHtml(getString(R.string.texto_instrucoes), Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", null)
                .show();
        });

        // --- PUXANDO O SOBRE DO STRINGS.XML COM FORMATAÇÃO HTML ---
        findViewById(R.id.btnSobre).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(R.string.titulo_sobre)
                .setMessage(Html.fromHtml(getString(R.string.texto_sobre), Html.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK", null)
                .show();
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

    public void bluetoothClickHandler(View v) {
        // Inicia a atividade original de Bluetooth do projeto
        Intent intent = new Intent(this, me.chester.minitruco.android.bluetooth.BluetoothActivity.class);
        startActivity(intent);
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
