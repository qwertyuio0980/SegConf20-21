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
Limitações do Trabalho da Fase 1(Entregar esta parte)

O unico argumento que deve ser passado obrigatoriamente para o servidor correr é 45678.

O serverAdress passado pelo cliente como argumento pode ser apenas <endereço IP> (por ex:localhost) ou <endereço IP>:<porto 45678> (por ex: localhost:45678).

Username e passwords passados como argumento em SeiTchiz não devem conter espaços nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Nomes de grupos nao devem conter espaços nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Mensagens nao devem conter dois pontos(:) nem hifens(-).

UserIDs inseridos nos comandos que recebem userIDs nao devem conter espaços nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Um user que ja esteja logged on nao deve fazer login enquanto a sessao inicial nao tenha sido terminada.

groupIDs inseridos nos comandos que recebem userIDs nao devem conter espaços nem dois pontos(:) nem hifens(-) nem forward slashes(/).

As fotos que se podem partilhar sao apenas as que se encontram no ficheiro Fotos na root do projeto.
Ou seja O argumento <photo> de post deve ser foto<1 a 4>.jpg

----------------------
Como compilar e executar o Trabalho da Fase 1(Entregar esta parte)

FAZER OS PASSOS NESTA ORDEM:
PARA COMPILAR SERVIDOR
javac -d bin src/server/SeiTchizServer.java src/server/Com.java

PARA COMPILAR CLIENTE
javac -d bin src/client/SeiTchiz.java src/client/ClientStub.java src/client/Com.java

PARA CORRER SERVIDOR COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy server.SeiTchizServer 45678

PARA CORRER CLIENTE COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz <IP>:<45678> <userID> <userPassword>