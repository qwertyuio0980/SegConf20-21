import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SeiTchiz {


    public static void main(String[] args) {
        System.out.println("---cliente iniciado---");

        int arglen = args.length;

        // numero de argumentos errado
        if (arglen > 3 || arglen < 2) {
            // TODO: O ip e username sempre vão ser passados, password é opcional e o
            // sistema vai pedir ao utilizador que introduza uma na linha de comando
            System.out.println(
                    "Numero de argumentos dado errado. Usar SeiTchiz <hostname ou IP:Porto> <clientID> [password]"
                            + "\n Ou SeiTchiz <clientID> [password] \n Ou  SeiTchiz <hostname ou IP:Porto> <clientID> \n Ou SeiTchiz <clientID>");
            System.exit(-1);
        }

        ClientStub cs = new ClientStub(args[0]);

        if(arglen == 3) {
            cs.login(args[1], args[2]);
        } else if(arglen == 2) {
            cs.login(args[1]);
        }
        
        boolean stop = false;

        
        //ciclo principal do cliente
        while(!stop) {
            
            //abstrair noutra classe??
            //mostrar menu com opcoes
            System.out.println("Que opção pretende executar? \n" +
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
            String[] option = new String[3];//tamanho maximo possivel
            
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


                    break;
                case "u": case "unfollow":

                    break;
                case "v": case "viewfollowers":

                    break;
                case "p": case "post":

                    break;
                case "w": case "wall":

                    break;
                case "l": case "like":

                    break;
                case "n": case "newgroup":

                    break;
                case "a": case "addu":

                    break;
                case "r": case "removeu":

                    break;
                case "g": case "ginfo":

                    break;
                case "m": case "msg":

                    break;
                case "c": case "collect":

                    break;
                case "h": case "history":

                    break;
                case "s": case "stop":
                    //caso extra para fechar a aplicacao por conta propria do cliente
                    stop = true;
                    break;
                default:
                    System.out.println("Input recebido nao faz parte dos comandos aceites \n" +
                    "Tente outra vez");
                    break;
            }
        }
    }
}
