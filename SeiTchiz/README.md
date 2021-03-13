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

----------------------
Como compilar e executar o Trabalho da Fase 1

PARA COMPILAR SERVIDOR COM POLICIES
javac -d bin src/server/SeiTchizServer.java src/server/Com.java

PARA COMPILAR CLIENTE COM POLICIES
javac -d bin src/client/SeiTchiz.java src/client/ClientStub.java src/client/Com.java

PARA CORRER SERVIDOR COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy server.SeiTchizServer 45678

PARA CORRER CLIENTE COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz <IP>:<45678> <userID> <userPassword>
tal como diz no enunciado o porto e a password podem ser omitidas aqui

PARA CORRER SERVIDOR COM POLICIES POR JAR
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy -jar SeiTchizServer.jar 45678

PARA CORRER CLIENTE COM POLICIES POR JAR
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy -jar SeiTchiz.jar <IP>:<45678> <userID> <userPassword>
tal como diz no enunciado o porto e a password podem ser omitidas aqui

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
