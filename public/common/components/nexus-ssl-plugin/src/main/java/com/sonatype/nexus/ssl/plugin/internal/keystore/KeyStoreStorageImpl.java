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

import static java.util.Objects.requireNonNull;

/**
 * MyBatis {@link KeyStoreStorage} implementation.
 *
 * @since 3.21
 */
public class KeyStoreStorageImpl
    implements KeyStoreStorage
{
  private final PersistentKeyStoreStorageManager storage;

  private final String keyStoreName;

  public KeyStoreStorageImpl(final PersistentKeyStoreStorageManager storage, final String keyStoreName) {
    this.storage = requireNonNull(storage);
    this.keyStoreName = requireNonNull(keyStoreName);
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
    try (ByteArrayInputStream in = load()) {
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
      save(out);
    }
  }

  private void save(final ByteArrayOutputStream out) {
    KeyStoreData data = new KeyStoreData();
    data.setName(keyStoreName);
    data.setBytes(out.toByteArray());
    storage.save(data);
  }

  private ByteArrayInputStream load() {
    return storage.load(keyStoreName)
        .map(KeyStoreData::getBytes)
        .map(ByteArrayInputStream::new)
        .orElseThrow(() -> new IllegalStateException("key store %s does not exist".formatted(keyStoreName)));
  }
}
