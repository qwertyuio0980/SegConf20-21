trabalho todo


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
 Vamos ter um ficheiro a funcionar como base de dados com os userid's, os seus nomes e as respetivas pw's. (<user>:<nome user>:<password>).