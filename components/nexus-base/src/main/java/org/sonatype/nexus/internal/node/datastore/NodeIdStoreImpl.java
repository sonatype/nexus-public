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

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.datastore.api.DuplicateKeyException;
import org.sonatype.nexus.internal.node.NodeIdEncoding;
import org.sonatype.nexus.node.datastore.NodeIdStore;
import org.sonatype.nexus.transaction.Transactional;

import com.google.common.hash.Hashing;

/**
 * @since 3.37
 */
@Named("mybatis")
@Singleton
public class NodeIdStoreImpl
    extends ConfigStoreSupport<NodeIdDAO>
    implements NodeIdStore
{
  @Inject
  public NodeIdStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  /**
   * Remove the currently persisted node id, this will not change the {@link NodeAccess}.
   */
  @Transactional
  @Override
  public void clear() {
    dao().clear();
  }

  /**
   * Retrieve the current node id if it exists.
   *
   * @return the node id
   */
  @Transactional
  @Override
  public Optional<String> get() {
    return dao().get();
  }

  /**
   * Set the current node id, this will not update the {@link NodeAccess}
   *
   * @param nodeId
   */
  @Transactional
  @Override
  public void set(final String nodeId) {
    dao().set(nodeId);
  }

  @Transactional(retryOn = DuplicateKeyException.class)
  @Override
  public String getOrCreate() {
    return get()
        .orElseGet(() -> {
          String newNodeId = generateNodeId();
          dao().create(newNodeId);
          return newNodeId;
        });
  }

  private String generateNodeId() {
    log.debug("Generating nodeId");

    // Generate something unique
    UUID cn = UUID.randomUUID();

    // Hash it to match old certificate style
    @SuppressWarnings("deprecation")
    String newNodeId = NodeIdEncoding.nodeIdForSha1(Hashing.sha1().hashBytes(cn.toString().getBytes()).toString());

    return newNodeId;
  }
}
