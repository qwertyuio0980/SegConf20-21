package security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Classe que possui métodos úteis quando se utiliza encriptação em Java
 */
public class Security {

    /**
	 * Devolve a chave requerida contida na keystore passada
	 * 
	 * @param alias alias da chave a ser devolvida
	 * @param keyStore nome da keystore que contém a chave pretendida
	 * @param passwordKS password da keystore
	 * @param passwordK password da key
	 * @return Chave requerida ou null caso a mesma não exista
	 */
	public Key getKey(String alias, String keyStore, String passwordKS, String passwordK, String storeType) {
		// Obter keystore que guarda a chave requerida
		FileInputStream kfile = null;
		KeyStore kstore = null;
		try {
			kfile = new FileInputStream(keyStore);
			kstore = KeyStore.getInstance(storeType);
			kstore.load(kfile, passwordKS.toCharArray());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if(keyStore  == null) {
			System.out.println("Erro: ao obter a keystore");
			System.exit(-1);
		}
		if(kstore == null) {
			System.out.println("kstore null");
		}
		// Obter chave requerida caso exista
		Key key = null;
		try {
			key = kstore.getKey(alias, passwordK.toCharArray());
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return key;
	}

	/**
	 * Procura e retorna o Certificate com alias passado contido no KeyStore passado
	 * 
	 * @param alias alias da keypair
	 * @param keyStore keystore
	 * @param passwordKS password da keystore
	 * @param storeType storetype da keystore
	 * @return Certificate caso exista um com alias no keystore 
	 */
	public Certificate getCert(String alias, String keyStore, String passwordKS, String storeType) {
		// Obter keystore que guarda a chave requerida
		FileInputStream kfile = null;
		KeyStore kstore = null;
		try {
			kfile = new FileInputStream(keyStore);
			kstore = KeyStore.getInstance(storeType);
			kstore.load(kfile, passwordKS.toCharArray());
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		if(keyStore  == null) {
			System.out.println("Erro: ao obter a keystore");
			System.exit(-1);
		}
		try {
			return kstore.getCertificate(alias);
		} catch (KeyStoreException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	/**
	 * Decifra o conteúdo do ficheiro na path passada cifFile e armazena o conteúdo no ficheiro com path decFile
	 * @param cifFile ficheiro com conteúdo cifrado
	 * @param decFile ficheiro no qual o conteúdo decifrado será guardado
	 * @param key key que será usada para decifrar o ficheiro
	 * @return 0 caso a operação seja bem sucedida, -1 caso contrário
	 */
	public int decFile(String cifFile, String decFile, Key key) {

		// Verificar se o ficheiro está vazio 
		File f = new File(cifFile);
		if(f.length() == 0) {
			File newFile = new File(decFile);
			try {
				newFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
			return 0;
		}

		// Obter o algoritmo da chave
		String alg = key.getAlgorithm();

		// Criar a capacidade de decifrar no algoritmo da chave
		Cipher cDec;
		FileInputStream fisDes = null;
		CipherInputStream cis = null;
		FileOutputStream fosDes = null;
		File df = null;
		try {

			cDec = Cipher.getInstance(alg);
			cDec.init(Cipher.DECRYPT_MODE, key);
			// Ler o conteúdo cifrado e colocar em outro ficheiro o conteúdo descodificado
			fisDes = new FileInputStream(cifFile);
			cis = new CipherInputStream(fisDes, cDec);
	
			df = new File(decFile);
			df.createNewFile();
			fosDes = new FileOutputStream(df);
	
			byte[] b = new byte[16];
	
			int j = cis.read(b);
			while(j != -1) {
				fosDes.write(b, 0, j);
				j = cis.read(b);
			}

			cis.close();
			fisDes.close();
			fosDes.close();

		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException e) {
			e.printStackTrace();
			return -1;
		}

		return 0;

	}

	/**
	 * Cifra, usando a PublicKey passada, o conteúdo do ficheiro inputFile e coloca o mesmo no ficheiro outputFile. 
	 * Ao final do processo deleta o ficheiro inputFile 
	 * @param inputFile
	 * @param outputFile
	 * @param key
	 * @return 0 caso o conteúdo do ficheiro inputFile tenha sido cifrado e colocado no outputFile com sucesso
	 * -1 caso contrário 
	 */
	public int cifFile(String inputFile, String outputFile, Key key) {

		File inputF = new File(inputFile);
		
		if(!inputF.exists()) {
			System.out.println("Ficheiro <" + inputFile + "> não existe");
			return -1;
		}

		// obter algoritmo da key passada
		String alg = key.getAlgorithm();

		// Obter capacidade de cifrar usando o algoritmo da key
		Cipher c = null;
		FileInputStream fis = null;
		FileOutputStream fos = null;
		CipherOutputStream cos = null;
		try {
			c = Cipher.getInstance(alg);
			c.init(Cipher.ENCRYPT_MODE, key);
			fis = new FileInputStream(inputF);
			fos = new FileOutputStream(outputFile);
		
			cos = new CipherOutputStream(fos, c);
			byte[] b = new byte[16];  
			int i = fis.read(b);
			while (i != -1) {
				cos.write(b, 0, i);
				i = fis.read(b);
			}
			
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException e) {
			e.printStackTrace();
			return -1;
		}
		
		try {
			cos.close();
			fis.close();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		
		// Deletar ficheiro input
		// inputF.delete();

		return 0;
	}

	/**
	 * Faz um unwrap da chave wrappedKey passada usando a chave key passada
	 * @param wrappedKey wrapped key
	 * @param key chave que será usada para fazer unwrap
	 * @param wrappedKeyAlg algoritmo da wrappedKey
	 * @return Key unwrapped key
	 */
	public Key unwrapKey(byte[] wrappedKey,String wrappedKeyAlg, Key key) {
		Cipher c = null;
		try {
			c = Cipher.getInstance(key.getAlgorithm());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		}
		Key unwrappedKey = null;
		try {
			c.init(Cipher.UNWRAP_MODE, key);
			unwrappedKey = c.unwrap(wrappedKey, wrappedKeyAlg, Cipher.SECRET_KEY);
		} catch (InvalidKeyException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
		return unwrappedKey;
	}

	/**
	 * 
	 */
	public byte[] getWrappedKey(String serverSecKey) {
			File f = new File(serverSecKey);
			byte[] wrappedKey = null;
			try {
				wrappedKey = Files.readAllBytes(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			return wrappedKey;
	}

	/**
	 * Encripta a chave key passada com a chave pública pk passada
	 * @param key chave simétrica que será encriptada
	 * @param pk chave pública usada para encriptar
	 * @return byte[] contendo a chave simétrica encriptada
	 * @require key != null && pk != null 
	 */
    public byte[] wrapKey(Key key, PublicKey pk) {
		// Encriptar chave simétrica
		Cipher c = null;
		byte[] wrappedKey = null;
		try {
			c = Cipher.getInstance("RSA");
			c.init(Cipher.WRAP_MODE, pk);
			wrappedKey = c.wrap(key);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException e) {
			e.printStackTrace();
			return null;
		}
		
        return wrappedKey;
    }



}