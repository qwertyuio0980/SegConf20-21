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

    public static void main(String[] args) {
        System.out.println("---cliente iniciado---");

        //numero de argumentos errado
        if(args.length > 3 || args.length < 1) {
            System.out.println("Numero de argumentos dado errado. Usar SeiTchiz <hostname ou IP:Porto> <clientID> [password]" +
            "\n Ou SeiTchiz <clientID> [password] \n Ou  SeiTchiz <hostname ou IP:Porto> <clientID> \n Ou SeiTchiz <clientID>");
            System.exit(-1);
        }

        //-- Iniciar cliente -- maybe temos de verificar se tem porto or not default 45678
        if(conectarServidor(args[0]) == 1) {
            System.out.println("Houve um erro a fazer ligação com o servidor SeiTchiz");
            System.exit(-1);
        }

        System.out.println("A partir deste momento a ligação com o servidor foi estabelecida");
        
        // Usuário não passou a passwrd
        String passwrd = null;
        if(args.length == 3) {
            // Ler Input
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Indique uma password :");
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
                    " \n Indique uma password valida:");
                }
            }            
        } else {
            passwrd = args[3];
        }

        // Tentar efetuar o login
        if(login(args[1], passwrd)) {
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
}
