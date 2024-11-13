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
package org.sonatype.nexus.internal.security.apikey.store;

import java.nio.CharBuffer;

import org.sonatype.nexus.security.authc.apikey.ApiKey;
import org.sonatype.nexus.datastore.mybatis.handlers.PasswordCharacterArrayTypeHandler;

/**
 * {@link ApiKey} token holder; internal use only.
 *
 * This wrapper makes it easy to apply a {@link ApiKeyTokenTypeHandler special} MyBatis type handler to the enclosed
 * character array to allow queries against the encrypted value. The {@link PasswordCharacterArrayTypeHandler default}
 * handler cannot support queries because it adds random salt to every encryption request, so even if the source array
 * was the same the encrypted value would differ each time.
 *
 * @since 3.21
 */
class ApiKeyToken
{
  private final char[] chars;

  public ApiKeyToken(final char[] chars) { // NOSONAR
    this.chars = chars; // NOSONAR: this is just a temporary transfer object
  }

  public char[] getChars() { // NOSONAR
    return chars; // NOSONAR: this is just a temporary transfer object
  }

  public CharBuffer getCharBuffer() {
    return CharBuffer.wrap(chars);
  }
}
