<div align="center">

# Orin

### Microfone sem fio via Wi-Fi para Linux e Windows, utilizando Android

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Java](https://img.shields.io/badge/Java_25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

<br/>

[Tecnologias](#tecnologias) •
[Objetivo](#objetivo-da-primeira-versão) •
[Estado atual](#estado-do-desenvolvimento) •
[Desenvolvimento](#desenvolvimento-local) •
[Comunicação](#comunicação-e-áudio) •
[Documentação](#documentação) •
[Roadmap](#próximas-etapas)

</div>

---

O Orin é um projeto de aplicativo que permite usar o microfone de um celular Android como entrada de áudio em computadores Linux e Windows pela rede Wi-Fi local. A ideia surgiu da necessidade de usar programas diferentes para essa função em um desktop com dual boot.

O objetivo inicial é substituir o AudioRelay no Linux e o WO Mic no Windows por uma solução própria, com o mesmo aplicativo Android e um receptor desktop multiplataforma. O projeto será desenvolvido primeiro para uso pessoal.

> **Estado atual:** o Orin está na fase de preparação do repositório e dos projetos. A captura de áudio, a transmissão pela rede e o microfone virtual ainda não foram implementados. As decisões descritas abaixo são o ponto de partida do desenvolvimento e podem mudar após os primeiros testes.

## Tecnologias

O projeto usa um monorepo com dois builds Gradle independentes.

| Componente | Tecnologias previstas |
| --- | --- |
| Android | Kotlin, Jetpack Compose, Material 3 e `AudioRecord` |
| Desktop | Java 25, Java Sound API e interface simples em Swing ou JavaFX |
| Comunicação | UDP e áudio PCM sem compressão nos primeiros testes |
| Linux | PipeWire para disponibilizar o áudio como microfone virtual |
| Windows | Dispositivo de áudio virtual existente, inicialmente VB-CABLE ou equivalente |
| Build e testes | Gradle Kotlin DSL, JUnit e ferramentas de teste do Android |

Rust ou C++ poderão ser usados em componentes nativos, caso as APIs disponíveis no Java não atendam a alguma integração. Nenhum componente desse tipo está previsto para a primeira etapa.

## Objetivo da primeira versão

O fluxo de uso planejado é simples: abrir o receptor no computador, conectar o aplicativo Android pela rede local e selecionar o microfone virtual do Orin no Discord ou em outro programa.

Para chegar à versão 1.0 pessoal, o projeto deverá:

1. Capturar o áudio do microfone Android e transmiti-lo por Wi-Fi.
2. Receber e encaminhar o áudio no desktop com latência adequada para conversas.
3. Disponibilizar uma entrada de microfone no Linux e no Windows.
4. Permitir conectar, desconectar e acompanhar o estado da conexão por interfaces pequenas.
5. Lidar com interrupções da rede, perda de pacotes e encerramento da captura.
6. Proteger a comunicação com pareamento, autenticação e criptografia.

A primeira entrega utilizável será voltada ao Linux. O suporte ao Windows virá depois, aproveitando o núcleo Java e adaptando a integração com áudio.

## Estado do desenvolvimento

Ainda não há funcionalidades prontas para uso. O trabalho começa pelo desktop Java, com testes de geração e reprodução de áudio. Em seguida serão implementados o receptor UDP e a captura no Android.

Os marcos estão detalhados em [docs/roadmap.md](docs/roadmap.md). O roteiro foi organizado para que os experimentos iniciais possam ser aproveitados no aplicativo, sem exigir uma arquitetura extensa antes de termos áudio funcionando.

## Desenvolvimento local

### Pré-requisitos

- Git.
- IntelliJ IDEA e JDK 25 para o projeto desktop.
- Android Studio e Android SDK para o aplicativo Android.
- Um dispositivo Android com microfone para os testes reais.
- Linux com PipeWire para os primeiros experimentos de microfone virtual.

O aplicativo Android será testado em um celular físico conectado por USB ao Android Studio. **O USB será usado para instalar e depurar o aplicativo; o transporte de áudio continuará sendo feito pelo Wi-Fi.**

Cada projeto terá seu próprio Gradle Wrapper. Não é necessário instalar Gradle globalmente.

### Desktop

Abra `desktop/` no IntelliJ IDEA como projeto Gradle. Quando o projeto tiver sido criado, será possível verificar o build com:

```bash
cd desktop
./gradlew build
```

A execução e os argumentos do receptor serão documentados depois da implementação inicial.

### Android

Abra `android/` no Android Studio, configure o SDK e selecione o dispositivo físico para executar a aplicação. O APK de debug poderá ser compilado pelo terminal:

```bash
cd android
./gradlew :app:assembleDebug
```

Esses comandos pressupõem que os projetos Gradle já tenham sido gerados pelas IDEs.

## Estrutura do repositório

```text
orin/
├── android/                 # Aplicativo Kotlin, aberto no Android Studio
├── desktop/                 # Aplicativo Java, aberto no IntelliJ IDEA
├── docs/
│   ├── architecture.md      # Componentes, stack e integração com os sistemas
│   ├── protocol.md          # Formato de áudio e comunicação entre dispositivos
│   └── roadmap.md           # Fases e critérios de conclusão
├── .github/                 # Workflows quando os builds estiverem configurados
├── .editorconfig
├── .gitattributes
├── .gitignore
└── README.md
```

A raiz contém um único repositório Git. Android e desktop são projetos Gradle separados. Inicialmente, a documentação do protocolo será compartilhada entre as implementações em Kotlin e Java; um módulo de código compartilhado só será criado se houver necessidade.

## Comunicação e áudio

A proposta inicial utiliza PCM mono de 16 bits a 48 kHz, dividido em blocos de 10 ms e enviado por UDP. Esse formato facilita observar os dados e testar buffers antes de considerar compressão com Opus.

O receptor Java será responsável por receber os pacotes, organizar o áudio e encaminhá-lo ao dispositivo selecionado. A integração com PipeWire ou com o dispositivo virtual no Windows ficará separada da lógica de rede.

O formato definitivo dos pacotes, a negociação de parâmetros e o mecanismo de proteção do transporte ainda serão definidos. Consulte [docs/protocol.md](docs/protocol.md) para as decisões em estudo.

## Privacidade e limitações

O Orin foi planejado para funcionar na rede local, sem conta, servidor na nuvem ou telemetria. O áudio não deverá ser gravado em disco durante o uso normal.

A rede local não garante privacidade por si só. Testes iniciais poderão usar UDP sem criptografia apenas em ambiente controlado. **O aplicativo não deverá ser considerado seguro para uso cotidiano antes da implementação e validação de autenticação e criptografia.**

No Windows, a primeira versão poderá exigir a instalação separada de um driver de áudio virtual. O Orin não pretende desenvolver nem distribuir um driver próprio no MVP.

## Documentação

| Arquivo | Conteúdo |
| --- | --- |
| [Arquitetura](docs/architecture.md) | Organização dos aplicativos, fluxo do áudio e integrações com Linux e Windows |
| [Protocolo](docs/protocol.md) | PCM, pacotes UDP, buffers, compatibilidade e segurança |
| [Roadmap](docs/roadmap.md) | Etapas de implementação e resultados esperados |

## Próximas etapas

1. Concluir a criação dos projetos Android e desktop e verificar seus builds.
2. Gerar e reproduzir áudio no desktop com a Java Sound API.
3. Transmitir blocos PCM entre dois programas Java por UDP.
4. Capturar áudio no Android e recebê-lo no desktop.
5. Integrar o microfone virtual no Linux.

Descoberta automática de dispositivos, USB como transporte de áudio, Bluetooth, filtros avançados e driver próprio para Windows ficam fora da primeira versão.