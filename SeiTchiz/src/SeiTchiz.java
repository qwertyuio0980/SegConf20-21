import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SeiTchiz {

    public static void main(String[] args) {
       
    	int arglen = args.length;
        String separador = "------------------------------------------";

        // numero de argumentos errado
        if (arglen > 3 || arglen < 2) {
            System.out.println("Numero de argumentos dado errado. Usar SeiTchiz <IP:45678> <clientID> [password]"
                    + "\n ou SeiTchiz <IP> <clientID> [password] \n ou  SeiTchiz <IP:45678> <clientID> \n ou SeiTchiz <IP> <clientID>");
            System.exit(-1);
        }

        // cria ligacao com socket
        ClientStub cs = new ClientStub(args[0]);

        // efetuar login
        System.out.println(separador);
        if(arglen == 3) {
            cs.login(args[1], args[2]);
        } else if(arglen == 2) {
            cs.login(args[1]);
        }
        System.out.println(separador);

        boolean stop = false;
        //ciclo principal do cliente
        while(!stop) {
            
            //mostrar menu com opcoes
            System.out.println("Que operacao pretende executar? \n" +
            "f ou follow <userID> \n" +
            "u ou unfollow <userID> \n" +
            "v ou viewfollowers \n" +
            "p ou post <photo> \n" +
            "w ou wall <nPhotos> \n" +
            "l ou like <photoID> \n" +
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
            String[] option = null; 
            int resultado;
            String followersList = null;
            String mensagem = null;
            StringBuilder sbMensagem = new StringBuilder();
            String[] listaGinfo = null;

            
            try {
                System.out.print(">>>");
                reader = new BufferedReader(new InputStreamReader(System.in)); 
                input = reader.readLine();
                option = input.split(" ");
            } catch(IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
            
            switch(option[0]) {
                case "f": case "follow":
                
                    if(option.length != 2 || option[1].contains("/") || option[1].contains(":") || option[1].contains("-")) {
                        System.out.println(separador);
                        System.out.println("Opcao \"follow\" recebe argumento <userID> que nao pode conter: " +
                        "espacos, dois pontos, hifens ou forward slashes.");
                        System.out.println(separador);
                        break;
                    }
                    
                    // envia-se o userID que se procura e o userID que fez o pedido
                    resultado = cs.follow(option[1], args[1]);
                    
                    if(resultado == 0) {
                        System.out.println(separador);
                        System.out.println("Esta a seguir o user " + option[1]);
                        System.out.println(separador);
                    } else {
                        System.out.println(separador);
                        System.out.println("Ocorreu um erro a fazer a operacao... \n" +
                                "Razoes possiveis: -O userID inserido nao pertence nenhum user existente no sistema; \n" +
                                "-O user com o userID escolhido ja esta a ser seguido; \n" +
                                "-O user com o userID escolhido nao pode ser o cliente; \n" +
                                "-O userID que procurou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
                        System.out.println(separador);
                    }                
                    break;
                    
                case "u": case "unfollow":

                    if(option.length != 2 || option[1].contains("/") || option[1].contains(":") || option[1].contains("-")) {
                        System.out.println(separador);
                        System.out.println("Opcao \"unfollow\" recebe o argumento <userID> que nao pode conter: " +
                        "espacos, dois pontos, hifens ou forward slashes.");
                        System.out.println(separador);
                        break;
                    }
                                 
                    // envia-se o userID que se procura e o userID que fez o pedido
                    resultado = cs.unfollow(option[1], args[1]);
                    
                    if(resultado == 0) {
                        System.out.println(separador);
                        System.out.println("Deixou de seguir o user " + option[1]);
                        System.out.println(separador);
                    } else {
                        System.out.println(separador);
                        System.out.println("Ocorreu um erro a fazer a operacao... \n " +
                                "Razoes possiveis: -O userID inserido nao pertence nenhum user existente no sistema; \n " +
                                "-O user com o userID escolhido nao esta a ser seguido; \n " +
                                "-O user com o userID escolhido nao pode ser o cliente; \n " +
                                "-O userID que procurou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
                        System.out.println(separador);
                    }
                    break;

                case "v": case "viewfollowers":
                    
                    if(option.length != 1) {
                        System.out.println(separador);
                        System.out.println("Opcao \"viewfollowers\" nao recebe argumentos adicionais.");
                        System.out.println(separador);
                        break;
                    }
                    
                    // envia-se o senderID que quer saber quais os seus seguidores
                    followersList = cs.viewfollowers(args[1]);
                    
                    if(followersList.isEmpty()) {
                        System.out.println(separador);
                        System.out.println("O cliente nao tem followers.");
                        System.out.println(separador);
                    } else {
                        System.out.println(separador);
                        System.out.println("Os seus followers sao:\n" + followersList);
                        System.out.println(separador);
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
                    
                    if(option.length != 2 || option[1].contains("/") || option[1].contains(":") || option[1].contains("-")) {
                        System.out.println(separador);
                        System.out.println("Opcao \"newgroup\" recebe argumento <groupID> que nao pode conter espacos, " +
                        "dois pontos, hifens ou forward slashes no nome.");
                        System.out.println(separador);
                        break;
                    }
                    
                    // envia-se o userID que se procura e o userID que fez o pedido
                    resultado = cs.newgroup(option[1], args[1]); 
                    
                    if(resultado == 0) {
                        System.out.println(separador);
                        System.out.println("O cliente e agora dono do novo grupo " + option[1]);
                        System.out.println(separador);
                    } else {
                        System.out.println(separador);
                        System.out.println("Ocorreu um erro a fazer a operacao... \n " +
                                "Razoes possiveis: -O grupo com o groupID que designou ja existe; \n " +
                                "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
                        System.out.println(separador);
                    }
                    break;
                    
                case "a": case "addu":

                    if(option.length != 3 || option[1].contains(":") || option[1].contains("/") ||
                    option[1].contains("-") || option[2].contains("-") || option[2].contains("/") || 
                    option[2].contains(":")) {
                        System.out.println(separador);
                        System.out.println("Opcao \"addu\" recebe os argumentos <userID> e <groupID> que nao " + 
                                "podem conter espacos, dois pontos, hifens ou forward slashes nos nomes.");
                        System.out.println(separador);
                        break;
                    }
                    
                    // envia-se o userID que se pretende adicionar ao grupo, o grupoID e o senderID que fez o pedido
                    resultado = cs.addu(option[1], option[2], args[1]);
                    
                    if(resultado == 0) {
                        System.out.println(separador);
                        System.out.println("O utilizador selecionado e agora membro do grupo " + option[2]);
                        System.out.println(separador);
                    }  else {
                        System.out.println(separador);
                        System.out.println("Ocorreu um erro a fazer a operacao... \n " +
                                "Razoes possiveis: -O userID inserido ja pertence ao grupo; \n " +
                                "-O userID indicado nao corresponde a nenhum utilizador desta aplicacao; \n " +
                                "-O grupo indicado nao existe; \n " +
                                "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome;" +
                                "-O userID que procurou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome;" +
                                "-Apenas o dono do grupo indicado pode adicionar membros ao mesmo.");
                        System.out.println(separador);
                    }
                    break;
                    
                case "r": case "removeu":

                    if(option.length != 3 || option[1].contains(":") || option[1].contains("/") ||
                    option[1].contains("-") || option[2].contains("-") || option[2].contains("/") || 
                    option[2].contains(":")) {
                        System.out.println(separador);
                        System.out.println("Opcao \"removeu\" recebe os argumentos <userID> e <groupID> que nao " + 
                                "podem conter espacos, dois pontos, hifens ou forward slashes nos nomes.");
                        System.out.println(separador);
                        break;
                    }
                    
                    // envia-se o userID que se pretende adicionar ao grupo, o grupoID e o senderID que fez o pedido
                    resultado = cs.removeu(option[1], option[2], args[1]);
                    
                    if(resultado == 0) {
                        System.out.println(separador);
                        System.out.println("O utilizador selecionado deixou de fazer parte do grupo " + option[2]);
                        System.out.println(separador);
                    }  else {
                        System.out.println(separador);
                        System.out.println("Ocorreu um erro a fazer a operacao... \n " +
                                "Razoes possiveis: -O userID inserido nao pertence ao grupo; \n " +
                                "-O userID indicado nao corresponde a nenhum utilizador desta aplicacao; \n " +
                                "-O grupo indicado nao existe; \n " +
                                "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome;" +
                                "-O userID que procurou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome;" +
                                "-Apenas o dono do grupo indicado pode remover membros ao mesmo.");
                        System.out.println(separador);
                    }
                    break;
                    
                case "g": case "ginfo":
                    
                    /*
                    //caso meter o groupID
                    if(option.length == 2 && !option[1].contains("/") || !option[1].contains(":") || !option[1].contains("-")) {

                        // envia-se o senderID e o groupID
                        listaGinfo = cs.ginfo(args[1], option[1]);

                        for(int i = 0; i < listaGinfo.length(); i++){
                            System.out.println(listaGinfo[i]);
                        }
                        
                    //caso nao meter o groupID
                    } else if(option.length == 1) {

                        // envia-se o senderID
                        listaGinfo = cs.ginfo(args[1]);


                        for(int i = 0; i < listaGinfo.length(); i++){
                            System.out.println(listaGinfo[i]);
                        }
                        
                    } else {
                        System.out.println(separador);
                        System.out.println("Opcao \"ginfo\" pode ou nao receber um argumento <groupID> que por sua vez " +
                        "nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
                        System.out.println(separador);
                    }
                    */
                    break;
                    
                case "m": case "msg":

                    if(option.length < 3 || option[1].contains("/") || option[1].contains(":") || option[1].contains("-")) {
                        System.out.println(separador);
                        System.out.println("Opcao \"msg\" recebe dois argumentos <groupID> e <msg>" + 
                        "e <groupID> nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
                        System.out.println(separador);
                    }
                    
                    // colocar input correspondente a msg numa var "mensagem"
                    for(int i = 2; i < option.length; i++) {
                        sbMensagem.append(option[i] + " ");
                    }
                    sbMensagem.deleteCharAt(sbMensagem.length()-1);
                    mensagem = sbMensagem.toString();

                    // envia-se o groupID, o utilizador que fez o pedido e a mensagem
                    resultado = cs.msg(option[1], args[1], mensagem);

                    if(resultado == 0) {
                        System.out.println(separador);
                        System.out.println("A sua mensagem foi enviada para o grupo " + option[1]);
                        System.out.println(separador);
                    } else {
                        System.out.println(separador);
                        System.out.println("Ocorreu um erro a fazer a operacao... \n " +
                                "Razoes possiveis: -O cliente nao pertence ao grupo; \n " +
                                "-O grupo indicado nao existe; \n " +
                                "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
                        System.out.println(separador);
                    }
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
