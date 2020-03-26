/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2019 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.crypto;

import org.junit.Assert;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SecureRandomTest {
    @Test
    public void simpleSecureRandom() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[32];
        for (int i=0; i<30; i++) {
            secureRandom.nextBytes(iv);
        }
    }

    @Test
    public void sha1prngSecureRandom() {
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            byte[] iv = new byte[32];
            for (int i=0; i<30; i++) {
                secureRandom.nextBytes(iv);
            }
        } catch (NoSuchAlgorithmException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void strongSecureRandom() {
        try {
            //If too slow, generate more randomness for /dev/random, or change "securerandom.strongAlgorithms" to
            // NativePRNGNonBlocking:SUN in ${java.home}/jre/lib/security/java.security
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            byte[] iv = new byte[32];
            for (int i=0; i<30; i++) {
                secureRandom.nextBytes(iv);
            }
        } catch (NoSuchAlgorithmException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void reproducibleRandom() throws NoSuchAlgorithmException {
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        long seed = 12345678L;
        secureRandom.setSeed(seed);
        Assert.assertEquals(3325995872096263519L, secureRandom.nextLong());
        System.out.println(secureRandom.nextLong());
        SecureRandom secureRandom2 = SecureRandom.getInstance("SHA1PRNG");
        secureRandom2.setSeed(seed);
        Assert.assertEquals(3325995872096263519L, secureRandom2.nextLong());
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new SecureRandomTest().simpleSecureRandom();
        System.out.println("simpleSecureRandom:" + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        new SecureRandomTest().sha1prngSecureRandom();
        System.out.println("sha1prngSecureRandom:" + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        new SecureRandomTest().strongSecureRandom();
        System.out.println("strongSecureRandom:" + (System.currentTimeMillis() - startTime));
    }
}
