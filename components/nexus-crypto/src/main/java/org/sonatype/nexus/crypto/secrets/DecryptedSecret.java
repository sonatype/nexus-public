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
package org.sonatype.nexus.crypto.secrets;

/**
 * Intended to hold an unencrypted secret in memory for passing around in the application.
 *
 * Use this in a scenario where you have a class which expects a Secret but that secret should not be stored in the
 * database.
 */
public class DecryptedSecret
    implements Secret
{
  private final char[] secret;

  public DecryptedSecret(final char[] secret) {
    this.secret = secret;
  }

  public String getId() {
    throw new UnsupportedOperationException();
  }

  public char[] decrypt() {
    return secret;
  }
}
