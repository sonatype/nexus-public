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
import java.util.Optional;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityVersion;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.ssl.spi.KeyStoreStorage;
import org.sonatype.nexus.ssl.spi.KeyStoreStorageManager;
import org.sonatype.nexus.transaction.Transactional;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * MyBatis {@link KeyStoreStorageManager} implementation.
 *
 * @since 3.21
 */
@Named(KeyStoreManagerImpl.NAME)
@Singleton
public class KeyStoreStorageManagerImpl
    extends ConfigStoreSupport<KeyStoreDAO>
    implements KeyStoreStorageManager
{
  private final EventManager eventManager;

  @Inject
  public KeyStoreStorageManagerImpl(final DataSessionSupplier sessionSupplier, final EventManager eventManager) {
    super(sessionSupplier);
    this.eventManager = checkNotNull(eventManager);
  }

  @Override
  public KeyStoreStorage createStorage(final String keyStoreName) {
    return new KeyStoreStorageImpl(this, keyStoreName);
  }

  @Transactional
  @Nullable
  public boolean exists(final String keyStoreName) {
    return dao().load(keyStoreName).isPresent();
  }

  public ByteArrayInputStream load(final String keyStoreName) {
    Optional<KeyStoreData> data = doLoad(keyStoreName);
    checkState(data.isPresent(), "key store %s does not exist", keyStoreName);
    postEvent(keyStoreName);
    return new ByteArrayInputStream(data.get().getBytes());
  }

  @Transactional
  protected Optional<KeyStoreData> doLoad(final String keyStoreName) {
    return dao().load(keyStoreName);
  }

  public void save(final String keyStoreName, final ByteArrayOutputStream out) {
    KeyStoreData data = new KeyStoreData();
    data.setName(keyStoreName);
    data.setBytes(out.toByteArray());
    doSave(data);
    postEvent(keyStoreName);
  }

  @Transactional
  protected void doSave(final KeyStoreData data) {
    dao().save(data);
  }

  private void postEvent(final String keyStoreName) {
    // trigger invalidation of TrustStoreImpl context
    eventManager.post(new KeyStoreDataEvent()
    {
      @Override
      public boolean isLocal() {
        return true;
      }

      @Override
      public EntityVersion getVersion() {
        return null;
      }

      @Override
      public String getRemoteNodeId() {
        return null;
      }

      @Override
      public String getKeyStoreName() {
        return keyStoreName;
      }
    });
  }
}
