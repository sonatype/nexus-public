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

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.entity.EntityVersion;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * MyBatis {@link PersistentKeyStoreStorageManager} implementation.
 *
 * @since 3.21
 */
@Component
@Qualifier(KeyStoreManagerImpl.NAME)
public class KeyStoreStorageManagerImpl
    extends ConfigStoreSupport<KeyStoreDAO>
    implements PersistentKeyStoreStorageManager
{
  @Autowired
  public KeyStoreStorageManagerImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Transactional
  @Nullable
  @Override
  public boolean exists(final String keyStoreName) {
    return dao().load(keyStoreName).isPresent();
  }

  @Transactional
  @Override
  public Optional<KeyStoreData> load(final String keyStoreName) {
    postCommitEvent(() -> event(keyStoreName));
    return dao().load(keyStoreName);
  }

  @Transactional
  @Override
  public void save(final KeyStoreData data) {
    postCommitEvent(() -> event(data.getName()));
    dao().save(data);
  }

  @Transactional
  @Override
  public List<KeyStoreData> browse() {
    return ImmutableList.copyOf(dao().browse());
  }

  private static KeyStoreDataEvent event(final String keyStoreName) {
    // trigger invalidation of TrustStoreImpl context
    return new KeyStoreDataEvent()
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
    };
  }
}
