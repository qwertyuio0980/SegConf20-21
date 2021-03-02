import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.Writer;
import java.io.FilenameFilter;

public class SeiTchizServer {

	public int port;
	public File filesFolder;
	public File serverStuffFolder;
	public File userStuffFolder;
	public File usersFile;
	public File groupsFolder;
	public File globalPhotoCounterFile;

	public static void main(String[] args) {
		System.out.println("");
		System.out.println("--------------servidor iniciado-----------");
		SeiTchizServer server = new SeiTchizServer();
		if (args.length == 1 && args[0].contentEquals("45678")) {
			server.startServer(args[0]);
		} else {
			System.out.println("Argumento de SeiTchizServer tem de ser obrigatoriamente \"45678\"");
		}
	}

	/**
	 * Metodo que cria a socket de ligacao do lado do servidor e
	 * cria todos os ficheiros e folders de base caso ainda nao tenham sido
	 * criados que o servidor usa para satisfazer pedidos de clientes
	 * 
	 * @param port String que representa o porto onde estara a socket
	 */
	public void startServer(String port) {

		ServerSocket sSoc = null;

		try {
			sSoc = new ServerSocket(Integer.parseInt(port));
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}

		// criacao dos folders e files vazios por default
		try {

			filesFolder = new File("../files");
			filesFolder.mkdir();

			serverStuffFolder = new File("../files/serverStuff");
			serverStuffFolder.mkdir();

			userStuffFolder = new File("../files/userStuff");
			userStuffFolder.mkdir();

			usersFile = new File("../files/serverStuff/users.txt");
			usersFile.createNewFile();

			globalPhotoCounterFile = new File("../files/serverStuff/globalPhotoCounter.txt");
			globalPhotoCounterFile.createNewFile();

			FileWriter fwGPC = new FileWriter(globalPhotoCounterFile, true);
			BufferedWriter bwGPC = new BufferedWriter(fwGPC);
			bwGPC.write("0");
			bwGPC.close();

			// Criar diretorio para guardar informacoes sobre os grupos
			groupsFolder = new File("../files/groups");
			groupsFolder.mkdirs();
			
			System.out.println("ficheiros de esqueleto do servidor criados");
			System.out.println("------------------------------------------");
		} catch (IOException e) {
			System.out.println("Houve um erro na criacao de algum dos folders ou ficheiros de esqueleto");
			System.exit(-1);
		}

		while (true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		// sSoc.close();
	}

	/**
	 * Metodo que verifica se um par user/passwd ja estao na lista de ficheiros do
	 * servidor
	 * 
	 * @param clientID String que identifica o cliente que se pretende autenticar
	 * @param passwd String que representa a password do cliente
	 * @return -1 se o par nao corresponde ao que esta no ficheiro do servidor ou
	 *         houve algum erro que impossibilitou a autenticacao 0 se autenticacao
	 *         foi bem sucedida e 1 se "conta" nao existia antes e foi agora criada
	 *         e autenticada
	 */
	public int isAuthenticated(String clientID, String passwd) {
		String line;
		String[] currentUserPasswd;
		try (Scanner scanner = new Scanner(usersFile)) {
			while (scanner.hasNextLine()) {
				line = scanner.nextLine();
				currentUserPasswd = line.split(":");
				if (clientID.equals(currentUserPasswd[0])) {
					// o usuario ja existe na lista de users
					if (passwd.equals(currentUserPasswd[2])) {
						// o par user/passwd checks out
						return 0;
					}
					return -1;
				}
			}

		} catch (FileNotFoundException e) {
			System.out.println("lista de users/passwords nao existe");
			e.printStackTrace();
		}

		// toda a lista foi percorrida e o user nao foi encontrado
		// por isso trata-se de um novo user
		return 1;
	}

	/**
	 * Metodo que adiciona um par user, username e password á lista do servidor
	 * e cria todos os ficheiros base que cada cliente tera
	 * 
	 * @param clientID String que representa o ID do cliente para o qual se esta 
	 * a adicionar os ficheiros base do mesmo
	 * @param userName String que representa o nome pessoal do cliente
	 * @param passwd String que representa a password do cliente
	 * @return 0 se coloca com sucesso -1 caso contrario
	 */
	public int addUserPasswd(String clientID, String userName, String passwd) {

		try {
			Writer output = new BufferedWriter(new FileWriter("../files/serverStuff/users.txt", true));
			output.append(clientID + ":" + userName + ":" + passwd + "\n");
			output.close();

			File userPage = new File("../files/userStuff/" + clientID);
			userPage.mkdir();

			Writer userFollowers = new BufferedWriter(
					new FileWriter("../files/userStuff/" + clientID + "/followers.txt", true));
			userFollowers.close();

			Writer userFollowing = new BufferedWriter(
					new FileWriter("../files/userStuff/" + clientID + "/following.txt", true));
			userFollowing.close();

			Writer userParticipant = new BufferedWriter(
					new FileWriter("../files/userStuff/" + clientID + "/participant.txt", true));
			userParticipant.close();

			Writer userOwner = new BufferedWriter(
					new FileWriter("../files/userStuff/" + clientID + "/owner.txt", true));
			userOwner.close();

			File photosFolder = new File("../files/userStuff/" + clientID + "/photos");
			photosFolder.mkdir();

			File counterPhotosFile = new File("../files/userStuff/" + clientID + "/photos/counter.txt");
			counterPhotosFile.createNewFile();

			System.out.println("Dados e ficheiros base do utilizador adicionados ao servidor");
			return 0;
		} catch (IOException e) {
			System.out.println("Nao foi possivel autenticar este user");
			e.printStackTrace();
		}
		return -1;
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("Thread a correr no server para cada um cliente");
		}

		/**
		 * Metodo que contem toda a logica de cada thread criada para cada utilizador ligado ao servidor
		 */
		public void run() throws NullPointerException {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
				
				Com com = new Com(socket, inStream, outStream);
				
				String userID = null;
				String passwd = null;

				// autenticacao
				try {
					userID = (String) inStream.readObject();
					passwd = (String) inStream.readObject();

					System.out.println("UserID e password recebidos");

					int isAuth = isAuthenticated(userID, passwd);
					if (isAuth == -1) {
						outStream.writeObject(-1);
						System.out.println("SignIn do utilizador nao ocorreu. UserID/Password errados");
					} else if (isAuth == 0) {
						outStream.writeObject(0);
						System.out.println("SignIn do utilizador ocorreu. UserID/Password certos");
					} else {
						outStream.writeObject(1);
						System.out.println("SignUp do utilizador ocorreu pela primeira vez. \n"
								+ "A espera do nome do utilizador para finalizar o SignUp");
						String userName = (String) inStream.readObject();
						addUserPasswd(userID, userName, passwd);
					}
					System.out.println("------------------------------------------");

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
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
						File fileCounter = new File("../files/serverStuff/globalPhotoCounter.txt");
						int counter = 0; // valor 0 por default so para nao chatear o sonarlint
						
						try(Scanner scCounter= new Scanner(fileCounter);
						FileWriter fwCounter= new FileWriter(fileCounter, false);) {
							counter = Integer.parseInt(scCounter.nextLine());
							counter += 1;
							fwCounter.write(String.valueOf(counter));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						try {
							com.receiveFilePost(userID);
							com.send(true);
						} catch (ClassNotFoundException e2) {
							com.send(false);	
							e2.printStackTrace();
							System.out.println("Ocorreu um erro enquanto o utilizador " + userID + " tentava postar uma fotografia.");
						}
						break;

					case "w":

						try {
							// receber senderID do user que fez o pedido
							aux = (String) inStream.readObject();

							// receber nPhotos
							nPhotos = (int) inStream.readObject();

							// Chamar funcao wall passando os argumentos recebidos e receber a resposta
							// TODO: tratar a resposta do método
							//outStream.writeObject(wall(aux, nPhotos));

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
					case "l":

						// TODO

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
         * Faz com que o senderID siga o userID
         * 
         * @param userID   Usuario a ser seguido
         * @param senderID Usuario que seguira o userID
         * @return 0 se a operacao foi feita com sucesso e -1 caso contrario
         */
		public int follow(String userID, String senderID) {
			int resultado = -1;
			boolean encontrado = false;

			// userID nao pode ter ":" nem pode ser igual ao senderID
			if (userID.contains(":") || userID.contains("-") || userID.contentEquals(senderID)) {
				return resultado;
			}

			// procurar da lista de users.txt se o userID pretendido existe
			// TODO: tornar isto um metodo aux
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

			// caso userID existe em users.txt
			if (encontrado) {
				File sendersFollowingFile = new File("../files/userStuff/" + senderID + "/following.txt");
				Scanner sc;
				try {
					sc = new Scanner(sendersFollowingFile);
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						// caso userID ja se encontre no ficheiro de following de senderID devolver -1
						if (line.contentEquals(userID)) {
							sc.close();
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

				// adicionar senderID a followers.txt de userID
				File userIDsFollowersFile = new File("../files/userStuff/" + userID + "/followers.txt");
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
		public int unfollow(String userID, String senderID) {
			// TODO: Nao e necessario criar esta var
			int resultado = -1;
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

			File followingUserFile = new File("../files/userStuff/" + senderID + "/following.txt");
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
		public void unfollowAux(String userID, String senderID) {

			File sendersFollowingFile = new File("../files/userStuff/" + senderID + "/following.txt");
			File sendersFollowingTEMPFile = new File("../files/userStuff/" + senderID + "/followingTemp.txt");
			File usersFollowersFile = new File("../files/userStuff/" + userID + "/followers.txt");
			File usersFollowersTEMPFile = new File("../files/userStuff/" + userID + "/followersTemp.txt");

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
		 * Metodo que devolve uma String contendo todos os seguidores do
		 * cliente que fez o pedido
		 * 
		 * @param senderID Usuario que quer ver os seus seguidores
		 * @return String com todos os seguidores ou string vazia se
		 * o cliente nao tiver seguidores
		 */
		public String viewfollowers(String senderID) {

			// procurar no folder de users no do sender se o ficheiro followers.txt esta
			// vazio
			File sendersFollowersFile = new File("../files/userStuff/" + senderID + "/followers.txt");
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
		public int newgroup(String groupID, String senderID) {
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

			File ownerGroupFolder = new File("../files/groups/" + senderIDgroupID);
			for(int i = 0; i < files.length; i++) {
				System.out.println(files[i].getName());
			}
			if (files.length == 0) {
				// caso nao existir criar esse folder
				if (ownerGroupFolder.mkdir()) {
					// Criar ficheiros counter.txt e participants.txt
					File counter = new File("../files/groups/" + senderIDgroupID + "/counter.txt");
					File participants = new File("../files/groups/" + senderIDgroupID + "/participants.txt");
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
						File fUserOwner = new File("../files/userStuff/" + senderID + "/owner.txt");
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
						File fUserParticipant = new File("../files/userStuff/" + senderID + "/participant.txt");
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
		public int addu(String userID, String groupID, String senderID) {

			File groupFolder = new File("../files/groups/" + senderID + "-" + groupID);
			File counterFile = new File("../files/groups/" + senderID + "-" + groupID + "/counter.txt");
			File groupMembersFile = new File("../files/groups/" + senderID + "-" + groupID + "/participants.txt");
			File participantFile = new File("../files/userStuff/" + userID + "/participant.txt");

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
		public int removeu(String userID, String groupID, String senderID) {
			// Remove o userID do ficheiro participants.txt no diretorio do grupo
			// Remove o senderID-groupID do ficheiro participant.txt no diretorio userID

			File groupMembersFile = new File("../files/groups/" + senderID + "-" + groupID + "/participants.txt");
			File participantFile = new File("../files/userStuff/" + userID + "/participant.txt");

			// Verifica se ha um grupo com nome senderID-groupID e se o userID existe
			if (!groupMembersFile.exists() || !participantFile.exists() || userID.contentEquals(senderID)) {
				return -1;
			}

			// Percorre o ficheiro participants.txt e ir colocando o conteudo do mesmo
			// num ficheiro temporario

			File groupMembersTEMPFile = new File(
					"../files/groups/" + senderID + "-" + groupID + "/participantsTemp.txt");

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

			File participantTEMPFile = new File("../files/groups/" + senderID + "-" + groupID + "/participantTemp.txt");

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
		public String ginfo(String userID) {
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

            File owner = new File("../files/userStuff/" + userID + "/owner.txt");
            File participant = new File("../files/userStuff/" + userID + "/participant.txt");
            

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
            File owner = new File("../files/userStuff/" + senderID + "/owner.txt");
            File participant = new File("../files/userStuff/" + senderID+ "/participant.txt");
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

            groupParticipants = new File("../files/groups/" + fileName + "/participants.txt");

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
		public int msg(String groupID, String senderID, String mensagem) {

			File senderParticipantFile = new File("../files/userStuff/" + senderID + "/participant.txt");
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
		public void msgAux(String groupFolder, String senderID, String mensagem) {
			// metodo onde se envia mesmo a mensagem

			// 1.incrementar valor do counter em counter.txt
			File fileCounter = new File("../files/groups/" + groupFolder + "/counter.txt");
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
			File fMsg = new File("../files/groups/" + groupFolder + "/msg" + counter);
			fMsg.mkdir();

			// 3.criar o content.txt com senderID como 1a linha
			// seguido pela mensagem na linha seguinte
			File fContent = new File("../files/groups/" + groupFolder + "/msg" + counter + "/content.txt");
			try (FileWriter fwContent = new FileWriter(fContent);
					BufferedWriter bwContent = new BufferedWriter(fwContent);) {
				bwContent.write("-" + senderID + ":" + mensagem);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// 4.criar o seenby.txt e nao colocar la nada
			File fSeenBy = new File("../files/groups/" + groupFolder + "/msg" + counter + "/seenBy.txt");
			try {
				fSeenBy.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// 5.criar o notseenby.txt e colocar todos os participants no ficheiro incluindo
			// o senderID
			StringBuilder sbParticipants = new StringBuilder();
			File fParticipants = new File("../files/groups/" + groupFolder + "/participants.txt");
			try (Scanner scParticipants = new Scanner(fParticipants)) {
				while (scParticipants.hasNextLine()) {
					String lineParticipants = scParticipants.nextLine();
					sbParticipants.append(lineParticipants + "\n");
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			File fNotSeenBy = new File("../files/groups/" + groupFolder + "/msg" + counter + "/notSeenBy.txt");
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
		public boolean isCorrectGroup(String ownerGroupPair, String senderID) {
			File groupParticipantsFile = new File("../files/groups/" + ownerGroupPair + "/participants.txt");

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
		public int canCollectOrHistory(String groupID, String senderID) {

			File senderParticipantFile = new File("../files/userStuff/" + senderID + "/participant.txt");
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
		public String[] collect(String groupID, String senderID) {

			String[] listaMensagensDefault = { "-empty" };
			String parUserGroup = "";

			// 1.aceder ao folder do grupo
			File senderParticipantFile = new File("../files/userStuff/" + senderID + "/participant.txt");
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
				File fileCounter = new File("../files/groups/" + parUserGroup + "/counter.txt");
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
								"../files/groups/" + parUserGroup + "/msg" + i + "/content.txt");
						File currentNotSeenByFile = new File(
								"../files/groups/" + parUserGroup + "/msg" + i + "/notSeenBy.txt");
						File currentSeenByFile = new File(
								"../files/groups/" + parUserGroup + "/msg" + i + "/seenBy.txt");
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
									"../files/groups/" + parUserGroup + "/msg" + counter + "/notSeenByTemp.txt");
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
		public String[] history(String groupID, String senderID) {

			String[] listaMensagensDefault = { "-empty" };
			String parUserGroup = "";

			// 1.aceder ao folder do grupo
			File senderParticipantFile = new File("../files/userStuff/" + senderID + "/participant.txt");
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
				File fileCounter = new File("../files/groups/" + parUserGroup + "/counter.txt");
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
								"../files/groups/" + parUserGroup + "/msg" + i + "/content.txt");
						File currentSeenByFile = new File(
								"../files/groups/" + parUserGroup + "/msg" + i + "/seenBy.txt");
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
		 * @return TODO
		 */
		private ArrayList wall(String senderID, int nPhotos) {

			// 0.Criar um ArrayList de Strings
			// 		A estrutura do ArrayList será:
			// 			[<userID-counter>,<likes>,<filepath to picture>,...]
			// 			número total de fotos será obtido ao chamar size()/3 no ArrayList
			// 1.Obter o valor do counter global
			// 2.Criar uma lista de usuários que o utilizador segue
			// 3.Acessar os diretórios das fotos de cada usuário
			// 	 3.1.Obter o valor do counter atual
			//   3.2.Fotos a serem devolvidas estarão no intervalo /userID-[counter,counter-1,...,counter-nPhotos+1]/
			//  	3.2.1.Obter número de likes na foto atual
			// 		3.2.2.Colocar <userID-counter> no ArrayList
			// 		3.2.3.Colocar <likes> no ArrayList
			// 		3.2.4.Colocar <filepath to picture> no ArrayList

			// CODIGOS DE RESPOSTA ESPECIAL:
			// 1 = senderID não segue nenhum usuário
			// Entrada <userID-NOPHOTO> no ArrayList retorno = userID não postou nenhuma foto


			// 0.
			ArrayList retorno = new ArrayList();

			//1.
			int counterGlobal = 0;
			File gpcFile = new File("../files/serverStuff/globalPhotoCounter.txt");
			try(Scanner scGPC = new Scanner(gpcFile);) {
				if(scGPC.hasNextLine()) {
					counterGlobal = Integer.parseInt(scGPC.nextLine());
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			// 1.
			File currentUserFollowingFile = new File("../files/userStuff/" + senderID + "/following.txt");
			ArrayList following = new ArrayList();

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

			// 2.
			// Verificar se o senderID não segue nenhum usuário
			if(following.size() == 0) return retorno.add(1);
			
			for(int i = 0; i < following.size(); i++) {
				// 2.1.
				
				// número de photos do user following.get(i)
				File currentUserPhotoFolder = new File("../files/userStuff/" + following.get(i) + "/photos");
				FileFilter filter = new FileFilter() {
					public boolean accept(File f) {
						return f.getName().endsWith("jpg");
					}	
				};
				File[] photoFiles = currentUserPhotoFolder.listFiles(filter);
				int nPhotosCurrentUser = photoFiles.length;

				// Verificar se photos == 0
				if(nPhotosCurrentUser > 0) {
					// 2.2.
					for(int j = nPhotosCurrentUser; j > nPhotosCurrentUser - nPhotos; j--) {
						if(j < 0) break;
						int likes = 0;
						// Aplicar filenamefilter para obter o ficheiro da photo com nome que começa com j	
						// Obter o nome do ficheiro
						// Fazer um split a partir do "-"
						// Obter número de likes
						// Fazer add ao retorno de:
						// 	following.get(i) + "-" + j <Indentificador da photo no formato userID-counter>
						//  likes.toString()
						//  "../files/userStuff/" + following.get(i) + "/nPhotosCurrentUser/" + <photofilename> : filepath do ficheiro da foto 
						File userFollowed = new File("../files/userStuff/" + following.get(i) + "/photos/counter.txt");
						// int nPhotosCurrentUser = 0;
						// try (Scanner scFollowing = new Scanner(userFollowed)) {
						// 	nPhotosCurrentUser = Integer.parseInt(scFollowing.nextLine());
						// 	scFollowing.close();
						// } catch (Exception e) {
						// 	e.printStackTrace();
						// 	System.exit(-1);
						// }
		
	
					}
				} else {
					retorno.add(following.get(i) + "-NOPHOTO");
				}
				
			}	

        //     // Caso não tenha sido encontrado o grupo nos ficheiros do senderID devolver um estado de erro
        //     if(fileName == null) {
        //         return null;
        //     }

        //     groupParticipants = new File("../files/groups/" + fileName + "/participants.txt");

		// 	// 1.
		// 	try (Scanner scParticipants = new Scanner(groupParticipants)) {
		// 		while (scParticipants.hasNextLine()) {
		// 			String line = scParticipants.nextLine();
        //             ownerPaticipants.append(line + ',');
		// 		}
		// 		scParticipants.close();
		// 	} catch (Exception e) {
		// 		e.printStackTrace();
		// 		System.exit(-1);
		// 	}

		// 	return ownerPaticipants.toString();
        // }

		}
	}
}