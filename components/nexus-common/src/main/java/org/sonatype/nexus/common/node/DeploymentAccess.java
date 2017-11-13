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

package org.sonatype.nexus.common.node;

import javax.annotation.Nullable;

import org.sonatype.goodies.lifecycle.Lifecycle;

/**
 * Provides access to managing deployment identity.
 *
 * {@link #getId()} is uniquely generated at first run and is immutable. In non-clustered deployments,
 * this value is likely to be the same as {@link NodeAccess#getId()}.
 *
 * In clustered environments however, {@link #getId()} will be the same across all nodes, where as
 * node identity ({@link NodeAccess#getId()}) will differ from node to node.
 *
 * @since 3.6.1
 */
public interface DeploymentAccess
    extends Lifecycle
{
  /**
   * @return the String id (never null)
   */
  String getId();

  /**
   * Alias is intended to be a user-provided identity for this deployment, similar to friendly node name.
   * Like {@link #getId()}, the value returned by this method will be consistent across all nodes in a cluster.
   *
   * @return the String id, or null if not set
   */
  @Nullable
  String getAlias();

  /**
   * @param newAlias the new alias for this deployment
   */
  void setAlias(@Nullable String newAlias);
}
