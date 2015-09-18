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
package org.sonatype.nexus.crypto.maven;

import org.sonatype.nexus.crypto.internal.CryptoHelperImpl;

/**
 * UT for {@link MavenCipher} with {@link PasswordCipherMavenLegacyImpl}.
 */
public class PasswordCipherMavenLegacyImplTest
    extends MavenCipherTestSupport
{
  private static final String passPhrase = "foofoo";

  private static final String plaintext = "my testing phrase";

  private static final String encrypted = "{CFUju8n8eKQHj8u0HI9uQMRmKQALtoXH7lY=}";

  public PasswordCipherMavenLegacyImplTest() {
    super(passPhrase, plaintext, encrypted, new MavenCipher(new PasswordCipherMavenLegacyImpl(new CryptoHelperImpl())));
  }
}