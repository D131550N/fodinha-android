package me.chester.minitruco.android;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

import static android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET;
import static android.provider.Settings.Global.DEVICE_NAME;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.util.Consumer;
import androidx.preference.PreferenceManager;

import me.chester.minitruco.BuildConfig;
import me.chester.minitruco.R;
import me.chester.minitruco.android.multiplayer.bluetooth.ClienteBluetoothActivity;
import me.chester.minitruco.android.multiplayer.bluetooth.ServidorBluetoothActivity;
import me.chester.minitruco.android.multiplayer.internet.ClienteInternetActivity;
import me.chester.minitruco.android.multiplayer.internet.InternetUtils;
import me.chester.minitruco.core.Jogador;
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

        configuraBotoesMultiplayer();
        mostraNotificacaoInicial();

        // Links e Botões de Ajuda/Sobre
        TextView textViewLinkContato = findViewById(R.id.textViewLinkContato);
        SpannableString textoLinkContato = new SpannableString(textViewLinkContato.getText());
        textoLinkContato.setSpan(new UnderlineSpan(), 0, textoLinkContato.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        textViewLinkContato.setText(textoLinkContato);
        textViewLinkContato.setOnClickListener(v -> {
            String facebookUrl = "https://www.facebook.com/profile.php?id=61550014616104";
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=" + facebookUrl)));
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(facebookUrl)));
            }
        });

        findViewById(R.id.btnAjuda).setOnClickListener(v -> {
            mostraAlertBox("Regras da Fodinha", "A primeira rodada começa com 1 carta. " +
                    "Você deve prometer quantas rodadas vai ganhar. Quem errar o palpite perde 1 vida. " +
                    "A quantidade de cartas aumenta até alguém ser eliminado!");
        });

        findViewById(R.id.btnSobre).setOnClickListener(v -> {
            mostraAlertBox(this.getString(R.string.titulo_sobre), "Modificação de Fodinha baseada no Minitruco de Chester.");
        });
    }

    private void mostraNotificacaoInicial() {
        boolean mostraInstrucoes = preferences.getBoolean("mostraInstrucoes", true);
        if (mostraInstrucoes) {
            mostraAlertBox("Bem-vindo à Fodinha!", "Prepare-se para blefar e torcer para não perder suas vidas.");
            Editor e = preferences.edit();
            e.putBoolean("mostraInstrucoes", false);
            e.apply();
        }
    }

    private void configuraBotoesMultiplayer() {
        boolean temBluetooth = BluetoothAdapter.getDefaultAdapter() != null;
        boolean temInternet = true; // Deixamos true por padrão

        Button btnBluetooth = findViewById(R.id.btnBluetooth);
        Button btnInternet = findViewById(R.id.btnInternet);
        btnBluetooth.setVisibility(temBluetooth ? View.VISIBLE : View.GONE);
        btnInternet.setVisibility(temInternet ? View.VISIBLE : View.GONE);
        
        if (temBluetooth) {
            btnBluetooth.setOnClickListener(v -> perguntaCriarOuProcurarBluetooth());
        }
        if (temInternet) {
            btnInternet.setOnClickListener(v -> pedeNomeEConecta());
        }
    }

    private void pedeNomeEConecta() {
        if (conectadoNaInternet()) {
            pedeNome((nome) -> {
                startActivity(new Intent(getBaseContext(), ClienteInternetActivity.class));
            });
        } else {
            mostraAlertBox("Sem conexão", "Não foi possível conectar à Internet.");
        }
    }

    private boolean conectadoNaInternet() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                return cm.getNetworkCapabilities(cm.getActiveNetwork()).hasCapability(NET_CAPABILITY_INTERNET);
            } else {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void pedeNome(Consumer<String> callback) {
        String nome = preferences.getString("nome_multiplayer", null);
        if (nome == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            nome = Settings.System.getString(getContentResolver(), DEVICE_NAME);
        }
        if (nome == null) {
            try {
                nome = Settings.Secure.getString(getContentResolver(), "bluetooth_name");
            } catch (Exception ignored) {}
        }
        nome = Jogador.sanitizaNome(nome);

        if (!preferences.getBoolean("semprePerguntarNome", true)) {
            callback.accept(nome);
            return;
        }

        View viewConteudo = getLayoutInflater().inflate(R.layout.dialog_nome_jogador, null);
        final CheckBox checkBoxPerguntarSempre = viewConteudo.findViewById(R.id.checkBoxSemprePerguntaNome);
        final EditText editTextNomeJogador = viewConteudo.findViewById(R.id.editTextNomeJogador);
        editTextNomeJogador.setText(nome.replaceAll("_", " "));

        runOnUiThread(() -> {
            AlertDialog dialogNome = new AlertDialog.Builder(this)
                    .setTitle("Nome")
                    .setMessage("Qual nome você gostaria de usar?")
                    .setView(viewConteudo)
                    .setPositiveButton("Ok", (d, w) -> {
                        if (!checkBoxPerguntarSempre.isChecked()) {
                            preferences.edit().putBoolean("semprePerguntarNome", false).apply();
                        }
                        final String nomeFinal = Jogador.sanitizaNome(editTextNomeJogador.getText().toString());
                        preferences.edit().putString("nome_multiplayer", nomeFinal).apply();
                        callback.accept(nomeFinal);
                    })
                    .setNegativeButton("Cancela", null)
                    .create();

            dialogNome.setOnShowListener(d -> {
                Button btnOk = dialogNome.getButton(AlertDialog.BUTTON_POSITIVE);
                btnOk.setFocusable(true);
                btnOk.setFocusableInTouchMode(true);
                btnOk.requestFocus();
            });
            dialogNome.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            dialogNome.show();
        });
    }

    private void botoesHabilitados(boolean status) {
        findViewById(R.id.btnJogar).setActivated(status);
        findViewById(R.id.btnBluetooth).setActivated(status);
        findViewById(R.id.btnOpcoes).setActivated(status);
    }

    private void perguntaCriarOuProcurarBluetooth() {
        botoesHabilitados(false);
        OnClickListener listener = (dialog, which) -> {
            botoesHabilitados(true);
            switch (which) {
                case AlertDialog.BUTTON_NEGATIVE:
                    startActivity(new Intent(TituloActivity.this, ServidorBluetoothActivity.class));
                    break;
                case AlertDialog.BUTTON_POSITIVE:
                    startActivity(new Intent(TituloActivity.this, ClienteBluetoothActivity.class));
                    break;
            }
        };
        new AlertDialog.Builder(this).setTitle("Bluetooth")
                .setMessage("Para jogar via Bluetooth, um celular deve criar o jogo e os outros devem procurá-lo.\n\nCertifique-se de que todos os celulares estejam pareados.")
                .setNegativeButton("Criar Jogo", listener)
                .setPositiveButton("Procurar Jogo", listener)
                .show();
    }

    // --- MUDANÇA PRINCIPAL DA FODINHA AQUI ---
    public void jogarClickHandler(View v) {
        // Ao invés de iniciar direto, pergunta quantos bots colocar na mesa!
        String[] opcoes = {"2 Jogadores (1x1)", "3 Jogadores", "4 Jogadores", "5 Jogadores", "6 Jogadores"};
        
        new AlertDialog.Builder(this)
            .setTitle("Tamanho da Mesa")
            .setItems(opcoes, (dialog, qual) -> {
                int totalJogadores = qual + 2; // Índice 0 = 2 jogadores, etc.
                
                // Salva a escolha para o CriadorDePartida ler depois
                preferences.edit().putInt("qtd_jogadores_local", totalJogadores).apply();
                
                // Inicia a mesa de fato
                CriadorDePartida.setActivitySala(this);
                Intent intent = new Intent(TituloActivity.this, TrucoActivity.class);
                startActivity(intent);
            })
            .show();
    }

    public void opcoesButtonClickHandler(View v) {
        Intent settingsActivity = new Intent(getBaseContext(), OpcoesActivity.class);
        startActivity(settingsActivity);
    }

    @Override
    public void onBackPressed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.finishAndRemoveTask();
        }
    }

    // Ignorado na Fodinha, pois só temos um modo
    public void modoButtonClickHandler(View view) {
        Toast.makeText(this, "A Fodinha possui apenas uma regra oficial!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public Partida criaNovaPartida(JogadorHumano jogadorHumano) {
        String modo = "F"; // F de Fodinha
        boolean humanoDecide = true;
        boolean jogoAutomatico = false;
        Partida novaPartida = new PartidaLocal(humanoDecide, jogoAutomatico, modo);
        novaPartida.adiciona(jogadorHumano);

        // Puxa a quantidade de jogadores escolhida no pop-up (Padrão: 6)
        int totalJogadores = preferences.getInt("qtd_jogadores_local", 6);
        
        // Adiciona a quantidade exata de Bots na mesa
        for (int i = 1; i < totalJogadores; i++) {
            novaPartida.adiciona(new JogadorBot());
        }
        
        return novaPartida;
    }

    @Override
    public void enviaLinha(String linha) {
        throw new RuntimeException("Jogo single-player não possui conexão");
    }

    @Override
    public void enviaLinha(int slot, String linha) {
        throw new RuntimeException("Jogo single-player não possui conexão");
    }

    @Override
    protected void onResume() {
        super.onResume();
        CriadorDePartida.setActivitySala(this);
    }
}
