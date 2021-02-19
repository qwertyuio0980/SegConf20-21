import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.Writer;

public class SeiTchizServer {

	public int port;
	
	public File filesFolder; //
	public File serverStuffFolder;
	public File userStuffFolder;	
	public File usersFile; //database de usersID:name:pwd

	public static void main(String[] args) {
		System.out.println("---servidor iniciado---");
		SeiTchizServer server = new SeiTchizServer();
		if (args.length == 1 && args[0].contentEquals("45678")) {
			server.startServer(args[0]);
		} else {
			System.out.println("Argumento de SeiTchizServer tem de ser obrigatoriamente \"45678\"");
		}
	}

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

			System.out.println("ficheiros de esqueleto do servidor criados");
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
	 * Metodo que adiciona um par user password á lista do servidor
	 * 
	 * @param clientID
	 * @param userName
	 * @param passwd
	 * @return 0 se coloca com sucesso -1 caso contrario
	 */
	public int addUserPasswd(String clientID, String userName, String passwd) {

		try  {
			Writer output = new BufferedWriter(new FileWriter("../files/serverStuff/users.txt", true));		
			output.append(clientID + ":" + userName + ":" + passwd + "\n");
			output.close();
			File userPage = new File("../files/userStuff/" + clientID);
            userPage.mkdir();
			Writer userFollowers = new BufferedWriter(new FileWriter("../files/userStuff/" + clientID + "/followers.txt", true));
			Writer userFollowing = new BufferedWriter(new FileWriter("../files/userStuff/" + clientID + "/following.txt", true));
			System.out.println("Dados do utilizador adicionados a base de dados");
			return 0;
		} catch (IOException e) {
			System.out.println("nao foi possivel autenticar este user");
			e.printStackTrace();
		}
		return -1;
	}

	// Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("thread do server para cada cliente");
		}

		public void run() {
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				String userID = null;
				String passwd = null;

				try {
				    userID = (String) inStream.readObject();
					passwd = (String) inStream.readObject();

					System.out.println("UserID e password recebidos");

					// autenticacao
					int isAuth = isAuthenticated(userID, passwd);
					if (isAuth == -1) {
						outStream.writeObject(-1);
						System.out.println("SignIn do utilizador nao ocorreu. UserID/Password errados");
					} else if (isAuth == 0) {
						outStream.writeObject(0);
						System.out.println("SignIn do utilizador ocorreu. UserID/Password certos");
					} else {
						outStream.writeObject(1);
						System.out.println("SignUp do utilizador ocorreu pela primeira vez. \n " + 
						"A espera do nome do utilizador para finalizar o SignUp");
						String userName = (String) inStream.readObject();	
						addUserPasswd(userID, userName, passwd);	
					}

				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
				
				// executar a operacao pedida pelo cliente
				while(true) {
					// receber operacao pedida
					String op = null;
	                try {
	                    op = (String) inStream.readObject();
	                } catch (ClassNotFoundException e1) {
	                    e1.printStackTrace();
	                }

					String aux = null;
					String[] conteudo = null;
				    
	                // realizar a operacao pedida
					switch(op) {
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
                                // receber <userID que o cliente quer deixar de seguir>:<userID do proprio cliente>
                                aux = (String) inStream.readObject();
                                conteudo = aux.split(":");
                                
                                // enviar estado da operacao
                                outStream.writeObject(unfollow(conteudo[0], conteudo[1]));
                                
                            } catch (ClassNotFoundException e1) {
                                e1.printStackTrace();
                            }
						    
							break;
						case "v":

							break;
						case "p":

							break;
						case "w":

							break;
						case "l":

							break;
						case "n":

							break;
						case "a":

							break;
						case "r":

							break;
						case "g":

							break;
						case "m":

							break;
						case "c":

							break;
						case "h":

							break;
						default:
						    //caso default nunca atingido
						    
							break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

        public int follow(String userID, String senderID) {
			int resultado = -1;
			boolean encontrado = false;

			//userID nao pode ter ":"
			if(userID.contains(":")) {
				return resultado;
			}

			//procurar da lista de users.txt se o userID pretendido existe
			try {
				Scanner scanner = new Scanner(usersFile);
				while(scanner.hasNextLine() && !encontrado) {
					String line = scanner.nextLine();
					String[] lineSplit = line.split(":");
					if(lineSplit[0].contentEquals(userID)) {
						encontrado = true;
					}
				}
				scanner.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			//caso userID existe em users.txt
			if(encontrado) {
			    File sendersFollowingFile = new File("../files/userStuff/" + senderID + "/following.txt");
			    Scanner sc;
				try {
                    sc = new Scanner(sendersFollowingFile);
                    while(sc.hasNextLine()) {
                        String line = sc.nextLine();
                        
                        // caso userID ja se encontre no ficheiro de following de senderID devolver -1
                        if(line.contentEquals(userID)) {
                            sc.close();
                            return resultado;
                        }
                    }
                    sc.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
				
				//adicionar userID a following.txt de senderID
                try {
                    FileWriter fw = new FileWriter(sendersFollowingFile, true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    
                    //escrita de userID
                    bw.write(userID);
                    bw.newLine();
                    
                    bw.close();
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                
				//adicionar senderID a followers.txt de userID
                File userIDsFollowersFile = new File("../files/userStuff/" + userID + "/followers.txt");
                try {
                    FileWriter fw = new FileWriter(userIDsFollowersFile, true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    
                    //escrita de senderID
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
		
		
        public int unfollow(String userID, String senderID) {
            int resultado = -1;
            boolean encontrado = false;
            
            //userID nao pode ter ":"
            if(userID.contains(":")) {
                return resultado;
            }
            
            //procurar da lista de users.txt se o userID pretendido existe
            try {
                Scanner scanner = new Scanner(usersFile);
                while(scanner.hasNextLine() && !encontrado) {
                    String line = scanner.nextLine();
                    String[] lineSplit = line.split(":");
                    if(lineSplit[0].contentEquals(userID)) {
                        encontrado = true;
                    }
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            //caso userID existe em users.txt
            if(encontrado) {
                File sendersFollowingFile = new File("../files/userStuff/" + senderID + "/following.txt");
                Scanner sc;
                try {
                    sc = new Scanner(sendersFollowingFile);
                    while(sc.hasNextLine()) {
                        String line = sc.nextLine();
                        
                        // caso userID se encontre no ficheiro de followers de senderID devolver 0
                        if(line.contentEquals(userID)) {
                            
                            //----retirar userID de following de senderID----
                            //primeiro passar todo o conteudo de following menos o userID pretendido para um ficheiro auxiliar
                            File sendersFollowingTEMPFile = new File("../files/userStuff/" + senderID + "/followingTemp.txt");
                            Scanner sc2 = new Scanner(sendersFollowingFile);
                            FileWriter fw = null;
                            try {
                                fw = new FileWriter(sendersFollowingTEMPFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            BufferedWriter bw = new BufferedWriter(fw);
                            while(sc2.hasNextLine()) {
                                String line2 = sc2.nextLine();
                                if(!userID.contentEquals(line2)) {
                                    try {
                                        bw.write(line2);
                                        bw.newLine();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            
                            
                            //apagar o ficheiro following original
                            if(sendersFollowingFile.delete()) {
                                //nada acontece aqui
                            }
                            
                            //recriar following a partir do ficheiro auxiliar
                            sendersFollowingFile = new File("../files/userStuff/" + senderID + "/following.txt");          
                            Scanner sc3 = new Scanner(sendersFollowingTEMPFile);
                            FileWriter fw2 = null;
                            try {
                                fw2 = new FileWriter(sendersFollowingFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            BufferedWriter bw2 = new BufferedWriter(fw2);
                            while(sc3.hasNextLine()) {
                                String line3 = sc3.nextLine();
                                try {
                                    bw2.write(line3);
                                    bw2.newLine();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            
                            //apagar o ficheiro following temporario
                            if(sendersFollowingTEMPFile.delete()) {
                                //nada acontece aqui
                            }
                            
                            
                            //----retirar senderID de followers de userID----
                            //primeiro passar todo o conteudo de followers menos o senderID pretendido para um ficheiro auxiliar
                            File usersFollowersFile = new File("../files/userStuff/" + userID + "/followers.txt");
                            File usersFollowersTEMPFile = new File("../files/userStuff/" + userID + "/followersTemp.txt");
                            Scanner sc4 = new Scanner(usersFollowersFile);
                            FileWriter fw3 = null;
                            try {
                                fw3 = new FileWriter(sendersFollowingTEMPFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            BufferedWriter bw3 = new BufferedWriter(fw3);
                            while(sc4.hasNextLine()) {
                                String line4 = sc4.nextLine();
                                if(!senderID.contentEquals(line4)) {
                                    try {
                                        bw3.write(line4);
                                        bw3.newLine();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            
                            
                            //apagar o ficheiro followers original
                            if(usersFollowersFile.delete()) {
                                //nada acontece aqui
                            }
                            
                            //recriar followers a partir do ficheiro auxiliar
                            usersFollowersFile = new File("../files/userStuff/" + userID + "/followers.txt");          
                            Scanner sc5 = new Scanner(sendersFollowingTEMPFile);
                            FileWriter fw4 = null;
                            try {
                                fw4 = new FileWriter(sendersFollowingFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            BufferedWriter bw4 = new BufferedWriter(fw4);
                            while(sc3.hasNextLine()) {
                                String line5 = sc5.nextLine();
                                try {
                                    bw4.write(line5);
                                    bw4.newLine();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            
                            //apagar o ficheiro following temporario
                            if(usersFollowersTEMPFile.delete()) {
                                //nada acontece aqui
                            }
                            
                            resultado = 0;
                        }
                    }
                    sc.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            
            //caso se percorram todos os userIDs e nao se encontre userID entao o cliente
            //nao o estava a seguir e devolve-se -1
            return resultado;
        }
		
        
        /*
        public int viewfollowers(String senderID) {
            
        }
        */
        
	}
}

		


				
				
				
// 				/// Receber ficheiro do cliente
// 				// int fileSize = 0;
// 				// try {
// 				// fileSize = (Integer)inStream.readObject();
// 				// } catch (Exception e) {
// 				// e.printStackTrace();
// 				// }

// 				// System.out.println(Integer.toString(fileSize));

// 				// byte[] buffer = new byte[fileSize];
// 				// int bytesTotal = 0;
// 				// int bytesRead = 0;
// 				// try {
// 				// while(bytesTotal != fileSize) {
// 				// bytesRead = inStream.read(buffer, bytesTotal, buffer.length);
// 				// bytesTotal += bytesRead;
// 				// }
// 				// } catch (Exception e) {
// 				// e.printStackTrace();
// 				// }

// 				// if(bytesTotal != 0) {
// 				// System.out.println("servidor:... ficheiro recebido com sucesso");
// 				// }

// 				outStream.close();
// 				inStream.close();
// 				socket.close();

// 			} catch (IOException e) {
// 				e.printStackTrace();
// 			}
// 		}
// 	}
// }

// import java.io.IOException;
// import java.io.ObjectInputStream;
// import java.io.ObjectOutputStream;
// import java.io.Writer;
// import java.net.ServerSocket;
// import java.net.Socket;
// import java.io.BufferedWriter;
// import java.io.File;
// import java.io.FileNotFoundException;
// import java.io.FileWriter;
// import java.util.Scanner;

// public class SeiTchizServer {

//  // public static int port;
// 	public int port;
// 	public static File folderFile;
// 	public static File users;

// 	public static void main(String[] args) {
// 		System.out.println("---servidor iniciado---");

// 		//numero/formato de argumentos errado
//         if(args.length > 2) {
//             System.out.println("Numero dos argumentos passados errado. Usar SeiTchizServer <port> ou apenas SeiTchizServer e port por default sera 45678");
//             System.exit(-1);
//         }

// 		int port = 45678;

// 		if(args.length == 2) {
// 			try {
// 				port = Integer.parseInt(args[1]);
// 			} catch(NumberFormatException e) {  
// 				System.out.println("Formato dos argumentos passados errado. O porto deve ser um inteiro");
// 				System.exit(-1);
// 			}
// 		}

// 		//criacao do folder de files e os files vazios por default
// 		// try {

// 		// 	folderFile = new File("files");
// 		// 	folderFile.mkdir();
// 		// 	users = new File("files/users.txt");
// 		// 	users.createNewFile();

// 		// 	System.out.println("ficheiros de esqueleto do servidor criados no novo diretorio \"files\"");
// 		// } catch(IOException e) {
// 		// 	System.out.println("Houve um erro na criacao do folder \"files\" e dos seus ficheiros respetivos");
//         //     System.exit(-1);
// 		// }

// 		int port = 45678;

// 		if(args.length == 2) {
// 			try {
// 				port = Integer.parseInt(args[1]);
// 			} catch(NumberFormatException e) {  
// 				System.out.println("Formato dos argumentos passados errado. O porto deve ser um inteiro");
// 				System.exit(-1);
// 			}
// 		}

// 		//criacao do folder de files e os files vazios por default
// 		// try {

// 		// 	folderFile = new File("files");
// 		// 	folderFile.mkdir();
// 		// 	users = new File("files/users.txt");
// 		// 	users.createNewFile();

// 		// 	System.out.println("ficheiros de esqueleto do servidor criados no novo diretorio \"files\"");
// 		// } catch(IOException e) {
// 		// 	System.out.println("Houve um erro na criacao do folder \"files\" e dos seus ficheiros respetivos");
//         //     System.exit(-1);
// 		// }

// 		SeiTchizServer server = new SeiTchizServer();
// 		server.startServer(port);
// 	}

// 	public void startServer(int port) {
// 		ServerSocket sSoc = null;

// 		try {
// 			sSoc = new ServerSocket(port);
// 		} catch (IOException e) {
// 			System.err.println(e.getMessage());
// 			System.exit(-1);
// 		}

// 		System.out.println("Socket criada e a espera de pedidos de conexão");

// 		//pode ser usada uma flag para se poder fechar o servidor
// 		while(true) {
// 			try {
// 				Socket inSoc = sSoc.accept();
// 				ServerThread newServerThread = new ServerThread(inSoc);
// 				System.out.println("Pedido de conexão aceite");
// 				newServerThread.start();
// 		    }
// 		    catch (IOException e) {
// 		        e.printStackTrace();
// 		    }
// 		}

//         //por enquanto isto e dead code
// 		//sSoc.close();
// 	}

// 	//Threads utilizadas para comunicacao com os clientes
// 	class ServerThread extends Thread {

// 		private Socket socket = null;

// 		ServerThread(Socket inSoc) {
// 			socket = inSoc;
// 			System.out.println("Thread servidor conectada com novo cliente");
// 		}

// 		public void run(){
// 			try {
// 				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
// 				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

// 				String user = null;
// 				String passwd = null;

// 				try {
// 					user = (String) inStream.readObject();
// 					passwd = (String) inStream.readObject();
// 					System.out.println("user e password recebidos");
// 				} catch (ClassNotFoundException e1) {
// 					e1.printStackTrace();
// 				}

// 				//autenticacao
// 				// if(isAuthenticated(user, passwd) == -1) {
// 				//     outStream.writeObject("password errada");
// 				//     System.out.println("SignIn do utilizador nao ocorreu. Id/Password errados");
// 				// } else if(isAuthenticated(user, passwd) == 0) {
// 				//     outStream.writeObject("password certa");
//                 //     System.out.println("SignIn do utilizador ocorreu. Id/Password certos");
// 				// } else {
// 				//     outStream.writeObject("nova conta autenticada");
//                 //     System.out.println("SignUp do utilizador ocorreu pela primeira vez.");
// 				// }

// 				//---------------------------------------------------------------------------------------------------------

// 				//alterar codigo a partir daqui

// 		// 		if(user.length() != 0){
// 		// 			outStream.writeObject(Boolean.valueOf(true));
// 		// 		}
// 		// 		else {
// 		// 			outStream.writeObject(Boolean.valueOf(false));
// 		// 		}

// 		// 		/// Receber ficheiro do cliente
// 		// 		int fileSize = 0;
// 		// 		try {
// 		// 			fileSize = (Integer) inStream.readObject();
// 		// 		} catch (Exception e) {
// 		// 			e.printStackTrace();
// 		// 		}

// 		// 		System.out.println(Integer.toString(fileSize));

// 		// 		byte[] buffer = new byte[fileSize];
// 		// 		int bytesTotal = 0;
// 		// 		int bytesRead = 0;
// 		// 		try {
// 		// 			while(bytesTotal != fileSize) {
// 		// 				bytesRead = inStream.read(buffer, bytesTotal, buffer.length);
// 		// 				bytesTotal += bytesRead;
// 		// 			}
// 		// 		} catch (Exception e) {
// 		// 			e.printStackTrace();
// 		// 		}

// 		// 		if(bytesTotal != 0) {
// 		// 			System.out.println("servidor:... ficheiro recebido com sucesso");
// 		// 		}

// 				outStream.close();
// 				inStream.close();
// 				socket.close();

// 			} catch (IOException e) {
// 				e.printStackTrace();
// 			}
// 		}

// 		// /**
// 		//  * Metodo que verifica se um par user/passwd ja estao na lista de ficheiros
// 		//  * do servidor
// 		//  * 
// 		//  * @return -1 se o par nao corresponde ao que esta no ficheiro do servidor ou
// 		//  * houve algum erro que impossibilitou a autenticacao
// 		//  * 0 se autenticacao foi bem sucedida e 1 se "conta" nao existia antes e foi
// 		//  * agora criada e autenticada
// 		//  */
// 		// public int isAuthenticated(String user, String passwd) {
// 		//     String line;
//         //     String[] currentUserPasswd;
// 		//     try(Scanner scanner = new Scanner(users)) {
//         //         while(scanner.hasNextLine()) {
//         //             line = scanner.nextLine();
//         //             currentUserPasswd = line.split(" ");
//         //             if(user.equals(currentUserPasswd[0])) {
//         //                 //o usuario ja existe na lista de users
//         //                 if(passwd.equals(currentUserPasswd[1])) {
//         //                     //o par user/passwd checks out
//         //                     return 0;
//         //                 }
//         //                 return -1;
//         //             }
//         //         }
//         //         //toda a lista foi percorrida e o user nao foi encontrado
//         //         //entao na linha seguinte adiciona-se este user/pass e
//         //         //autentica-se o user
//         //         if(addUserPasswd(user,passwd) == 0) {
//         //             return 1;
//         //         }
//         //     } catch (FileNotFoundException e) {
//         //         System.out.println("lista de users/passwords nao existe");
//         //         e.printStackTrace();
//         //     }

// 		// 	return -1;
// 		// }

// 		// /**
// 		//  * Metodo que adiciona um par user password á lista do servidor
// 		//  * 
// 		//  * @param user
// 		//  * @param passwd
// 		//  * @return 0 se coloca com sucesso -1 caso contrario
// 		//  */
// 		// public int addUserPasswd(String user, String passwd) {
// 		//     try (Writer output = new BufferedWriter(new FileWriter("../files/users.txt", true))){               
//         //         output.append(user + " " + passwd);
//         //         return 0;
//         //     } catch (IOException e) {
//         //         System.out.println("nao foi possivel autenticar este user");
//         //         e.printStackTrace();
//         //     }
// 		//     return -1;
// 		// }

// 	}
// }