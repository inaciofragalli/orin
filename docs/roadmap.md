# Roadmap do Orin

**Status:** planejamento inicial  
**Atualizado em:** 12/10/2026

## Como vamos trabalhar

O Orin será desenvolvido por etapas, com o código escrito principalmente pelo próprio desenvolvedor e apoio pontual para aprender as APIs de áudio, redes e integração com os sistemas operacionais.

Cada fase termina com alguma coisa que pode ser executada ou testada. A intenção é aproveitar o código nas etapas seguintes. Geradores de áudio, arquivos WAV e pequenos programas de diagnóstico podem continuar no projeto como ferramentas de teste.

Não há datas fechadas. O Rivet continua sendo o projeto principal, e o ritmo do Orin dependerá do tempo disponível e dos resultados nos dispositivos reais.

## Fase 0: preparar o repositório

- [ ] Criar o repositório `orin` com um único `.git` na raiz.
- [ ] Criar `android/` no Android Studio com Kotlin e Jetpack Compose.
- [ ] Criar `desktop/` no IntelliJ IDEA com Java e Gradle Kotlin DSL.
- [ ] Configurar `.gitignore`, `.gitattributes` e `.editorconfig`.
- [ ] Conferir o Gradle Wrapper e o build de cada aplicação.
- [ ] Preparar o Galaxy S25 Ultra para depuração por USB.

**Pronto quando:** os dois aplicativos iniciais compilarem de forma independente e o Android abrir no celular físico.

## Fase 1: primeiro áudio em Java

- [ ] Entender amostras PCM, taxa de amostragem e representação em bytes.
- [ ] Gerar uma amostra de som ou ler um WAV de teste.
- [ ] Reproduzir o áudio pela Java Sound API.
- [ ] Experimentar buffers e seleção de dispositivo de saída.
- [ ] Adicionar testes para conversões simples de bytes e amostras, quando surgirem.

**Pronto quando:** o desktop reproduzir áudio com duração, velocidade e volume coerentes. O código de reprodução deve poder ser chamado sem depender da interface gráfica.

## Fase 2: primeiro transporte UDP

- [ ] Criar um emissor de blocos PCM em Java.
- [ ] Criar o receptor UDP em Java, inicialmente na própria máquina.
- [ ] Reproduzir os blocos recebidos usando a saída da fase 1.
- [ ] Testar perda de pacotes e encerramento da conexão.
- [ ] Documentar o que foi observado em `protocol.md`.

**Pronto quando:** o áudio enviado pelo emissor Java chegar ao receptor Java sem corrupção perceptível. Ainda não é necessário ter uma interface gráfica.

## Fase 3: capturar e transmitir no Android

- [ ] Pedir permissão de microfone.
- [ ] Capturar áudio com `AudioRecord`.
- [ ] Enviar blocos PCM por UDP para o IP do desktop.
- [ ] Separar captura, envio e interface Compose.
- [ ] Testar início, parada e liberação do microfone.
- [ ] Verificar estabilidade com o aplicativo Android aberto.

**Pronto quando:** falar no celular produzir áudio nos alto-falantes do computador via Wi-Fi.

## Fase 4: transformar o áudio recebido em microfone no Linux

- [ ] Inspecionar os dispositivos de áudio com ferramentas do PipeWire.
- [ ] Experimentar uma saída virtual e sua fonte de gravação.
- [ ] Encaminhar os bytes recebidos para o dispositivo escolhido.
- [ ] Selecionar essa fonte como microfone no Discord ou em outro aplicativo.
- [ ] Documentar como criar e remover os dispositivos de teste.

**Pronto quando:** a voz capturada no Android puder ser usada como microfone no Linux. Este será o primeiro protótipo funcional do produto, ainda sem compromisso de segurança e estabilidade para uso diário.

## Fase 5: tornar o Linux utilizável no dia a dia

- [ ] Tratar pacotes perdidos, fora de ordem e atrasados.
- [ ] Medir latência e ajustar os buffers.
- [ ] Implementar pareamento, autenticação e transporte criptografado com tecnologia consolidada.
- [ ] Tratar desconexão, reconexão e encerramento limpo.
- [ ] Permitir captura em segundo plano no Android, respeitando as restrições do sistema.
- [ ] Criar a interface desktop simples após analisar a referência do WO Mic.
- [ ] Completar a interface Compose com status e controles básicos.
- [ ] Testar sessões longas, suspensão de tela e mudanças de rede.

**Pronto quando:** o Orin substituir o AudioRelay no Linux com estabilidade aceitável, sem deixar o áudio desprotegido na rede local.

## Fase 6: adicionar suporte ao Windows

- [ ] Testar a detecção de dispositivos de áudio virtuais pela aplicação Java.
- [ ] Encaminhar o áudio para um dispositivo como o VB-CABLE.
- [ ] Isolar diferenças de Linux e Windows sem duplicar o receptor UDP.
- [ ] Testar conexão, interrupções e seleção de microfone em aplicativos reais.
- [ ] Registrar as dependências externas e suas condições de distribuição.

**Pronto quando:** o mesmo APK Android funcionar com as versões desktop para Linux e Windows. Se ainda for preciso instalar um driver virtual de terceiros, isso deve estar explícito nas instruções.

## Fase 7: empacotar e revisar

- [ ] Criar um pacote de instalação para cada sistema desktop.
- [ ] Gerar um APK instalável sem depender do Android Studio.
- [ ] Configurar builds e testes automáticos no GitHub Actions.
- [ ] Escrever instruções de instalação, uso e solução de problemas.
- [ ] Revisar permissões, dados persistidos e logs.
- [ ] Validar instalação e funcionamento fora das IDEs.

**Pronto quando:** for possível instalar o Orin nos três dispositivos/sistemas de destino e usá-lo sem ferramentas de desenvolvimento. Este é o critério planejado para a versão 1.0 pessoal.

## O que não entra na primeira versão

Descoberta automática por mDNS, Bluetooth, áudio por USB, Opus, filtros avançados, gravação de chamadas, múltiplos transmissores e driver virtual próprio para Windows. Esses itens só entram se surgirem necessidades concretas ou se forem úteis para uma versão pública.

## Riscos a acompanhar

| Risco | Como vamos abordar |
| --- | --- |
| Latência maior do que o esperado | Medir captura, transporte, buffer e saída separadamente |
| Cortes no Wi-Fi | Analisar perdas, jitter e tamanho do buffer |
| Mudanças de permissão no Android | Testar no celular físico, com tela ligada e apagada |
| Microfone virtual incompatível | Fazer uma prova de conceito com PipeWire antes de investir na interface |
| Integração do Windows mais difícil | Começar com dispositivo virtual existente e validar Java Sound cedo |
| Escopo crescendo demais | Priorizar a substituição dos dois aplicativos atuais |

## Marcos do produto

| Marco | Resultado |
| --- | --- |
| Prova de conceito | Áudio do Android chega ao Java por Wi-Fi |
| Protótipo Linux | Discord reconhece a voz como microfone |
| MVP Linux | Uso contínuo com conexão protegida e interface mínima |
| Versão 1.0 pessoal | Instalação e uso no Linux e Windows, com o mesmo aplicativo Android |

A escolha de abrir o código sob licença MIT será retomada se houver decisão de publicar o projeto. Não é uma dependência para começar o desenvolvimento.
