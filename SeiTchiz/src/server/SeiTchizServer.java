package server;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.IntStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import javax.print.event.PrintEvent;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;


import java.io.FileWriter;
import java.io.Writer;
import java.io.FilenameFilter;

import communication.ComServer;
import security.Security;

public class SeiTchizServer {

	private static final String GLOBALCOUNTERFILE = "files/serverStuff/globalPhotoCounter.txt";
	private int port;

	// Files & paths
	private File filesFolder;
	private File serverStuffFolder;
	private File userStuffFolder;
	private File usersFile;
	private File groupsFolder;
	private File globalPhotoCounterFile;
	private File keysFolder;

	public static void main(String[] args) {
				
		System.setProperty("javax.net.ssl.KeyStore", "keystores" + File.separator + "serverKeyStore");
		System.setProperty("javax.net.ssl.KeyStorePassword","passserver");

		System.out.println("--------------servidor iniciado-----------");
		SeiTchizServer server = new SeiTchizServer();
		if (args.length == 3 && args[0].equals("45678")) {
			server.startServer(args);
		} else {
			System.out.println("Argumento de SeiTchizServer tem de ser obrigatoriamente \"45678\" e tem de ter 3 argumentos" +
			"<port> <keystore> <keystore-password>");
		}
	}

	/**
	 * Metodo que cria a socket de ligacao do lado do servidor e
	 * cria todos os ficheiros e folders de base caso ainda nao tenham sido
	 * criados que o servidor usa para satisfazer pedidos de clientes
	 * 
	 * @param port String que representa o porto onde estara a socket
	 */
	public void startServer(String[] arguments) {

		ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
		SSLServerSocket ss = null;

		try {
			ss = (SSLServerSocket) ssf.createServerSocket(Integer.parseInt(arguments[0]));
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		Writer globalPhotoCounterFile;

		// criacao dos folders e files vazios por default
		try {

			filesFolder = new File("files");
			filesFolder.mkdir();

			serverStuffFolder = new File("files/serverStuff");
			serverStuffFolder.mkdir();

			userStuffFolder = new File("files/userStuff");
			userStuffFolder.mkdir();

			usersFile = new File("files/serverStuff/users.cif");
			usersFile.createNewFile();

			globalPhotoCounterFile = new BufferedWriter(
					new FileWriter(GLOBALCOUNTERFILE, false));
			globalPhotoCounterFile.write("0");
			globalPhotoCounterFile.close();
			
			groupsFolder = new File("files/groups");
			groupsFolder.mkdirs();

			System.out.println("ficheiros de esqueleto do servidor criados");
			System.out.println("------------------------------------------");
		} catch (IOException e) {
			System.out.println("Houve um erro na criação de algum dos folders ou ficheiros de esqueleto");
			System.exit(-1);
		}

		// guardar nos argumentos da thread o keystore e keystore-password e a nova key
		// mandar chave unica de acesso aos ficheiros de servidor

		while (true) {
			try {
				new ServerThread(ss.accept(), arguments[1], arguments[2]).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}


	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		//Canais de comunicacao
		private Socket socket;
		private ObjectOutputStream outStream;
		private ObjectInputStream inStream;


		private Security sec;

		// Server KeyStore & Keys
		protected String serverKeyStore;
		protected String serverAlias;
		protected String serverKeyStorePassword;
		protected String storeType = "JKS";
		
		private final String serverStuffPath = "files/serverStuff/";
		private final String userStuffPath = "files/userStuff/";

		private final String usersFileDec = "files/serverStuff/users.txt"; 
		private final String usersFileCif = "files/serverStuff/users.cif";
		

		ServerThread(Socket inSoc, String serverKeyStore, String serverKeyStorePassword) {
			this.serverKeyStore = "keystores/" + serverKeyStore;
			this.serverAlias = serverKeyStore;
			this.serverKeyStorePassword = serverKeyStorePassword;
			socket = inSoc;
			this.sec = new Security();
			System.out.println("Thread a correr no server para cada um cliente");
		}

		/**
		 * Metodo que contem toda a logica de cada thread criada para cada utilizador ligado ao servidor
		 */
		public void run() throws NullPointerException {
			try {
				outStream = new ObjectOutputStream(socket.getOutputStream());
				inStream = new ObjectInputStream(socket.getInputStream());
				ComServer com = new ComServer(socket, inStream, outStream);
				
				String clientID = null;
				Long sentNonce = 0L;
				Long receivedNonce = 0L;
				int newUserFlag = 1;//por default trata-se de um novo user
				int resultadoLogin = -1;//por default o login nao e bem sucedido
				
				// autenticacao
				try {
					//ler clientID
					clientID = (String) inStream.readObject();

					//criar o nonce e enviar ao cliente
					sentNonce = generateRandomLong();
					outStream.writeObject(sentNonce);

					//ver se clientID ja esta em users.txt (AINDA NAO ESTA FEITO DE MANEIRA CIFRADA)
					newUserFlag = isAuthenticated(clientID);
					
					if (newUserFlag == 1) {
						//caso user ser novo enviar flag com 1
						outStream.writeObject(newUserFlag);
						
						//1.receber nonce
						receivedNonce = (Long) inStream.readObject();

						//2.receber assinatura do nonce com chave privada do client
						byte signatureBytes[] = (byte[]) inStream.readObject();
						
						//3.receber o certificado
						byte certificadoBytes[] = (byte[]) inStream.readObject();
						CertificateFactory cFac = CertificateFactory.getInstance("X509");
						Certificate certificadoConteudo = cFac.generateCertificate(new ByteArrayInputStream(certificadoBytes));
						File certFile = new File("PubKeys/" + clientID + ".cer");//ficheiro cer do novo cliente colocado em PubKeys
						certFile.createNewFile();
						FileOutputStream fosCert = new FileOutputStream(certFile);
						fosCert.write(certificadoConteudo.getEncoded());

						//4.verificar essa assinatura com o certificado que o cliente mandou e que sentNonce e igual a receivedNonce
						PublicKey pk = certificadoConteudo.getPublicKey();
						Signature s = null;
						try {
							s = Signature.getInstance("MD5withRSA");
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try {
							s.initVerify(pk);
						} catch (InvalidKeyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						s.update(longToBytes(receivedNonce));
						if(s.verify(signatureBytes) && sentNonce.equals(receivedNonce)) {
							//adicionar novo elemento ao users.txt(TEM DE SER users.cif MAIS TARDE)
							int res = addUserCertPath(clientID, certFile.getPath());
							if(res == 0) {
								//mandar um boolean a dizer que correu bem o login
								resultadoLogin = 0;
							} else {
								//mandar um boolean a dizer que correu mal o login e apagar ficheiro cert
								certFile.delete();
								resultadoLogin = -1;
							}
						} else {
							//mandar um boolean a dizer que correu mal o login e apagar ficheiro cert
							certFile.delete();
							resultadoLogin = -1;
						}
					} else {
						//caso user ser antigo enviar flag com 0
						outStream.writeObject(newUserFlag);

						//1.receber nonce
						receivedNonce = (Long) inStream.readObject();

						//2.receber assinatura do nonce
						byte signatureBytes[] = (byte[]) inStream.readObject();
						
						File certFile = new File("PubKeys/" + clientID + ".cer");
						FileInputStream fis = new FileInputStream(certFile);
						CertificateFactory cFac = CertificateFactory.getInstance("X509");
						Certificate certificado = cFac.generateCertificate(fis);
						
						//3.verificar o nonce com a chave publica do cliente e as signatures tambem
						PublicKey pk = certificado.getPublicKey();
						Signature s = null;
						try {
							s = Signature.getInstance("MD5withRSA");
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						try {
							s.initVerify(pk);
						} catch (InvalidKeyException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						s.update(longToBytes(receivedNonce));
						if(s.verify(signatureBytes) && sentNonce.equals(receivedNonce)) {
							//adicionar novo elemento ao users.txt(TEM DE SER users.cif MAIS TARDE)
							int res = addUserCertPath(clientID, certFile.toString());
							if(res == 0) {
								resultadoLogin = 0;
							} 
						} 
					}
				outStream.writeObject(resultadoLogin);
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				} catch (CertificateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SignatureException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}


				// executar a operacao pedida pelo cliente
				boolean stop = false;
				String op = null;
				while (!stop) {
					// receber operacao pedida
					try {
						op = (String) inStream.readObject();
					} catch (ClassNotFoundException e1) {
						e1.printStackTrace();
					}

					String aux = null;
					String[] conteudo = null;
					StringBuilder sbMensagem = new StringBuilder();
					String mensagem = null;
					String groupID = null;
					String grupos = null;
					int nPhotos = 0;
					ArrayList arrayAEnviar;

					// realizar a operacao pedida
					switch (op) {
					case "f":

						try {
							// receber <userID que o cliente quer seguir>:<userID do proprio cliente>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// enviar estado da operacao
							outStream.writeObject(follow(conteudo[0], conteudo[1]));

						} catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						break;

					case "u":

						try {
							// receber <userID que o cliente quer deixar de seguir>:<userID do proprio
							// cliente>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// enviar estado da operacao
							outStream.writeObject(unfollow(conteudo[0], conteudo[1]));

						} catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						break;

					case "v":

						try {
							// receber <userID do proprio cliente>
							aux = (String) inStream.readObject();

							// enviar estado da operacao
							outStream.writeObject(viewfollowers(aux));

						} catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						break;

					case "p":

						//incrementar valor do counter em globalPhotoCounter.txt
						File fileCounter = new File(GLOBALCOUNTERFILE);
						int counter = 0; // valor 0 por default so para nao chatear o sonarlint
						
						try {
							Scanner scCounter= new Scanner(fileCounter);
							counter = Integer.parseInt(scCounter.nextLine());
							counter += 1;

							FileWriter fwCounter= new FileWriter(fileCounter, false);
							fwCounter.write(String.valueOf(counter));
							scCounter.close();
							fwCounter.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						try {
							post(clientID);
							com.send(true);
						} catch (ClassNotFoundException e2) {
							com.send(false);	
							e2.printStackTrace();
							System.out.println("Ocorreu um erro enquanto o utilizador " + clientID + " tentava postar uma fotografia.");
						}
						break;

					case "w":

						try {
							// receber senderID do user que fez o pedido
							aux = (String) inStream.readObject();

							// receber nPhotos
							nPhotos = (int) inStream.readObject();

							//executar o wall
							arrayAEnviar = wall(aux, nPhotos);

							//caso de erro
							if(arrayAEnviar.size() < 3) {
								//enviar numero de erro (que e -1)
								outStream.writeObject(-1);

								//enviar o array que neste caso apenas vai conter "1" ou "2"
								outStream.writeObject(arrayAEnviar.get(0));
								
							//caso funcionamento normal
							} else {

								//enviar numero de photoPaths
								outStream.writeObject(arrayAEnviar.size()/3);
								
								for(int j=0; j < arrayAEnviar.size(); j+=3) {
									// Enviar identificador da photo
									outStream.writeObject(arrayAEnviar.get(j));

									// Enviar o numero de likes da photo
									outStream.writeObject(arrayAEnviar.get(j+1));
									
									// Identificador da photo ao com.java
									com.sendFile(arrayAEnviar.get(j+2).toString());
								}
							}
							
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "l":

						try {
							// receber <photoID>
							aux = (String) inStream.readObject();

							// enviar estado da operacao
							outStream.writeObject(like(aux));

						} catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						break;

					case "n":

						try {
							// receber <groupID do grupo a criar>:<userID do proprio cliente>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// enviar estado da operacao
							outStream.writeObject(newgroup(conteudo[0], conteudo[1]));

						} catch (ClassNotFoundException e1) {
							e1.printStackTrace();
						}
						break;

					case "a":

						try {
							// receber <userID a adicionar>:<groupID do grupo>:<senderID do cliente que faz
							// a operacao>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// enviar estado da operacao
							outStream.writeObject(addu(conteudo[0], conteudo[1], conteudo[2]));

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "r":

						try {
							// receber <userID a remover>:<groupID do grupo>:<senderID do cliente que faz a
							// operacao>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// enviar estado da operacao
							outStream.writeObject(removeu(conteudo[0], conteudo[1], conteudo[2]));

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "g":

						try {
							// receber senderID do user que fez o pedido
							aux = (String) inStream.readObject();

							// receber groupID ou "/"
							groupID = (String) inStream.readObject();

							// Chamar funcao ginfo passando os argumentos recebidos e receber a resposta
							if (groupID.equals("/")) {
								grupos = ginfo(aux);
							} else {
								grupos = ginfo(aux,groupID);
							}

							// Enviar resposta
                            if(grupos == null) {
                                outStream.writeObject("");
                            } else {
                                outStream.writeObject(grupos);
                            }
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "m":

						try {
							// receber <groupID>:<ID do user que fez o pedido>:<mensagem>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// colocar input correspondente a msg numa var "mensagem"
							for (int i = 2; i < conteudo.length; i++) {
								sbMensagem.append(conteudo[i] + ":");
							}
							sbMensagem.deleteCharAt(sbMensagem.length() - 1);
							mensagem = sbMensagem.toString();

							// enviar estado da operacao
							outStream.writeObject(msg(conteudo[0], conteudo[1], mensagem));

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "ch":
						// este caso verifica se se pode fazer collect

						try {
							// receber <groupID>:<ID do user que fez o pedido>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// enviar estado da operacao
							outStream.writeObject(canCollectOrHistory(conteudo[0], conteudo[1]));

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "c":
						// este caso faz collect

						try {
							// receber <groupID>:<ID do user que fez o pedido>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// enviar estado da operacao
							outStream.writeObject(collect(conteudo[0], conteudo[1]));

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "h":

						try {
							// receber <groupID>:<ID do user que fez o pedido>
							aux = (String) inStream.readObject();
							conteudo = aux.split(":");

							// enviar estado da operacao
							outStream.writeObject(history(conteudo[0], conteudo[1]));

						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

					case "s":
						stop = true;
						break;

					default:
						// caso default nunca atingido
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("thread do cliente fechada");
			System.out.println("------------------------------------------");
		}

		/**
		 * Metodo que devolve um Long gerado aleatoriamente
		 * 
		 * @return Long aleatorio
		 */
		private Long generateRandomLong() {
			return new Random().nextLong();
		}

		/**
		 * Metodo que verifica se um user ja estao na lista de ficheiros do
		 * servidor
		 * 
		 * @param clientID String que identifica o cliente que se pretende autenticar
		 * @return 0 se autenticacao foi bem sucedida e 1 se registo do clientID não existe no servidor
		 */
		public int isAuthenticated(String clientID) {

			// Verificar se o ficheiro users.cif está vazio
			// Caso esteja apenas ciframos a nova entrada do cliente novo e colocamos
			// no novo ficheiro users.cif
			File usersF = new File("files/serverStuff/users.cif");
			if(usersF.length() == 0) {
				return 1;
			} else {
				// Decriptar o ficheiro users.cif
				// Obter chave privada para decifrar o conteúdo do ficheiro
				Key k = sec.getKey(this.serverAlias, this.serverKeyStore, this.serverKeyStorePassword, this.serverKeyStorePassword, this.storeType);
				// Decifrar ficheiro users.cif e colocar conteúdo no ficheiro users.txt
				sec.decFile("files/serverStuff/users.cif", "files/serverStuff/users.txt", k);
				// Procurar o clientID no aux.txt e devolve o resultado da busca
				return userRegistered(clientID);
			}
		}

		/**
		* Metodo que converte um long em array de bytes
		* 
		* @param x long a converter
		* @return array de bytes convertidos
		*/
		private byte[] longToBytes(long x) {
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
		private long bytesToLong(byte[] bytes) {
			ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
			buffer.put(bytes);
			buffer.flip();//need flip 
			return buffer.getLong();
		}
		
		/**
		 * Procura clientID passado no ficheiro que regista os clientes do sistema
			* @param clientID cliente a ser procurado nos registos
			* @return 0 caso o cliente está registado, 1 caso contrário, ou -1 caso o ficheiro contendo os registos não exista
			* @requires clientID != null && clientID != ""
			*/
		private int userRegistered(String clientID) {

			File usersF = new File("files/serverStuff/users.txt");
			if(!usersF.exists()) {
				return -1;
			}

			String line;
			String[] currentUserPublicKey;
			try (Scanner scanner = new Scanner(usersF)) {
				while (scanner.hasNextLine()) {
					line = scanner.nextLine();
					currentUserPublicKey = line.split(",");
					if (clientID.equals(currentUserPublicKey[0])) {
						// o usuario ja existe na lista de users
						// Deletar o ficheiro contendo os users
						if(!usersF.delete()) {
							System.out.println("Erro ao deletar o ficheiro users.txt");
							return -1;
						}
						return 0;
					}
				}
			} catch (FileNotFoundException e) {
				System.out.println("lista de users/public keys nao existe");
				e.printStackTrace();
			}

			// toda a lista foi percorrida e o user nao foi encontrado
			return 1;
		
		}

		/**
		 * Adiciona uma linha no formato <clientID,password> aos registos de clientes no servidor
		 * e cria os devidos recursos referentes ao novo cliente
		 * @param clientID String representando o cliente a ser adicionado aos registos
		 * @param certPath String representa o caminho para o ficheiro contendo o certificado do clientID
		 * @return 0 se registo foi feito com sucesso, -1 caso contrario
		 */
		public int addUserCertPath(String clientID, String certPath) {

			// Criar recursos para o novo cliente
			int resourcesCreated = createClientResources(clientID);
			// Verificar se os recursos foram criados com sucesso
			if(resourcesCreated == -1) {
				return -1;
			}

			if(this.serverKeyStore == null) {
				System.out.println("serverKeyStore null");
			}
			if(this.serverKeyStorePassword == null) {
				System.out.println("serverKeyStorePassword null");
			}
			if(this.storeType == null) {
				System.out.println("storeType null");
			}

			System.out.println();
			// Obter chave privada para decifrar o conteúdo do ficheiro contendo os registos dos clientes
			Key k = sec.getKey(this.serverAlias, this.serverKeyStore, this.serverKeyStorePassword, this.serverKeyStorePassword, this.storeType);

			// Decifrar ficheiro users.cif e colocar conteúdo no ficheiro users.txt
			if(sec.decFile("files/serverStuff/users.cif", "files/serverStuff/users.txt", k) == -1) {
				return -1;
			}

			// colocar a entrada <clientID,certPath> no ficheiro users.txt
			Writer wr = null;
			try {
				wr = new BufferedWriter(new FileWriter("files/serverStuff/users.txt", true));
				wr.append(clientID + "," + certPath);
			} catch (IOException e) {
				e.printStackTrace();
				try {
					wr.close();
				} catch (IOException | NullPointerException e1) {
					e1.printStackTrace();
				}
				return -1;
			}

			// Obter chave pública para cifrar o ficheiro users.txt
			Certificate cer  = sec.getCert(this.serverAlias, this.serverKeyStore, this.serverKeyStorePassword, this.storeType);
			PublicKey pk = cer.getPublicKey();

			// Fazer closes
			try {
				wr.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Cifrar o ficheiro users.txt como users.cif com a chave pública do servidor
			return sec.cifFilePK("files/serverStuff/users.txt", "files/serverStuff/users.cif", pk);
		}

		/**
		 * Cria os recursos necessários para o clienteID
		 * @param clientID cliente aos quais os recursos pertencerão
		 * @return 0 caso sejam criados todos os recursos com sucesso, -1 caso contrário
		 * @requires clientID != null && clientID != "" 
		 */
		private int createClientResources(String clientID) {

			try {
				File userPage = new File(userStuffPath + clientID);
				userPage.mkdir();

				File photosFolder = new File(userStuffPath + clientID + "/photos");
				photosFolder.mkdir();

				Writer userFollowers = new BufferedWriter(
						new FileWriter(userStuffPath + clientID + "/followers.cif", true));
				userFollowers.close();

				Writer userFollowing = new BufferedWriter(
						new FileWriter(userStuffPath + clientID + "/following.cif", true));
				userFollowing.close();

				Writer userParticipant = new BufferedWriter(
						new FileWriter(userStuffPath + clientID + "/participant.cif", true));
				userParticipant.close();

				Writer userOwner = new BufferedWriter(
						new FileWriter(userStuffPath + clientID + "/owner.cif", true));
				userOwner.close();

			} catch (IOException e) {
				System.out.println("Não foi possível criar os recursos necessários para o cliente corrente");
				e.printStackTrace();
				return -1;
			}

			System.out.println("Dados e ficheiros base do utilizador adicionados ao servidor");

			return 0;

		}

        /**
         * Faz com que o senderID siga o userID
         * 
         * @param userID   Usuario a ser seguido
         * @param senderID Usuario que seguira o userID
         * @return 0 se a operacao foi feita com sucesso e -1 caso contrario
         */
		private int follow(String userID, String senderID) {
			int resultado = -1;
			int encontrado = -1;

			// userID nao pode ter ":" nem pode ser igual ao senderID
			if (userID.contains(":") || userID.contains("-") || userID.contentEquals(senderID)) {
				return resultado;
			}
			
			// Decifrar users.cif e following.cif e followers.cif
			// usersFileDec = "files/serverStuff/users.txt"; 
			// followingFileDec = "files/userStuff/following.txt";
			// followersFileDec = "files/userStuff/followers.txt";
			
			// Decriptar o ficheiro users.cif
			// Obter chave privada para decifrar o conteúdo do ficheiro
			Key k = sec.getKey(this.serverAlias, this.serverKeyStore, this.serverKeyStorePassword, this.serverKeyStorePassword, this.storeType);
			
			// Decifrar ficheiro users.cif e colocar conteúdo no ficheiro users.txt
			sec.decFile(this.usersFileCif, this.usersFileDec, k);

			// Procurar o clientID no aux.txt e devolve o resultado da busca
			encontrado = userRegistered(userID);

			// caso userID existe em users.txt
			if (encontrado == 0) {

				// Obter public key para cifrar ficheiros que serão decifrados a seguir:
				PublicKey pk = sec.getCert(this.serverAlias, this.serverKeyStore, this.serverKeyStorePassword, this.storeType).getPublicKey();

				// Decifrar ficheiro following.cif e colocar conteúdo no ficheiro following.txt
				sec.decFile(this.userStuffPath + senderID + "/following.cif", this.userStuffPath + senderID + "/following.txt", k);

				System.out.println("Decifrou following");

				File sendersFollowingFile = new File(userStuffPath + senderID + "/following.txt");
				Scanner sc;
				try {
					sc = new Scanner(sendersFollowingFile);
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						// caso userID ja se encontre no ficheiro de following de senderID devolver -1
						if (line.contentEquals(userID)) {
							sc.close();
							//dar overwrite ao ficheiro following.cif com o conteudo de following.txt e apagar following.txt
							sec.cifFilePK(this.userStuffPath + senderID + "/following.txt", this.userStuffPath + senderID + "/following.cif", pk);
							return resultado;
						}
					}
					sc.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

				// adicionar userID a following.txt de senderID
				try {
					FileWriter fw = new FileWriter(sendersFollowingFile, true);
					BufferedWriter bw = new BufferedWriter(fw);

					// escrita de userID
					bw.write(userID);
					bw.newLine();

					bw.close();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				//dar overwrite ao ficheiro following.cif com o conteudo de following.txt e apagar following.txt
				sec.cifFilePK(this.userStuffPath + senderID + "/following.txt", this.userStuffPath + senderID + "/following.cif", pk);

				System.out.println("Cifrou following");

				// Decifrar ficheiro followers.cif e colocar conteúdo no ficheiro followers.txt
				sec.decFile(this.userStuffPath + userID + "/followers.cif", this.userStuffPath + userID + "/followers.txt", k);

				System.out.println("Decifrou followers");

				// adicionar senderID a followers.txt de userID
				File userIDsFollowersFile = new File(userStuffPath + userID + "/followers.txt");
				try {
					FileWriter fw = new FileWriter(userIDsFollowersFile, true);
					BufferedWriter bw = new BufferedWriter(fw);

					// escrita de senderID
					bw.write(senderID);
					bw.newLine();

					bw.close();
					fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				//dar overwrite ao ficheiro followers.cif com o conteudo de followers.txt e apagar followers.txt
				sec.cifFilePK(this.userStuffPath + senderID + "/followers.txt", this.userStuffPath + senderID + "/followers.cif", pk);

				System.out.println("Cifrou following");

				resultado = 0;
			}

			return resultado;
		}

		/**
		 * Faz com que o senderID deixe de seguir o userID
		 * 
		 * @param userID   Usuario deixado de seguir por senderID
		 * @param senderID Usuario que deixara de seguir userID
		 * @return 0 se a operacao foi feita com sucesso e -1 caso contrario
		 */
		private int unfollow(String userID, String senderID) {

			int resultado = -1; // TODO: Nao e necessario criar esta var
			boolean encontrado = false;

			// TODO: Tornar a verificacao da existencia do user uma funcao aux
			// userID nao pode ter ":" nem pode ser igual ao senderID
			if (userID.contains(":") || userID.contains("-") || userID.contentEquals(senderID)) {
				return resultado;
			}

			// procurar da lista de users.txt se o userID pretendido existe
			try {
				Scanner scanner = new Scanner(usersFile);
				while (scanner.hasNextLine() && !encontrado) {
					String line = scanner.nextLine();
					String[] lineSplit = line.split(":");
					if (lineSplit[0].contentEquals(userID)) {
						encontrado = true;
					}
				}
				scanner.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			File followingUserFile = new File(userStuffPath + senderID + "/following.txt");
			try (Scanner scfollowingUsers = new Scanner(followingUserFile)) {

				boolean found = false;
				while (scfollowingUsers.hasNextLine()) {
					String lineFollowingUsers = scfollowingUsers.nextLine();
					if (lineFollowingUsers.contentEquals(userID)) {
						found = true;
					}
				}

				if (!found) {
					return resultado;
				}

			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// caso userID existe em users.txt
			// Criar um novo ficheiro temp e copiar toda a informacao do ficheiro following
			if (encontrado) {
				unfollowAux(userID, senderID);
				return 0;
			}

			// caso se percorram todos os userIDs e nao se encontre userID entao o cliente
			// nao o estava a seguir e devolve-se -1
			return resultado;
		}

		
		@SuppressWarnings("null")
		/**
		 * Metodo que trata de escrever e apagar nomes de clientes dos ficheiros que
		 * representam quem esta a seguir o cliente que fez o pedido e os do proprio cliente
		 * que fez o pedido
		 * 
		 * @param userID Usuario deixado de seguir por senderID
		 * @param senderID Usuario que deixara de seguir userID
		 */
		private void unfollowAux(String userID, String senderID) {

			File sendersFollowingFile = new File(userStuffPath + senderID + "/following.txt");
			File sendersFollowingTEMPFile = new File(userStuffPath + senderID + "/followingTemp.txt");
			File usersFollowersFile = new File(userStuffPath + userID + "/followers.txt");
			File usersFollowersTEMPFile = new File(userStuffPath + userID + "/followersTemp.txt");

			try {
				if (sendersFollowingTEMPFile.createNewFile()) {
					// nada acontece aqui
				}
				if (usersFollowersTEMPFile.createNewFile()) {
					// nada acontece aqui
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// ----retirar userID de following de senderID----
			// 1.passar todo o conteudo de following menos o userID pretendido para um
			// ficheiro auxiliar
			try (Scanner scSendersFollowing = new Scanner(sendersFollowingFile);
					FileWriter fwSendersFollowingTEMP = new FileWriter(sendersFollowingTEMPFile);
					BufferedWriter bwSendersFollowingTEMP = new BufferedWriter(fwSendersFollowingTEMP);) {

				while (scSendersFollowing.hasNextLine()) {
					String lineSendersFollowing = scSendersFollowing.nextLine();
					if (!lineSendersFollowing.contentEquals(userID)) {
						bwSendersFollowingTEMP.write(lineSendersFollowing);
						bwSendersFollowingTEMP.newLine();
					}
				}

			} catch (FileNotFoundException e2) {
				e2.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 2.apagar o ficheiro original
			if (sendersFollowingFile.delete()) {
				// nada acontece aqui
			}

			// 3.renomear o ficheiro temporario como following.txt
			if (sendersFollowingTEMPFile.renameTo(sendersFollowingFile)) {
				// nada acontece aqui
			}

			// ----retirar senderID de followers de userID----
			// 1.passar todo o conteudo de followers menos o senderID pretendido para um
			// ficheiro auxiliar
			try (Scanner scUsersFollowers = new Scanner(usersFollowersFile);
					FileWriter fwUsersFollowersTEMP = new FileWriter(usersFollowersTEMPFile);
					BufferedWriter bwUsersFollowersTEMP = new BufferedWriter(fwUsersFollowersTEMP);) {

				while (scUsersFollowers.hasNextLine()) {
					String lineUsersFollowers = scUsersFollowers.nextLine();
					if (!lineUsersFollowers.contentEquals(senderID)) {
						bwUsersFollowersTEMP.write(lineUsersFollowers);
						bwUsersFollowersTEMP.newLine();
					}
				}

			} catch (FileNotFoundException e2) {
				e2.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 2.apagar o ficheiro original
			if (usersFollowersFile.delete()) {
				// nada acontece aqui
			}

			// 3.renomear o ficheiro temporario como followers.txt
			if (usersFollowersTEMPFile.renameTo(usersFollowersFile)) {
				// nada acontece aqui
			}

		}


		/**
		 * Recebe fotos do cliente userName e criar os ficheiros necessários
		* dentro da área deste cliente no servidor
		* 
		* @param userName String que representa o ID do cliente para onde 
		* os ficheiros relacionados a foto sao colocados
		* @throws ClassNotFoundException
		* @throws IOException
		*/
		private void post(String userName) throws ClassNotFoundException, IOException {
	
			String nomeFicheiro = (String) inStream.readObject();
			long tamanho = inStream.readLong();
			int bytesRead = 0;
			int offset = 0;
			byte[] byteArray = new byte[1024];
	
			File gpcFile = new File(GLOBALCOUNTERFILE);
			int globalCounter = 0;
			try(Scanner scGPC = new Scanner(gpcFile);) {
				if(scGPC.hasNextLine()) {
					globalCounter = Integer.parseInt(scGPC.nextLine());
				}
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
	
			File photoFile = new File(userStuffPath + userName + "/photos/photo-" + globalCounter + ".jpg");
			
			if(nomeFicheiro.endsWith(".png")) {
				photoFile = new File(userStuffPath + userName + "/photos/photo-" + globalCounter + ".png");
			}
	
			File likesFile = new File(userStuffPath + userName + "/photos/photo-" + globalCounter + ".txt");
			FileWriter fwLikes = new FileWriter(likesFile, true);
			BufferedWriter bwLikes = new BufferedWriter(fwLikes);
			bwLikes.write("0");
			bwLikes.close();

			try {
				FileOutputStream fos = new FileOutputStream(photoFile);
				while ((offset + 1024) < (int) tamanho) {
					bytesRead = inStream.read(byteArray, 0, 1024);

					// Escrever bytes para o ficheiro que guardará a foto
					fos.write(byteArray, 0, bytesRead);
					fos.flush();

					offset += bytesRead;
				}
		
				if ((1024 + offset) != (int) tamanho) {
					bytesRead = inStream.read(byteArray, 0, (int) tamanho - offset);
					fos.write(byteArray, 0, bytesRead);
					fos.flush();
				}	

				// Criar ficheiro contendo a hash do conteúdo da foto recebida
					// Obter o conteudo da foto como um array de bytes 
				BufferedImage bImage = ImageIO.read(photoFile);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ImageIO.write(bImage, photoFile.getName().split("\\.")[1], bos);
				byte[] data = bos.toByteArray();
					// Obter hash do conteudo da foto
				MessageDigest hashMd = MessageDigest.getInstance("SHA");
				hashMd.update(data);
				byte[] hash = hashMd.digest();
					// Guardar hash no ficheiro 
				File hashFile = new File(userStuffPath + userName + "/photos/photo-" + globalCounter + ".hash");
				FileOutputStream HashFos = null;
				try {
					HashFos = new FileOutputStream(hashFile);
					HashFos.write(hash);
				} catch (FileNotFoundException e) {
					System.out.println("File not found" + e);
					System.exit(-1);
				} finally {
					if (HashFos != null) {
						HashFos.close();
					}
				}
				bos.close();
				fos.close();


			} catch(FileNotFoundException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		
		/**
		 * Metodo que devolve uma String contendo todos os seguidores do
		 * cliente que fez o pedido
		 * 
		 * @param senderID Usuario que quer ver os seus seguidores
		 * @return String com todos os seguidores ou string vazia se
		 * o cliente nao tiver seguidores
		 */
		private String viewfollowers(String senderID) {

			// procurar no folder de users no do sender se o ficheiro followers.txt esta
			// vazio
			File sendersFollowersFile = new File(userStuffPath + senderID + "/followers.txt");
			if (sendersFollowersFile.length() == 0) {
				// caso esteja vazio devolver esta string especifica
				return "";
			}

			StringBuilder sb = new StringBuilder();
			Scanner sc;
			try {
				sc = new Scanner(sendersFollowersFile);
				while (sc.hasNextLine()) {
					String line = sc.nextLine();
					sb.append(line + "\n");
				}
				sc.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			// devolver string que contem todos os followers de senderID separados por \n
			return sb.toString();
		}

		/**
		 * Criar um diretorio com o nome senderID:groupID caso nao exita, dentro estao
		 * contidos as informacoes do grupo Cada diretorio de grupo contera dois
		 * diretorios, um que guardara as mensagens e outro que guardara os
		 * participantes
		 * 
		 * @param groupID  no do grupo a ser criado
		 * @param senderID usuario dono do grupo return 0 caso o grupo tenha sido criado
		 *                 com sucesso, -1 caso ja exista um grupo com o groupID passado
		 *                 ou 1 caso haja algum erro no processo
		 * @return 0 se for criado o grupo com sucesso e -1 caso contrario
		 */
		private int newgroup(String groupID, String senderID) {
			String senderIDgroupID = senderID + "-" + groupID;


			// verificar se dentro do folder dos grupos existe um folder com o nome 
			// "*-groupID"

			//1. Obter nomes dos ficheiros e diretorios no diretorio grupos, aplicando
			// o filtro que aceita Strings que terminam com "-groupID"
			
			// Criar filter
			FilenameFilter filter = new FilenameFilter(){
				public boolean accept(File f, String name) {
					return name.endsWith(groupID);
				}	
			};

			// Obter nomes dos ficheiros e diretorios válidos
			File[] files = groupsFolder.listFiles(filter);

			File ownerGroupFolder = new File("files/groups/" + senderIDgroupID);
			
			if (files.length == 0) {
				// caso nao existir criar esse folder
				if (ownerGroupFolder.mkdir()) {
					// Criar ficheiros counter.txt e participants.txt
					File counter = new File("files/groups/" + senderIDgroupID + "/counter.txt");
					File participants = new File("files/groups/" + senderIDgroupID + "/participants.txt");
					try {
						counter.createNewFile();
						participants.createNewFile();

						// Inicializar o ficheiro counter.txt com um inteiro 0, indicando que ainda nao
						// ha mensagens no grupo
						FileWriter fwCounter = new FileWriter(counter, true);
						BufferedWriter bwCounter = new BufferedWriter(fwCounter);
						bwCounter.write("0");
						bwCounter.newLine();
						bwCounter.close();

						// Inicializar o ficheiro participants.txt no folder groups com o primeiro
						// participante: senderID
						FileWriter fwParticipants = new FileWriter(participants, true);
						BufferedWriter bwParticipants = new BufferedWriter(fwParticipants);
						bwParticipants.write(senderID);
						bwParticipants.newLine();
						bwParticipants.close();
					} catch (IOException e) {
						e.printStackTrace();
					}

					// Adicionar o nome do grupo ao ficheiro owner.txt do senderID
					try {
						File fUserOwner = new File(userStuffPath + senderID + "/owner.txt");
						FileWriter fwUserOwner = new FileWriter(fUserOwner, true);
						BufferedWriter bwUserOwner = new BufferedWriter(fwUserOwner);
						bwUserOwner.write(groupID);
						bwUserOwner.newLine();
						bwUserOwner.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// Adicionar o dono do grupo-nome do grupo ao ficheiro participant.txt do
					// senderID
					try {
						File fUserParticipant = new File(userStuffPath + senderID + "/participant.txt");
						FileWriter fwUserParticipant = new FileWriter(fUserParticipant, true);
						BufferedWriter bwUserParticipant = new BufferedWriter(fwUserParticipant);
						bwUserParticipant.write(senderID + "-" + groupID);
						bwUserParticipant.newLine();
						bwUserParticipant.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					return 0;
				} else {
					return 1;
				}
			} else {
				// Ja existe um grupo com senderID e groupID passados
				return -1;
			}
		}

		/**
		 * Adiciona o utilizador userID como membro do grupo groupID. Apenas os donos
		 * dos grupos podem adicionar utilizadores aos seus grupos
		 * 
		 * @param userID   usuario a ser adicionado ao grupo
		 * @param groupID  grupo ao qual o usuario sera adicionado
		 * @param senderID usuario atual
		 * @return 0 caso sucesso ao adicionar o usuario ao grupo, -1 caso contrario
		 */
		private int addu(String userID, String groupID, String senderID) {

			File groupFolder = new File("files/groups/" + senderID + "-" + groupID);
			File counterFile = new File("files/groups/" + senderID + "-" + groupID + "/counter.txt");
			File groupMembersFile = new File("files/groups/" + senderID + "-" + groupID + "/participants.txt");
			File participantFile = new File(userStuffPath + userID + "/participant.txt");

			if (!groupFolder.exists() || !participantFile.exists()) {
				// se senderID nao tiver o folder com nome groupID
				// ou se o ficheiro participant.txt nao existe no folder do userID
				// tambem devolver -1(porque isto significa que o userID inserido nao
				// corresponde a nenhum user existente)
				return -1;
			}

			// Verifica se o userID ja participa do grupo
			try {
				Scanner scGroupMembers = new Scanner(groupMembersFile);
				while (scGroupMembers.hasNextLine()) {
					String lineMember = scGroupMembers.nextLine();
					if (lineMember.contentEquals(userID)) {
						scGroupMembers.close();
						return -1;
					}
				}
				scGroupMembers.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Verifica se o grupo ja esta nos registros de grupos ao qual o userID pertence
			try {
				Scanner scParticipant = new Scanner(participantFile);
				while (scParticipant.hasNextLine()) {
					String lineParticipant = scParticipant.nextLine();
					if (lineParticipant.contentEquals(groupID)) {
						scParticipant.close();
						return -1;
					}
				}
				scParticipant.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// proceder á adicao do userID ao grupo que implica:
			// 1.colocar senderID-groupID no ficheiro participant.txt do userID
			try {
				FileWriter fwParticipant = new FileWriter(participantFile, true);
				BufferedWriter bwParticipant = new BufferedWriter(fwParticipant);
				bwParticipant.write(senderID + "-" + groupID);
				bwParticipant.newLine();
				bwParticipant.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 2.colocar userID no ficheiro members.txt do grupo
			try {
				FileWriter fwMember = new FileWriter(groupMembersFile, true);
				BufferedWriter bwMember = new BufferedWriter(fwMember);
				bwMember.write(userID);
				bwMember.newLine();
				bwMember.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return 0;
		}

		/**
		 * Remove o utilizador userID do grupo indicado (groupID) caso o senderID seja o
		 * dono do grupo
		 * 
		 * @param userID   usuario a ser removido do grupo
		 * @param groupID  grupo do qual o usuario sera removido
		 * @param senderID usuario atual
		 * @return 0 caso o usuario tenha sido removido do grupo com sucesso, -1 caso
		 *         contrario
		 */
		private int removeu(String userID, String groupID, String senderID) {
			// Remove o userID do ficheiro participants.txt no diretorio do grupo
			// Remove o senderID-groupID do ficheiro participant.txt no diretorio userID

			File groupMembersFile = new File("files/groups/" + senderID + "-" + groupID + "/participants.txt");
			File participantFile = new File(userStuffPath + userID + "/participant.txt");

			// Verifica se ha um grupo com nome senderID-groupID e se o userID existe
			if (!groupMembersFile.exists() || !participantFile.exists() || userID.contentEquals(senderID)) {
				return -1;
			}

			// Percorre o ficheiro participants.txt e ir colocando o conteudo do mesmo
			// num ficheiro temporario

			File groupMembersTEMPFile = new File(
					"files/groups/" + senderID + "-" + groupID + "/participantsTemp.txt");

			try {
				if (groupMembersTEMPFile.createNewFile()) {
					// nada acontece aqui
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(-1);
			}

			// ----retirar userID de participants do grupo----
			// 1.passar todo o conteudo de participants menos o userID para um ficheiro
			// auxiliar
			try (Scanner scgroupMembers = new Scanner(groupMembersFile);
					FileWriter fwgroupMembersTEMP = new FileWriter(groupMembersTEMPFile);
					BufferedWriter bwgroupMembersTEMP = new BufferedWriter(fwgroupMembersTEMP);) {

				while (scgroupMembers.hasNextLine()) {
					String line = scgroupMembers.nextLine();
					if (!line.contentEquals(userID)) {
						bwgroupMembersTEMP.write(line);
						bwgroupMembersTEMP.newLine();
					}
				}

			} catch (FileNotFoundException e2) {
				e2.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			// 2.apagar o ficheiro original
			if (groupMembersFile.delete()) {
				// nada acontece aqui
			}

			// 3.renomear o ficheiro temporario como following.txt
			if (groupMembersTEMPFile.renameTo(groupMembersFile)) {
				// nada acontece aqui
			}

			// ----retirar o senderID-groupID do participant.txt do usuario userID----
			// 1.passar todo o conteudo de participant.txt menos o senderID-groupID
			// pretendido para um ficheiro auxiliar

			File participantTEMPFile = new File("files/groups/" + senderID + "-" + groupID + "/participantTemp.txt");

			try {
				if (participantTEMPFile.createNewFile()) {
					// nada acontece aqui
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(-1);
			}

			try (Scanner scParticipant = new Scanner(participantFile);
					FileWriter fwParticipantTEMP = new FileWriter(participantTEMPFile);
					BufferedWriter bwParticipantTEMP = new BufferedWriter(fwParticipantTEMP);) {

				while (scParticipant.hasNextLine()) {
					String line = scParticipant.nextLine();
					if (!line.contentEquals(senderID)) {
						bwParticipantTEMP.write(line);
						bwParticipantTEMP.newLine();
					}
				}

			} catch (FileNotFoundException e2) {
				e2.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			// 2.apagar o ficheiro original
			if (participantFile.delete()) {
				// nada acontece aqui
			} else {
				// nada acontece aqui
				return -1;
			}

			// 3.renomear o ficheiro temporario como followers.txt
			if (participantTEMPFile.renameTo(participantFile)) {
				// nada acontece aqui
			} else {
				// nada acontece aqui
				return -1;
			}

			return 0;
		}

		/**
		 * Devolve todos os grupos que userID eh dono e dos quais participa Caso nao
		 * seja participante ou dono de nenhum grupo isto eh assinalado
		 * 
		 * @param userID usuario a ser usado na busca por grupos
		 * @return Optional<List> contendo todos os grupos achados
		 */
		private String ginfo(String userID) {
			// Ler os ficheiros owner.txt e participant.txt
			// 1. Criar um StringBuilder e ir colocando as linhas do ficheiro owner.txt
			// Os grupos dos quais eh dono serao os primeiros a serem adicionados a SB.
			// 2. Em seguida serao acionados os grupos do qual o usuario eh participante (do
			// ficheiro participant.txt)
			// caso o mesmo participe em algum grupo. Caso contrario nao serao adicionados
			// nenhum grupo a SB.
			// 3. Por fim, sera criado uma instancia Optional<String> usando a SB dos
			// grupos.
			// A mesma sera retornada.

			StringBuilder grupos = new StringBuilder();

            File owner = new File(userStuffPath + userID + "/owner.txt");
            File participant = new File(userStuffPath + userID + "/participant.txt");
            

			// 1.
			try (Scanner scOwner = new Scanner(owner)) {
				while (scOwner.hasNextLine()) {
					String line = scOwner.nextLine();
					grupos.append(line + ",");
				}
				scOwner.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

			// 2.
			try (Scanner scParticipant = new Scanner(participant)) {
				while (scParticipant.hasNextLine()) {
					String line = scParticipant.nextLine();
					grupos.append(line + ",");
				}
				scParticipant.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

			// 3.
			return grupos.toString();
		}

        /**
         * Devolve o dono do groupID e dos membros do groupID,
         * caso senderID seja dono ou participante do groupID.
		 * 
         * @param senderID usuário corrente 
         * @param groupID grupo a ser investigado
         * @return String contendo os nomes do dono do groupID
         * e respetivos participantes. O nome do dono será seguido pelo carácter "/" e
         * eventualmente os nomes dos participantes.
         */
		private String ginfo(String senderID, String groupID) {
            StringBuilder ownerPaticipants = new StringBuilder();
            File owner = new File(userStuffPath + senderID + "/owner.txt");
            File participant = new File(userStuffPath + senderID+ "/participant.txt");
            File groupParticipants = null;
            String fileName = null;

            // 1. Procurar no ficheiro owner.txt pelo groupID
			try (Scanner scOwner = new Scanner(owner)) {
				while (scOwner.hasNextLine()) {
					String line = scOwner.nextLine();
                    if(line.equals(groupID)) {
                        fileName = senderID + "-" + line;
                    }
				}
				scOwner.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

            if(fileName == null) {
                // 2. Procurar no ficheiro participant.txt pelo groupID
                try (Scanner scParticipant = new Scanner(participant)) {
                    while (scParticipant.hasNextLine()) {
                        String line = scParticipant.nextLine();
                        String[] lineAux = line.split("-");
                        if(lineAux[1].equals(groupID)) {
                            fileName = line;
                        }
                    }
                    scParticipant.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }

            // Caso não tenha sido encontrado o grupo nos ficheiros do senderID devolver um estado de erro
            if(fileName == null) {
                return null;
            }

            groupParticipants = new File("files/groups/" + fileName + "/participants.txt");

			// 1.
			try (Scanner scParticipants = new Scanner(groupParticipants)) {
				while (scParticipants.hasNextLine()) {
					String line = scParticipants.nextLine();
                    ownerPaticipants.append(line + ',');
				}
				scParticipants.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

			return ownerPaticipants.toString();
        }

		/**
		 * Metodo que trata de colocar uma mensagem nova do cliente que fez
		 * o pedido no grupo
		 * 
		 * @param groupID String com o ID do grupo
		 * @param senderID String com o ID do cliente que fez o pedido msg
		 * @param mensagem String com a mensagem em si
		 * @return 0 se a operacao foi realizada com sucesso, -1 caso contrario
		 */
		private int msg(String groupID, String senderID, String mensagem) {

			File senderParticipantFile = new File(userStuffPath + senderID + "/participant.txt");
			try (Scanner scSenderParticipant = new Scanner(senderParticipantFile)) {

				while (scSenderParticipant.hasNextLine()) {
					String lineSenderParticipant = scSenderParticipant.nextLine();
					// pode ser o grupo procurado ou outro com o mesmo nome mas owner diferente
					if (lineSenderParticipant.contains(groupID) && isCorrectGroup(lineSenderParticipant, senderID)) {
						msgAux(lineSenderParticipant, senderID, mensagem);
						return 0;
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return -1;
		}

		/**
		 * Metodo auxiliar onde se efetua de facto a criacao do folder da mensagem
		 * com todos os subficheiros relacionados a mesma dentro do folder do grupo
		 * 
		 * @param groupFolder String com o par userID-GroupID em que userID e o 
		 * identificador do dono do grupo e groupID e o identificador do grupo
		 * @param senderID String com o ID do cliente que fez o pedido msg
		 * @param mensagem String com a propria mensagem
		 */
		private void msgAux(String groupFolder, String senderID, String mensagem) {
			// metodo onde se envia mesmo a mensagem

			// 1.incrementar valor do counter em counter.txt
			File fileCounter = new File("files/groups/" + groupFolder + "/counter.txt");
			int counter = 0; // valor 0 por default so para nao chatear o sonarlint
			Scanner scCounter;
			FileWriter fwCounter;
			try {
				scCounter = new Scanner(fileCounter);
				counter = Integer.parseInt(scCounter.nextLine());
				counter += 1;
				scCounter.close();

				fwCounter = new FileWriter(fileCounter, false);
				fwCounter.write(String.valueOf(counter));
				fwCounter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 2.criar o msg<current counter value> folder
			File fMsg = new File("files/groups/" + groupFolder + "/msg" + counter);
			fMsg.mkdir();

			// 3.criar o content.txt com senderID como 1a linha
			// seguido pela mensagem na linha seguinte
			File fContent = new File("files/groups/" + groupFolder + "/msg" + counter + "/content.txt");
			try (FileWriter fwContent = new FileWriter(fContent);
					BufferedWriter bwContent = new BufferedWriter(fwContent);) {
				bwContent.write("-" + senderID + ":" + mensagem);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 4.criar o seenby.txt e nao colocar la nada
			File fSeenBy = new File("files/groups/" + groupFolder + "/msg" + counter + "/seenBy.txt");
			try {
				fSeenBy.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// 5.criar o notseenby.txt e colocar todos os participants no ficheiro incluindo
			// o senderID
			StringBuilder sbParticipants = new StringBuilder();
			File fParticipants = new File("files/groups/" + groupFolder + "/participants.txt");
			try (Scanner scParticipants = new Scanner(fParticipants)) {
				while (scParticipants.hasNextLine()) {
					String lineParticipants = scParticipants.nextLine();
					sbParticipants.append(lineParticipants + "\n");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			File fNotSeenBy = new File("files/groups/" + groupFolder + "/msg" + counter + "/notSeenBy.txt");
			try (FileWriter fwNotSeenBy = new FileWriter(fNotSeenBy);
					BufferedWriter bwNotSeenBy = new BufferedWriter(fwNotSeenBy);) {
				bwNotSeenBy.write(sbParticipants.toString());
				bwNotSeenBy.newLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * Metodo que verifica se o ID do grupo em questao se encontra no ficheiro de participantes
		 * do cliente que fez o pedido e se o ID desse cliente se encontra no ficheiro de participantes
		 * do proprio grupo
		 * 
		 * @param ownerGroupPair String que contem o par userID-groupID sendo 
		 * userID o dono do grupo com o groupID
		 * @param senderID String que representa o cliene que fez o pedido
		 * @return true se o grupo passado como argumento e o correto e false caso contrario
		 */
		private boolean isCorrectGroup(String ownerGroupPair, String senderID) {
			File groupParticipantsFile = new File("files/groups/" + ownerGroupPair + "/participants.txt");

			// grupo tem de existir
			if (!groupParticipantsFile.exists()) {
				return false;
			}

			// percorre os participants.txt do folder userID-groupID a procura do senderID
			try (Scanner scGroupParticipants = new Scanner(groupParticipantsFile)) {
				while (scGroupParticipants.hasNextLine()) {
					String lineGroupParticipants = scGroupParticipants.nextLine();
					if (lineGroupParticipants.contentEquals(senderID)) {
						return true;
					}
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return false;
		}

		/**
		 * Metodo que verifica se e possivel ou nao fazer os pedidos collect ou
		 * history por um especifico cliente sem haver problemas
		 * 
		 * @param groupID String com o ID do grupo a verificar se se pode fazer
		 * collect ou history
		 * @param senderID String com o ID do cliente que fez o pedido canCollectOrHistory
		 * @return 0 se for possivel fazer o pedido collect ou o pedido history, -1 caso contrario
		 */
		private int canCollectOrHistory(String groupID, String senderID) {

			File senderParticipantFile = new File(userStuffPath + senderID + "/participant.txt");
			try (Scanner scSenderParticipant = new Scanner(senderParticipantFile)) {

				while (scSenderParticipant.hasNextLine()) {
					String lineSenderParticipant = scSenderParticipant.nextLine();
					// pode ser o grupo procurado ou outro com o mesmo nome mas owner diferente
					if (lineSenderParticipant.contains(groupID) && isCorrectGroup(lineSenderParticipant, senderID)) {
						return 0;
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return -1;
		}

		/**
		 * Metodo que devolve uma lista de strings contendo cada uma um par
		 * userID:mensagem em que userID e quem criou a mensagem e mensagem o
		 * conteudo da mesma para cada mensagem que o cliente ainda nao leu
		 * 
		 * @param groupID String que identifica o grupo
		 * @param senderID String que identifica o cliente que fez o pedido collect
		 * @return lista de strings contendo cada uma um par
		 * userID:mensagem em que userID e quem criou a mensagem e mensagem o
		 * conteudo da mesma e se o cliente nao tem mensagens nenhumas por ler
		 * devolve "-empty"
		 */
		private String[] collect(String groupID, String senderID) {

			String[] listaMensagensDefault = { "-empty" };
			String parUserGroup = "";

			// 1.aceder ao folder do grupo
			File senderParticipantFile = new File(userStuffPath + senderID + "/participant.txt");
			try (Scanner scSenderParticipant = new Scanner(senderParticipantFile)) {

				while (scSenderParticipant.hasNextLine()) {
					String lineSenderParticipant = scSenderParticipant.nextLine();

					if (lineSenderParticipant.contains(groupID) && isCorrectGroup(lineSenderParticipant, senderID)) {
						parUserGroup = lineSenderParticipant;
						break;
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// o canCollectOrHistory ja garante que se vai entrar neste if
			if (!parUserGroup.isEmpty()) {
				File fileCounter = new File("files/groups/" + parUserGroup + "/counter.txt");
				int counter = 0;
				Scanner scCounter;
				try {
					scCounter = new Scanner(fileCounter);
					counter = Integer.parseInt(scCounter.nextLine());
					scCounter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 2.se o valor de counter for maior que 0
				// percorrer cada folder de mensagens e em cada um deles faz:
				if (counter > 0) {
					StringBuilder sbAllMsgs = new StringBuilder();

					for (int i = 1; i <= counter; i++) {
						File currentContentFile = new File(
								"files/groups/" + parUserGroup + "/msg" + i + "/content.txt");
						File currentNotSeenByFile = new File(
								"files/groups/" + parUserGroup + "/msg" + i + "/notSeenBy.txt");
						File currentSeenByFile = new File(
								"files/groups/" + parUserGroup + "/msg" + i + "/seenBy.txt");
						boolean msgUnread = false;

						try (Scanner scNotSeenBy = new Scanner(currentNotSeenByFile)) {
							while (scNotSeenBy.hasNextLine()) {
								String lineUser = scNotSeenBy.nextLine();
								if (lineUser.contentEquals(senderID)) {
									msgUnread = true;
									break;
								}
							}
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						// se encontrar o senderID no respetivo notseenby.txt
						if (msgUnread) {

							// 1.tira o seu ID de notseenby.txt
							File currentNotSeenByTEMPFile = new File(
									"files/groups/" + parUserGroup + "/msg" + counter + "/notSeenByTemp.txt");
							try {
								if (currentNotSeenByTEMPFile.createNewFile()) {
									// nada acontece aqui
								}
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

							try (Scanner scNotSeenBy = new Scanner(currentNotSeenByFile);
									FileWriter fwNotSeenByTEMP = new FileWriter(currentNotSeenByTEMPFile);
									BufferedWriter bwNotSeenBy = new BufferedWriter(fwNotSeenByTEMP);) {

								while (scNotSeenBy.hasNextLine()) {
									String lineUser = scNotSeenBy.nextLine();
									if (!lineUser.contentEquals(senderID)) {
										bwNotSeenBy.write(lineUser);
										bwNotSeenBy.newLine();
									}
								}

							} catch (FileNotFoundException e2) {
								e2.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}

							if (currentNotSeenByFile.delete()) {
								// nada acontece aqui
							}

							if (currentNotSeenByTEMPFile.renameTo(currentNotSeenByFile)) {
								// nada acontece aqui
							}

							// 2.coloca o seu ID no seenby.txt
							try (FileWriter fw = new FileWriter(currentSeenByFile, true);
									BufferedWriter bw = new BufferedWriter(fw);) {
								// escrita de senderID
								bw.write(senderID);
								bw.newLine();
							} catch (IOException e) {
								e.printStackTrace();
							}

							// 3.faz append do conteudo de content.txt a stringbuilder
							try (Scanner scContent = new Scanner(currentContentFile);) {
								while (scContent.hasNextLine()) {
									String lineContent = scContent.nextLine();
									sbAllMsgs.append(lineContent);
								}
							} catch (FileNotFoundException e2) {
								e2.printStackTrace();
							}
						}

						// se nao encontrar o senderID no respetivo notseenby.txt passa para prox folder
						// msg
					}

					if (sbAllMsgs.length() != 0) {
						// primeiro apagar o "-" inicial do primeiro user
						sbAllMsgs.deleteCharAt(0);

						// finalmente passar do stringbuilder para string e fazer split com - para
						// construir o String[] a enviar
						// System.out.println(sbAllMsgs.toString().split("-"));
						return sbAllMsgs.toString().split("-");
					}
				}
			}

			// 3. se counter.txt tinha valor 0 ou sefoi encontrado senderID em nenhum
			// notseenby.txt devolve
			// o array de Strings contendo apenas -empty
			return listaMensagensDefault;
		}

		/**
		 * Metodo que devolve uma lista de strings contendo cada uma um par
		 * userID:mensagem em que userID e quem criou a mensagem e mensagem o
		 * conteudo da mesma para cada mensagem que o cliente ja leu
		 * 
		 * @param groupID String que identifica o grupo
		 * @param senderID String que identifica o cliente que fez o pedido history
		 * @return lista de strings contendo cada uma um par
		 * userID:mensagem em que userID e quem criou a mensagem e mensagem o
		 * conteudo da mesma e se o cliente nao tem mensagens nenhumas ja lidas
		 * devolve "-empty"
		 */
		private String[] history(String groupID, String senderID) {

			String[] listaMensagensDefault = { "-empty" };
			String parUserGroup = "";

			// 1.aceder ao folder do grupo
			File senderParticipantFile = new File(userStuffPath + senderID + "/participant.txt");
			try (Scanner scSenderParticipant = new Scanner(senderParticipantFile)) {

				while (scSenderParticipant.hasNextLine()) {
					String lineSenderParticipant = scSenderParticipant.nextLine();

					if (lineSenderParticipant.contains(groupID) && isCorrectGroup(lineSenderParticipant, senderID)) {
						parUserGroup = lineSenderParticipant;
						break;
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// o canCollectOrHistory ja garante que se vai entrar neste if
			if (!parUserGroup.isEmpty()) {
				File fileCounter = new File("files/groups/" + parUserGroup + "/counter.txt");
				int counter = 0;
				Scanner scCounter;
				try {
					scCounter = new Scanner(fileCounter);
					counter = Integer.parseInt(scCounter.nextLine());
					scCounter.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// 2.se o valor de counter for maior que 0
				// percorrer cada folder de mensagens e em cada um deles faz:
				if (counter > 0) {
					StringBuilder sbAllMsgs = new StringBuilder();

					for (int i = 1; i <= counter; i++) {
						File currentContentFile = new File(
								"files/groups/" + parUserGroup + "/msg" + i + "/content.txt");
						File currentSeenByFile = new File(
								"files/groups/" + parUserGroup + "/msg" + i + "/seenBy.txt");
						boolean msgRead = false;

						try (Scanner scSeenBy = new Scanner(currentSeenByFile)) {
							while (scSeenBy.hasNextLine()) {
								String lineUser = scSeenBy.nextLine();
								if (lineUser.contentEquals(senderID)) {
									msgRead = true;
									break;
								}
							}
						} catch (FileNotFoundException e) {
							// 
							e.printStackTrace();
						}

						// se encontrar o senderID no respetivo seenby.txt
						if (msgRead) {

							// faz append do conteudo de content.txt a stringbuilder
							try (Scanner scContent = new Scanner(currentContentFile);) {
								while (scContent.hasNextLine()) {
									String lineContent = scContent.nextLine();
									sbAllMsgs.append(lineContent);
								}
							} catch (FileNotFoundException e2) {
								e2.printStackTrace();
							}
						}

						// se nao encontrar o senderID no respetivo seenby.txt passa para prox folder
						// msg
					}

					if (sbAllMsgs.length() != 0) {
						// primeiro apagar o "-" inicial do primeiro user
						sbAllMsgs.deleteCharAt(0);

						// finalmente passar do stringbuilder para string e fazer split com - para
						// construir o String[] a enviar
						// System.out.println(sbAllMsgs.toString().split("-"));
						return sbAllMsgs.toString().split("-");
					}
				}
			}

			// 3. se counter.txt tinha valor 0 ou se foi encontrado senderID em nenhum
			// seenby.txt devolve
			// o array de Strings contendo apenas -empty
			return listaMensagensDefault;
		}

		/**
		 * Retorna as nPhotos mais recentes dos usuários seguidos por senderID bem como o número
		 * de likes em cada fotografia.
		 * Caso existam menos de nPhotos de um certo usuário disponíveis, são retornadas as que estão.
		 * Caso o usuário seguido não tenha nenhuma foto, isto será assinalado
		 * 
		 * @param senderID String designando o usuário corrente
		 * @param nPhotos int represntando o número de fotos mais recentes a serão retornadas
		 * @return ArrayList com cada fotoID, numero de likes e path dessa foto das mais recentes de
		 * users que o senderID segue
		 */
		private ArrayList wall(String senderID, int nPhotos) {
			// LOGICA:
			// 1.Buscar todos as fotos de todos os usuarios seguidos
			// 2.Criar uma lista com os counters das fotos
			// 3.Obter o maior indice
			// 4.Buscar as fotos no intervalo: range(<maior indice>,<maior indice> - nPhotos)
			// 5.Fazer um loop pra percorrer os counters no intervalo
			//  5.2.Verificar se i nao eh menor que o numero de fotos no array de fotos obtido
			// 	5.1.Buscar a foto com counter i
			// 		5.2.Caso nao exista foto com counter i, buscar foto counter i-1, pular uma posicao da lista de counters
			// 		5.3.Adicionar foto ao array retorno com as informacoes: <photoID>, <likes>, <filepath>
			// 6.Retornar array com todas as fotos validas
			// TODO: verificar se podemos representar o photoID como: <userID>-<photo><counter>

			// codigos de resposta de erro
			// "1" = senderID não segue nenhum usuário
			// "2" = todos os usuarios que o senderID segue nao tem fotos

			//inicializar o arraylist que sera devolvido e array a filtrar
			ArrayList<String> retorno = new ArrayList<>();
			ArrayList<String> arrayComTudo = new ArrayList<>();
			
			//ter todos os users que o senderID segue num arraylist
			File currentUserFollowingFile = new File(userStuffPath + senderID + "/following.txt");
			ArrayList<String> following = new ArrayList<>();
			try (Scanner scCurrentUserFollowing = new Scanner(currentUserFollowingFile)) {
				while (scCurrentUserFollowing.hasNextLine()) {
					String line = scCurrentUserFollowing.nextLine();
                    following.add(line);
				}
				scCurrentUserFollowing.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}

			//verificar se o senderID não segue nenhum usuário
			if(following.isEmpty()) {
				retorno.add("1");
			} else {
				//percorrer cada usuario que senderID segue
				for(int i = 0; i < following.size(); i++) {

					//obter photos do user following.get(i)
					File currentUserPhotoFolder = new File(userStuffPath + following.get(i) + "/photos");
					FileFilter filterPhotos = new FileFilter() {
						public boolean accept(File f) {
							//aceitam se so fotos nos formatos jpg e png
							return f.getName().endsWith("jpg") || f.getName().endsWith("png");
						}	
					};

					File[] photoFiles = currentUserPhotoFolder.listFiles(filterPhotos);
					int nPhotosCurrentUser = photoFiles.length;
	
					// se houver fotos no folder
					if(nPhotosCurrentUser > 0) {
						//percorrer todas as fotos do currentUser e colocar em arrayComTudo: photoID, numero de likes, photoPath
						for(int j = 0; j < nPhotosCurrentUser; j++) {
							//add photoID
							// TODO: mudar para <userID-photoID>
							String[] aux = photoFiles[j].getName().split("\\.");
							arrayComTudo.add(aux[0]);

							//add numero de likes
							File likeFile = new File(userStuffPath + following.get(i) + "/photos/" + aux[0] + ".txt");
							try(Scanner scLike = new Scanner(likeFile);) {
								if(scLike.hasNextLine()) {
									arrayComTudo.add(scLike.nextLine());
								}
							} catch (IOException e) {
								e.printStackTrace();
								System.exit(-1);
							}
							
							//add photoPath
							arrayComTudo.add(userStuffPath + following.get(i) + "/photos/" + photoFiles[j].getName());
						}
					}
				}

				//se no fim de percorrer todos os users que o cliente segue e nenhum tem fotos
				if(arrayComTudo.isEmpty()) {
					retorno.add("2");
				} else {
					if(nPhotos >= (arrayComTudo.size()/3)) {
						//se o nPhotos pedido e maior ou igual a arrayComTudo entao devolver logo arrayComTudo
						retorno = arrayComTudo;
					} else {
						//caso contrario eh preciso filtrar as nPhotos fotos de todas as fotos em arrayComTudo com os valores de counter mais alto
						for(int i = 0; i < nPhotos; i++) {
							int highestIDNumber = 0;
							int indexOfHighestIDNumber = 0;

							for(int j = 0; j < arrayComTudo.size(); j += 3) {
								//conta apenas depois do hifen (por exemplo photo-5 fica 5)
								if(Integer.parseInt(arrayComTudo.get(j).substring(6)) > highestIDNumber) {
									highestIDNumber = Integer.parseInt(arrayComTudo.get(j).substring(6));
									indexOfHighestIDNumber = j;
								}
							}

							//add do photoID
							retorno.add(arrayComTudo.get(indexOfHighestIDNumber));


							//add do numero de likes
							retorno.add(arrayComTudo.get(indexOfHighestIDNumber + 1));

							//add do path da photo
							retorno.add(arrayComTudo.get(indexOfHighestIDNumber + 2));

							//remover o mesmo photoID de arrayComTudo
							arrayComTudo.remove(indexOfHighestIDNumber);

							//remover o mesmo numero de likes de arrayComTudo
							arrayComTudo.remove(indexOfHighestIDNumber);

							//remover o mesmo path da photo de arrayComTudo
							arrayComTudo.remove(indexOfHighestIDNumber);
						}
					}
				}
			}

			return retorno;
		}

		/**
		 * Metodo que coloca um like na foto com o id photoID
		 * 
		 * @param photoID String com o ID da foto a adicionar like
		 * @return 0 se a operacao foi feita com sucesso e -1 caso contrario
		 */
		private int like(String photoID) {

			//para fazer like a uma foto
			//percorrer todos os folders de users em userStuff e ver se existe um ficheiro photoID.txt no subfolder photos
			File userStuffFolder = new File("files/userStuff");
			File[] allUserFiles = userStuffFolder.listFiles();
			for(File userFile: allUserFiles) {
				File likesFile = new File(userStuffPath + userFile.getName() + "/photos/" + photoID + ".txt");
				if(likesFile.exists()) {
					//se encontrar incrementar o valor que la estiver por 1 e devolver 0
					int nLikes = 0;
					try {
						Scanner scLikes = new Scanner(likesFile);
						if(scLikes.hasNextLine()) {
							nLikes = Integer.valueOf(scLikes.nextLine());
						}
						scLikes.close();
						FileWriter fwLikes = new FileWriter(likesFile, false);
						fwLikes.write(String.valueOf(nLikes + 1));
						fwLikes.close();
						return 0;
					} catch(IOException e) {
						e.printStackTrace();
						System.exit(-1);
					}
					return 0;
				}
			}

			//se percorrer todos os subfolders photos de todos os users e nao encontrar o ficheiro devolver -1
			return -1;
		}
		

	}
}