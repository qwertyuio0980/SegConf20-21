public class SeiTchizClientStub {

    private SeiTchizNetworkClient network;

    public SeiTchizClientStub() {
        network = new SeiTchizNetworkClient();
    }

    /**
     * Refina serverAddress para que tenhamos o ip e porto (caso tenha sido passado).
     * Em seguida, chama a função da classe SeiTchizNetworkClient para se conectar 
     * ao servidor que possui o endereço passado.
     * @param serverAddress String contendo o endereço do servidor a ser conectado
     * @return 0 caso a conexão seja bem sucessedida, -1 caso contrário.
     */
    public int conectarServidor(String serverAddress) {
        String[] ipPorto = serverAddress.split(":");
        // Por omissão vamos adicionar o port 45678
        if(ipPorto.length == 1) {
            return network.conectarServidor(serverAddress);
        } else if(ipPorto.length == 2) {
            return network.conectarServidor(ipPorto[0], ipPorto[1]);
        } else {
            System.out.println("serverAddress não possui o formato <ip:porto> ou <hostname:porto> ou <ip> ou <hostname>");
        }
        return -1;
    }

    //faltam os metodos do enunciado

}

    











    

    

    
