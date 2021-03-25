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

----------------------
Grupo 35
Martim Silva 51304
Francisco Freire 52177
David Rodrigues 53307
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

----------------------
Limitacoes do Trabalho da Fase 2

Nao e tratado o caso de um user que ja esta ligado ao sistema e estar autenticado poder fazer login mais vezes por outros terminais. 

ADICIONAR MAIS LIMITACOES

----------------------
Como compilar e executar o Trabalho da Fase 2

---PARA COMPILAR SERVIDOR---
javac -d bin src/server/SeiTchizServer.java src/communication/ComServer.java src/security/Security.java

---PARA COMPILAR CLIENTE---
javac -d bin src/client/SeiTchiz.java src/client/ClientStub.java src/communication/ComClient.java src/security/Security.java

---PARA CORRER SERVIDOR COM POLICIES---
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy server.SeiTchizServer 45678 server passserver
---PARA CORRER SERVIDOR SEM POLICIES---
java -cp bin server/SeiTchizServer 45678 server passserver

---PARA CORRER CLIENTE COM POLICIES---
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz localhost truststore/ts_client keystores/KS1 passclient1 passclient1
---PARA CORRER CLIENTE SEM POLICIES---
java -cp bin client.SeiTchiz localhost truststore/ts_client keystores/KS1 passclient1 passclient1

---PARA CORRER SERVIDOR COM POLICIES POR JAR---
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy -jar SeiTchizServer.jar 45678 server passserver

---PARA CORRER CLIENTE COM POLICIES POR JAR---
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy -jar SeiTchiz.jar localhost truststore/ts_client keystores/KS1 passclient1 passclient1

----------------------

---Chaves assimétricas---

keytool -genkeypair -alias server -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/server 
*Password: passserver
keytool -genkeypair -alias 1KS -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/1KS
*Password: passclient1
keytool -genkeypair -alias 2KS -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/2KS
*Password: passclient2
keytool -genkeypair -alias 3KS -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/3KS
*Password: passclient3
keytool -genkeypair -alias 4KS -keyalg RSA -keysize 2048 -storetype JKS -keystore keystores/4KS
*Password: passclient4


---Chave simétrica do servidor--- 
keytool -genseckey -alias serverSecKey -storetype JCEKS -keystore keystores/serverSecKey


---Como verificar keystores---
keytool -list -keystore keystores/1KS
keytool -list -keystore keystores/2KS
keytool -list -keystore keystores/3KS
keytool -list -keystore keystores/4KS
keytool -list -keystore keystores/server
keytool -list -keystore truststore/ts_client


---Como fazer a parte da truststore---
    ---Verificar o keystore---
    keytool -list -keystore keystores/server

    ---Fazer export do certificado---
    keytool -exportcert -alias server -file keystores/certServer.cer -keystore keystores/server

    ---Fazer import do certificado--- 
    keytool -importcert -alias server -file keystores/certServer.cer -keystore truststore/ts_client

    ---Verificar se o certificado tem o mesmo sha1 e é uma trustedCertEntry--- 
    keytool -list -keystore truststore/ts_client

----------------------

---TODO---

1. Cada grupo mantido pelo servidor usarÃ¡ uma chave de grupo simÃ©trica AES para cifrar e decifrar mensagens trocadas nesse grupo. A cifra serÃ¡ fim-a-fim, i.e., o servidor nÃ£o terÃ¡ acesso ao conteÃºdo das mensagens trocadas, significando que ambas as operaÃ§Ãµes de cifrar e decifrar sÃ£o efetuadas pelo cliente. Por exemplo, quando um utilizador envia uma mensagem para um grupo, esta serÃ¡ cifrada no cliente antes do seu envio. O servidor recebe a mensagem cifrada e armazena-a. Da mesma forma, quando um utilizador pede ao servidor as mensagens do grupo, este envia para a mÃ¡quina cliente as mensagens cifradas (tal como estÃ£o armazenadas) e serÃ¡ o cliente que as decifra e as mostra ao utilizador.
    1.1. ClientStub farÃ¡ a cifra do conteÃºdo das mensagens e enviarÃ¡ o array de bytes correspondente. O SeiTchizServer apenas guardarÃ¡ a mensagem. 

2. Finalmente, para maximizar ainda mais a confianÃ§a no ambiente de execuÃ§Ã£o, o servidor deve armazenar a lista de utilizadores, a lista de seguidores de cada utilizador, e a associaÃ§Ã£o entre utilizadores e grupos, em ficheiros cifrados. Isto garante que ninguÃ©m alÃ©m do servidor corretamente inicializado consegue ler esses ficheiros. 
    2.1. SeiTchizServer cifra os ficheiros:
            follower.txt
            following.txt
            owner.txt
            participant.txt

3. A informaÃ§Ã£o sobre likes e as prÃ³prias fotografias nÃ£o precisam de ser cifradas Contudo, o servidor deve ser capaz de verificar se a integridade das fotografias armazenadas nÃ£o foi comprometida, antes de as enviar aos clientes.
    3.1. Armazenar a hash das fotos em um ficheiro encriptado
    3.2. Sempre que enviar fotos ao cliente, farÃ¡: 
            3.2.1. DescodificaÃ§Ã£o da hash encriptada
            3.2.2. Hash da foto nÃ£o encriptada
            3.2.3. ComparaÃ§Ã£o entre as duas Hash


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

----------------------