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
package org.sonatype.nexus.internal.node.datastore;

import java.security.cert.Certificate;
import java.sql.Connection;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.internal.node.KeyStoreManagerImpl;
import org.sonatype.nexus.internal.node.NodeIdEncoding;
import org.sonatype.nexus.node.datastore.NodeIdStore;
import org.sonatype.nexus.ssl.KeyStoreManager;
import org.sonatype.nexus.upgrade.datastore.DatabaseMigrationStep;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Migrates the legacy on disk node identifier to the database where it may be used to identify a single node, or a
 * cluster.
 */
@Named
public class NodeIdUpgradeStep_1_13
    extends ComponentSupport
    implements DatabaseMigrationStep
{
  private final NodeIdStore nodeIdStore;

  private final Provider<KeyStoreManager> keyStoreProvider;

  @Inject
  public NodeIdUpgradeStep_1_13(
      @Named(KeyStoreManagerImpl.NAME) final Provider<KeyStoreManager> keyStoreProvider,
      final NodeIdStore nodeIdStore)
  {
    this.nodeIdStore = checkNotNull(nodeIdStore);
    this.keyStoreProvider = checkNotNull(keyStoreProvider);
  }

  @Override
  public Optional<String> version() {
    return Optional.of("1.13");
  }

  @Override
  public void migrate(final Connection connection) throws Exception {
    if (nodeIdStore.get().isPresent()) {
      log.debug("Found existing nodeId.");
      return;
    }

    migrateNodeId();
  }

  private void migrateNodeId() throws Exception {
    KeyStoreManager keyStoreManager = keyStoreProvider.get();

    if (!keyStoreManager.isKeyPairInitialized()) {
      nodeIdStore.getOrCreate();
      return;
    }

    // Migrating an existing key
    log.info("Migrating node-id to database");

    Certificate certificate = keyStoreManager.getCertificate();
    log.trace("Certificate:\n{}", certificate);

    String id = NodeIdEncoding.nodeIdForCertificate(certificate);

    nodeIdStore.set(id);
  }
}
