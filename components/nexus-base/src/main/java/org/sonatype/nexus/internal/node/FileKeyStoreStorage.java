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
package org.sonatype.nexus.internal.node;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.sonatype.nexus.ssl.spi.KeyStoreStorage;

import com.google.common.annotations.VisibleForTesting;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of {@link KeyStoreStorage} backed by local filesystem.
 * 
 * @since 3.1
 */
public class FileKeyStoreStorage
    implements KeyStoreStorage
{
  private final File keyStoreFile;

  private long lastRead;

  public FileKeyStoreStorage(final File keyStoreFile) {
    this.keyStoreFile = checkNotNull(keyStoreFile);
  }

  @VisibleForTesting
  public File getKeyStoreFile() {
    return keyStoreFile;
  }

  @Override
  public boolean exists() {
    return keyStoreFile.exists();
  }

  @Override
  public boolean modified() {
    return lastRead < keyStoreFile.lastModified();
  }

  @Override
  public void load(
      final KeyStore keyStore,
      final char[] password) throws NoSuchAlgorithmException, CertificateException, IOException
  {
    long readStart = System.currentTimeMillis();
    try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(keyStoreFile))) {
      keyStore.load(bis, password);
    }
    lastRead = readStart;
  }

  @Override
  public void save(
      final KeyStore keyStore,
      final char[] password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
  {
    keyStoreFile.getParentFile().mkdirs(); // NOSONAR
    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(keyStoreFile))) {
      keyStore.store(bos, password);
    }
    lastRead = System.currentTimeMillis();
  }

  @Override
  public String toString() {
    return keyStoreFile.toURI().toString();
  }
}
