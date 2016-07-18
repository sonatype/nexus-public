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

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.entity.EntityVersion;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Implementation of {@link KeyStoreStorage} backed by OrientDB.
 * 
 * @since 3.1
 */
public class OrientKeyStoreStorage
    extends ComponentSupport
    implements KeyStoreStorage
{
  private final KeyStoreStorageManagerImpl storageManager;

  private final String keyStoreName;

  private volatile EntityVersion loadedVersion;

  private volatile EntityVersion latestVersion;

  public OrientKeyStoreStorage(final KeyStoreStorageManagerImpl storageManager, final String keyStoreName) {
    this.storageManager = checkNotNull(storageManager);
    this.keyStoreName = checkNotNull(keyStoreName);
  }

  @VisibleForTesting
  public String getKeyStoreName() {
    return keyStoreName;
  }

  @Override
  public boolean exists() {
    return storageManager.load(keyStoreName) != null;
  }

  @Override
  public boolean modified() {
    return loadedVersion == null ? exists() : (latestVersion != null && !latestVersion.equals(loadedVersion));
  }

  @Subscribe
  public void onKeyStoreDataUpdated(final KeyStoreDataEvent event) {
    if (keyStoreName.equals(event.getKeyStoreName())) {
      latestVersion = event.getVersion();
      log.debug("Observed version {} of key store {} updated by node {}", latestVersion, keyStoreName,
          event.getRemoteNodeId());
    }
  }

  @Override
  public void load(final KeyStore keyStore, final char[] password)
      throws NoSuchAlgorithmException, CertificateException, IOException
  {
    KeyStoreData entity = storageManager.load(keyStoreName);
    checkState(entity != null, "key store %s does not exist", keyStoreName);
    keyStore.load(new ByteArrayInputStream(entity.getBytes()), password);
    loadedVersion = entity.getEntityMetadata().getVersion();
    log.debug("Loaded version {} of key store {}", loadedVersion, keyStoreName);
  }

  @Override
  public void save(final KeyStore keyStore, final char[] password)
      throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(16 * 1024);
    keyStore.store(baos, password);
    KeyStoreData entity = new KeyStoreData();
    entity.setName(keyStoreName);
    entity.setBytes(baos.toByteArray());
    storageManager.save(entity);
    loadedVersion = entity.getEntityMetadata().getVersion();
    log.debug("Saved version {} of key store {}", loadedVersion, keyStoreName);
  }

  @Override
  public String toString() {
    return "orient:/" + keyStoreName;
  }
}
