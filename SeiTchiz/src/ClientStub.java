import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class ClientStub {

    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private int defaultPort = 45678;

    public ClientStub(String ipPort) {
        
        conectarServidor(ipPort);

        // Criar Streams de leitura e escrita
        this.in = null;
        this.out = null;
        try {
            this.in = new ObjectInputStream(clientSocket.getInputStream());
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
	    
        String[] aux = ipPort.split(":");
        if(aux.length == 1) {
            conectar(aux[0], defaultPort);
        } else if(aux.length == 2 && aux[1].contentEquals("45678")) {
            conectar(aux[0], Integer.parseInt(aux[1]));
        } else {
            System.out.println("Formato do argumento <ServerAdress> invalido");
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
     */
    public void login(String clientID, String passwrd) {
        
        // nomes criados nao podem ter ":" ou "/" ou "-" ou " "
        if(clientID.contains(":") || clientID.contains("/") || clientID.contains("-") || clientID.contains(" ")) {
            System.out.println("Formato do userID incorreto(userID nao deve conter " +
            "dois pontos \":\" ou espaco \" \" ou hifen \"-\" ou forward slash \"/\"");
            System.exit(-1);
        }

        if(passwrd.contains(" ") || passwrd.equals("")){
            // as passwrds poderiam ter um espaço no meio. Seria melhor restringir apenas passwrds vazias. 
            System.out.println("Formato de password incorreto(password nao deve conter espaços e ter no minimo um caracter)" +
            " \n Indique uma password valida: ");
            System.exit(-1);
        } else {
            efetuarLogin(clientID,passwrd);
        }
    }

    /**
     * Método que efetua o login do cliente na plataforma SeiTchiz
     * Pede a password do clientID passado e tenta efetuar o login com as credenciais 
     * @param clientID String contendo o client id do usuário
     */
	public void login(String clientID) {
        
        // nomes criados nao podem ter ":" ou "/" ou "-" ou " "
        if(clientID.contains(":") || clientID.contains("/") || clientID.contains("-") || clientID.contains(" ")) {
            System.out.println("Formato do userID incorreto(userID nao deve conter " +
            "dois pontos \":\" ou espaco \" \" ou hifen \"-\" ou forward slash \"/\"");
            System.exit(-1);
        }

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

        efetuarLogin(clientID,passwrd);
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
     * @param clientID
     * @param passwrd
     */
	public void efetuarLogin(String clientID, String passwrd) {
	    
        // Mandar os objetos para o servidor
        try {
            this.out.writeObject(clientID);
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
                System.out.println("Sign up e login feitos com sucesso, Insira o seu nome de utilizador para continuar.");
                try {
                	Scanner sc = new Scanner(System.in); // não podemos fechar o scanner ou fechamos a porta (System.In)
                	String a = sc.nextLine();
                	while (a.contains(":") || a.contains(" ")) {
                		System.out.println("Username nao pode conter \":\"(dois pontos ou \" \"(espacos)");
                		a = sc.nextLine();	
                	}
                    out.writeObject(a);
                } 
                  //  sc.close();  caso fechemos o scanner, fechamos tbm o system.IN o que faz com que dê erro (stream closed)
                 catch (IOException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                }         
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
	
	public int follow(String userID, String senderID) {
   
        int resultado = -1;
	    try {

            //enviar tipo de operacao
	        out.writeObject("f");
	        
	        // enviar userID que o cliente quer seguir:userID do proprio cliente
            out.writeObject(userID + ":" + senderID);
            
            //receber codigo de resposta do servidor
            resultado = (int) in.readObject();

        } catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch(ClassNotFoundException e1) {
            e1.printStackTrace();
            System.exit(-1);
        }
	    
	    return resultado; 
	}

    public int unfollow(String userID, String senderID) {
 
        int resultado = -1;
        try {

            //enviar tipo de operacao
            out.writeObject("u");
            
            // enviar userID que o cliente quer deixar de seguir:userID do proprio cliente
            out.writeObject(userID + ":" + senderID);
            
            //receber codigo de resposta do servidor
            resultado = (int) in.readObject();

        } catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        } catch(ClassNotFoundException e1) {
            e1.printStackTrace();
            System.exit(-1);
        }
        
        return resultado; 
    }
        
    public String viewfollowers(String senderID) {
        
        String followersList = null;
        try {
            
            //enviar tipo de operacao
            out.writeObject("v");
            
            //enviar ID do cliente que quer ver os seus followers
            out.writeObject(senderID);
            
            //receber a lista de seguidores
            followersList = (String) in.readObject();
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } 
              
        return followersList;
    }

    public void stopClient() {
        
        try {
            //enviar tipo de operacao
            out.writeObject("s");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int newgroup(String groupID, String senderID) {
        
        int resultado = -1;
        try {
            //enviar tipo de operacao
            out.writeObject("n");
            
            //enviar ID do grupo a ser criado:ID do cliente que o pretende criar
            out.writeObject(groupID + ":" + senderID);
            
            //receber o resultado da operacao
            resultado = (int) in.readObject();
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
          
        return resultado;
    }

    public int addu(String userID, String groupID, String senderID) {

        int resultado = -1;
        try {
            //enviar tipo de operacao
            out.writeObject("a");
            
            //enviar ID do cliente que se quer adicionar ao grupo:ID do grupo
            out.writeObject(userID + ":" + groupID + ":" + senderID);
            
            //receber o resultado da operacao
            resultado = (int) in.readObject();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return resultado;
    }

    public int removeu(String userID, String groupID, String senderID) {
        int resultado = -1;
        try {
            //enviar tipo de operacao
            out.writeObject("r");
            
            //enviar ID do cliente que se quer adicionar ao grupo:ID do grupo:ID do sender
            out.writeObject(userID + ":" + groupID + ":" + senderID);
            
            //receber o resultado da operacao
            resultado = (int) in.readObject();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return resultado;
    }
    
    public int msg(String groupID, String senderID, String mensagem) {
        int resultado = -1;
        try {
            //enviar tipo de operacao
            out.writeObject("m");
            
            // enviar groupID:ID do user que fez o pedido:mensagem
            out.writeObject(groupID + ":" + senderID + ":" + mensagem);
            
            //receber o resultado da operacao
            resultado = (int) in.readObject();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return resultado;
    }

    public int canCollectOrHistory(String groupID, String senderID) {
        int resultado = -1;
        try {
            //enviar tipo de operacao
            out.writeObject("ch");
            
            // enviar groupID:ID do user que fez o pedido
            out.writeObject(groupID + ":" + senderID);
            
            //receber o resultado da operacao
            resultado = (int) in.readObject();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return resultado;
    }

    public String[] collect(String groupID, String senderID) {
        String[] listaMensagens = null;
        try {
            //enviar tipo de operacao
            out.writeObject("c");
            
            // enviar groupID:ID do user que fez o pedido
            out.writeObject(groupID + ":" + senderID);
            
            //receber o resultado da operacao
            listaMensagens = (String[]) in.readObject();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return listaMensagens;
    }

    public String[] history(String groupID, String senderID) {
        String[] listaMensagens = null;
        try {
            //enviar tipo de operacao
            out.writeObject("h");
            
            // enviar groupID:ID do user que fez o pedido
            out.writeObject(groupID + ":" + senderID);
            
            //receber o resultado da operacao
            listaMensagens = (String[]) in.readObject();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return listaMensagens;
    }

    public String[] ginfo(String senderID) {

        String resultado = null;
        try {
            //enviar tipo de operacao
            out.writeObject("g");
            //enviar senderID
            out.writeObject(senderID);
            //enviar infomacao que foi apenas um argumento
            out.writeObject("/");
            //receber o resultado da operacao
            resultado = (String) in.readObject();
            if(resultado.equals("")) {
                return null;
            }
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Tratar resposta
        String[] groups = new String[0];

        groups = resultado.split(",");
        
        return groups;
    }

    /*


    public String ginfo(String groupID, senderID) {

        String []resultado = null;
        try {
            //enviar tipo de operacao
            out.writeObject("g");
            //enviar argumento
            out.writeObject(senderID);
            out.writeObject(groupID);
            //receber o resultado da operacao
            resultado = (list) in.readObject();
            
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return resultado;

    }
    */
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
