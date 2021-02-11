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

public class SeiTchizClient {

    //POR VER
    public String sAdress = "45678";

    public static void main(String[] args) {
        System.out.println("cliente: main");

        //-- Iniciar cliente 
        
    
        // Conectar com servidor 
        conectarServidor(args[1]);

        // Verificar se o password foi passado e fazer login
        if(args.length = 3) {
            login(args[2], null);
        } else {
            login(args[2], args[3]);
        }
        
        //--




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
