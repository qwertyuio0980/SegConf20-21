# SegConf20-21
Página da cadeira de Segurança e Confiabilidade 20/21 

Martim Silva 
Francisco Freire
David Rodrigues

----------------------
Limitações do Trabalho da Fase 1

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

FALTA LIMITACOES DO WALL

----------------------
Como compilar e executar o Trabalho da Fase 1

FAZER OS PASSOS NESTA ORDEM:
PARA COMPILAR SERVIDOR
javac -d bin src/SeiTchizServer.java src/Com.java

PARA COMPILAR CLIENTE
javac -d bin src/SeiTchiz.java src/ClientStub.java src/Com.java

PARA CORRER SERVIDOR COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==server.policy SeiTchizServer 45678

PARA CORRER CLIENTE COM POLICIES
java -cp bin -Djava.security.manager -Djava.security.policy==client.policy SeiTchiz <IP>:<45678> <userID> <userPassword>

PARA CORRER CLIENTE COM JARS


PARA CORRER CLIENTE COM JARS
