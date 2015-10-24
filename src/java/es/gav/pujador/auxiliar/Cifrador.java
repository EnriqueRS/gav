package es.gav.pujador.auxiliar;

import com.sun.org.apache.xml.internal.security.utils.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Cifrador {
    
    static Cifrador Instance;
    static final String key = "hKdgFG47";
    private static final String algoritmo = "DES/ECB/NoPadding";

    /**
     * Devuelve una instancia de la clase cifrador siguiendo el patrón singleton
     * @return 
     */
    public static Cifrador getInstance() {
        if (Instance == null) {
            Instance = new Cifrador();
        }
        return Instance;
    }

    /**
     * Cifra los datos que se recibe
     * @param data
     * @return 
     */
    public String Cifrar(String data) {
        try {

            Cipher cipher = Cipher.getInstance(algoritmo);
            int blockSize = cipher.getBlockSize();

            byte[] dataBytes = data.getBytes();
            int plaintextLength = dataBytes.length;
            if (plaintextLength % blockSize != 0) {
                plaintextLength = plaintextLength + (blockSize - (plaintextLength % blockSize));
            }

            byte[] plaintext = new byte[plaintextLength];
            System.arraycopy(dataBytes, 0, plaintext, 0, dataBytes.length);

            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "DES");

            cipher.init(Cipher.ENCRYPT_MODE, keyspec);
            byte[] encrypted = cipher.doFinal(plaintext);

            return ReemplazarLineBreaks(Base64.encode(encrypted));

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * Esta funcion evita que en el cliente se interpreten los \n como saltos de línea
     * @param data
     * @return 
     */
    private String ReemplazarLineBreaks(String data){
        String nuevaCadena= data.replaceAll("\\n", "").replaceAll("\\r", "").replaceAll("\\t","");
        return nuevaCadena;
        
    }

    /**
     * Descifra los datos recibidos
     * @param data
     * @return 
     */
    public String Descifrar(String data) {
        try {
            byte[] encrypted1 = Base64.decode(data);

            Cipher cipher = Cipher.getInstance(algoritmo);
            SecretKeySpec keyspec = new SecretKeySpec(key.getBytes(), "DES");

            cipher.init(Cipher.DECRYPT_MODE, keyspec);

            byte[] original = cipher.doFinal(encrypted1);
            String originalString = new String(original, "UTF-8");
            return originalString;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}