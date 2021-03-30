# SegConf20-21
Página da cadeira de Segurança e Confiabilidade 20/21 

Martim Silva 51304
Francisco Freire 52177
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

----------------------
Limitacoes do Trabalho da Fase 1

O unico argumento que deve ser passado obrigatoriamente para o servidor correr e 45678.

O serverAdress passado pelo cliente como argumento pode ser apenas <endereco IP> (por ex:localhost) ou <endereco IP>:<porto 45678> (por ex: localhost:45678).

Username e passwords passados como argumento em SeiTchiz nao devem conter espacos nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Nomes de grupos nao devem conter espacos nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Mensagens nao devem conter dois pontos(:) nem hifens(-).

UserIDs inseridos nos comandos que recebem userIDs nao devem conter espacos nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Um user que ja esteja logged on nao deve fazer login enquanto a sessao inicial nao tenha sido terminada.

groupIDs inseridos nos comandos que recebem userIDs nao devem conter espacos nem dois pontos(:) nem hifens(-) nem forward slashes(/).

As fotos de stock que se podem partilhar sao apenas as que se encontram no ficheiro Fotos na root do projeto.
Ou seja O argumento <photo> de post deve ser foto<1 a 4>.jpg

A pasta bin nao deve ser apagada.

A pasta files pode ser apagada para dar um "restart" do servidor e todos os seus conteudos

Como nao foi dito no enunciado nao foi implementado o impedimento de um utilizador dar multiplos likes a mesma fotografia nem um utilizador poder dar like a sua propria fotografia.

Para interromper o funcionamento de um cliente usar a opcao s ou stop

Limitacoes do Trabalho da Fase 2

Nao e tratado o caso de um user que ja esta ligado ao sistema e estar autenticado poder fazer login mais vezes por outros terminais. 

ADICIONAR MAIS LIMITACOES


----------------------
Como compilar e executar o Trabalho da Fase 2

PARA COMPILAR SERVIDOR
javac -d bin src/server/SeiTchizServer.java src/communication/Com.java src/security/Security.java

PARA COMPILAR CLIENTE
javac -d bin src/client/SeiTchiz.java src/client/ClientStub.java src/communication/Com.java src/security/Security.java

PARA CORRER SERVIDOR COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy server.SeiTchizServer 45678 <keystore> <keystore-password>
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy server.SeiTchizServer 45678 serverKeyStore passserver

PARA CORRER SERVIDOR SEM POLICIES
java -cp bin server.SeiTchizServer 45678 serverKeyStore passserver

PARA CORRER CLIENTE COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz <serverAddress> <truststore> <keystore> <keystore-password> <clientID>
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost ts_client 1KS passclient1 client1
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost ts_client 2KS passclient2 client2
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost ts_client 3KS passclient3 client3
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost ts_client 4KS passclient4 client4

PARA CORRER CLIENTE SEM POLICIES
java -cp bin client.SeiTchiz localhost serverKeyStore 1KS passclient1 client1

PARA CORRER SERVIDOR COM POLICIES POR JAR
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy -jar SeiTchizServer.jar 45678 <keystore> <keystore-password>

PARA CORRER CLIENTE COM POLICIES POR JAR
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy -jar SeiTchiz.jar <serverAddress> <truststore> <keystore> <keystore-password> <clientID>

----------------------

**Criar Chaves & Keystores**

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


**Verificar chaves**
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

**Criar chaves em uma certa keystore**

//Chave simétrica para o servidor:
keytool -genseckey -alias serverKey -storetype JCEKS -keystore ServerKeyStore

//Chave assimétrica para cada cliente:
keytool -genkeypair -alias <clientID + 'KS'> -keystore <clientID + 'KS'>



//CIFRAR COISAS:
//WRAP E PARA CHAVES
//ENCRYPT E PARA CONTEUDOS