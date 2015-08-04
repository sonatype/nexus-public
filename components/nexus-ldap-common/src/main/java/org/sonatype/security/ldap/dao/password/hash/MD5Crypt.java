/**
 * FreeBSD-compatible md5-style password crypt,
 * based on crypt-md5.c by Poul-Henning Kamp, which was distributed
 * with the following notice:
 *
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * <phk@login.dknet.dk> wrote this file.  As long as you retain this notice you
 * can do whatever you want with this stuff. If we meet some day, and you think
 * this stuff is worth it, you can buy me a beer in return.   Poul-Henning Kamp
 * ----------------------------------------------------------------------------
 *
 * Retrieved from: http://www.koders.com/java/fid0A789D124DFABE0FA7F734697B6A85A141F90A76.aspx on 01/10/2012
 */

package org.sonatype.security.ldap.dao.password.hash;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Class containing static methods for encrypting passwords
 * in FreeBSD md5 style.
 *
 * @author Nick Johnson <freebsd@spatula.net>
 * @version 1.0
 */

public class MD5Crypt
{

  private static final String magic = "$1$";

  private static final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  private static String cryptTo64(int value, int length) {
    StringBuilder output = new StringBuilder();

    while (--length >= 0) {
      output.append(itoa64.substring(value & 0x3f, (value & 0x3f) + 1));
      value >>= 6;
    }

    return (output.toString());
  }

  /**
   * Encrypts a password using FreeBSD-style md5-based encryption
   *
   * @param password The cleartext password to be encrypted
   * @return The encrypted password, or an empty string on error
   * @throws java.security.NoSuchAlgorithmException
   *          if java.security
   *          does not support MD5
   */
  public final String crypt(String password) throws java.security.NoSuchAlgorithmException {
    StringBuilder salt = new StringBuilder();
    SecureRandom randgen = new SecureRandom();
    while (salt.length() < 8) {
      int index = (int) (randgen.nextFloat() * itoa64.length());
      salt.append(itoa64.substring(index, index + 1));
    }
    return crypt(password, salt.toString());
  }

  /**
   * Encrypts a password using FreeBSD-style md5-based encryption
   *
   * @param password The cleartext password to be encrypted
   * @param salt     The salt used to add some entropy to the encryption
   * @return The encrypted password, or an empty string on error
   * @throws java.security.NoSuchAlgorithmException
   *          if java.security
   *          does not support MD5
   */
  public final String crypt(String password, String salt)
      throws java.security.NoSuchAlgorithmException
  {

    /* First get the salt into a proper format.  It can be no more than
         * 8 characters, and if it starts with the magic string, it should
         * be skipped.
     */

    if (salt.startsWith(magic)) {
      salt = salt.substring(magic.length());
    }

    int saltEnd = salt.indexOf('$');
    if (saltEnd != -1) {
      salt = salt.substring(0, saltEnd);
    }

    if (salt.length() > 8) {
      salt = salt.substring(0, 8);
    }

    /* now we have a properly formatted salt */

    int saltLength = salt.length();

    MessageDigest md5_1, md5_2;

    md5_1 = MessageDigest.getInstance("MD5");

        /* First we update one MD5 with the password, magic string, and salt */
    md5_1.update(password.getBytes());
    md5_1.update(magic.getBytes());
    md5_1.update(salt.getBytes());

    md5_2 = MessageDigest.getInstance("MD5");

    /* Now start a second MD5 with the password, salt, and password again */
    md5_2.update(password.getBytes());
    md5_2.update(salt.getBytes());
    md5_2.update(password.getBytes());

    byte[] md5_2_digest = md5_2.digest();

    int md5Size = md5_2_digest.length; // XXX
    int pwLength = password.length();

    /* Update the first MD5 a few times starting at the first
         * character of the second MD5 digest using the smaller
        * of the MD5 length or password length as the number of
     * bytes to use in the update.
     */
    for (int i = pwLength; i > 0; i -= md5Size) {
      md5_1.update(md5_2_digest, 0, i > md5Size ? md5Size : i);
    }

    /* the FreeBSD code does a memset to 0 on "final" (md5_2_digest) here
     * which may be a bug, since it references "final" again if the
     * conditional below is true, meaning it always is equal to 0
     */

    md5_2.reset();

    /* Again, update the first MD5 a few times, this time
      * using either 0 (see above) or the first byte of the
      * password, depending on the lowest order bit's value
     */
    byte[] pwBytes = password.getBytes();
    for (int i = pwLength; i > 0; i >>= 1) {
      if ((i & 1) == 1) {
        md5_1.update((byte) 0);
      }
      else {
        md5_1.update(pwBytes[0]);
      }
    }

    /* Set up the output string. It'll look something like
     * $1$salt$ to begin with
     */
    StringBuilder output = new StringBuilder(magic);
    output.append(salt);
    output.append("$");

    byte[] md5_1_digest = md5_1.digest();

    /* According to the original source, this bit of madness
     * is introduced to slow things down.  It also further
     * mutates the result.
     */
    byte[] saltBytes = salt.getBytes();
    for (int i = 0; i < 1000; i++) {
      md5_2.reset();
      if ((i & 1) == 1) {
        md5_2.update(pwBytes);
      }
      else {
        md5_2.update(md5_1_digest);
      }
      if (i % 3 != 0) {
        md5_2.update(saltBytes);
      }
      if (i % 7 != 0) {
        md5_2.update(pwBytes);
      }
      if ((i & 1) != 0) {
        md5_2.update(md5_1_digest);
      }
      else {
        md5_2.update(pwBytes);
      }
      md5_1_digest = md5_2.digest();
    }

    /* Reorder the bytes in the digest and convert them to base64 */
    int value;
    value = ((md5_1_digest[0] & 0xff) << 16) | ((md5_1_digest[6] & 0xff) << 8) | (md5_1_digest[12] & 0xff);
    output.append(cryptTo64(value, 4));
    value = ((md5_1_digest[1] & 0xff) << 16) | ((md5_1_digest[7] & 0xff) << 8) | (md5_1_digest[13] & 0xff);
    output.append(cryptTo64(value, 4));
    value = ((md5_1_digest[2] & 0xff) << 16) | ((md5_1_digest[8] & 0xff) << 8) | (md5_1_digest[14] & 0xff);
    output.append(cryptTo64(value, 4));
    value = ((md5_1_digest[3] & 0xff) << 16) | ((md5_1_digest[9] & 0xff) << 8) | (md5_1_digest[15] & 0xff);
    output.append(cryptTo64(value, 4));
    value = ((md5_1_digest[4] & 0xff) << 16) | ((md5_1_digest[10] & 0xff) << 8) | (md5_1_digest[5] & 0xff);
    output.append(cryptTo64(value, 4));
    value = md5_1_digest[11] & 0xff;
    output.append(cryptTo64(value, 2));

    /* Drop some hints to the GC */
    md5_1 = null;
    md5_2 = null;
    md5_1_digest = null;
    md5_2_digest = null;
    pwBytes = null;
    saltBytes = null;
    password = salt = "";
    saltLength = pwLength = 0;

    return output.toString();
  }

}