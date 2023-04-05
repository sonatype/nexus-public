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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.lifecycle.Lifecycle;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.node.NodeAccess;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;

/**
 * {@link NodeAccess} booter.
 *
 * @since 3.24
 */
@Named
@ManagedLifecycle(phase = STORAGE)
@Priority(Integer.MAX_VALUE) // make sure this starts first
@Singleton
public class NodeAccessBooter
    implements Lifecycle
{
  private final NodeIdInitializer nodeIdInitializer;

  private final NodeAccess nodeAccess;

  @Inject
  public NodeAccessBooter(final NodeIdInitializer nodeIdInitializer, final NodeAccess nodeAccess) {
    this.nodeIdInitializer = checkNotNull(nodeIdInitializer);
    this.nodeAccess = checkNotNull(nodeAccess);
  }

  @Override
  public void start() throws Exception {
    nodeIdInitializer.initialize();
    nodeAccess.start();
  }

  @Override
  public void stop() throws Exception {
    nodeAccess.stop();
  }
}
