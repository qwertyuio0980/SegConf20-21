package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SeiTchiz {

	public static void main(String[] args) {

		System.setProperty("javax.net.ssl.trustStore", "truststore/ts_client");

		// O programa será corrido deste jeito:
		// 					[0]			[1]         [2]           [3]   			[4]
		// SeiTchiz <serverAddress> <truststore> <keystore> <keystore-password> <clientID>
		// <truststore> = "truststore/<truststorename>"
		// <keystore> = "keystores/<keystorename>"


		String separador = "------------------------------------------";
		int arglen = args.length;

		// numero de argumentos passados errado
		if (arglen != 5 || args[4].contains(":") || args[4].contains("/") || 
		args[4].contains("-") || args[4].contains(" ") || args[4].contains(",") || args[3].length() < 7) {
			System.out.println(separador);
			System.out.println("Erro encontrado nos argumentos passados..." +
			"Usar SeiTchiz <serverAddress> <truststore> <keystore> <keystore-password> <clientID> sem espacos em nenhum argumento;" +
			"<clientID> nao deve conter dois pontos \":\" ou espaco \" \" ou hifen \"-\" ou forward slash \"/\" ou virgula \",\";" +
			"<truststore>,<keystore> e <keystore-password> devem conter cada um no minimo 7 caracteres.");
			System.out.println(separador);
			System.exit(-1);
		}

		System.out.println(separador);
		System.out.println("cliente iniciado");
		System.out.println(separador);

		// cria ligacao com socket
		ClientStub cs = new ClientStub(args);

		System.out.println("------problema TLS passado------");

		// efetuar login
		cs.efetuarLogin();

		System.out.print("------problema autenticacao passado------");

		// ciclo principal do cliente
		boolean stop = false;
		while (!stop) {

			// mostrar menu com opcoes
			System.out.println("Que operacao pretende executar? \n" + "f ou follow <userID> \n"
					+ "u ou unfollow <userID> \n" + "v ou viewfollowers \n" + "p ou post <photo> \n"
					+ "w ou wall <nPhotos> \n" + "l ou like <photoID> \n" + "n ou newgroup <groupID> \n"
					+ "a ou addu <userID> <groupID> \n" + "r ou removeu <userID> <groupID> \n"
					+ "g ou ginfo [groupID] \n" + "m ou msg <groupID> <msg> \n" + "c ou collect <groupID> \n"
					+ "h ou history <groupID> \n" + "s ou stop");

			BufferedReader reader;
			String input;
			String[] option = null;
			int resultado;
			String followersList = null;
			String mensagem = null;
			StringBuilder sbMensagem = new StringBuilder();
			String[] listaMensagens = null;
			String[] photoIDLikesPair = null;

			try {
				System.out.print(">>>");
				reader = new BufferedReader(new InputStreamReader(System.in));
				input = reader.readLine();
				option = input.split(" ");
			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.exit(-1);
			}

			switch (option[0]) {
			case "f":
			case "follow":

				if (option.length != 2 || option[1].contains("/") || option[1].contains(":")
						|| option[1].contains("-")) {
					System.out.println(separador);
					System.out.println("Opcao \"follow\" recebe argumento <userID> que nao pode conter: "
							+ "espacos, dois pontos, hifens ou forward slashes.");
					System.out.println(separador);
					break;
				}

				// envia-se o userID que se procura e o userID que fez o pedido
				resultado = cs.follow(option[1], args[4]);

				if (resultado == 0) {
					System.out.println(separador);
					System.out.println("Esta a seguir o user " + option[1]);
					System.out.println(separador);
				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n"
							+ "Razoes possiveis: -O userID inserido nao pertence nenhum user existente no sistema; \n"
							+ "-O user com o userID escolhido ja esta a ser seguido; \n"
							+ "-O user com o userID escolhido nao pode ser o cliente; \n"
							+ "-O userID que procurou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
				}
				break;

			case "u":
			case "unfollow":

				if (option.length != 2 || option[1].contains("/") || option[1].contains(":")
						|| option[1].contains("-")) {
					System.out.println(separador);
					System.out.println("Opcao \"unfollow\" recebe o argumento <userID> que nao pode conter: "
							+ "espacos, dois pontos, hifens ou forward slashes.");
					System.out.println(separador);
					break;
				}

				// envia-se o userID que se procura e o userID que fez o pedido
				resultado = cs.unfollow(option[1], args[4]);

				if (resultado == 0) {
					System.out.println(separador);
					System.out.println("Deixou de seguir o user " + option[1]);
					System.out.println(separador);
				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n "
							+ "Razoes possiveis: -O userID inserido nao pertence nenhum user existente no sistema; \n "
							+ "-O user com o userID escolhido nao esta a ser seguido; \n "
							+ "-O user com o userID escolhido nao pode ser o cliente; \n "
							+ "-O userID que procurou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
				}
				break;

			case "v":
			case "viewfollowers":

				if (option.length != 1) {
					System.out.println(separador);
					System.out.println("Opcao \"viewfollowers\" nao recebe argumentos adicionais.");
					System.out.println(separador);
					break;
				}

				// envia-se o senderID que quer saber quais os seus seguidores
				followersList = cs.viewfollowers(args[4]);

				if (followersList.isEmpty()) {
					System.out.println(separador);
					System.out.println("O cliente nao tem followers.");
					System.out.println(separador);
				} else {
					System.out.println(separador);
					System.out.println("Os seus followers sao:\n" + followersList);
					System.out.println(separador);
				}
				break;

			case "p":
			case "post":
				
				if (option.length == 2 && !option[1].contains("-") && (option[1].endsWith(".jpg") || option[1].endsWith(".png"))) {
					boolean answer = cs.post(option[1]);
					if (answer) {
						System.out.println(separador);
						System.out.println("A fotografia foi postada com sucesso");
						System.out.println(separador);
					} else {
						System.out.println(separador);
						System.out.println("Ocorreu um erro a fazer a operacao... \n " +
						"A fotografia nao foi postada com sucesso");
						System.out.println(separador);
					}
				} else {
					System.out.println(separador);
					System.out.println("Opcao \"post\" recebe o argumento <photo> que tem " +
					"de acabar obrigatoriamente com \".jpg\" ou \".png\" e nao pode conter hifens.");
					System.out.println(separador);
				}
				break;
				
			case "w":
			case "wall":

				if(option.length != 2 || !SeiTchiz.isPositiveInt(option[1])) {
					System.out.println(separador);
					System.out.println("Opcao \"wall\" recebe o argumento <nPhotos> que tem de ser um numero inteiro positivo.");
					System.out.println(separador);
					break;
				}

				// envia-se o senderID e o numero de fotos
				photoIDLikesPair = cs.wall(args[4], Integer.parseInt(option[1]));

				if(photoIDLikesPair.length == 1 && photoIDLikesPair[0].contentEquals("1")) {
					System.out.println(separador);
					System.out.println("O cliente nao segue nenhum user.");
					System.out.println(separador);
				} else if(photoIDLikesPair.length == 1 && photoIDLikesPair[0].contentEquals("2"))  {
					System.out.println(separador);
					System.out.println("Todos os users que o cliente segue nao tem fotos nos seus murais.");
					System.out.println(separador);
				} else {
					System.out.println(separador);
					for(int i = 0; i < photoIDLikesPair.length; i += 2) {
						System.out.println("Foto com ID: " + photoIDLikesPair[i] + "\nNumero de likes: " +
						photoIDLikesPair[i + 1]);
					}
					System.out.println(separador);
				}
				break;

			case "l":
			case "like":

				if (option.length != 2) {
					System.out.println(separador);
					System.out.println("Opcao \"like\" recebe argumento <photoID> que consiste apenas em photo-<numero da foto>.");
					System.out.println(separador);
				break;
				}

				// envia-se o photoID que se procura
				resultado = cs.like(option[1]);

				if (resultado == 0) {
					System.out.println(separador);
					System.out.println("Colocou um \"like\" na foto com ID:" + option[1]);
					System.out.println(separador);
				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n "
							+ "Razoes possiveis: -A foto com o photoID indicado nao existe.");
					System.out.println(separador);
				}
				break;

			case "n":
			case "newgroup":

				if (option.length != 2 || option[1].contains("/") || option[1].contains(":")
						|| option[1].contains("-")) {
					System.out.println(separador);
					System.out.println("Opcao \"newgroup\" recebe argumento <groupID> que nao pode conter espacos, "
							+ "dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
					break;
				}

				// envia-se o userID que se procura e o userID que fez o pedido
				resultado = cs.newgroup(option[1], args[4]);

				if (resultado == 0) {
					System.out.println(separador);
					System.out.println("O cliente e agora dono do novo grupo " + option[1]);
					System.out.println(separador);
				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n "
							+ "Razoes possiveis: -O grupo com o groupID que designou ja existe; \n "
							+ "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
				}
				break;

			case "a":
			case "addu":

				if (option.length != 3 || option[1].contains(":") || option[1].contains("/") || option[1].contains("-")
						|| option[2].contains("-") || option[2].contains("/") || option[2].contains(":")) {
					System.out.println(separador);
					System.out.println("Opcao \"addu\" recebe os argumentos <userID> e <groupID> que nao "
							+ "podem conter espacos, dois pontos, hifens ou forward slashes nos nomes.");
					System.out.println(separador);
					break;
				}

				// envia-se o userID que se pretende adicionar ao grupo, o grupoID e o senderID
				// que fez o pedido
				resultado = cs.addu(option[1], option[2], args[4]);

				if (resultado == 0) {
					System.out.println(separador);
					System.out.println("O utilizador " + option[1] + " e agora membro do grupo " + option[2]);
					System.out.println(separador);
				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n "
							+ "Razoes possiveis: -O userID inserido ja pertence ao grupo; \n "
							+ "-O userID indicado nao corresponde a nenhum utilizador desta aplicacao; \n "
							+ "-O grupo indicado nao existe; \n "
							+ "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome; \n"
							+ "-O userID que procurou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome; \n"
							+ "-Apenas o dono do grupo indicado pode adicionar membros ao mesmo.");
					System.out.println(separador);
				}
				break;

			case "r":
			case "removeu":

				if (option.length != 3 || option[1].contains(":") || option[1].contains("/") || option[1].contains("-")
						|| option[2].contains("-") || option[2].contains("/") || option[2].contains(":")) {
					System.out.println(separador);
					System.out.println("Opcao \"removeu\" recebe os argumentos <userID> e <groupID> que nao "
							+ "podem conter espacos, dois pontos, hifens ou forward slashes nos nomes.");
					System.out.println(separador);
					break;
				}

				// envia-se o userID que se pretende remover do grupo, o grupoID e o senderID
				// que fez o pedido
				resultado = cs.removeu(option[1], option[2], args[4]);

				if (resultado == 0) {
					System.out.println(separador);
					System.out.println("O utilizador " + option[1] + " deixou de fazer parte do grupo " + option[2]);
					System.out.println(separador);
				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n "
							+ "Razoes possiveis: -O userID inserido nao pertence ao grupo; \n "
							+ "-O userID indicado nao corresponde a nenhum utilizador desta aplicacao; \n "
							+ "-O grupo indicado nao existe; \n "
							+ "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome; \n"
							+ "-O userID que procurou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome; \n"
							+ "-O dono do grupo nao se pode remover a si mesmo; \n"
							+ "-Apenas o dono do grupo indicado pode remover membros ao mesmo.");
					System.out.println(separador);
				}
				break;

			case "g":
			case "ginfo":

				// caso nao meter o groupID
				if (option.length == 1) {

					// envia-se o senderID
					String[] listaGinfo = cs.ginfo(args[4]);
					boolean ownerFirst = true;
					boolean participantFirst = true;
					if (listaGinfo == null) {
						System.out.println(separador);
						System.out.println("O cliente nao e dono ou membro de nenhum grupo.");
						System.out.println(separador);
					} else {
						for (int i = 0; i < listaGinfo.length; i++) {
							String[] aux = listaGinfo[i].split("-");
							if (aux.length == 1) {
								if (ownerFirst) {
									System.out.println("Grupos dos quais o user " + args[4] + " e dono:");
									ownerFirst = false;
								}
								System.out.println(" - " + aux[0]);
							} else {
								if (participantFirst) {
									System.out.println(
											"Grupos dos quais o user" + args[4] + " participa, antecedidos dos seus respectivos donos:");
									participantFirst = false;
								}
								System.out.println(aux[0] + " - " + aux[1]);
							}
						}
					}

				} else {
					// envia-se o senderID e o groupID
					System.out.println(separador);
					String[] listaGinfo = cs.ginfo(args[4], option[1]);
					if(listaGinfo != null) {
						System.out.println("Grupo: " + option[1]);
						System.out.println("Dono: " + listaGinfo[0]);
						if(listaGinfo.length > 1) {
							System.out.println("Participantes:");
							for(int i = 1; i < listaGinfo.length; i++) {
								System.out.println(i + " - " + listaGinfo[i]);
							}
						}
					}
					System.out.println(separador);
				}
				break;

			case "m":
			case "msg":
				boolean leaveMsg = false;

				if (option.length < 3 || option[1].contains("/") || option[1].contains(":")
						|| option[1].contains("-")) {
					System.out.println(separador);
					System.out.println("Opcao \"msg\" recebe dois argumentos <groupID> e <msg>"
							+ "sendo que <groupID> nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
					break;
				}

				// msg nao pode conter : ou -
				for (int i = 2; i < option.length; i++) {
					if (option[i].contains(":") || option[i].contains("-")) {
						System.out.println(separador);
						System.out.println("Opcao \"msg\" recebe dois argumentos <groupID> e <msg>"
								+ "sendo que <msg> nao pode conter dois pontos ou hifens.");
						System.out.println(separador);
						leaveMsg = true;
						break;
					}
				}

				if (leaveMsg) {
					break;
				}

				// colocar input correspondente a msg numa var "mensagem"
				for (int i = 2; i < option.length; i++) {
					sbMensagem.append(option[i] + " ");
				}
				sbMensagem.deleteCharAt(sbMensagem.length() - 1);
				mensagem = sbMensagem.toString();

				// envia-se o groupID, o utilizador que fez o pedido e a mensagem
				resultado = cs.msg(option[1], args[4], mensagem);

				if (resultado == 0) {
					System.out.println(separador);
					System.out.println("A sua mensagem foi enviada para o grupo " + option[1]);
					System.out.println(separador);
				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n "
							+ "Razoes possiveis: -O cliente nao pertence ao grupo; \n "
							+ "-O grupo indicado nao existe; \n "
							+ "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
				}
				break;

			case "c":
			case "collect":

				if (option.length != 2 || option[1].contains("/") || option[1].contains(":")
						|| option[1].contains("-")) {
					System.out.println(separador);
					System.out.println("Opcao \"collect\" recebe argumento <groupID> que nao pode conter espacos, "
							+ "dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
					break;
				}

				// envia-se o groupID e o senderID
				resultado = cs.canCollectOrHistory(option[1], args[4]);
				// resultado aqui apenas diz se collect pode ser feito ou se por alguma razao
				// nao ira devolver mensagem nenhuma

				if (resultado == 0) {
					// envia-se o groupID e o senderID
					listaMensagens = cs.collect(option[1], args[4]);

					if (listaMensagens.length == 1 && listaMensagens[0].contentEquals("-empty")) {
						System.out.println(separador);
						System.out.println("Nao existem novas mensagens por receber");
						System.out.println(separador);
					} else {
						System.out.println(separador);
						if (listaMensagens.length == 1) {
							System.out.println("1 mensagem nova:");
						} else {
							System.out.println(String.valueOf(listaMensagens.length) + " mensagens novas:");
						}
						for (int i = 0; i < listaMensagens.length; i++) {
							String[] parEscritorConteudo = listaMensagens[i].split(":");
							System.out.println("-do user " + parEscritorConteudo[0] + ":");
							System.out.println(parEscritorConteudo[1]);
						}
						System.out.println(separador);
					}

				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n "
							+ "Razoes possiveis: -O cliente nao pertence ao grupo; \n "
							+ "-O grupo indicado nao existe; \n "
							+ "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
				}
				break;

			case "h":
			case "history":

				// envia-se o groupID indicado e o userID que fez o pedido
				if (option.length != 2 || option[1].contains("/") || option[1].contains(":")
						|| option[1].contains("-")) {
					System.out.println(separador);
					System.out.println("Opcao \"history\" recebe argumento <groupID> que nao pode conter espacos, "
							+ "dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
					break;
				}

				// envia-se o groupID e o senderID
				resultado = cs.canCollectOrHistory(option[1], args[4]);
				// resultado aqui apenas diz se history pode ser feito ou se por alguma razao
				// nao ira devolver mensagem nenhuma

				if (resultado == 0) {
					// envia-se o groupID e o senderID
					listaMensagens = cs.history(option[1], args[4]);

					if (listaMensagens.length == 1 && listaMensagens[0].contentEquals("-empty")) {
						System.out.println(separador);
						System.out.println(
								"O cliente nao leu nenhumas mensagens previamente, ou seja, o seu historico esta vazio.");
						System.out.println(separador);
					} else {
						System.out.println(separador);
						System.out.println("Historico das mensagens que ja leu:");
						for (int i = 0; i < listaMensagens.length; i++) {
							String[] parEscritorConteudo = listaMensagens[i].split(":");
							System.out.println("-do user " + parEscritorConteudo[0] + ":");
							System.out.println(parEscritorConteudo[1]);
						}
						System.out.println(separador);
					}
				} else {
					System.out.println(separador);
					System.out.println("Ocorreu um erro a fazer a operacao... \n "
							+ "Razoes possiveis: -O cliente nao pertence ao grupo; \n "
							+ "-O grupo indicado nao existe; \n "
							+ "-O groupID que indicou nao pode conter espacos, dois pontos, hifens ou forward slashes no nome.");
					System.out.println(separador);
				}
				break;

			case "s":
			case "stop":
				System.out.println("Selecionou a opcao \"stop\" que termina a aplicacao.");
				cs.stopClient();
				stop = true;
				break;

			default:
				System.out.println(separador);
				System.out.println("Input recebido invalido.");
				System.out.println(separador);
				break;

			}
		}
		System.out.println("---Sessao cliente terminada---");
	}

	/**
	 * Metodo auxiliar que verifica se uma string pode ser convertida num
	 * inteiro positivo maior que 0
	 * 
	 * @param texto String que se quer testar
	 * @return true se a string e convertivel e false caso contrario
	 */
	public static boolean isPositiveInt(String texto) {
		try {
			return Integer.parseInt(texto) > 0;
		} catch(NumberFormatException e) {
			return false;
		}
	}
}
