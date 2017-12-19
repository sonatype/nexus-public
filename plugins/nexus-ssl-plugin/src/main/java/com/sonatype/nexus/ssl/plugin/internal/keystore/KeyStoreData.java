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
package com.sonatype.nexus.ssl.plugin.internal.keystore;

import org.sonatype.nexus.common.entity.Entity;

/**
 * The persisted data of a named {@link java.security.KeyStore}.
 * 
 * @since 3.1
 */
public class KeyStoreData
    extends Entity
{
  private String name;

  private byte[] bytes;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public byte[] getBytes() {
    return bytes; // NOSONAR
  }

  public void setBytes(final byte[] bytes) { // NOSONAR
    this.bytes = bytes; // NOSONAR
  }
}
