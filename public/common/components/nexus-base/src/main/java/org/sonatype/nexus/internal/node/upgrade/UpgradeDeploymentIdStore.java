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
package org.sonatype.nexus.internal.node.upgrade;

import java.util.Optional;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.UPGRADE;

/**
 * UPGRADE-phase-safe equivalent of {@code DeploymentIdStore} that reads/writes the {@code deployment_id}
 * table directly via SQL, avoiding the {@code ConfigStoreSupport} -> {@code EventManager} (EVENTS phase)
 * dependency.
 */
@Component
@ManagedLifecycle(phase = UPGRADE)
public class UpgradeDeploymentIdStore
    extends SingleRowStringUpgradeStore
{
  @Autowired
  public UpgradeDeploymentIdStore(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier, "deployment_id", "deployment_id");
  }

  public Optional<String> get() {
    return read();
  }

  public void set(final String deploymentId) {
    write(deploymentId);
  }
}
