<div align="center">

# Orin

### Microfone sem fio via Wi-Fi para Linux e Windows, utilizando Android

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Java](https://img.shields.io/badge/Java_25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

![Repo size](https://img.shields.io/github/repo-size/inaciofragalli/orin?style=flat-square)
![Last commit](https://img.shields.io/github/last-commit/inaciofragalli/orin?style=flat-square)

[Visão geral](#visão-geral) •
[Estado atual](#estado-do-desenvolvimento) •
[Tecnologias](#tecnologias) •
[Desenvolvimento](#desenvolvimento-local) •
[Documentação](#documentação) •
[Roadmap](#próximas-etapas)

</div>

---

## Visão geral

O **Orin** é um projeto de aplicativo para transformar o microfone de um celular Android em uma entrada de áudio para computadores Linux e Windows, usando a rede Wi-Fi local.

A primeira entrega utilizável será voltada ao **Linux**. O suporte ao Windows virá depois, aproveitando o núcleo Java e adaptando a integração com os dispositivos de áudio do sistema.

> **Estado atual:** as bases de reprodução PCM, recepção UDP e codificação/decodificação de pacotes já foram implementadas e testadas no desktop Java. Esses componentes **ainda não formam um fluxo integrado**. O Orin ainda não captura áudio do Android, não cria um microfone virtual e não está pronto para substituir os aplicativos existentes.

## Estado do desenvolvimento

| Componente | Estado | O que existe |
| --- | --- | --- |
| Projetos e automações | Implementado | Monorepo com builds Gradle independentes para Android e desktop; CI e Dependabot |
| Saída PCM no desktop | Implementado e testado | `PcmAudioOutput`, `AudioLineFactory`, controle de abertura/escrita/fechamento e `drain()` |
| Reprodução real no Linux | Validada manualmente | Programa que gera e reproduz um tom de 440 Hz por um segundo |
| Receptor UDP | Implementado e testado | `UdpAudioReceiver`, porta configurável, timeout, liberação do socket e testes em loopback |
| Codec de pacotes | Implementado e testado | `AudioPacketCodec` e `AudioPacket`; cabeçalho experimental e testes de entradas inválidas |
| Integração UDP → codec → áudio | Pendente | Receber um datagrama completo, decodificá-lo e reproduzir o payload |
| Captura e transmissão Android | Pendente | `AudioRecord` e envio dos quadros PCM por Wi-Fi |
| Microfone virtual Linux | Pendente | Encaminhamento do áudio para dispositivo virtual via PipeWire |
| Microfone virtual Windows | Pendente | Integração com um dispositivo virtual existente, como VB-CABLE |
| Segurança da conexão | Pendente | Pareamento, autenticação e transporte criptografado |

Os testes de unidade e integração local cobrem os componentes de áudio, rede e protocolo. As quatro entregas iniciais do desktop foram integradas à `main`: PCM, receptor UDP, codec experimental e configuração do Gradle Configuration Cache.

**O que já foi demonstrado:** reprodução de PCM por uma saída real no Linux e troca de datagramas UDP entre sockets no próprio computador. **O que ainda não foi demonstrado:** áudio vindo do celular e chegando a um programa que reconheça o Orin como microfone.

## Tecnologias

| Área | Tecnologias e decisões |
| --- | --- |
| Android | Kotlin, Jetpack Compose e Material 3; captura futura com `AudioRecord` |
| Desktop | Java 25, Java Sound API; interface gráfica ainda não definida |
| Rede | UDP na rede local, inicialmente com IP e porta informados manualmente |
| Áudio | PCM mono, 48 kHz, 16 bits com sinal, little-endian |
| Linux | PipeWire para integração com microfone virtual (pendente) |
| Windows | Dispositivo de áudio virtual existente, inicialmente VB-CABLE ou equivalente (pendente) |
| Build | Gradle Kotlin DSL; projetos Android e desktop independentes |
| Testes | JUnit, Mockito e testes com sockets UDP reais em loopback |
| Automação | GitHub Actions para builds/testes independentes e Dependabot |

Não há necessidade planejada de servidor na nuvem, conta de usuário ou banco de dados. Código nativo em Rust ou C++ será considerado apenas se a integração com os sistemas operacionais exigir.

## Comunicação e áudio

O formato de áudio adotado nos primeiros experimentos é **PCM sem compressão**:

| Propriedade | Valor |
| --- | --- |
| Taxa de amostragem | 48.000 Hz |
| Canais | 1 (mono) |
| Profundidade | 16 bits com sinal |
| Ordem dos bytes PCM | Little-endian |
| Duração de um quadro | 10 ms |
| Payload por quadro | 960 bytes |

O codec experimental do desktop monta um datagrama de **976 bytes**, formado por um **cabeçalho de 16 bytes** em big-endian e **960 bytes de PCM** em little-endian. O cabeçalho inclui a assinatura `ORIN`, versão, tipo, número de sequência, timestamp e comprimento do payload.

```text
Android (planejado)                 Desktop (parcialmente implementado)
AudioRecord
     │
Quadros PCM
     │
Envio por UDP ───── Wi-Fi ──────> UdpAudioReceiver
                                        │
                                 AudioPacketCodec
                                        │
                                  PcmAudioOutput
                                        │
                              Saída de áudio / diagnóstico
                                        │
                             Microfone virtual (futuro)
```

**Atenção:** esse diagrama representa o fluxo pretendido, **não um pipeline já integrado**. O `UdpAudioReceiver` atual utiliza um buffer de até **960 bytes**, enquanto o `AudioPacketCodec` espera **976 bytes**. Antes de conectá-los, será necessário ajustar a recepção para suportar o datagrama completo e detectar pacotes grandes demais sem truncamento silencioso.

Ainda faltam controle de sessão, ordenação e temporização de pacotes, jitter buffer e proteção criptográfica. O cabeçalho é experimental e poderá mudar após os testes de interoperabilidade com Kotlin ou a avaliação de protocolos consolidados. Consulte [a documentação do protocolo](docs/protocol.md) para detalhes dos campos e limitações.

## Desenvolvimento local

### Pré-requisitos

- Git e os Gradle Wrappers incluídos no repositório.
- IntelliJ IDEA e JDK 25 para desenvolver o desktop.
- Android Studio e Android SDK para o aplicativo Android.
- Um dispositivo Android com microfone para os testes futuros de captura.
- Um dispositivo de saída de áudio disponível para o teste manual de reprodução.

O repositório contém **dois projetos Gradle separados** e um único histórico Git. Os comandos de build devem ser executados dentro da pasta correspondente.

### Desktop

Abra `desktop/` no IntelliJ IDEA como projeto Gradle. Para compilar e executar os testes:

```bash
cd desktop
./gradlew build
```

Para executar somente os testes do componente PCM:

```bash
./gradlew test --tests dev.orin.desktop.audio.PcmAudioOutputTest
```

O programa de diagnóstico `dev.orin.desktop.audio.AudioOutputManualCheck` pode ser executado pela ação **Run** no IntelliJ. Ele gera um tom senoidal de **440 Hz** com duração de um segundo e amplitude reduzida. Comece com o volume do sistema baixo. O programa usa `drain()` para aguardar a reprodução antes de fechar a saída.

A aplicação desktop ainda não possui um fluxo completo de conexão ou uma interface final para utilização como microfone.

### Android

Abra `android/` no Android Studio, instale o SDK correspondente e execute no dispositivo desejado. Para compilar o APK de depuração e executar os testes unitários disponíveis:

```bash
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

O projeto Android ainda não implementa a captura nem a transmissão do microfone. Uma conexão USB pode ser usada para instalação e depuração pelo Android Studio; o transporte de áudio pretendido continua sendo o Wi-Fi.

### Integração contínua

O workflow em `.github/workflows/ci.yml` executa separadamente o build do desktop com **JDK 25** e a compilação/testes do Android com **JDK 17** e Android SDK. As dependências são acompanhadas pelo Dependabot.

O desktop tem **Gradle Configuration Cache** habilitado, atualmente configurado para reportar eventuais problemas de compatibilidade como avisos.

## Estrutura do repositório

```text
orin/
├── android/                 # Aplicativo Android em Kotlin e Compose
├── desktop/                 # Núcleo Java, áudio, rede e protocolo
├── docs/
│   ├── architecture.md      # Visão arquitetural e integração com os sistemas
│   ├── protocol.md          # Formato experimental dos pacotes e comunicação
│   └── roadmap.md           # Fases e critérios de conclusão
├── .github/
│   ├── workflows/ci.yml     # Build e testes dos dois projetos
│   └── dependabot.yml       # Atualização de dependências
├── .editorconfig
├── .gitattributes
├── .gitignore
└── README.md
```

## Objetivo do MVP

A primeira versão realmente utilizável do Orin deverá permitir usar a voz capturada no Android como **microfone no Linux**, em aplicações como Discord e navegador, com latência adequada para conversas.

Para isso, ainda será necessário integrar os componentes desktop, capturar e transmitir áudio no Android, criar a entrada virtual via PipeWire e implementar pareamento, autenticação e criptografia. Também serão necessários controles de início/parada, tratamento de desconexões e uma interface mínima.

O Windows será uma expansão posterior, aproveitando a mesma aplicação Android e a maior parte do receptor Java. A versão 1.0 pessoal tem como objetivo oferecer instalação e uso nos dois sistemas desktop.

## Privacidade e limitações

O Orin foi projetado para operar na rede local, sem contas, serviços de nuvem ou telemetria. O áudio não deverá ser gravado durante o funcionamento normal; quaisquer arquivos ou tons de diagnóstico serão utilizados somente em testes explícitos.

**Uma rede Wi-Fi local não torna o tráfego privado.** Os testes experimentais sem criptografia devem ocorrer apenas em ambientes controlados. O Orin **não está pronto para uso cotidiano com dados sensíveis** enquanto não houver autenticação, integridade e criptografia do transporte.

O Orin não oferece hoje conexão automática entre dispositivos, microfone virtual ou gerenciamento completo de sessões. No Windows, poderá ser necessária a instalação independente de um dispositivo virtual de terceiros; não há driver próprio planejado para o MVP.

## Documentação

| Documento | Conteúdo |
| --- | --- |
| [Arquitetura](docs/architecture.md) | Organização do sistema, responsabilidades e integração com Linux/Windows |
| [Protocolo](docs/protocol.md) | PCM, cabeçalho experimental, validações, temporização e segurança |
| [Roadmap](docs/roadmap.md) | Etapas previstas e critérios para um protótipo e MVP úteis |

## Próximas etapas

1. Integrar o receptor UDP ao codec e à saída PCM, corrigindo o tamanho do buffer e validando o fluxo ponta a ponta no desktop.
2. Implementar captura com `AudioRecord` e envio de quadros PCM no Android.
3. Ouvir no Linux o áudio capturado pelo celular por Wi-Fi.
4. Integrar PipeWire para disponibilizar o áudio como microfone virtual.
5. Medir e melhorar latência, estabilidade, segurança e experiência de conexão antes de considerar um MVP utilizável.

Descoberta automática, áudio via USB, Bluetooth, compressão com Opus, filtros avançados, vários transmissores e driver virtual próprio ficam fora do escopo inicial.
