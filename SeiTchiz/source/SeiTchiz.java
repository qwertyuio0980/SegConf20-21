public class SeiTchiz {
    


    /*
     * 0 1 2 SeiThciz 127.0.0.1:24569 martim pass SeiThciz 127.0.0.1 martim pass
     * SeiThciz 127.0.0.1:24569 martim SeiThciz 127.0.0.1 martim
     */

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
        
    }
}
