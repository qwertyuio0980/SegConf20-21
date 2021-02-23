import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SeiTchiz {

    public static void main(String[] args) {
       
    	int arglen = args.length;

        // numero de argumentos errado
        if (arglen > 3 || arglen < 2) {
            System.out.println("Numero de argumentos dado errado. Usar SeiTchiz <IP:45678> <clientID> [password]"
                    + "\n ou SeiTchiz <IP> <clientID> [password] \n ou  SeiTchiz <IP:45678> <clientID> \n ou SeiTchiz <IP> <clientID>");
            System.exit(-1);
        }

        ClientStub cs = new ClientStub(args[0]);

        // efetuar login
        if(arglen == 3) {
            cs.login(args[1], args[2]);
        } else if(arglen == 2) {
            cs.login(args[1]);
        }
        
        System.out.println("---Sessao cliente iniciada---");
        
        
        boolean stop = false;

        //ciclo principal do cliente
        while(!stop) {
            
            //abstrair noutra classe??
            //mostrar menu com opcoes
            System.out.println("Que operacao pretende executar? \n" +
            "f ou follow <userID> \n" +
            "u ou unfollow <userID> \n" +
            "v ou viewfollowers \n" +
            "p ou post <photo> \n" +
            "w ou wall <nPhotos> \n" +
            "l ou like <userID> \n" +
            "n ou newgroup <groupID> \n" +
            "a ou addu <userID> <groupID> \n" +
            "r ou removeu <userID> <groupID> \n" +
            "g ou ginfo [groupID] \n" +
            "m ou msg <groupID> <msg> \n" +
            "c ou collect <groupID> \n" +
            "h ou history <groupID> \n" +
            "s ou stop");

            BufferedReader reader;
            String input;
            
            // pode dar erro porque isto nao foi inicializado mas se for depois como se verifica se o user meteu os argumentos bem ou nao
            String[] option = null; 
            
            int resultado;
            String followersList = null;
            
            try {
                reader = new BufferedReader(new InputStreamReader(System.in)); 
                input = reader.readLine();
                option = input.split(" ");
            } catch(IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
            
            switch(option[0]) {
                case "f": case "follow":
                
                    if(option.length != 2) {
                        System.out.println("------------------------------------------");
                        System.out.println("Opcao \"follow\" recebe argumento <userID> que nao pode ter espacos. Tente novamente");
                        System.out.println("------------------------------------------");
                        break;
                    }
                    
                    // envia-se o userID que se procura e o userID que fez o pedido
                    resultado = cs.follow(option[1], args[1]);
                    
                    if(resultado == 0) {
                        System.out.println("------------------------------------------");
                        System.out.println("Esta a seguir o user com userID: " + option[1]);
                        System.out.println("------------------------------------------");
                    } else {
                        System.out.println("------------------------------------------");
                        System.out.println("Ocorreu um erro a fazer a operacao... \n " +
                                "Razoes possiveis: -O userID inserido nao pertence nenhum user existente no sistema; \n " +
                                "-O user com o userID escolhido ja esta a ser seguido; \n " +
                                "-O userID que procurou nao deve ter \":\" no nome.");
                        System.out.println("------------------------------------------");
                    }                
                    break;
                    
                case "u": case "unfollow":

                    if(option.length != 2) {
                        System.out.println("------------------------------------------");
                        System.out.println("Opcao \"unfollow\" recebe argumento <userID> que nao pode ter espacos. Tente novamente");
                        System.out.println("------------------------------------------");
                        break;
                    }
                    
                    // envia-se o userID que se procura e o userID que fez o pedido
                    resultado = cs.unfollow(option[1], args[1]);
                    
                    if(resultado == 0) {
                        System.out.println("------------------------------------------");
                        System.out.println("Deixou de seguir o user com userID: " + option[1]);
                        System.out.println("------------------------------------------");
                    } else {
                        System.out.println("------------------------------------------");
                        System.out.println("Ocorreu um erro a fazer a operacao... \n " +
                                "Razoes possiveis: -O userID inserido nao pertence nenhum user existente no sistema; \n " +
                                "-O user com o userID escolhido nao esta a ser seguido; \n " +
                                "-O userID que procurou nao deve ter \":\" no nome.");
                        System.out.println("------------------------------------------");
                    }
                    
                    break;
                case "v": case "viewfollowers":
                    
                    if(option.length != 1) {
                        System.out.println("------------------------------------------");
                        System.out.println("Opcao \"viewfollowers\" nao recebe argumentos adicionais. Tente novamente");
                        System.out.println("------------------------------------------");
                        break;
                    }
                    
                    // envia-se o senderID que quer saber quais os seus seguidores
                    followersList = cs.viewfollowers(args[1]);
                    
                    if(followersList.isEmpty()) {
                        System.out.println("------------------------------------------");
                        System.out.println("O cliente nao tem followers");
                        System.out.println("------------------------------------------");
                    } else {
                        System.out.println("------------------------------------------");
                        System.out.println("Os seus followers sao: \n" + followersList);
                        System.out.println("------------------------------------------");
                    }
                    break;
                    
                case "p": case "post":

                    //TODO
                    
                    break;
                case "w": case "wall":

                    //TODO
                    
                    break;
                case "l": case "like":

                    //TODO
                    
                    break;
                case "n": case "newgroup":
                    
                    if(option.length != 2 || option[1].contains("/") || option[1].contains(":")) {
                        System.out.println("------------------------------------------");
                        System.out.println("Opcao \"newgroup\" recebe argumento <groupID> que nao pode ter espacos" +
                    " ou forward slashes(/) ou dois pontos(:). Tente novamente");
                        System.out.println("------------------------------------------");
                        break;
                    }
                    
                    // envia-se o userID que se procura e o userID que fez o pedido
                    resultado = cs.newgroup(option[1], args[1]); 
                    
                    if(resultado == 0) {
                        System.out.println("------------------------------------------");
                        System.out.println("O cliente e agora dono do novo grupo com groupID: " + option[1]);
                        System.out.println("------------------------------------------");
                    } else {
                        System.out.println("------------------------------------------");
                        System.out.println("Nao foi possivel criar o grupo pois o grupo com o groupID que designou ja existe");
                        System.out.println("------------------------------------------");
                    }
                    break;
                    
                case "a": case "addu":

                    if(option.length != 3 || option[1].contains(":") || option[2].contains("/") || option[2].contains(":")) {
                        System.out.println("------------------------------------------");
                        System.out.println("Opcao \"addu\" recebe os argumentos <userID> que nao pode ter espacos ou " +
                                "dois pontos (:) e <groupID> que nao pode ter espacos ou forward slashes(/) ou " +
                                "dois pontos(:). Tente novamente");
                        System.out.println("------------------------------------------");
                        break;
                    }
                    
                    // envia-se o userID que se pretende adicionar ao grupo, o grupoID e o senderID que fez o pedido
                    resultado = cs.addu(option[1], option[2], args[1]);
                    
                    if(resultado == 0) {
                        System.out.println("------------------------------------------");
                        System.out.println("O utilizador selecionado e agora membro do seu grupo indicado");
                        System.out.println("------------------------------------------");
                    }  else {
                        System.out.println("------------------------------------------");
                        System.out.println("Ocorreu um erro a fazer a operacao... \n " +
                                "Razoes possiveis: -O userID inserido ja pertence ao grupo; \n " +
                                "-O userID indicado nao corresponde a nenhum utilizador desta aplicacao; \n " +
                                "-O grupo indicado nao existe; \n " +
                                "-Apenas o dono do grupo indicado pode adicionar membros ao mesmo.");
                        System.out.println("------------------------------------------");
                    }
                    
                    break;
                    
                case "r": case "removeu":

                    //TODO
                    
                    break;
                case "g": case "ginfo":
                    
                    //TODO
                    
                    break;
                case "m": case "msg":

                    //TODO
                    
                    break;
                case "c": case "collect":

                    //TODO
                    
                    break;
                case "h": case "history":

                    //TODO
                    
                    break;
                case "s": case "stop":
                    System.out.println("Selecionou a opcao \"stop\" que termina a aplicacao");
                    cs.stop();                  
                    stop = true;
                    break;
                default:
                    System.out.println("Input recebido invalido");
                    break;
            }
        }
        
        System.out.println("---Sessao cliente terminada---");
    }
}
