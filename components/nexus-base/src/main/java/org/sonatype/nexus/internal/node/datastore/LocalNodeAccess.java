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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.internal.node.KeyStoreManagerImpl;
import org.sonatype.nexus.internal.node.NodeIdEncoding;
import org.sonatype.nexus.node.datastore.NodeIdStore;
import org.sonatype.nexus.ssl.KeyStoreManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;

/**
 * Local {@link NodeAccess}.
 *
 * @since 3.0
 */
@Named("local")
@Singleton
public class LocalNodeAccess
    extends StateGuardLifecycleSupport
    implements NodeAccess
{
  private final Provider<KeyStoreManager> keyStoreProvider;

  private String id;

  private NodeIdStore nodeIdStore;

  private Map<String, String> memberAliases = Collections.emptyMap();

  @Inject
  public LocalNodeAccess(
      @Named(KeyStoreManagerImpl.NAME) final Provider<KeyStoreManager> keyStoreProvider,
      final NodeIdStore nodeIdStore)
  {
    this.keyStoreProvider = checkNotNull(keyStoreProvider);
    this.nodeIdStore = checkNotNull(nodeIdStore);
  }

  @Override
  protected void doStart() throws Exception {
    Optional<String> nodeId = nodeIdStore.get();

    if (nodeId.isPresent()) {
      id = nodeId.get();
    }
    else {
      id = migrateNodeId().orElseGet(this::generateNodeId);
    }
    log.info("ID: {}", id);

    memberAliases = ImmutableMap.of(id, id);
  }

  @Override
  protected void doStop() throws Exception {
    id = null;
  }

  @Override
  @Guarded(by = STARTED)
  public String getId() {
    return id;
  }

  @Override
  public boolean isClustered() {
    return false;
  }

  @Override
  public boolean isOldestNode() {
    return true;
  }

  @Override
  public Set<String> getMemberIds() {
    return memberAliases.keySet();
  }

  @Override
  public Map<String, String> getMemberAliases() {
    return memberAliases;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        '}';
  }

  private Optional<String> migrateNodeId() throws Exception {
    KeyStoreManager keyStoreManager = keyStoreProvider.get();

    if (!keyStoreManager.isKeyPairInitialized()) {
      return Optional.empty();
    }

    // Migrating an existing key
    log.info("Migrating to database");

    Certificate certificate = keyStoreManager.getCertificate();
    log.trace("Certificate:\n{}", certificate);

    String id = NodeIdEncoding.nodeIdForCertificate(certificate);

    nodeIdStore.set(id);

    return Optional.of(id);
  }


  private String generateNodeId() {
    log.info("Generating");

    // Generate something unique
    UUID cn = UUID.randomUUID();

    // Hash it to match old certificate style
    @SuppressWarnings("deprecation")
    String newNodeId = NodeIdEncoding.nodeIdForSha1(Hashing.sha1().hashBytes(cn.toString().getBytes()).toString());

    nodeIdStore.set(newNodeId);
    return newNodeId;
  }
}
