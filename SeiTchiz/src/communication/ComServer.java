package communication;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;

import java.io.FileWriter;
import java.io.BufferedWriter;	


public class ComServer {

	ObjectInputStream in = null;
	ObjectOutputStream out = null;
	BufferedOutputStream bos = null;
	String user;
	String password;
	Socket socket;

	public ComServer(Socket socket) {

		this.socket = socket;

	}

	public ComServer(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
		this.socket = socket;
		this.in = in;
		this.out = out;
	}

	/**
	 * Metodo que inicializa os canais de comunicacao
	 */
	public void open() {

		try {
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.in = new ObjectInputStream(socket.getInputStream());

		} catch (IOException e) {
			System.err.println("Error creating streams");
			e.printStackTrace();
		}
	}

	/**
	 * Metodo usado para enviar objetos
	 * 
	 * @param obj objeto a enviar
	 * @throws IOException
	 */
	public void send(Object obj) throws IOException {

		try {
			out.writeObject(obj.getClass().getName());
			out.flush();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		try {
			this.out.writeObject(obj);
			out.flush();
		} catch (IOException e) {
			System.err.println("Error sending object");
			System.err.println(e.getMessage());
		}

	}

	/**
	 * Metodo para receber objetos
	 * 
	 * @return Objecto recebido pelo canal de comunicacao
	 */
	public Object receive() {

		Object obj = null;

		try {
			obj = in.readObject();
		} catch (ClassNotFoundException | IOException e) {
			System.err.println("Error receiving object");
			System.err.println(e.getMessage());
		}

		try {
			obj = in.readObject();
		} catch (ClassNotFoundException | IOException e) {
			System.err.println(e.getClass());
		}

		return obj;

	}

	/**
	 * Metodo para enviar ficheiros
	 * 
	 * @param nomeFicheiro String que representa o nome do ficheiro
	 * @throws IOException
	 */
	public void sendFile(String nomeFicheiro) throws IOException {

		File file = new File(nomeFicheiro);
		int bytesRead = 0;
		int offset = 0;
		byte[] byteArray = new byte[1024];
		out.writeObject(file.getName());
		out.flush();
		out.writeLong(file.length());
		out.flush();

		FileInputStream fis = new FileInputStream(file);

		while ((offset + 1024) < (int) file.length()) {
			bytesRead = fis.read(byteArray, 0, 1024);
			out.write(byteArray, 0, bytesRead);
			out.flush();
			offset += bytesRead;
		}

		if ((1024 + offset) != (file.length())) {
			bytesRead = fis.read(byteArray, 0, (int) file.length() - offset);
			out.write(byteArray, 0, bytesRead);
			out.flush();
		}
		fis.close();
	}

	/**
	 * Metodo para receber ficheiros
	 * 
	 * @param userName String que representa o ID do cliente para onde os ficheiros sao colocados
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void receiveFilePost(String userName) throws ClassNotFoundException, IOException {

		String nomeFicheiro = (String) in.readObject();
		long tamanho = in.readLong();
		int bytesRead = 0;
		int offset = 0;
		byte[] byteArray = new byte[1024];

		File gpcFile = new File("files/serverStuff/globalPhotoCounter.txt");
		int globalCounter = 0;
		try(Scanner scGPC = new Scanner(gpcFile);) {
			if(scGPC.hasNextLine()) {
				globalCounter = Integer.parseInt(scGPC.nextLine());
			}
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		File photoFile = new File("files/userStuff/" + userName + "/photos/photo-" + globalCounter + ".jpg");
		
		if(nomeFicheiro.endsWith(".png")) {
			photoFile = new File("files/userStuff/" + userName + "/photos/photo-" + globalCounter + ".png");
		}

		File likesFile = new File("files/userStuff/" + userName + "/photos/photo-" + globalCounter + ".txt");
		FileWriter fwLikes = new FileWriter(likesFile, true);
		BufferedWriter bwLikes = new BufferedWriter(fwLikes);
		bwLikes.write("0");
		bwLikes.close();

		try(FileOutputStream fos = new FileOutputStream(photoFile)) {
			while ((offset + 1024) < (int) tamanho) {
				bytesRead = in.read(byteArray, 0, 1024);
				fos.write(byteArray, 0, bytesRead);
				fos.flush();
				offset += bytesRead;
			}
	
			if ((1024 + offset) != (int) tamanho) {
				bytesRead = in.read(byteArray, 0, (int) tamanho - offset);
				fos.write(byteArray, 0, bytesRead);
				fos.flush();
			}
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Metodo para receber ficheiros
	 * 
	 * @param userName String que representa o ID do cliente para onde os ficheiros sao colocados
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public void receiveFileWall() throws ClassNotFoundException, IOException {

		String nomeFicheiro = (String) in.readObject();
		long tamanho = in.readLong();
		int bytesRead = 0;
		int offset = 0;
		byte[] byteArray = new byte[1024];
		File file = new File("wall/" + nomeFicheiro);
		
		FileOutputStream fos = new FileOutputStream(file);
		while ((offset + 1024) < (int) tamanho) {
			bytesRead = in.read(byteArray, 0, 1024);
			fos.write(byteArray, 0, bytesRead);
			fos.flush();
			offset += bytesRead;
		}

		if ((1024 + offset) != (int) tamanho) {
			bytesRead = in.read(byteArray, 0, (int) tamanho - offset);
			fos.write(byteArray, 0, bytesRead);
			fos.flush();
		}
		System.out.println();
		fos.close();
	}

	/**
	 * Metodo que fecha os canais de comunicacao
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {

		try {
			out.flush();
			while (in.available() > 0)
				System.out.println("Closing streams...");

			this.in.close();
			this.out.close();
		} catch (IOException e) {
			System.err.println("Error closing streams");
			System.err.println(e.getMessage());
		}
	}

}
