import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.File;

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
            port = Integer.parseInt(args[1]);
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
				if(!isAuthenticated(user, passwd)) {

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
		 * Metodo que verifica se um par user/passwd ja estao 
		 * 
		 * @return
		 */
		public boolean isAuthenticated(user, passwd) {

			return false;
		}

	}
}