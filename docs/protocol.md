# Protocolo de comunicação do Orin

**Status:** proposta de trabalho; codec experimental de pacotes implementado no desktop Java
**Atualizado em:** 13/10/2026

## Escopo inicial

Um celular Android transmite áudio para um desktop na mesma rede Wi-Fi. No primeiro teste, informaremos manualmente no Android o IP e a porta do receptor Java. A descoberta automática virá depois.

O envio de áudio começa por UDP. Vamos usar PCM sem compressão para facilitar a inspeção dos dados. O formato poderá mudar quando tivermos medições reais.

## Formato de áudio proposto

| Propriedade | Valor inicial |
| --- | --- |
| Codec | PCM linear, sem compressão |
| Canais | 1 (mono) |
| Taxa de amostragem | 48.000 amostras/segundo |
| Profundidade | 16 bits por amostra |
| Ordem dos bytes | Little-endian (`PCM_S16LE`) |
| Duração de um bloco | 10 ms |
| Bytes de áudio por bloco | 960 |
| Taxa de dados, sem cabeçalhos | 96.000 bytes/s (768 kbit/s) |

Cálculo para cada bloco:

```text
48.000 amostras/s × 0,010 s = 480 amostras
480 amostras × 2 bytes = 960 bytes
```

Escolhemos 10 ms como ponto de partida porque 20 ms de PCM nessas condições resultariam em 1.920 bytes de áudio por pacote. Em uma rede Ethernet comum com MTU de 1.500 bytes, isso pode provocar fragmentação IP. Com 960 bytes, sobra espaço para cabeçalhos de rede e metadados do protocolo. A MTU real da rede ainda deverá ser considerada.

Os dispositivos talvez não aceitem exatamente esse formato de captura ou reprodução. Se ocorrer, investigaremos a conversão ou renegociação do formato antes de fixar o contrato.

## Mensagens e responsabilidades

Há duas necessidades distintas:

1. **Áudio:** pacotes curtos e frequentes, enviados por UDP. Um pacote perdido não deve obrigar o destinatário a esperar por ele indefinidamente.
2. **Controle:** iniciar, encerrar, confirmar a configuração e informar erros. O transporte e o formato dessas mensagens ainda serão escolhidos.

A primeira prova de conceito pode enviar somente blocos PCM para um receptor conhecido. Antes de criar um jitter buffer de verdade, precisaremos incluir informações para identificar a sessão, ordenar os pacotes e reconhecer intervalos perdidos.

### Metadados candidatos para os pacotes de áudio

| Campo | Finalidade |
| --- | --- |
| Versão do protocolo | Distinguir formatos incompatíveis |
| Identificador da sessão | Ignorar áudio de conexões anteriores |
| Número de sequência | Detectar perda e mudança de ordem |
| Timestamp de áudio | Posicionar os blocos na linha do tempo |
| Tipo de conteúdo | Identificar PCM e, futuramente, outros codecs |
| Payload | Amostras de áudio do bloco |

### Formato experimental implementado no desktop

`AudioPacketCodec` codifica e decodifica um bloco PCM de exatamente 960 bytes. O pacote completo tem 976 bytes: cabeçalho de 16 bytes seguido do áudio.

| Offset em bytes | Tamanho | Campo | Valor ou representação |
| --- | --- | --- | --- |
| 0 | 4 | Assinatura | `0x4F52494E`, bytes ASCII `ORIN` |
| 4 | 1 | Versão | `1` |
| 5 | 1 | Tipo de pacote | `1`, áudio |
| 6 | 4 | Número de sequência | `int` Java de 32 bits |
| 10 | 4 | Timestamp | `int` Java de 32 bits |
| 14 | 2 | Comprimento do PCM | `960` |
| 16 | 960 | Payload | Bytes PCM copiados sem conversão |

Os campos de múltiplos bytes do cabeçalho usam **big-endian**. O áudio continua **little-endian**, conforme o formato PCM proposto. O codec não converte nem valida as amostras; preserva os bytes recebidos.

`encode` rejeita PCM nulo com `NullPointerException` e tamanhos diferentes de 960 com `IllegalArgumentException`. `decode` rejeita datagramas nulos com `NullPointerException`; tamanho diferente de 976, assinatura, versão, tipo ou comprimento de PCM inválidos resultam em `IllegalArgumentException`.

O codec apenas transporta os valores de sequência e timestamp fornecidos pelo chamador. Unidades, origem do timestamp, política de sequência e tratamento de overflow ainda não foram definidos. Não há identificador de sessão, ordenação, jitter buffer, autenticação ou criptografia neste formato experimental.

A implementação atual existe somente em Java, com testes de bytes do cabeçalho, preservação do payload, codificação, decodificação e rejeição de pacotes inválidos. A implementação Kotlin e os testes de compatibilidade entre os aplicativos ficam para a integração Android.

### Pipeline experimental UDP → codec → reprodução

`DesktopAudioPipeline.receiveAndPlayNext()` recebe um datagrama com `UdpAudioReceiver`, decodifica-o com `AudioPacketCodec` e escreve o payload em `PcmAudioOutput`. Cada chamada processa um pacote. O chamador é responsável por abrir e fechar os recursos e por chamar `drain()` quando precisar aguardar o término da reprodução.

O buffer do receptor tem **977 bytes**, um byte a mais que o pacote válido de 976 bytes. Isso permite receber o pacote completo e rejeitar excesso: um datagrama de 977 bytes é inválido pelo tamanho; um datagrama maior é truncado pelo socket para 977 bytes e também é rejeitado, mesmo quando começa com um pacote válido. O receptor não recupera o tamanho original de um datagrama truncado.

Pacotes inválidos não chegam à saída PCM. Erros de decodificação, timeout e falhas de reprodução são propagados ao chamador. Após tratar um pacote inválido, é possível processar uma nova chamada; isso não implica descarte ou recuperação automática nos programas de diagnóstico, que encerram ao encontrar um erro.

O fluxo foi validado manualmente em loopback no Linux, com um tom de 440 Hz durante aproximadamente três segundos, tanto em um processo Java como com emissor e receptor em processos independentes. Os testes automatizados usam sockets reais em loopback e saída PCM mockada; cobrem também truncamento com prefixo válido, timeout e uma chamada válida após rejeição de pacote inválido.

A recepção e a escrita de áudio são síncronas. Ainda não há controle de sessão, ordenação, jitter buffer, compensação de perda, autenticação ou criptografia. Captura Android, interoperabilidade Kotlin, validação em Wi-Fi e integração com microfone virtual permanecem pendentes. A avaliação de RTP ou de outro protocolo consolidado permanece pendente antes de estabilizar o formato.

## Recepção e temporização

O receptor deverá separar a chegada dos pacotes do momento de entregar amostras ao dispositivo de áudio. A rede pode atrasar, perder ou reordenar pacotes. Um *jitter buffer* pequeno permite absorver parte dessas variações, ao custo de alguma latência adicional.

As primeiras medições devem considerar:

- Tempo entre captura e chegada ao receptor, medido com cuidado para não comparar relógios não sincronizados.
- Pacotes recebidos, perdidos ou fora de ordem.
- Ocorrência de cortes e áudio atrasado.
- Tamanho efetivo das filas e buffers.

Em caso de perda, podemos começar preenchendo a lacuna com silêncio. Técnicas melhores de ocultação de perdas só serão estudadas se o resultado exigir.

## Sessão e segurança

O fluxo de conexão previsto é:

```text
Desktop inicia o receptor
        |
Android informa IP/porta e solicita conexão
        |
As duas aplicações validam parâmetros e autorização
        |
Android inicia a captura e envia áudio
        |
Uma das partes encerra a sessão
```

No experimento inicial, a validação da conexão poderá ser manual e simplificada, restrita a uma rede de testes controlada. Uma implementação pronta para uso diário precisará:

- Autenticar o dispositivo pareado.
- Impedir que qualquer computador da rede envie comandos ou áudio aceitos como válidos.
- Proteger conteúdo e integridade dos pacotes.
- Tratar expiração, reconexão e troca de sessão.

A escolha do mecanismo deverá considerar soluções existentes para áudio em tempo real, como DTLS-SRTP ou outro transporte seguro consolidado. O desenho exato fica pendente de um teste de viabilidade nas duas plataformas. **Não devemos lançar um transporte próprio sem revisão de segurança.**

## Compatibilidade

As versões dos aplicativos Android e desktop poderão diferir durante atualizações. Cada lado deverá rejeitar formatos não suportados com uma mensagem compreensível, sem tentar interpretar dados desconhecidos como áudio.

Mudanças incompatíveis no formato dos pacotes exigirão atualização da versão do protocolo. A política de compatibilidade será definida quando houver uma primeira versão funcional.

## Critérios para validar a primeira versão

- Um emissor Java consegue enviar blocos PCM para um receptor Java em rede local ou loopback.
- O receptor monta a sequência de áudio sem inverter bytes ou alterar a velocidade da reprodução.
- O Android transmite áudio real para o receptor Java.
- A recepção não interrompe toda a aplicação quando falta um pacote.
- Os testes documentam as condições usadas: rede, dispositivo, formato e duração.

A versão inicial deste documento descreve escolhas de engenharia, não uma especificação de rede definitiva.
