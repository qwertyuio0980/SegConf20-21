import java.io.BufferedReader;
import java.io.File;
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
	private Com com;

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

		com = new Com(clientSocket, in, out);

		// Criar ficheiro onde ficarao guardadas as fotos recebidas
		File wall = new File("../wall");
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
	public void conectarServidor(String ipPort) {

		String[] aux = ipPort.split(":");
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
	 * Método que verifica se a informacao pessoal do cliente e aceitavel na
	 * aplicacao SeiTchiz caso nao for devolve uma mensagem de erro a esclarecer
	 * o cliente caso contrario efetua o login
	 * 
	 * @param clientID String que representa o ID do cliente
	 * @param passwd String que representa a password do cliente
	 */
	public void login(String clientID, String passwrd) {

		// nomes criados nao podem ter ":" ou "/" ou "-" ou " "
		if (clientID.contains(":") || clientID.contains("/") || clientID.contains("-") || clientID.contains(" ")) {
			System.out.println("Formato do userID incorreto(userID nao deve conter "
					+ "dois pontos \":\" ou espaco \" \" ou hifen \"-\" ou forward slash \"/\"");
			System.exit(-1);
		}

		if (passwrd.contains(" ") || passwrd.equals("")) {
			// as passwrds poderiam ter um espaço no meio. Seria melhor restringir apenas
			// passwrds vazias.
			System.out.println(
					"Formato de password incorreto(password nao deve conter espaços e ter no minimo um caracter)"
							+ " \n Indique uma password valida: ");
			System.exit(-1);
		} else {
			efetuarLogin(clientID, passwrd);
		}
	}

	/**
	 * Método que verifica se a informacao pessoal do cliente e aceitavel na
	 * aplicacao SeiTchiz tambem pede a password pois esta ainda nao foi fornecida
	 * e caso estas informacoes nao forem aceitaveis devolve uma mensagem de erro 
	 * a esclarecer o cliente caso contrario efetua o login
	 * 
	 * @param clientID String contendo o client id do usuário
	 */
	public void login(String clientID) {

		// nomes criados nao podem ter ":" ou "/" ou "-" ou " "
		if (clientID.contains(":") || clientID.contains("/") || clientID.contains("-") || clientID.contains(" ")) {
			System.out.println("Formato do userID incorreto(userID nao deve conter "
					+ "dois pontos \":\" ou espaco \" \" ou hifen \"-\" ou forward slash \"/\"");
			System.exit(-1);
		}

		String passwrd = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("password: ");
		while (passwrd == null) {
			try {
				passwrd = reader.readLine();
			} catch (Exception e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}

			if (passwrd.contains(" ") || passwrd.equals("")) {
				// as passwrds poderiam ter um espaço no meio. Seria melhor restringir apenas
				// passwrds vazias.
				passwrd = null;
				System.out.println(
						"Formato de password incorreto(password nao deve conter espaços e ter no minimo um caracter)"
								+ " \n Indique uma password valida: ");
			}
		}

		efetuarLogin(clientID, passwrd);
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

		try {
			this.clientSocket = new Socket(ip, port);
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
	 * @param clientID String que representa o nome do cliente
	 * @param passwrd Sring que representa a password do cliente
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
			// sc.close(); caso fechemos o scanner, fechamos tbm o system.IN o que faz com
			// que dê erro (stream closed)
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

		File file = new File("../Fotos/" + pathFile);
		boolean bool = false;
		try {
			com.send("p");
			com.sendFile("../Fotos/" + pathFile);

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
			
			tamanhoArray = (int) in.readObject();

			// receber numero que e valor de erro ou tamanho do array
			if(tamanhoArray == -1) {
				resultado = new String[1];
				resultado[0] = (String) in.readObject();
			} else {
				// Cada photo sera representada por duas entradas no array resultado:
				// 1.photoID
				// 2.likes
				resultado = new String[tamanhoArray*2];

				// Loop for para receber os 3 instreams de cada foto
				for(int i = 0; i < tamanhoArray; i+=2) {
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

	// /**
	//  * Metodo que faz pedido ao servidor para dar like na foto com photoID dado
	//  * 
	//  * @param senderID String que representa o ID da foto
	//  * @return 0 se o pedido teve sucesso e -1 caso contrario
	//  */
	// public int like(String photoID) {
	// 	int resultado = -1;
	// 	try {
	// 		// enviar tipo de operacao
	// 		out.writeObject("l");

	// 		// enviar groupID:ID do user que fez o pedido
	// 		out.writeObject(photoID);

	// 		// receber o resultado da operacao
	// 		resultado = (int) in.readObject();

	// 	} catch (IOException e) {
	// 		// TODO Auto-generated catch block
	// 		e.printStackTrace();
	// 	} catch (ClassNotFoundException e) {
	// 		// TODO Auto-generated catch block
	// 		e.printStackTrace();
	// 	}

	// 	return resultado;
	// }

}