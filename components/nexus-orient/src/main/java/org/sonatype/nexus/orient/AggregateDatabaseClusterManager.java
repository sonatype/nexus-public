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
package org.sonatype.nexus.orient;

import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;

import com.google.common.collect.Sets;

/**
 * Implementation of {@link DatabaseClusterManager} that supports registration.
 *
 * @since 3.4
 */
@Named
@Singleton
public class AggregateDatabaseClusterManager
    extends ComponentSupport
    implements DatabaseClusterManager, DatabaseClusterManagerRegistry
{

  private final Set<DatabaseClusterManager> registrations = Sets.newCopyOnWriteArraySet();

  @Override
  public void removeServer(final String nodeId) {
    registrations.forEach(registration -> registration.removeServer(nodeId));
  }

  @Override
  public void removeNodeFromConfiguration(final String nodeId) {
    registrations.forEach(registration -> registration.removeNodeFromConfiguration(nodeId));
  }

  public void registerClusterManager(final DatabaseClusterManager manager) {
    registrations.add(manager);
  }

}
