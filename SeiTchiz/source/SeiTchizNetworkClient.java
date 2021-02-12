public class SeiTchizNetworkClient {

    private Socket clientSocket;

    private int port = 45678;
    
    public SeiTchizNetworkClient();

    /**
     * Conectar com o servidor com o ip passado e pelo porto 45678
     * @param ip String representando o ip do servidor
     * @return 0 caso a conexão seja bem sucedida, -1 caso contrário
     * @requires ip != null
     */
    public int conectarServidor(String ip) {
        try {
            clientSocket = new Socket(ip, this.port);
            return 0;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return -1;
    }

    /**
     * Conectar com o servidor com o ip passado e pelo porto 45678
     * @param ip String representando o ip do servidor
     * @param port int representando o porto pelo qual se dará a conexão 
     * @return 0 caso a conexão seja bem sucedida, -1 caso contrário
     * @requires ip != null
     */
    public int conectarServidor(String ip, int port) {
        try {
            clientSocket = new Socket(ip, port);
            return 0;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        return -1;
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
    public boolean login(String clientID, String passwd) {
        //TODO
    }

