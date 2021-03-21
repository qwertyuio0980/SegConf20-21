package security;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Classe que possui métodos úteis quando se utiliza encriptação em Java
 */
public class Security {

    /**
	 * Devolve a chave requerida contida na keystore passada
	 * 
	 * @param keyType tipo da chave a ser devolvida, 'k' (private key ou secret key), 'pk'(public key)
	 * @param alias alias da chave a ser devolvida
	 * @param keyStore nome da keystore que contém a chave pretendida
	 * @param passwordKS password da keystore
	 * @param passwordK password da key
	 * @return Chave requerida ou null caso a mesma não exista
	 */
	public Key getKey(String keyType, String alias, String keyStore, String passwordKS, String passwordK, String storeType) {
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
		// Obter chave requerida caso exista
		if(keyType.equals("pk")) {
			Certificate cert = null;
			try {
				cert = kstore.getCertificate(alias);
			} catch (KeyStoreException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			if(cert == null) {
				System.out.println("Erro: ao obter certificado");
				System.exit(-1);
			} 
			return cert.getPublicKey();
		} else if(keyType.equals("p")) {
			try {
				return kstore.getKey(alias, passwordK.toCharArray());
			} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		return null;
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



    
}
