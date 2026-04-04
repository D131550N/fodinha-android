# Fodinha para Android 🃏

Um jogo de cartas multiplayer para Android, focado na dinâmica do jogo **Fodinha** (também conhecido em algumas regiões como *Oh Hell* adaptado com baralho espanhol). 

Este projeto é um *fork* modificado do excelente [minitruco-android](https://github.com/chesterbr/minitruco-android), criado originalmente por Carlos Duarte do Nascimento (Chester).

## 📌 O que muda em relação ao Minitruco?

A mecânica principal do jogo foi reescrita para acomodar as regras da Fodinha:
- **Modo Free-For-All:** Cada um por si, sem equipes. Suporta até 6 jogadores simultâneos via Bluetooth.
- **Fase de Palpites:** Antes de jogar as cartas, cada jogador deve declarar quantas rodadas vai "fazer".
- **Dinâmica de Cartas:** A primeira rodada começa com 1 carta para cada jogador, a segunda com 2 cartas, e assim sucessivamente até alguém perder todas as vidas.
- **Sistema de Vidas:** Os jogadores começam com 2 vidas. Quem não cumpre o palpite feito no início da rodada, perde uma vida.

## ⚖️ Força das Cartas e Manilhas

O jogo utiliza o baralho espanhol com a seguinte ordem de força (da mais fraca para a mais forte):
`4, 5, 6, 7, Q(11), J(10), K(12), 1, 2, 3`

**Manilhas (Desempate):**
Cartas comuns empardam o jogo, mas as manilhas seguem a ordem de naipes para desempate:
1. Paus (Gato/Zap) ♣️ - *Mais forte*
2. Copas ♥️
3. Espadas ♠️
4. Ouros ♦️ - *Mais fraca*

## 🛠️ Como rodar o projeto

1. Faça o clone deste repositório: `git clone https://github.com/D131550N/nome-do-seu-repo.git`
2. Abra o projeto no **Android Studio**.
3. Aguarde o Gradle sincronizar as dependências.
4. Compile e rode no seu emulador ou dispositivo físico via USB.

## 🤝 Créditos e Agradecimentos

Este projeto só foi possível graças ao código aberto do **Minitruco**, desenvolvido por [chesterbr](https://github.com/chesterbr/minitruco-android). A estrutura de UI, renderização de cartas e comunicação Bluetooth foram herdadas deste projeto fantástico.

---
Desenvolvido com ☕ por Dieisson.
