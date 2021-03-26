# SegConf20-21
Página da cadeira de Segurança e Confiabilidade 20/21 

Martim Silva 
Francisco Freire
David Rodrigues


----------------------

PARA FAZER OS JARS:
1.Compilar ficheiros ATUAIS para a pasta bin
2.right click no package do server ou cliente
3.export -> java -> jarfile
4.verificar se o package correto e o que esta selecionado
5.selecionar a opcao export java source files and resources(3a opcao)
6.selecionar o path na root do projeto e o nome do jar é SeiTchizServer para servidor e SeiTchiz para o cliente
7.Fazer Next e nao fazer nada na pagina que aparece
8.Fazer Next denovo e em "Select the class of the application entry point" fazer browse
e meter a classe que tem o main

PARA CORRER SERVIDOR COM POLICIES E JAR
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy -jar SeiTchizServer.jar 45678

PARA CORRER CLIENTE COM POLICIES E JAR
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy -jar SeiTchiz.jar <IP>:<45678> <userID> <userPassword>


----------------------
Grupo 35
Martim Silva 51304
Francisco Freire 52177
David Rodrigues 53307
----------------------
Limita��es do Trabalho da Fase 1

O unico argumento que deve ser passado obrigatoriamente para o servidor correr � 45678.

O serverAdress passado pelo cliente como argumento pode ser apenas <endere�o IP> (por ex:localhost) ou <endere�o IP>:<porto 45678> (por ex: localhost:45678).

Username e passwords passados como argumento em SeiTchiz n�o devem conter espa�os nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Nomes de grupos nao devem conter espa�os nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Mensagens nao devem conter dois pontos(:) nem hifens(-).

UserIDs inseridos nos comandos que recebem userIDs nao devem conter espa�os nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Um user que ja esteja logged on nao deve fazer login enquanto a sessao inicial nao tenha sido terminada.

groupIDs inseridos nos comandos que recebem userIDs nao devem conter espa�os nem dois pontos(:) nem hifens(-) nem forward slashes(/).

As fotos de stock que se podem partilhar sao apenas as que se encontram no ficheiro Fotos na root do projeto.
Ou seja O argumento <photo> de post deve ser foto<1 a 4>.jpg

A pasta bin nao deve ser apagada.

A pasta files pode ser apagada para dar um "restart" do servidor e todos os seus conteudos

Como n�o foi dito no enunciado nao foi implementado o impedimento de um utilizador dar multiplos likes a mesma fotografia nem um utilizador poder dar like a sua propria fotografia.

Para interromper o funcionamento de um cliente usar a opcao s ou stop

Limita��es do Trabalho da Fase 2

Nao e tratado o caso de um user que ja esta ligado ao sistema e estar autenticado poder fazer login mais vezes por outros terminais. 

ADICIONAR MAIS LIMITACOES


----------------------
Como compilar e executar o Trabalho da Fase 2

PARA COMPILAR SERVIDOR COM POLICIES
javac -d bin src/server/SeiTchizServer.java src/communication/Com.java src/security/Security.java

PARA COMPILAR CLIENTE COM POLICIES
javac -d bin src/client/SeiTchiz.java src/client/ClientStub.java src/communication/Com.java src/security/Security.java

PARA CORRER SERVIDOR COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy server.SeiTchizServer 45678 <keystore> <keystore-password>
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy server.SeiTchizServer 45678 serverKeyStore passserver

PARA CORRER CLIENTE COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz <serverAddress> <truststore> <keystore> <keystore-password> <clientID>
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost serverKeyStore 1KS passclient1 client1
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost serverKeyStore 2KS passclient2 client2
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost serverKeyStore 3KS passclient3 client3
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost serverKeyStore 4KS passclient4 client4


PARA CORRER SERVIDOR COM POLICIES POR JAR
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy -jar SeiTchizServer.jar 45678 <keystore> <keystore-password>

PARA CORRER CLIENTE COM POLICIES POR JAR
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy -jar SeiTchiz.jar <serverAddress> <truststore> <keystore> <keystore-password> <clientID>

----------------------

TODO:
1. Cada grupo mantido pelo servidor usará uma chave de grupo simétrica AES para cifrar e decifrar mensagens trocadas nesse grupo. A cifra será fim-a-fim, i.e., o servidor não terá acesso ao conteúdo das mensagens trocadas, significando que ambas as operações de cifrar e decifrar são efetuadas pelo cliente. Por exemplo, quando um utilizador envia uma mensagem para um grupo, esta será cifrada no cliente antes do seu envio. O servidor recebe a mensagem cifrada e armazena-a. Da mesma forma, quando um utilizador pede ao servidor as mensagens do grupo, este envia para a máquina cliente as mensagens cifradas (tal como estão armazenadas) e será o cliente que as decifra e as mostra ao utilizador.
    1.1. ClientStub fará a cifra do conteúdo das mensagens e enviará o array de bytes correspondente. O SeiTchizServer apenas guardará a mensagem. 

2. Finalmente, para maximizar ainda mais a confiança no ambiente de execução, o servidor deve armazenar a lista de utilizadores, a lista de seguidores de cada utilizador, e a associação entre utilizadores e grupos, em ficheiros cifrados. Isto garante que ninguém além do servidor corretamente inicializado consegue ler esses ficheiros. 
    2.1. SeiTchizServer cifra os ficheiros:
            follower.txt
            following.txt
            owner.txt
            participant.txt

3. A informação sobre likes e as próprias fotografias não precisam de ser cifradas Contudo, o servidor deve ser capaz de verificar se a integridade das fotografias armazenadas não foi comprometida, antes de as enviar aos clientes.
    3.1. Armazenar a hash das fotos em um ficheiro encriptado
    3.2. Sempre que enviar fotos ao cliente, fará: 
            3.2.1. Descodificação da hash encriptada
            3.2.2. Hash da foto não encriptada
            3.2.3. Comparação entre as duas Hash


Mudar argumentos recebidos pelo servidor e cliente

Estabelecimento de ligacao com TLS/SSL(truststore/keystore)

Autenticacao do cliente com nonce

Comandos:
	-post
	-newgroup
	-addu
	-removeu
	-msg
	-collect
	-history
	
NAO ESQUECER:
contexto do utilizador tem de estar fora do servidor
quando aceder ao servidor enquanto cliente ja tenho de ter a keystore numa pasta de keystores            

**DÚVIDAS**
1. Estrutura dos keystores
    1.1. Cada cliente terá sua própria keystore
    1.2. O servidor terá sua própria keystore
    1.3. A truststore será uma keystore com import do ficheiro .cert contendo 

2. Em que forma será passado o certificado passado pelo cliente quando o mesmo é corrido.

3. O alias de cada cliente pode ser igual ao nome do ficheiro keystore?Nao sabemos como buscar o alias dentro do java para fazer o getCertificate



**Chaves & Keystores**

comandos:

## Chaves assimétricas
keytool -genkeypair -alias <ALIASDACHAVE> -keyalg RSA -keysize 2048 -keystore <NOMEFICHEIROKEYSTORE>

keytool -genkeypair -alias serverKeyStore -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/serverKeyStore 
*Password: passserver
keytool -genkeypair -alias 1KS -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/1KS
*Password: passclient1
keytool -genkeypair -alias 2KS -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/2KS
*Password: passclient2
keytool -genkeypair -alias 3KS -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/3KS
*Password: passclient3
keytool -genkeypair -alias 4KS -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/4KS
*Password: passclient4

## Chave simétrica do servidor 
keytool -genseckey -alias serverKey -storetype JCEKS -keystore ServerKeyStore


**Verificar chaves:
keytool -list-keystore <clientID + 'KS'>

keytool -list -keystore keystores.serverKeyStore
keytool -list -keystore keystores.1KS
keytool -list -keystore keystores.2KS
keytool -list -keystore keystores.3KS
keytool -list -keystore keystores.4KS

------------------------------------------------------------------
(Server):      serverKeyStore   serverKeyStore       passserver
(Client1):          1KS             1KS              passclient1
(Client2):          2KS             2KS              passclient2
(Client3):          3KS             3KS              passclient3
(Client4):          4KS             4KS              passclient4
                <keystore>        <alias>            <password>
------------------------------------------------------------------

**Criar chaves em uma certa keystore:**

//Chave simétrica para o servidor:
keytool -genseckey -alias serverKey -storetype JCEKS -keystore ServerKeyStore

//Chave assimétrica para cada cliente:
keytool -genkeypair -alias <clientID + 'KS'> -keystore <clientID + 'KS'>
