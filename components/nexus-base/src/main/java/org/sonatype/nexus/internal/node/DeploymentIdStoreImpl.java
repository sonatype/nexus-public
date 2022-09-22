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

import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.datastore.ConfigStoreSupport;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.transaction.Transactional;

import static com.google.common.base.Preconditions.checkNotNull;

@Named
@Singleton
public class DeploymentIdStoreImpl
    extends ConfigStoreSupport<DeploymentIdDAO>
    implements DeploymentIdStore
{
  private boolean initialized;

  private Optional<String> deploymentId;

  @Inject
  public DeploymentIdStoreImpl(final DataSessionSupplier sessionSupplier) {
    super(sessionSupplier);
  }

  @Transactional
  @Override
  public Optional<String> get() {
    if (!initialized) {
      deploymentId = dao().get();

      if (deploymentId.isPresent()) {
        initialized = true;
      }
    }

    return deploymentId;
  }

  @Transactional
  @Override
  public void set(final String deploymentId) {
    checkNotNull(deploymentId);
    dao().set(deploymentId);

    this.deploymentId = Optional.of(deploymentId);
    this.initialized = true;
  }
}
