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

/**
 * Registration for {@link DatabaseClusterManager}s that need to perform operations on {@link DatabaseClusterManager}
 * operations.
 *
 * @since 3.4
 */
public interface DatabaseClusterManagerRegistry
{

  /**
   * Adds other cluster managers that may need to be called when removing servers from a cluster
   *
   * @param manager
   */
  void registerClusterManager(final DatabaseClusterManager manager);

}
