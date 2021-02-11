Trabalho todo


Dúvidas: Argumentos Cliente????
         Argumentos Server????


Autenticação-

SeiTchizServer 45678 - Port para aceitar clients.

SeiTchiz <serverAddress> <clientID> [password] Faz o registo caso não exista ainda a conta e dá add no ficheiro, ou entao dá login.

Methods:


follow <userID> –

unfollow <userID> -

viewfollowers – 

post <photo> –

wall <nPhotos> - 

like <photoID> –

newgroup <groupID> –

addu <userID> <groupID> –

removeu <userID> <groupID> –

ginfo [groupID] –

msg <groupID> <msg> –

collect <groupID> –

history <groupID> –


Funcionamento:
 
Utilizamos uma sandbox tanto para o servidor como para o cliente (Para limitarmos o acesso ao sistema de ficheiros etc).

 Vamos ter um ficheiro a funcionar como base de dados com os userid's, os seus nomes e as respetivas pw's. (<user>:<nome user>:<password>).

 As mensagens e as fotografias também têm de ser guardadas em disco no servidor.