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
package org.sonatype.nexus.ssl.spi;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Persistent storage for a {@link KeyStore}.
 * 
 * @since 3.1
 */
public interface KeyStoreStorage
{
  /**
   * Checks whether the storage has been created/populated before.
   * 
   * @return {@code true} if the storage contains data already, {@code false} otherwise.
   */
  boolean exists();

  /**
   * Checks whether the storage has been modified (externally) since the last time it was loaded or saved.
   * 
   * @return {@code true} if the key store needs to be refreshed from the storage, {@code false} otherwise.
   */
  boolean modified();

  /**
   * Loads the specified key store from the storage. May only be called if the storage {@link #exists() exists}.
   */
  void load(KeyStore keyStore, char[] password) // NOSONAR
      throws NoSuchAlgorithmException, CertificateException, IOException;

  /**
   * Saves the specified key store to the storage.
   */
  void save(KeyStore keyStore, char[] password) // NOSONAR
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException;
}
