import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientNetwork {

    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientNetwork() {
        // Criar Streams de leitura e escrita
        this.in = null;
        this.out = null;
        try {
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e1) {
            System.err.println(e1.getMessage());
            System.exit(-1);
        }
   
       
      
    }

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


	}


    
}
