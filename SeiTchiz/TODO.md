TODO

------------------------------------------------------------------------------------------------------------------------------------------------------
NOTA
Sera que temos de manter +1 ficheiro de variaveis de servidor que guarda variaveis importantes?
ex: o "usernumber" ao fechar o server e reabrir o server o ficheiro de users mantem-se como esta e o proximo user a 
fazer signup ia acabar por ter usernumber = 1 porque o servidor nao manteu o valor da variavel da ultima vez que estava 
ligado o que gera problemas mais tarde com as operacoes do server
------------------------------------------------------------------------------------------------------------------------------------------------------

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