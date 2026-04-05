package me.chester.minitruco.core;

/* SPDX-License-Identifier: BSD-3-Clause */
/* Modificado para o jogo Fodinha */

/**
 * Funções utilitárias para a comunicação do jogo (Bluetooth/Local).
 * Adaptado para suportar mesas dinâmicas de 2 a 6 jogadores.
 */
public class TrucoUtils {

    /**
     * String que deve ser substituída pela posição do jogador
     */
    public static final String POSICAO_PLACEHOLDER = "$POSICAO";

    /**
     * Monta a notificação de informação da sala (enviada para clientes bluetooth).
     * 
     * @param nomes array de Jogador ou de nomes
     * @param modo modo de partida ("F" de Fodinha)
     * @param sala string que diz o tipo e código da sala
     * @return String no formato "I ..." definido no protocolo
     */
    public static String montaNotificacaoI(Object[] nomes, String modo, String sala) {
        StringBuilder sb = new StringBuilder("I ");
        
        // Agora varre dinamicamente o tamanho do array (suportando até 6 jogadores)
        for (int i = 0; i < nomes.length; i++) {
            if (nomes[i] == null) continue; // Pula assentos vazios

            String nome = nomes[i] instanceof Jogador ? ((Jogador) nomes[i]).getNome() : (String) nomes[i];
            
            if (nome == null || nome.equals("")) {
                nome = "bot";
            }
            
            if (i > 0) {
                sb.append('|');
            }
            sb.append(nome);
        }
        
        sb.append(' ')
          .append(modo)
          .append(' ')
          .append(POSICAO_PLACEHOLDER)
          .append(' ')
          .append(sala);

        return sb.toString();
    }

    /**
     * Renderiza o HTML para o nome do jogador que vai aparecer na tela,
     * indicando quem você é e quem é o gerente (dono da sala).
     */
    public static String nomeHtmlParaDisplay(String notificacaoI, int posicaoNaTela) {
        String[] partes = notificacaoI.split(" ");
        String[] nomes = partes[1].split("\\|");
        
        // Total de jogadores na sala no momento (de 2 a 6)
        int numJogadores = nomes.length; 
        
        boolean isPublica = partes.length > 4 && partes[4].equals("PUB");
        int posicaoNoJogo = Integer.parseInt(partes[3]);
        
        // Lógica circular dinâmica: calcula quem senta aonde, baseado no número de jogadores!
        int indiceDoNomeNaPosicao = (posicaoNoJogo - 1 + posicaoNaTela - 1) % numJogadores;
        
        // O jogador no índice 0 da lista original do servidor é sempre o criador (gerente)
        boolean isGerente = (indiceDoNomeNaPosicao == 0);

        boolean mostraGerente = isGerente && !isPublica;
        
        // Evita travar caso a tela tente renderizar uma posição vazia
        if (indiceDoNomeNaPosicao >= nomes.length) {
            return "";
        }
        
        String nome = nomes[indiceDoNomeNaPosicao].replaceAll("_", " ");
        
        if (isPublica && nome.equals("bot")) {
            nome = "";
        }
        
        boolean isVoce = (posicaoNaTela == 1);
        
        return new StringBuilder()
            .append(mostraGerente ? "<b>" : "")
            .append(nome)
            .append(isVoce ? " (você)" : "")
            .append(mostraGerente ? " (gerente)</b>" : "")
            .toString();
    }
}
