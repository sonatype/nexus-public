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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.sonatype.nexus.ssl.spi.KeyStoreStorage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * MyBatis {@link KeyStoreStorage} implementation.
 *
 * @since 3.21
 */
public class KeyStoreStorageImpl
    implements KeyStoreStorage
{
  private final KeyStoreStorageManagerImpl storage;

  private final String keyStoreName;

  public KeyStoreStorageImpl(final KeyStoreStorageManagerImpl storage, final String keyStoreName) {
    this.storage = checkNotNull(storage);
    this.keyStoreName = checkNotNull(keyStoreName);
  }

  @Override
  public boolean exists() {
    return storage.exists(keyStoreName);
  }

  @Override
  public boolean modified() {
    return false; // we don't track the external version at the moment
  }

  @Override
  public void load(
      final KeyStore keyStore,
      final char[] password) throws NoSuchAlgorithmException, CertificateException, IOException
  {
    try (ByteArrayInputStream in = storage.load(keyStoreName)) {
      keyStore.load(in, password);
    }
  }

  @Override
  public void save(
      final KeyStore keyStore,
      final char[] password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
  {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream(16 * 1024)) {
      keyStore.store(out, password);
      storage.save(keyStoreName, out);
    }
  }
}
