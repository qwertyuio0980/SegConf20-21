import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Com {

	ObjectInputStream in = null;
	ObjectOutputStream out = null;
	BufferedOutputStream bos = null;
	String user;
	String password;
	Socket socket;

	public Com(Socket socket) {

		this.socket = socket;
	}

	public Com(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
		this.socket = socket;
		this.in = in;
		this.out = out;
	}

	public void open() {

		try {
			this.out = new ObjectOutputStream(socket.getOutputStream());
			this.in = new ObjectInputStream(socket.getInputStream());

		} catch (IOException e) {
			System.err.println("Error creating streams");
			e.printStackTrace();
		}
	}

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

	public void receiveFile(String userName) throws ClassNotFoundException, IOException {

		String nomeFicheiro = (String) in.readObject();
		long tamanho = in.readLong();
		int bytesRead = 0;
		int offset = 0;
		byte[] byteArray = new byte[1024];
		File file;

		file = new File("../files/userStuff/" + userName + "/photos/" + nomeFicheiro);

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
