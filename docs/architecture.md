# Arquitetura do Orin

**Status:** planejamento inicial  
**Atualizado em:** 12/10/2026

## Objetivo

O Orin vai usar o microfone de um celular Android como entrada de áudio de um computador pela rede Wi-Fi local. O primeiro destino é o Linux; depois, o mesmo aplicativo desktop deverá funcionar no Windows. O projeto nasce para uso pessoal e pode ser publicado como código aberto no futuro.

A meta prática é conseguir conversar pelo Discord e usar outros programas que dependem de microfone sem instalar um cliente diferente em cada sistema operacional.

## Decisões atuais

| Área | Escolha | Observação |
| --- | --- | --- |
| Repositório | Monorepo `orin` | Um único histórico Git |
| Android | Kotlin, Jetpack Compose e Material 3 | Android Studio |
| Captura no Android | `AudioRecord` | Permissão de microfone |
| Desktop | Java 25 | IntelliJ IDEA |
| Interface desktop | Swing ou JavaFX | Escolha pendente; será simples |
| Áudio no desktop | Java Sound API | Primeiro para testar reprodução e seleção de saída |
| Comunicação | UDP na rede local | Um celular conectado a um desktop no início |
| Áudio inicial | PCM mono, 48 kHz, 16 bits | Configuração a validar nos dispositivos |
| Linux | PipeWire | Integração com microfone virtual |
| Windows | Adaptador para dispositivo virtual existente | Testar primeiro com VB-CABLE ou equivalente |
| Código nativo | Rust ou C++ | Só se a integração exigir |
| Build | Gradle Kotlin DSL | Builds separados para Android e desktop |

As ferramentas de depuração do Android Studio usarão o celular conectado por USB. A transmissão de áudio continuará acontecendo pelo Wi-Fi; suporte a áudio via USB não faz parte da primeira versão.

## Organização do repositório

```text
orin/
├── android/                 # Projeto Gradle próprio, aberto no Android Studio
├── desktop/                 # Projeto Gradle próprio, aberto no IntelliJ
├── docs/
│   ├── architecture.md
│   ├── protocol.md
│   └── roadmap.md
├── .github/                 # Automações quando os builds estiverem prontos
├── .editorconfig
├── .gitattributes
├── .gitignore
└── README.md
```

Não haverá um build Gradle obrigatório na raiz. Os projetos poderão ser compilados separadamente. No começo, `protocol.md` será a referência comum entre Kotlin e Java. Um módulo compartilhado de código só será criado se o benefício compensar a configuração adicional.

## Caminho do áudio

```text
Android (Kotlin)                       Desktop (Java)
Microfone
    |
AudioRecord
    |
Blocos de PCM
    |
Emissor UDP  ─────── Wi-Fi ───────> Receptor UDP
                                          |
                                     Jitter buffer
                                          |
                                   Saída de áudio
                                          |
                         +----------------+----------------+
                         |                                 |
                    Testes: alto-falante           Uso: microfone virtual
                                                          |
                                                Discord, navegador, jogos
```

Durante os primeiros testes, a saída será um alto-falante ou um arquivo de diagnóstico. Isso facilita saber se a captura, o envio e a recepção estão corretos antes de mexer na configuração do sistema operacional.

### Aplicativo Android

A interface mostrará a conexão e os controles básicos. A captura do áudio e a comunicação de rede ficarão fora dos componentes de interface.

Responsabilidades previstas:

- Solicitar e respeitar a permissão de microfone.
- Capturar amostras por meio de `AudioRecord`.
- Agrupar as amostras em blocos com duração conhecida.
- Enviar esses blocos ao desktop e apresentar o estado da conexão.
- Encerrar a captura e liberar o microfone ao desconectar.

Para continuar capturando áudio com a tela apagada ou o aplicativo fora de foco, estudaremos um *Foreground Service* do tipo `microphone`. Ele requer permissões específicas, e o Android restringe sua inicialização quando o aplicativo já está em segundo plano. Vamos tratar disso depois que a captura com a tela aberta funcionar.

### Aplicativo desktop

O Java cuidará da conexão, recepção, ordenação dos pacotes, buffers e encaminhamento do áudio. A interface será pequena e independente dessas operações.

Não precisamos criar muitas classes antes de observar o primeiro fluxo de áudio. A separação inicial será conceitual: rede, áudio, integração com o sistema operacional e interface. Os pacotes Java serão definidos quando essas responsabilidades aparecerem no código.

### Linux

O primeiro experimento vai verificar como escrever áudio em uma saída gerenciada pelo PipeWire e disponibilizá-lo como fonte de gravação. A camada de compatibilidade PulseAudio do PipeWire oferece módulos como `module-null-sink` e `module-remap-source`, que podem ajudar a criar esse caminho.

A solução definitiva dependerá do que funcionar na máquina de desenvolvimento. O programa também terá que lidar com a ausência do dispositivo virtual e com alterações nos dispositivos de áudio durante a execução.

### Windows

A primeira abordagem será usar um dispositivo de áudio virtual já instalado, como o VB-CABLE, e verificar se o Java consegue escrever nele de forma confiável. O Orin não terá driver próprio no MVP.

Um driver de terceiros tem instalação, licença e distribuição independentes do projeto. Caso esse caminho não seja suficiente, avaliaremos uma integração nativa específica para Windows.

## Limites e privacidade

O Orin funcionará na rede local. Não está previsto criar conta, API na nuvem, banco de dados ou telemetria. O áudio não será salvo em disco durante o uso normal. Gravações curtas poderão ser produzidas apenas em testes explícitos.

Wi-Fi local, por si só, não protege a transmissão. Durante uma prova de conceito isolada, poderemos experimentar pacotes sem criptografia para entender o fluxo. **Antes de usar o Orin regularmente, será necessário implementar pareamento, autenticação e criptografia do transporte com uma solução consolidada.** Não vamos desenvolver nossa própria criptografia.

Logs devem registrar eventos técnicos, como erros de dispositivo, desconexões e contagem de pacotes, sem incluir conteúdo de áudio.

## O que fica para depois

Opus, descoberta automática pela rede, transmissão via USB, Bluetooth, filtros de ruído, vários celulares conectados e drivers próprios. Essas possibilidades não devem influenciar a estrutura inicial além do necessário para manter o código compreensível.

## Decisões pendentes

- Escolher Swing ou JavaFX após analisar a interface de referência do WO Mic.
- Testar quais dispositivos de saída o Java Sound enxerga no Linux e no Windows.
- Definir a solução de pareamento e transporte criptografado antes do uso diário.
- Decidir se será necessário negociar formatos de áudio entre dispositivos.
- Avaliar Opus apenas depois de medir qualidade, tráfego e latência com PCM.

## Referências técnicas

- [Java Sound API: `SourceDataLine`](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/javax/sound/sampled/SourceDataLine.html)
- [PipeWire: módulos compatíveis com PulseAudio](https://pipewire.pages.freedesktop.org/pipewire/page_pulse_modules.html)
- [PipeWire: `module-remap-source`](https://pipewire.pages.freedesktop.org/pipewire/page_pulse_module_remap_source.html)
- [Android: serviço em primeiro plano do tipo microfone](https://developer.android.com/develop/background-work/services/fgs/service-types?hl=pt-br)
