import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.Scanner;

public class SeiTchizServer {

    public static int port;
	public static File folderFile;
	public static File userpassFile;

	public static void main(String[] args) {
		System.out.println("---servidor iniciado---");

		//numero/formato de argumentos errado
        if(args.length != 1) {
            System.out.println("Numero dos argumentos passados errado. Usar SeiTchizServer <port>");
            System.exit(-1);
        }
        try {
            port = Integer.parseInt(args[0]);
        } catch(NumberFormatException e) {  
            System.out.println("Formato dos argumentos passados errado. Usar SeiTchizServer <port>");
            System.exit(-1);
        }

		//criacao do folder de files e os files vazios por default
		try {
		    
			folderFile = new File("files");
			folderFile.mkdir();
			userpassFile = new File("files/userpassFile.txt");
			userpassFile.createNewFile();
			
			System.out.println("ficheiros de esqueleto do servidor criados no novo diretorio \"files\"");
		} catch(IOException e) {
			System.out.println("Houve um erro na criacao do folder \"files\" e dos seus ficheiros respetivos");
            System.exit(-1);
		}
		
		SeiTchizServer server = new SeiTchizServer();
		server.startServer();
	}

	public void startServer() {
		ServerSocket sSoc = null;
        
		try {
			sSoc = new ServerSocket(SeiTchizServer.port);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
        
		//pode ser usada uma flag para se poder fechar o servidor
		while(true) {
			try {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
		    }
		    catch (IOException e) {
		        e.printStackTrace();
		    }
		    
		}

        //por enquanto isto e dead code
		//sSoc.close();
	}


	//Threads utilizadas para comunicacao com os clientes
	class ServerThread extends Thread {

		private Socket socket = null;

		ServerThread(Socket inSoc) {
			socket = inSoc;
			System.out.println("nova thread criada novo cada cliente");
		}
 
		public void run(){
			try {
				ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());

				String user = null;
				String passwd = null;
			
				try {
					user = (String) inStream.readObject();
					passwd = (String) inStream.readObject();
					System.out.println("user e password recebidos");
				} catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
 			
				//alterar codigo a partir daqui
				//autenticacao
				if(isAuthenticated(user, passwd) != 0) {
				    outStream.writeObject("password errada");
				    System.out.println("Login do utilizador nao ocorreu. Par Id/Password errados");
				}
				
				
				//----------------------------------------
				if(user.length() != 0){
					outStream.writeObject(Boolean.valueOf(true));
				}
				else {
					outStream.writeObject(Boolean.valueOf(false));
				}

				/// Receber ficheiro do cliente
				int fileSize = 0;
				try {
					fileSize = (Integer) inStream.readObject();
				} catch (Exception e) {
					e.printStackTrace();
				}

				System.out.println(Integer.toString(fileSize));

				byte[] buffer = new byte[fileSize];
				int bytesTotal = 0;
				int bytesRead = 0;
				try {
					while(bytesTotal != fileSize) {
						bytesRead = inStream.read(buffer, bytesTotal, buffer.length);
						bytesTotal += bytesRead;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				if(bytesTotal != 0) {
					System.out.println("servidor:... ficheiro recebido com sucesso");
				}

				outStream.close();
				inStream.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Metodo que verifica se um par user/passwd ja estao na lista de ficheiros
		 * do servidor
		 * 
		 * @return -1 se o par nao corresponde ao que esta no ficheiro do servidor ou
		 * houve algum erro que impossibilitou a autenticacao
		 * 0 se autenticacao foi bem sucedida e 1 se "conta" nao existia antes e foi
		 * agora criada e autenticada
		 */
		public int isAuthenticated(String user, String passwd) {
		    String line;
            String[] currentUserPasswd;
		    try(Scanner scanner = new Scanner(userpassFile)) {
                while(scanner.hasNextLine()) {
                    line = scanner.nextLine();
                    currentUserPasswd = line.split(" ");
                    if(user.equals(currentUserPasswd[0])) {
                        //o usuario ja existe na lista de users
                        if(passwd.equals(currentUserPasswd[1])) {
                            //o par user/passwd checks out
                            return 0;
                        }
                        return -1;
                    }
                }
                //toda a lista foi percorrida e o user nao foi encontrado
                //entao na linha seguinte adiciona-se este user/pass e
                //autentica-se o user
                if(addUserPasswd(user,passwd) == 0) {
                    return 1;
                }
            } catch (FileNotFoundException e) {
                System.out.println("lista de users/passwords nao existe");
                e.printStackTrace();
            }

			return -1;
		}

		/**
		 * Metodo que adiciona um par user password á lista do servidor
		 * 
		 * @param user
		 * @param passwd
		 * @return 0 se coloca com sucesso -1 caso contrario
		 */
		public int addUserPasswd(String user, String passwd) {
		    try (Writer output = new BufferedWriter(new FileWriter("files/userpassFile.txt", true))){               
                output.append(user + " " + passwd);
                return 0;
            } catch (IOException e) {
                System.out.println("nao foi possivel autenticar este user");
                e.printStackTrace();
            }
		    return -1;
		}
		
	}
}