/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.util;

import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Wrappers to make using javax.crypto.Cipher a little less unfriendly.
 *
 * @author tarkvara
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
public class CryptoUtils {
    private static final Log LOG = LogFactory.getLog(CryptoUtils.class);
    private static final byte[] salt = {
        (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
        (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
    };

    private static Cipher ENCRYPTOR;
    private static Cipher DECRYPTOR;

    static {
        try {
            KeySpec keySpec = new PBEKeySpec("brucrew".toCharArray(), salt, 19);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(keySpec);
            ENCRYPTOR = Cipher.getInstance(key.getAlgorithm());
            DECRYPTOR = Cipher.getInstance(key.getAlgorithm());
            AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, 19);
            ENCRYPTOR.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            DECRYPTOR.init(Cipher.DECRYPT_MODE, key, paramSpec);
        } catch (Exception x) {
            LOG.error("Unable to initialise crypto utils.", x);
        }
    }
    
    /**
     * Decrypt a BASE64 encoded encrypted string.
     * 
     * @param str BASE64 representation of encrypted string
     * @return plain-text
     */
    public static String decrypt(String str) {
        try {
            return new String(DECRYPTOR.doFinal(new BASE64Decoder().decodeBuffer(str)));
        } catch (Exception x) {
            LOG.error("Unable to decrypt string.", x);
        }
        return null;
    }

    
    /**
     * Encrypt the string and return a BASE64 representation suitable for framing or wrapping fish.
     *
     * @param str the plaintext string to be encrypted
     * @return BASE64 encoding of encrypted string
     */
    public static String encrypt(String str) {
        try {
            // Encode bytes to base64 to get a string
            return new BASE64Encoder().encode(ENCRYPTOR.doFinal(str.getBytes("UTF-8")));
        } catch (Exception x) {
            LOG.error("Unable to encrypt string.", x);
        }
        return null;
    }

    public static void main(String[] argv) {
    }
}
