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
package org.sonatype.nexus.internal.node.datastore.upgrade;

import java.util.Optional;
import java.util.UUID;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.internal.node.NodeIdEncoding;
import org.sonatype.nexus.internal.node.upgrade.SingleRowStringUpgradeStore;

import com.google.common.hash.Hashing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;

/**
 * UPGRADE-phase-safe equivalent of {@code NodeIdStore} that reads/writes the {@code node_id} table
 * directly via SQL, avoiding the {@code ConfigStoreSupport} -> {@code EventManager} (EVENTS phase)
 * dependency.
 */
@Component
@ManagedLifecycle(phase = UPGRADE)
public class UpgradeNodeIdStore
    extends SingleRowStringUpgradeStore
{
  @Autowired
  public UpgradeNodeIdStore(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier, "node_id", "node_id");
  }

  public Optional<String> get() {
    return read();
  }

  public void set(final String nodeId) {
    write(nodeId);
  }

  public String getOrCreate() {
    return get().orElseGet(() -> {
      String newNodeId = generateNodeId();
      set(newNodeId);
      return newNodeId;
    });
  }

  // SHA-1 (and the platform-default charset) are preserved deliberately: this mirrors the production
  // NodeIdStore algorithm so migrated node IDs match the values legacy installs already generated.
  // Changing the hash or encoding here would produce different IDs and break that compatibility.
  @SuppressWarnings("deprecation")
  private String generateNodeId() {
    UUID cn = UUID.randomUUID();
    return NodeIdEncoding.nodeIdForSha1(Hashing.sha1().hashBytes(cn.toString().getBytes()).toString());
  }
}
