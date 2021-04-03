----------------------
Grupo 35
Martim Silva 51304
Francisco Freire 52177
David Rodrigues 53307
----------------------
Limitações do Trabalho da Fase 1

O unico argumento que deve ser passado obrigatoriamente para o servidor correr é 45678.

O serverAdress passado pelo cliente como argumento pode ser apenas endereço IP (por ex: localhost) ou endereço IP:45678 (por ex localhost 45678).

Username e passwords passados como argumento em SeiTchiz não devem conter espaços nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Nomes de grupos nao devem conter espaços nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Mensagens nao devem conter dois pontos(:) nem hifens(-).

UserIDs inseridos nos comandos que recebem userIDs nao devem conter espaços nem dois pontos(:) nem hifens(-) nem forward slashes(/).

Um user que ja esteja logged on nao deve fazer login enquanto a sessao inicial nao tenha sido terminada.

groupIDs inseridos nos comandos que recebem userIDs nao devem conter espaços nem dois pontos(:) nem hifens(-) nem forward slashes(/).

As fotos de stock que se podem partilhar sao apenas as que se encontram no ficheiro Fotos na root do projeto.
Ou seja o argumento <photo> de post deve ser foto<1 ou 2 ou 3 ou 4>.jpg

A pasta bin nao deve ser apagada.

A pasta files pode ser apagada para dar um restart do servidor e todos os seus conteudos tal como a pasta wall

Como não foi dito no enunciado nao foi implementado o impedimento de um utilizador dar multiplos likes a mesma fotografia nem um utilizador poder dar like a sua propria fotografia.

Para interromper o funcionamento de um cliente usar a opcao s ou stop

----------------------
Como compilar e executar o Trabalho da Fase 1

PARA COMPILAR SERVIDOR COM POLICIES
javac -d bin srcserverSeiTchizServer.java srcserverCom.java

PARA COMPILAR CLIENTE COM POLICIES
javac -d bin srcclientSeiTchiz.java srcclientClientStub.java srcclientCom.java

PARA CORRER SERVIDOR COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy server.SeiTchizServer 45678

PARA CORRER CLIENTE COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy client.SeiTchiz IP45678 userID userPassword
tal como diz no enunciado o porto e a password podem ser omitidas aqui

PARA CORRER SERVIDOR COM POLICIES POR JAR
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy -jar SeiTchizServer.jar 45678

PARA CORRER CLIENTE COM POLICIES POR JAR
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy -jar SeiTchiz.jar IP45678 userID userPassword
tal como diz no enunciado o porto e a password podem ser omitidas aqui

----------------------