package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.security.cert.CertificateFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


import java.util.*;
import java.util.Base64;


import communication.Com;
import security.Security;

public class ClientStub {

	private final String keyGenSimAlg = "AES";
	private SSLSocket clientSocket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	
	private int defaultPort = 45678;

	private Com com;
	private Security sec;
	
	private String truststore; 
	private String keystore;
	private String keystorePassword;
	private String clientID;
	private final String storetype = "JKS";

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

		com = new Com(clientSocket, in, out);
		sec = new Security();

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
			this.clientSocket = (SSLSocket) sf.createSocket(ip, port);
		} catch(IOException e) {
			System.out.println("Erro a criar a socket");
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

        // Enviar assinatura para o servidor de acordo com a flag recebida pelo mesmo
        int res = sendSigned(nonce, flag);

        if(res == 0) {
			if(flag == 0) {
				// Client corrente já registado previamente no servidor. Assinatura e nonce enviados foram validados
				System.out.println("Login efetuado com sucesso.");
			} else if(flag == 1) {
				// Client corrente foi registado com sucesso. Assinatura e nonce enviados foram validados
				System.out.println("Sign up e autenticacao efetuados com sucesso.");
			}
        } else {
            // Ocorreu um erro ao servidor verificar nonce e assinatura passados
            System.out.println("Erro ao fazer autenticacao.");
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
			kstore = KeyStore.getInstance("JKS");
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
	 * @param senderID String que representa o ID do cliente que fez 
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
		// Envia para o servidor o identificador do grupo
		// Envia o identificador da chave
		// Cria uma lista contendo o [<ownerID,chave cifrada com chave pública do servidor>]

		int resultado = -1;
		try {
			// enviar tipo de operacao
			out.writeObject("n");

			// enviar ID do grupo a ser criado:ID do cliente que o pretende criar
			out.writeObject(groupID + ":" + senderID);

			// Criar nova chave simétrica para o grupo
			Key groupKey = generateKey();
			if(groupKey == null) {
				System.out.println("Erro:... Não foi possível criar uma chave simétrica para o novo grupo");
				return resultado;
			}
			// Cifrar a chave com a chave pública do senderID
			// Obter chave pública do senderID
			PublicKey pk = sec.getCert(this.keystore, "keystores/" + this.keystore, this.keystorePassword, this.storetype).getPublicKey();
			byte[] wrappedKey = sec.wrapKey(groupKey, pk);
			if(wrappedKey == null) {
				System.out.println("Erro:... Não foi possível fazer um wrap da chave simétrica para o novo grupo");
				return resultado;
			}

			System.out.println("Chave enconded enviada: " + Base64.getEncoder().encodeToString(wrappedKey));

			// Enviar chave para o servidor como uma String
			out.writeObject(Base64.getEncoder().encodeToString(wrappedKey));

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
	 * Metodo que gera uma string do tipo <userID1,chave cifrada1>,<userID1,chave cifrada1>,
	 * para cada participantes no array de participantes recebido
	 * 
	 * @param array com todos os participantes
	 * @return string do tipo <userID1,chave cifrada1>,<userID1,chave cifrada1>,...
	 */
	public String generateParticipantCipheredKeyPair(String[] participantes, Key groupKey, String participanteEspecial, int opType) {

		// Criar uma StringBuilder
		StringBuilder sb = new StringBuilder();

		PublicKey pk = null;
		PublicKey pkNovo = null;
		byte[] chaveCifrada = null;
		byte[] chaveCifradaGajoNovo = null;

		// Percorrer a lista de participantes
		for(int i = 0; i < participantes.length; i++) {
			// Buscar a chave pública de cada participante
			pk = getParticipantPK(participantes[i]);
			if(pk == null) {
				System.out.println("Erro:... Não foi possível obter a chave pública do participante");
				return null;
			}
			// cifrar a chave recebida com a chave pública do participante
			chaveCifrada = sec.wrapKey(groupKey, pk);
			if(chaveCifrada == null) {
				System.out.println("Erro:... Não foi possível fazer wrap da chave de grupo com o ");
				return null;
			}

			// Adicionar <participante,chave cifrada> à StringBuilder
			if(i == (participantes.length - 1) && opType == 1){
				sb.append(participantes[i] + "," + Base64.getEncoder().encodeToString(chaveCifrada));	
			}else{
				sb.append(participantes[i] + "," + Base64.getEncoder().encodeToString(chaveCifrada) + ",");
			} 
			// sb.append(participantes[i] + "," + Base64.getEncoder().encodeToString(chaveCifrada) + ",");
		}

		// Se for optype de addu adiciona-se o novo participante
		if(opType == 0) {
			pkNovo = getParticipantPK(participanteEspecial);
			chaveCifradaGajoNovo = sec.wrapKey(groupKey, pkNovo);
			sb.append(participanteEspecial + "," + Base64.getEncoder().encodeToString(chaveCifradaGajoNovo));
		}	

		return sb.toString();
	}

	/**
	 * Procura o chave pública do participant passado
	 * @param participant
	 * @return PublicKey do participante
	 */
	private PublicKey getParticipantPK(String participant) {

		FileInputStream fis = null;
		Certificate cert = null;
		try {
			fis = new FileInputStream("PubKeys/" + participant + ".cer");
			CertificateFactory cf = CertificateFactory.getInstance("X509");
			cert = cf.generateCertificate(fis);
		} catch (FileNotFoundException | CertificateException e) {
			e.printStackTrace();
		}

		try {
			if(fis != null) fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		PublicKey pk = null;
		if(cert != null) pk = cert.getPublicKey();
		
		return pk;

	}

	/**
	 * Cria uma chave simétrica usando o algoritmo AES e devolve a mesma em formato byte[]
	 * @return byte[] da chave criada 
	 */
	private Key generateKey() {
		// Criar chave simétrica
		KeyGenerator kg = null;
		try {
			kg = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}

		kg.init(128);
		// Chave simétrica
		return kg.generateKey();
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
		
		//Cria uma nova chave secreta
		//obtem todos os participantes ate ao momento que se encontram no grupo
		//obtem o counter corrente de chave do grupo
		//para cada um destes manda <counter corrente>:<ID do membro, nova chave secreta cifrada pelo membro>

		int resultado = -1;
		String participantes = null;
		String[] listaParticipantes = null;

		try {
			// enviar tipo de operacao
			out.writeObject("a");

			// enviar ID do grupo a ser criado:ID do cliente que o pretende criar:
			out.writeObject(userID + ":" + groupID + ":" + senderID);

			//--------------------------------------------------------------------------------------------
			// Obter todos os participantes do grupo separados por ":"
			participantes = (String) in.readObject();
			if(participantes.isEmpty()) {
				System.out.println("Erro:... Não foi possível obter a lista de participantes do grupo");
				return resultado;
			}
			listaParticipantes = participantes.split(":");
			// qntParticipantes = listaParticipantes.length;

			// Criar nova chave simétrica para o grupo
			Key groupKey = generateKey();
			if(groupKey == null) {
				System.out.println("Erro:... Não foi possível criar uma chave simétrica para o novo grupo");
				return resultado;
			}

			// Adicionar o id de cada participante e a chave cifrada 
			// com a chave pública desse participante ja existente + o id do participante a adicionar e a chave cifrada pela chave publica do mesmo
			String retorno = generateParticipantCipheredKeyPair(listaParticipantes, groupKey, userID, 0);

			// Enviar unica string que ficara no ficheiro de chaves do grupo
			// com o aspeto <donoID,chave nova>,<participant1,chave nova>,<participant2,chave nova>,...,<participantNovo,chave nova>
			out.writeObject(retorno);
			
			//--------------------------------------------------------------------------------------------

			// receber o resultado da operacao
			resultado = (int) in.readObject();

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
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
		String participantes = null;
		String[] listaParticipantes = null;
		try {
			// enviar tipo de operacao
			out.writeObject("r");

			// enviar ID do cliente que se quer adicionar ao grupo:ID do grupo:ID do sender
			out.writeObject(userID + ":" + groupID + ":" + senderID);

			//--------------------------------------------------------------------------------------------
			// Obter todos os participantes do grupo separados por ":"
			participantes = (String) in.readObject();
			if(participantes.isEmpty()) {
				System.out.println("Erro:... Não foi possível obter a lista de participantes do grupo");
				return resultado;
			}
			listaParticipantes = participantes.split(":");

			// Criar nova chave simétrica para o grupo
			Key groupKey = generateKey();
			if(groupKey == null) {
				System.out.println("Erro:... Não foi possível criar uma chave simétrica para o novo grupo");
				return resultado;
			}
			
			//remover o participante a remover da listaParticipantes
			List<String> list = new ArrayList<>(Arrays.asList(listaParticipantes));
			list.remove(userID);
			listaParticipantes = list.toArray(new String[0]);


			// Adicionar o id de cada participante e a chave cifrada 
			// com a chave pública desse participante ja existente + o id do participante a adicionar e a chave cifrada pela chave publica do mesmo
			String retorno = generateParticipantCipheredKeyPair(listaParticipantes, groupKey, userID, 1);

			// Enviar unica string que ficara no ficheiro de chaves do grupo
			// com o aspeto <donoID,chave nova>,<participant1,chave nova>,<participant2,chave nova>,...,<participantNovo,chave nova>
			out.writeObject(retorno);
			
			//--------------------------------------------------------------------------------------------

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

		// Enviar parâmetros do pedido
		// Recebe a chave stringfied e cifrada 
		// Transforma em bytes e decifra
		// Cifra a mensagem
		// Envia mensagem
		// Recebe resposta 

		//APAGAR ESTAS LINHAS NO FIM
		//String originalInput = "test input";
		//String encodedString = Base64.getEncoder().encodeToString(originalInput.getBytes());
		//byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
		//String decodedString = new String(decodedBytes);
		//Base64.getMimeDecoder().decode(encodedMime);
		//Base64.getMimeEncoder().encodeToString(encodedAsBytes);

		int resultado = -1;
		
		String chaveSimetricaCifradaRecebida = null;
		byte[] chaveSimetricaCifrada = null;
		Key chaveSimetricaUnWrapped = null;
		byte[] mensagemCifrada = null;
		String mensagemCifradaStringified = null;
		Key chavePriv = null;

		try {
			// enviar tipo de operacao
			out.writeObject("m");

			//------------------------------------------------------
			// enviar groupID:ID do user que fez o pedido SEM a mensagem
			out.writeObject(groupID + ":" + senderID);

			//1.Buscar ao allKeys o counter da linha que tem a chave simetrica que foi wrapped por senderID e a propria chave
			//(ou seja primeiro procurar na linha em que counter das keys é o counter atual
			//se senderID nao estiver na linha final que é a linha do counter atual e porque
			//senderID ja nao faz parte do grupo e entao nao pode mandar mensagem)
			chaveSimetricaCifradaRecebida = (String) in.readObject();

			//2.1 Se a String voltar vazia aborta-se tudo e a operacao correu mal
			if(chaveSimetricaCifradaRecebida.isEmpty()) {
				return resultado;
			}
			
			//2.2.Essa chave vai estar stringified e por isso é preciso voltar a meter em bytes com o encoder base64
			chaveSimetricaCifrada = Base64.getDecoder().decode(chaveSimetricaCifradaRecebida);

			//3.Essa chave ainda esta wrapped com a chave publica do senderID por isso e preciso dar unwrap dela com a chave PRIVADA do senderID
			//3.1.Buscar chave privada do senderID
			chavePriv = sec.getKey(this.keystore, "keystores/" + this.keystore, this.keystorePassword, this.keystorePassword, this.storetype);


			//3.2.Obter chave simetrica unwrapped
			chaveSimetricaUnWrapped = sec.unwrapKey(chaveSimetricaCifrada, keyGenSimAlg, chavePriv);

			//4.O resultado do unwrap vai ser a chave simetrica original e é com essa chave que se vai dar wrap Á MENSAGEM que se quer enviar
			// Encriptar mensagem
			Cipher c = null;
			try {
				c = Cipher.getInstance(this.keyGenSimAlg);
				c.init(Cipher.ENCRYPT_MODE, chaveSimetricaUnWrapped);
				byte[] mensagemEmBytes = mensagem.getBytes();
				c.update(mensagemEmBytes);
				mensagemCifrada = c.doFinal();
			} catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException e2) {
				e2.printStackTrace();
			}

			//------------------------------------------------------

			//5.esta mensagem e enviada
			System.out.println(Base64.getEncoder().encodeToString(mensagemCifrada));
			out.writeObject(mensagemCifrada);
			
			// receber o resultado da operacao
			resultado = (int) in.readObject();

		} catch (IOException | ClassNotFoundException e) {
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
	 * Metodo que gera uma string da chave simetrica cifrada pela chave publica do dono do grupo
	 *
	 * @return Nova instancia de chave simetrica cifrada pela chave publica do dono do grupo
	 */
	public String[] collect(String groupID, String senderID) {
		String[] resposta = null;
		List<String> listaMensagens = new ArrayList<>();
		try {
			// enviar tipo de operacao
			out.writeObject("c");
		
			// enviar groupID:ID do user que fez o pedido
			out.writeObject(groupID + ":" + senderID);

			// receber o resultado da operacao
			resposta = (String[]) in.readObject();

			if(resposta.length == 0) {
				return null;
			}
			
			// Tratar a resposta
			// Obter chave privada do cliente
			Key key = sec.getKey(this.keystore, "keystores/" + this.keystore, this.keystorePassword, this.keystorePassword, this.storetype); 

			// 1. Percorrer o array e tratar cada mensagem
			for(int i = 0; i < resposta.length; i+=3) {
				// Transformar a chave em um array de bytes
				Key unwrappedKey = sec.unwrapKey(Base64.getDecoder().decode(resposta[i+2]), this.keyGenSimAlg, key) ;
				// Decifrar a mensagem
				Cipher c = null;
				try {
					c = Cipher.getInstance(this.keyGenSimAlg);
					c.init(Cipher.DECRYPT_MODE, unwrappedKey);
					c.update(Base64.getDecoder().decode(resposta[i+1]));
					// Adicionar quem enviou a mensagem e a mensagem em si a lista
					String s = new String(c.doFinal());
					listaMensagens.add(resposta[i] + ":" + s);
				} catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException e2) {
					e2.printStackTrace();
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return listaMensagens.toArray(new String[0]);
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
		String[] resposta = null;
		List<String> listaMensagens = new ArrayList<>();
		try {
			// enviar tipo de operacao
			out.writeObject("h");
		
			// enviar groupID:ID do user que fez o pedido
			out.writeObject(groupID + ":" + senderID);

			// receber o resultado da operacao
			resposta = (String[]) in.readObject();

			if(resposta.length == 0) {
				return null;
			}
			
			// Tratar a resposta
			// Obter chave privada do cliente
			Key key = sec.getKey(this.keystore, "keystores/" + this.keystore, this.keystorePassword, this.keystorePassword, this.storetype); 

			// 1. Percorrer o array e tratar cada mensagem
			for(int i = 0; i < resposta.length; i+=3) {
				// Transformar a chave em um array de bytes
				Key unwrappedKey = sec.unwrapKey(Base64.getDecoder().decode(resposta[i+2]), this.keyGenSimAlg, key) ;
				// Decifrar a mensagem
				Cipher c = null;
				try {
					c = Cipher.getInstance(this.keyGenSimAlg);
					c.init(Cipher.DECRYPT_MODE, unwrappedKey);
					c.update(Base64.getDecoder().decode(resposta[i+1]));
					// Adicionar quem enviou a mensagem e a mensagem em si a lista
					String s = new String(c.doFinal());
					listaMensagens.add(resposta[i] + ":" + s);
				} catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | InvalidKeyException | IllegalBlockSizeException e2) {
					e2.printStackTrace();
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return listaMensagens.toArray(new String[0]);
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
		try(FileInputStream kfile = new FileInputStream("keystores/" + this.keystore)) {
			// kstore.load(kfile, this.keystorePassword.toCharArray());
		} catch (IOException e2) {
			e2.printStackTrace();
            System.exit(-1);
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
		
		// // Tratar resposta
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

		// File file = new File("Fotos/" + pathFile);
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
	 * Metodo que pede ao servidor para criar um novo grupo 
	 * sendo o cliente que faz o pedido o seu dono
	 * 
	 * @param senderID usuário corrente
	 * @param nPhotos número de fotos mais recentes a serem devolvidas
	 * @return Lista de Strings contendo o identificador individual de cada foto,
	 * assim como seu número de likes
	 * @param groupID String que representa o ID unico que o grupo vai ter
	 * @param senderID String que representa o ID do user a fazer o pedido de newgroup
	 * @return 0 se o pedido for sucedido e o grupo criado e -1 caso contrario
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
			// Enviar foto
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