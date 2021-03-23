package client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.Scanner;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import communication.ComClient;
import security.Security;

public class ClientStub {

	private SSLSocket clientSocket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	
	private int defaultPort = 45678;

	private ComClient com;
	
	private String truststore; 
	private String keystore;
	private String keystorePassword;
	private String clientID;

	public ClientStub(String[] argsClient) {

		conectarServidor(argsClient[0]);

		// Inicializar variaveis globais
		this.truststore = argsClient[1];
		this.keystore = argsClient[2];
		this.keystorePassword = argsClient[3];
		this.clientID = argsClient[4];

		// Criar streams de leitura e escrita
		this.in = null;
		this.out = null;
		try {
			this.in = new ObjectInputStream(clientSocket.getInputStream());
			this.out = new ObjectOutputStream(clientSocket.getOutputStream());
		} catch (IOException e1) {
			System.err.println(e1.getMessage());
			System.exit(-1);
		}

		com = new ComClient(clientSocket, in, out);

		// Criar ficheiro onde ficarao guardadas as fotos recebidas
		File wall = new File("wall");
		// Caso ainda nao exista o diretorio wall, criar-lo
		if(!wall.isDirectory()) {
			try{
				wall.mkdir();
			} catch(Exception e) {
				System.out.println("Erro ao criar diretorio 'wall'");
				System.exit(-1);
			}
		}
	}

	/**
	 * Metodo que faz a verificacao se o login pode ser feito com os
	 * argumentos que o cliente da ao ser iniciado e caso possa fazer
	 * login faz o mesmo
	 * 
	 * @param ipPort String que representa o par IP:Porto
	 */
	public void conectarServidor(String serverAdress) {

		String[] aux = serverAdress.split(":");
		if (aux.length == 1) {
			conectar(aux[0], defaultPort);
		} else if (aux.length == 2 && aux[1].contentEquals("45678")) {
			conectar(aux[0], Integer.parseInt(aux[1]));
		} else {
			System.out.println("Formato do argumento <ServerAdress> invalido");
			System.exit(-1);
		}
	}

	/**
	 * Conectar com o servidor com o ip passado e porto passados por uma nova
	 * socket
	 * 
	 * @param ip   String representando o ip do servidor
	 * @param port int representando o porto pelo qual se dará a conexão
	 * @requires ip != null
	 */
	public void conectar(String ip, int port) {

		SocketFactory sf = SSLSocketFactory.getDefault();
		try {
			clientSocket = (SSLSocket) sf.createSocket(ip, port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

	/**
     * Metodo de comunicacao com o servidor sobre a autenticacao
     * do cliente e ve se o cliente pos alguma informacao errada
     * ou se se trata de um user novo ou um user que esta a voltar
     * a sua conta que ja tinha sido criada antes
     * 
     * @param truststore String que representa o certificado de chave publica do servidor
     * @param keystore String que representa o par de chaves do clientID
     * @param keystorePassword String que representa a password da keystore
     * @param clientID String que representa o ID do cliente
     */
    public void efetuarLogin() {

        // Mandar o clientID para o servidor
        try {
			// 1. Mandar clientID
            this.out.writeObject(clientID);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            closeConnection();
            System.exit(-1);
        }

        // Receber resposta do servidor, um (nonce) e int (flag de registo no servidor)
        Long nonce = 0L;
        try {
            nonce = (Long) in.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            closeConnection();
            System.exit(-1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            closeConnection();
            System.exit(-1);
		}

        int flag = 1;
        try {
            flag = (Integer) in.readObject();
        } catch (ClassNotFoundException e) {
            System.err.println(e.getMessage());
            closeConnection();
            System.exit(-1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            closeConnection();
            System.exit(-1);
		}
		
		//apaga sysout
		System.out.println("flag recebida:" + flag);

        // Enviar assinatura para o servidor de acordo com a flag recebida pelo mesmo
        int res = sendSigned(nonce, flag);

        if(res == 0) {

			if(flag == 0) {
				// Client corrente já registado previamente no servidor. Assinatura e nonce enviados foram validados
				System.out.println("Login efetuado com sucesso.");
			} else if(flag == 1) {
				// Client corrente foi registado com sucesso. Assinatura e nonce enviados foram validados
				System.out.println("Sign up e autenticação efetuados com sucesso.");
			}
        } else {
            // Ocorreu um erro ao servidor verificar nonce e assinatura passados
            System.out.println("Erro ao fazer autenticação.");
            closeConnection();
            System.exit(-1);
        }
    }

	/**
     * Envia nonce assinado para o servidor
     * Caso a flag seja igual a 0 (Client corrente já registado no servidor) envia o nonce e o mesmo assinado para ser verificado
     * pelo servidor 
     * Caso a flag seja igual a 1 (Client corrente ainda não registado no servidor) envia o nonce, o mesmo assinado 
     * assim como a chave publica do client
	 * 
     * @param nonce nonce a ser assinado e enviado ao servidor
     * @param flag indica o registo ou nao do Client corrente no servidor
     * @return devolve -1 se a autenticacao correu mal e 0 se correu bem
     */
    private int sendSigned(Long nonce, int flag) {
        // 1. Obter chaves do Client corrente
        // 1.1. Obter certificado
		KeyStore kstore = null;
		try {
			kstore = KeyStore.getInstance("JCEKS");
		} catch (KeyStoreException e2) {
			e2.printStackTrace();
			System.out.println("Erro ao obter keystore");
            closeConnection();
            System.exit(-1);
		}
		try(FileInputStream kfile = new FileInputStream("keystores/" + keystore)) {
			kstore.load(kfile, this.keystorePassword.toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			System.out.println("Erro ao dar load de keystore");
            closeConnection();
            System.exit(-1);
		}
        
        Certificate cert = null;
		try {
			cert = (Certificate) kstore.getCertificate(keystore);//alias e igual ao nome do keystore
		} catch (KeyStoreException e1) {
			System.out.println("Erro ao buscar certificado do cliente");
            closeConnection();
            System.exit(-1);
		}
		
        // 1.2. Obter chave privada
        PrivateKey privKey = null;
		try {
			privKey = (PrivateKey) kstore.getKey(keystore, this.keystorePassword.toCharArray());
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			System.out.println("Erro ao obter chave privada");
            closeConnection();
            System.exit(-1);
		}

        // Assinar nonce
        Signature s = null;
		try {
			s = Signature.getInstance("MD5withRSA");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Erro ao obter assinatura");
            closeConnection();
            System.exit(-1);
		}
        try {
			s.initSign(privKey);
		} catch (InvalidKeyException e) {
			System.out.println("erro: chave invalida");
            closeConnection();
            System.exit(-1);
		}
        try {
			s.update(longToBytes(nonce));
		} catch (SignatureException e) {
			System.out.println("erro a fazer update");
            closeConnection();
            System.exit(-1);
		}
        
		// Enviar nonce
		try {
			out.writeObject(nonce);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			closeConnection();
			System.exit(-1);
		}

        // Enviar assinatura do nonce
        try {
			out.writeObject(s.sign());
		} catch (SignatureException | IOException e) {
			System.out.println("erro a enviar assinatura");
            closeConnection();
            System.exit(-1);
		}

        // Client corrente ainda não possui registo prévio no servidor
        if(flag == 1) {
	        // Enviar certificado
            try {
				out.writeObject(cert.getEncoded());
			} catch (IOException | CertificateEncodingException e) {
				System.out.println("erro a enviar certificado");
	            closeConnection();
	            System.exit(-1);
			}
        }

        // Devolver resposta do servidor recebida
        int res = -1;
		try {
			res = (int) in.readObject();
		} catch (ClassNotFoundException | IOException e) {
			System.err.println(e.getMessage());
            closeConnection();
            System.exit(-1);
		}

        return res;
    }

	/**
	 * Metodo que converte um long em array de bytes
	 * 
	 * @param x long a converter
	 * @return array de bytes convertidos
	 */
	public byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}
	
	/**
	 * Metodo que converte um array de bytes em long
	 * 
	 * @param bytes array de bytes a converter
	 * @return long convertido
	 */
	public long bytesToLong(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(bytes);
		buffer.flip();//need flip 
		return buffer.getLong();
	}

	/**
	 * Metodo que efetua a comunicacao entre um user que quer seguir outro
	 * user e  o servidor
	 * 
	 * @param userID String que representa o userID a seguir
	 * @param senderID String que representa o ID do user a fazer o pedido de follow
	 * @return 0 se o pedido teve sucesso e -1 caso contrario
	 */
	public int follow(String userID, String senderID) {

		int resultado = -1;
		try {

			// enviar tipo de operacao
			out.writeObject("f");

			// enviar userID que o cliente quer seguir:userID do proprio cliente
			out.writeObject(userID + ":" + senderID);

			// receber codigo de resposta do servidor
			resultado = (int) in.readObject();

		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}

		return resultado;
	}

	/**
	 * Metodo que efetua a comunicacao entre um user que quer deixar de seguir
	 * outro user e o servidor
	 * 
	 * @param userID String que representa o userID a deixar de seguir
	 * @param senderID String que representa o ID do user a fazer o pedido de unfollow
	 * @return 0 se o pedido teve sucesso e -1 caso contrario
	 */
	public int unfollow(String userID, String senderID) {

		int resultado = -1;
		try {

			// enviar tipo de operacao
			out.writeObject("u");

			// enviar userID que o cliente quer deixar de seguir:userID do proprio cliente
			out.writeObject(userID + ":" + senderID);

			// receber codigo de resposta do servidor
			resultado = (int) in.readObject();

		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}

		return resultado;
	}

	/**
	 * Metodo que faz a ligacao com o servidor e lhe pede quais os
	 * seguidores do cliente
	 * 
	 * @param senderID String que representa o ID do clinte que fez 
	 * o pedido viewfollowers
	 * @return String que tem todos os followers do cliente ou esta vazia
	 * se este nao tem followers
	 */
	public String viewfollowers(String senderID) {

		String followersList = null;
		try {

			// enviar tipo de operacao
			out.writeObject("v");

			// enviar ID do cliente que quer ver os seus followers
			out.writeObject(senderID);

			// receber a lista de seguidores
			followersList = (String) in.readObject();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return followersList;
	}

	/**
	 * Metodo que interrompe a ligacao do cliente com o servidor 
	 * 
	 */
	public void stopClient() {

		try {
			// enviar tipo de operacao
			out.writeObject("s");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Metodo que pede ao servidor para criar um novo grupo 
	 * sendo o cliente que faz o pedido o seu dono
	 * 
	 * @param groupID String que representa o ID unico que o grupo vai ter
	 * @param senderID String que representa o ID do user a fazer o pedido de newgroup
	 * @return 0 se o pedido for sucedido e o grupo criado e -1 caso contrario
	 */
	public int newgroup(String groupID, String senderID) {

		int resultado = -1;
		try {
			// enviar tipo de operacao
			out.writeObject("n");

			// enviar ID do grupo a ser criado:ID do cliente que o pretende criar
			out.writeObject(groupID + ":" + senderID);

			// receber o resultado da operacao
			resultado = (int) in.readObject();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return resultado;
	}

	/**
	 * Metodo que pede ao servidor para adicionar um user a um grupo existente
	 * do qual o dono e o cliente que faz o pedido
	 * 
	 * @param userID String que representa o ID do user a adicionar ao grupo
	 * @param groupID String que representa o ID do grupo
	 * @param senderID  String que representa o ID do user a fazer o pedido de addu
	 * @return 0 se a operacao tiver sucesso e -1 caso contrario
	 */
	public int addu(String userID, String groupID, String senderID) {

		int resultado = -1;
		try {
			// enviar tipo de operacao
			out.writeObject("a");

			// enviar ID do cliente que se quer adicionar ao grupo:ID do grupo
			out.writeObject(userID + ":" + groupID + ":" + senderID);

			// receber o resultado da operacao
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

	/**
	 * Metodo que pede ao servidor para remover um user que pertence a um 
	 * grupo existente do qual o dono e o cliente que faz o pedido
	 * 
	 * @param userID String que representa o ID do user a remover ao grupo
	 * @param groupID String que representa o ID do grupo
	 * @param senderID  String que representa o ID do user a fazer o pedido de removeu
	 * @return 0 se a operacao tiver sucesso e -1 caso contrario
	 */
	public int removeu(String userID, String groupID, String senderID) {
		int resultado = -1;
		try {
			// enviar tipo de operacao
			out.writeObject("r");

			// enviar ID do cliente que se quer adicionar ao grupo:ID do grupo:ID do sender
			out.writeObject(userID + ":" + groupID + ":" + senderID);

			// receber o resultado da operacao
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

	/**
	 * Metodo que pede ao servidor para enviar uma nova mensagem para o grupo
	 * do qual o cliente que fez o pedido participa
	 * 
	 * @param groupID String que representa o ID do grupo
	 * @param senderID String que representa o ID do user a fazer o pedido de msg
	 * @param mensagem String que representa a mensagem a enviar
	 * @return 0 se a mensagem foi enviada com sucesso para o grupo e -1 caso contrario
	 */
	public int msg(String groupID, String senderID, String mensagem) {
		int resultado = -1;
		try {
			// enviar tipo de operacao
			out.writeObject("m");

			// enviar groupID:ID do user que fez o pedido:mensagem
			out.writeObject(groupID + ":" + senderID + ":" + mensagem);

			// receber o resultado da operacao
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

	/**
	 * Metodo que pergunta ao servidor se o cliente pode fazer um pedido de
	 * collect ou history sem que ocorram erros ou anomalias
	 * 
	 * @param groupID String que representa o ID do grupo
	 * @param senderID String que representa o ID do user a fazer o 
	 * pedido de canCollectOrHistory
	 * @return 0 se pode fazer esses pedidos sem problemas ou -1 caso contrario
	 */
	public int canCollectOrHistory(String groupID, String senderID) {
		int resultado = -1;
		try {
			// enviar tipo de operacao
			out.writeObject("ch");

			// enviar groupID:ID do user que fez o pedido
			out.writeObject(groupID + ":" + senderID);

			// receber o resultado da operacao
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

	/**
	 * Metodo que faz pedido ao servidor para receber todas as mensagens
	 * nao lidas ainda pelo cliente que fez o pedido
	 * 
	 * @param groupID String que representa o ID do grupo
	 * @param senderID String que representa o ID do user a fazer o 
	 * pedido de collect
	 * @return lista de Strings em que cada uma representa o dono de uma mensagem
	 * ainda nao lida e o conteudo da mesma separada por : e se nao houverem mensagens
	 * por ler devolve-se a lista de strings contendo apenas 1 entrada com conteudo "-empty"
	 */
	public String[] collect(String groupID, String senderID) {
		String[] listaMensagens = null;
		try {
			// enviar tipo de operacao
			out.writeObject("c");

			// enviar groupID:ID do user que fez o pedido
			out.writeObject(groupID + ":" + senderID);

			// receber o resultado da operacao
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

	/**
	 * Metodo que faz pedido ao servidor para receber todas as mensagens ja lidas
	 * pelo cliente que fez o pedido
	 * 
	 * @param groupID String que representa o ID do grupo
	 * @param senderID String que representa o ID do user a fazer o 
	 * pedido de history
	 * @return lista de Strings em que cada uma representa o dono de uma mensagem
	 * ja lida e o conteudo da mesma separada por : e se nao houverem mensagens
	 * ja lidas devolve-se a lista de strings contendo apenas 1 entrada com conteudo "-empty"
	 */
	public String[] history(String groupID, String senderID) {
		String[] listaMensagens = null;
		try {
			// enviar tipo de operacao
			out.writeObject("h");

			// enviar groupID:ID do user que fez o pedido
			out.writeObject(groupID + ":" + senderID);

			// receber o resultado da operacao
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

	/**
	 * Recebe um senderID e envia o mesmo para o servidor.
	 * É recebida uma lista de grupos dos quais o senderID é membro ou é dono.
	 * Caso não participe em nenhum grupo ou não seja dono de nenhum
	 * é recebida uma String vazia
	 * 
	 * @param senderID usuario corrente
	 * @return Array de Strings contendo primeiro os grupos dos quais o senderID é dono,
	 * caso seja dono de algum. 
	 * Em seguida, veêm os donos e os nomes dos grupos dos quais o senderID participa
	 * no formato <ownerID-groupID>.
	 * Todos os grupos são separados por ','.
	 */
	public String[] ginfo(String senderID) {

		String resultado = null;
		try {
			// enviar tipo de operacao
			out.writeObject("g");
			// enviar senderID
			out.writeObject(senderID);
			// enviar infomacao que foi apenas um argumento
			out.writeObject("/");
			// receber o resultado da operacao
			resultado = (String) in.readObject();
			if (resultado.equals("")) {
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

	/**
	 * Pede ao servidor o nome do dono e participantes do groupID,
	 * caso o senderID seja dono ou participante do groupID.
	 * 
	 * @param senderID usuário corrente
	 * @param groupID grupo do qual a identificação do dono e dos membros será procurada
	 * @return Array de Strings contendo primeiro os grupos dos quais o senderID é dono,
	 * caso seja dono de algum. 
	 * Em seguida, veêm os donos e os nomes dos grupos dos quais o senderID participa
	 * no formato <ownerID-groupID>.
	 * Todos os grupos são separados por ','.
	 */
	public String[] ginfo(String senderID, String groupID) {

		String resultado = null;
		try {
			// enviar tipo de operacao
			out.writeObject("g");
			// enviar senderID
			out.writeObject(senderID);
			// enviar informação que foi apenas um argumento
			out.writeObject(groupID);
			// receber o resultado da operacao
			resultado = (String) in.readObject();

			// Caso em que o groupID não existe
			// Caso em que o usuario nao eh membro nem dono do groupID
			if(resultado.equals("")) {
				System.out.println("O grupo nao existe ou voce nao e dono ou participa no mesmo");
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

	/**
	 * Metodo que faz o pedido ao servidor para postar uma fotografia no mural
	 * do cliente que fez o pedido
	 * 
	 * @param pathFile String que representa o pathFile da fotografia a enviar
	 * @return true se houve sucesso a postar a fotografia e false caso contrario
	 */
	public boolean post(String pathFile) {

		File file = new File("Fotos/" + pathFile);
		boolean bool = false;
		try {
			com.send("p");
			com.sendFile("Fotos/" + pathFile);

			if ((boolean) com.receive()) {
				bool = true;
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return bool;
	}

	/**
	 * Pede ao servidor as nPhotos mais recentes dos usuários que o segue
	 * 
	 * @param senderID usuário corrente
	 * @param nPhotos número de fotos mais recentes a serem devolvidas
	 * @return Lista de Strings contendo o identificador individual de cada foto,
	 * assim como seu número de likes
	 */
	public String[] wall(String senderID, int nPhotos) {

		String resultado[] = null;
		int tamanhoArray = 0;
		try {
			// enviar tipo de operação
			out.writeObject("w");
			// enviar senderID
			out.writeObject(senderID);
			// enviar número de fotografias mais recentes
			out.writeObject(nPhotos);
			
			//receber tamanho do array a devolver
			tamanhoArray = (int) in.readObject();

			if(tamanhoArray == -1) {
				resultado = new String[1];
				resultado[0] = (String) in.readObject();
			} else {
				// Cada photo sera representada por duas entradas no array resultado:
				// 1.photoID
				// 2.likes
				resultado = new String[tamanhoArray*2];

				// Loop for para receber os 3 instreams de cada foto
				for(int i = 0; i < tamanhoArray*2; i+=2) {
					// Receber identificador da photo
					resultado[i] = (String) in.readObject();
					// Receber numero de likes na foto atual
					resultado[i+1] = (String) in.readObject();
					// Receber e guardar ficheiro da foto
					com.receiveFileWall();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}

		return resultado;
	}

	/**
	 * Metodo que faz pedido ao servidor para dar like na foto com photoID dado
	 * 
	 * @param photoID String que representa o ID da foto
	 * @return 0 se o pedido teve sucesso e -1 caso contrario
	 */
	public int like(String photoID) {
		int resultado = -1;
		try {
			// enviar tipo de operacao
			out.writeObject("l");

			// enviar groupID:ID do user que fez o pedido
			out.writeObject(photoID);

			// receber o resultado da operacao
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

	/**
	 * Fecha a conexao com o servidor
	 */
	protected void closeConnection() {
		try {
			this.out.close();
			this.in.close();
			this.clientSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}