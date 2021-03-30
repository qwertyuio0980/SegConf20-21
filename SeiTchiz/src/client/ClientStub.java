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

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
//import javax.xml.bind.DatatypeConverter;


import communication.Com;
import security.Security;

public class ClientStub {

	private static final String keyGenSimAlg = "AES";
	private SSLSocket clientSocket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	
	private int defaultPort = 45678;

	private Com com;
	
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

		com = new Com(clientSocket, in, out);

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
	 * Metodo que devolve a string correspondente usando DatatypeConverter de um array de bytes
	 *
	 * @param data array de bytes a converter
	 * @return String resultado pretendido em formato String
	 */
//	public static String getStringFromBytes(byte[] data) {
//		return javax.xml.bind.DatatypeConverter.printHexBinary(data);
//	}

	/**
	 * Metodo que devolve o array de bytes correspondente usando DatatypeConverter de uma String
	 *
	 * @param data String a converter
	 * @return byte[] resultado pretendido em formato array de bytes
	 */
	public static byte[] getBytesFromString(String info) {
		return DatatypeConverter.parseHexBinary(info);
	}
	
	/**
	 * Metodo que gera uma string da chave simetrica cifrada pela chave publica do dono do grupo
	 *
	 * @return Nova instancia de chave simetrica cifrada pela chave publica do dono do grupo
	 */
	public String generateWrappedStringifiedSimKey() {
		//feito com recurso ao slide 9 do powerpoint de chaves assimetricas

		// Gerar chave simetrica
		KeyGenerator kg = KeyGenerator.getInstance(keyGenSimAlg);
		kg.init(128);
		SecretKey secretKey = kg.generateKey();

		//-----------------
		//ISTO ESTA MAL OU A MAIS???

		Cipher c1 = Cipher.getInstance(keyGenSimAlg);//NAO ESTOU SEGURO SOBRE QUAL ALGORITMO AQUI SE E AES COMO OS OUTROS OU RSA
		c1.init(Cipher.ENCRYPT_MODE, secretKey);
		byte[] keyEncoded = secretKey.getEncoded();//Do outro lado(para desencriptar usa-se o secretkeyspec do powerpoint de chaves simetricas)
		byte[] encripted = c1.doFinal(keyEncoded);
		//-----------------

		// Obter chave publica do dono a partir do seu certificado
		KeyStore kstore = null;
		try {
			kstore = KeyStore.getInstance("JKS");//AQUI E JKS OU JCEKS?
		} catch (KeyStoreException e) {
			e.printStackTrace();
            System.exit(-1);
		}

		try(FileInputStream kfile = new FileInputStream("keystores/" + this.keystore)) {
			kstore.load(kfile, this.keystorePassword.toCharArray());
		} catch (NoSuchAlgorithmException | CertificateException | IOException e) {
			e2.printStackTrace(e);
            System.exit(-1);
		}
        
        Certificate cert = null;
		try {
			cert = (Certificate) kstore.getCertificate(this.keystore);
		} catch (KeyStoreException e1) {
            closeConnection();
            System.exit(-1);
		}
		PublicKey pubk = cert.getPublicKey();

		// Preparar o algoritmo de cifra para cifrar a chave secreta
		Cipher c2 = Cipher.getInstance("RSA");//NAO TENHO 100% CERTEZA MAS AQUI E RSA PORQUE SE TRATA DA CHAVE PUBLICA VINDA DAS KEYSTORES QUE FORAM GUARDADAS COM O TIPO RSA
		c2.init(Cipher.WRAP_MODE, pubk);

		// Cifrar a chave secreta que queremos enviar
		byte[] wrappedKey = c.wrap(secretKey);


		//Finalmente passar este array de bytes que ja se trata de um criptograma numa String que nao estrague o formato que estara (no ficheiro userID de Keys do grupo??? pergunta francisco)
		String chaveAEnviar = getStringFromBytes(wrappedKey);

		return chaveAEnviar;
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
		// Criar chave simétrica
		// Cifra a chave simétrica com a chave pública do servidor
		// Envia para o servidor o identificador do grupo
		// Envia o identificador da chave
		// Cria uma lista contendo o [<ownerID,chave cifrada com chave pública do servidor>]

		int resultado = -1;
		try {
			// enviar tipo de operacao
			out.writeObject("n");

			// enviar ID do grupo a ser criado:ID do cliente que o pretende criar:formato string da chave simetrica cifrada pela chave publica do dono do grupo
			out.writeObject(groupID + ":" + senderID ":" + generateWrappedStringifiedSimKey());

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
	 * do qual o