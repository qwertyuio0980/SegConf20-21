import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientStub {

    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    private int defaultPort = 45678;

    public ClientStub(String ipPort) {
        conectarServidor(ipPort);

        // Criar Streams de leitura e escrita
        this.in = null;
        try {
            this.in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
            System.exit(-1);
        }
        this.out = null;
        try {
            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
            System.exit(-1);
        }
    }

    /**
     * 
     * @param string
     */
	public void conectarServidor(String ipPort) {
        String []aux = ipPort.split(":");

        if(aux.length == 1) {
            conectar(aux[0], defaultPort);
        } else if(aux.length == 2) {
            conectar(aux[0], Integer.parseInt(aux[1]));
        } else {
            System.out.println("Formato do argumento <ip/hostname:port> inválido");
            System.exit(-1);
        }
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
    public void login(String username, String passwrd) {
        if(passwrd.contains(" ") || passwrd.equals("")){
            // as passwrds poderiam ter um espaço no meio. Seria melhor restringir apenas passwrds vazias. 
            System.out.println("Formato de password incorreto(password nao deve conter espaços e ter no minimo um caracter)" +
            " \n Indique uma password valida: ");
            System.exit(-1);
        } else {
            efetuarLogin(username,passwrd);
        }
    }

    /**
     * Método que efetua o login do cliente na plataforma SeiTchiz
     * Pede a password do clientID passado e tenta efetuar o login com as credenciais 
     * @param clientID String contendo o client id do usuário
     */
	public void login(String username) {
        String passwrd = null;
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("password: ");
        while(passwrd == null) {
            try {
                passwrd = reader.readLine();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
            if(passwrd.contains(" ") || passwrd.equals("")){
                // as passwrds poderiam ter um espaço no meio. Seria melhor restringir apenas passwrds vazias. 
                passwrd = null;
                System.out.println("Formato de password incorreto(password nao deve conter espaços e ter no minimo um caracter)" +
                " \n Indique uma password valida: ");
            }
        }

        efetuarLogin(username,passwrd);

	}

    //-- NETWORK METHODS

        /**
     * Conectar com o servidor com o ip passado e porto passados
     * 
     * @param ip String representando o ip do servidor
     * @param port int representando o porto pelo qual se dará a conexão 
     * @requires ip != null
     */
	public void conectar(String ip, int port) {
        try {
            this.clientSocket = new Socket(ip, port);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
	}

    /**
     * 
     * @param username
     * @param passwrd
     */
	public void efetuarLogin(String username, String passwrd) {
        // Mandar os objetos para o servidor
        try {
            this.out.writeObject(username);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        try {
            out.writeObject(passwrd);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        System.out.println("cliente enviou username e passwd para o servidor");

        // Receber resposta do servidor
        int fromServer = 0;
        try {
            fromServer = (Integer) in.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        switch (fromServer) {
            case 0:
                // Caso fromServer == 0: Login foi feito com sucesso
                System.out.println("Login foi feito com sucesso");
                break;
            case 1:
                // Caso fromServer == 1: Novo cliente foi criado com as credenciais passadas
                System.out.println("Sign up e login feitos com sucesso");
                break;
            case (-1):
                // Caso fromServer == -1: Password invalida
                System.out.println("Password invalida. A fechar aplicacao...");
                System.exit(-1);
                break;
            default:
                break;
        }

	}

        // // Enviar ficheiro para o servidor
        // File file = null;
        // System.out.print("Insira o endereço do ficheiro: ");
        // String pathname = null;
        // try {
        //     pathname = reader.readLine();
        //     System.out.println("Passou");
        // } catch (Exception e) {
        //     System.err.println(e.getMessage());
        //     System.exit(-1);
        // }
        // file = new File(pathname);

        // /// Ler ficheiro
        // FileInputStream fin = null;
        // try {
        //     fin = new FileInputStream(file);
        // } catch (FileNotFoundException e) {
        //     System.err.println(e.getMessage());
        //     System.exit(-1);
        // }
        // InputStream input = new BufferedInputStream(fin);
        // /// Verificar tamanho do ficheiro 
        // byte[] buffer = new byte[1024];
        // int fileSize = 0;
        // try {
        //     fileSize = input.read(buffer);
        // } catch (IOException e) {
        //     System.err.println(e.getMessage());
        //     System.exit(-1);
        // }

        // System.out.println("cliente:... ficheiro foi lido por completo");

        // /// Enviar tamanho do ficheiro
        // try {
        //     out.writeObject(fileSize);
        // } catch (IOException e) {
        //     System.err.println(e.getMessage());
        //     System.exit(-1);
        // }

        // /// Enviar buffer com conteúdo do ficheiro
        // int bytesWriten = 0;
        // try {
        //     while(bytesWriten > -1) {
        //         out.write(buffer, 0, buffer.length);
        //     }
        // } catch (IOException e) {
        //     System.err.println(e.getMessage());
        //     System.exit(-1);
        // }
        // System.out.println("cliente:... ficheiro enviado ao servidor");


}