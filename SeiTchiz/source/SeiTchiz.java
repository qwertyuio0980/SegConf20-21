import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.print.event.PrintEvent;

public class SeiTchiz {

    private static Socket clientSocket;
    private static String username;
    private static String passwrd;
    
    /*               0         1      2
    SeiThciz 127.0.0.1:24569 martim pass
    SeiThciz 127.0.0.1 martim pass
    SeiThciz 127.0.0.1:24569 martim
    SeiThciz 127.0.0.1 martim
    */

    public static void main(String[] args) {
        System.out.println("---cliente iniciado---");

        int arglen = args.length;

        //numero de argumentos errado
        if(arglen > 3 || arglen <= 1) {
            System.out.println("Numero de argumentos dado errado. Usar SeiTchiz <hostname ou IP:Porto> <clientID> [password]" +
            "\n Ou SeiTchiz <clientID> [password] \n Ou  SeiTchiz <hostname ou IP:Porto> <clientID> \n Ou SeiTchiz <clientID>");
            System.exit(-1);
        }

        //tratamento do ServerAdress
        String[] ipPort = handlerIpPort(args[0]);
        if(conectServer(ipPort) == -1) {
            System.out.println("Houve um erro a fazer ligação com o servidor SeiTchiz");
            System.exit(-1);
        }
        
        //guardar username
        username = args[1];
        
        //casos 2 ou 3 argumentos passados
        if(arglen == 2) {
         // Usuario não passou a passwrd
            
            // Ler Input
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Indique uma password: ");
            while(passwrd == null) {
                try {
                    passwrd = reader.readLine();
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                }
                if(passwrd.contains(" ") || passwrd.equals("")){
                    passwrd = null;
                    System.out.println("Formato de password incorreto(password nao deve conter espaços e ter no minimo um caracter)" +
                    " \n Indique uma password valida: ");
                }
            }            
        } else {
            
            // Usuario passou a passwrd
            passwrd = args[2];
        }

        System.out.println("A partir deste momento a ligação com o servidor foi estabelecida");
        
        //-- Iniciar cliente --
        
        // Tentar efetuar o login
        if(login(username, passwrd)) {
            System.out.println("Login não foi bem sucedido... \n A terminar o cliente agora");
            System.exit(-1);
        }

        // Criar Socket do cliente e conectar com o servidor
        Socket clientSocket = null;

        try {
            clientSocket = new Socket("localhost", 23456);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        // Criar Streams de leitura e escrita
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
            System.exit(-1);
        }
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
            System.exit(-1);
        }

        // Ler Input
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.print("user: ");
        String user = null;
        try {
            user = reader.readLine();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        System.out.print("passwd: ");
        String passwd = null;
        try {
            passwd = reader.readLine();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        // Mandar os objetos para o servidor
        try {
            out.writeObject(user);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        try {
            out.writeObject(passwd);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        System.out.println("cliente:... enviou user e passwd para o servidor.");

        // Receber resposta do servidor
        Boolean fromServer = null;
        try {
            fromServer = (Boolean) in.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        System.out.println("cliente:... resposta recebida = " + Boolean.toString(fromServer));

        // Enviar ficheiro para o servidor
        File file = null;
        System.out.print("Insira o endereço do ficheiro: ");
        String pathname = null;
        try {
            pathname = reader.readLine();
            System.out.println("Passou");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        file = new File(pathname);

        /// Ler ficheiro
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        InputStream input = new BufferedInputStream(fin);
        /// Verificar tamanho do ficheiro 
        byte[] buffer = new byte[1024];
        int fileSize = 0;
        try {
            fileSize = input.read(buffer);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        System.out.println("cliente:... ficheiro foi lido por completo");

        /// Enviar tamanho do ficheiro
        try {
            out.writeObject(fileSize);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        /// Enviar buffer com conteúdo do ficheiro
        int bytesWriten = 0;
        try {
            while(bytesWriten > -1) {
                out.write(buffer, 0, buffer.length);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        System.out.println("cliente:... ficheiro enviado ao servidor");
        
    }

    /**
     * Método que efetua o login do cliente na plataforma SeiTchiz, ou seja,
     * envia ao servidor o seu id e password e este trata de verificar se o
     * login foi feito com sucesso ou se aconteceu de anormal sem sucesso 
     * 
     * @param clientID
     * @param passwd
     * @return true se login for sucedido e false caso contrario
     */
    public boolean login(String username, String passwd) {
        //TODO
    }

    /**
     * Conectar com o servidor com o ip passado e pelo porto 45678
     * 
     * @param ip String representando o ip do servidor
     * @param port int representando o porto pelo qual se dará a conexão 
     * @return 0 caso a conexão seja bem sucedida, -1 caso contrário
     * @requires ip != null
     */
    public static int conectServer(String[] ipPort) {
        try {
            clientSocket = new Socket(ipPort[0], Integer.parseInt(ipPort[1]));
            return 0;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return -1;
    }


    /**
     * TODO
     * 
     * @param ipPort
     * @return
     */
    public static String[] handlerIpPort(String ipPort) {
        String[] separado = new String[2];
        if(ipPort.contains(":")) {
            //caso ip:port
            
            separado = ipPort.split(":");
        } else {
            //caso localhost
            
            separado[0] = ipPort;
            separado[1] = "45678";
        }
        return separado;
    }

}
