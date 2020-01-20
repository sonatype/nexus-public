/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.crypto.internal;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.crypto.CryptoHelper;
import org.sonatype.nexus.crypto.maven.MavenCipher;

import com.google.common.base.CharMatcher;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Default implementation of {@link MavenCipher}.
 * 
 * @since 3.0
 */
@Named
@Singleton
public class MavenCipherImpl
    implements MavenCipher
{
  private static final char SHIELD_BEGIN = '{';

  private static final char SHIELD_END = '}';

  private static final CharMatcher BEGIN_MATCHER = CharMatcher.is(SHIELD_BEGIN);

  private static final CharMatcher END_MATCHER = CharMatcher.is(SHIELD_END);

  private static final int MIN_PAYLOAD_LENGTH = 32; // conservative estimate, given cipher implementation

  private static final Pattern BASE_64_REGEX = Pattern.compile("[A-Za-z0-9+/]*={0,2}");

  private final PasswordCipher passwordCipher;

  @Inject
  public MavenCipherImpl(final CryptoHelper cryptoHelper) {
    this.passwordCipher = new PasswordCipher(cryptoHelper);
  }

  @Override
  public String encrypt(final CharSequence str, final String passPhrase) {
    checkNotNull(str);
    checkNotNull(passPhrase);
    return SHIELD_BEGIN + doEncrypt(str, passPhrase) + SHIELD_END;
  }

  private String doEncrypt(final CharSequence str, final String passPhrase) {
    return new String(passwordCipher.encrypt(getBytesUTF8(str), passPhrase), UTF_8);
  }

  @Override
  public String decrypt(final String str, final String passPhrase) {
    checkNotNull(str);
    checkNotNull(passPhrase);
    return doDecrypt(peel(str), passPhrase).toString();
  }

  @Override
  public char[] decryptChars(final String str, final String passPhrase) {
    checkNotNull(str);
    checkNotNull(passPhrase);
    CharBuffer charBuffer = doDecrypt(peel(str), passPhrase);
    char[] chars = new char[charBuffer.remaining()];
    charBuffer.get(chars);
    return chars;
  }

  private CharBuffer doDecrypt(final CharSequence str, final String passPhrase) {
    checkArgument(str != null, "Input string is not a password cipher");
    return UTF_8.decode(ByteBuffer.wrap(passwordCipher.decrypt(getBytesUTF8(str), passPhrase)));
  }

  @Override
  public boolean isPasswordCipher(final CharSequence str) {
    return peel(str) != null;
  }

  /**
   * Peels of the start and stop "shield" braces from payload if possible, otherwise returns {@code null} signaling that
   * input is invalid.
   */
  @Nullable
  private static CharSequence peel(final CharSequence str) {
    if (str == null || str.length() == 0) {
      return null;
    }
    int start = BEGIN_MATCHER.indexIn(str) + 1; // first character of the payload
    if (start > 0) {
      int stop = END_MATCHER.lastIndexIn(str); // character immediately after payload
      int payloadLength = stop - start;
      // is the payload a Base64 encoded string of enough length?
      if (payloadLength >= MIN_PAYLOAD_LENGTH && payloadLength % 4 == 0
          && BASE_64_REGEX.matcher(str).region(start, stop).matches()) {
        return str.subSequence(start, stop);
      }
    }
    return null;
  }

  private static byte[] getBytesUTF8(final CharSequence str) {
    if (str instanceof String) {
      return ((String) str).getBytes(UTF_8);
    }

    // use NIO buffer to avoid copying contents into String
    ByteBuffer byteBuffer = UTF_8.encode(CharBuffer.wrap(str));
    byte[] bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);
    return bytes;
  }
}
