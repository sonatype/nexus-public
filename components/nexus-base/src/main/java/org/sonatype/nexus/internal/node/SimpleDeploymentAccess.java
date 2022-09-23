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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.node.DeploymentAccess;
import org.sonatype.nexus.common.node.NodeAccess;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple {@link DeploymentAccess} that just delegates to {@link NodeAccess} and doesn't allow aliases.
 *
 * @since 3.21
 */
@Named
@Singleton
public class SimpleDeploymentAccess
    implements DeploymentAccess
{
  private final DeploymentIdStore deploymentIdStore;

  @Inject
  public SimpleDeploymentAccess(final DeploymentIdStore deploymentIdStore) {
    this.deploymentIdStore = checkNotNull(deploymentIdStore);
  }

  @Override
  public String getId() {
    return deploymentIdStore.get()
        .orElseThrow(() -> new RuntimeException("Deployment id is absent."));
  }

  @Override
  public String getAlias() {
    return null; // alias is never set
  }

  @Override
  public void setAlias(final String newAlias) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void start() {
    // no-op
  }

  @Override
  public void stop() {
    // no-op
  }
}
