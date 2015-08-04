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
package org.sonatype.nexus.proxy.item;

import java.io.UnsupportedEncodingException;

import org.codehaus.plexus.util.StringUtils;

/**
 * A simple content locator that emits a string actually.
 *
 * @author cstamas
 */
public class StringContentLocator
    extends ByteArrayContentLocator
{
  private static final String ENCODING = "UTF-8";

  public StringContentLocator(String content) {
    this(content, null);
  }

  public StringContentLocator(String content, String mimeType) {
    super(toByteArray(content), StringUtils.isBlank(mimeType) ? "text/plain" : mimeType);
  }

  public static byte[] toByteArray(String string) {
    try {
      return string.getBytes(ENCODING);
    }
    catch (UnsupportedEncodingException e) {
      // heh? will not happen
      return new byte[0];
    }
  }
}
