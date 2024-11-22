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
package org.sonatype.nexus.common.upgrade.events;

import java.util.Collection;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An event fired when a database migration has completed.
 */
public class UpgradeCompletedEvent
    extends UpgradeEventSupport
{
  private Collection<String> nodeIds;

  protected UpgradeCompletedEvent() {
    // deserialization
  }

  public UpgradeCompletedEvent(
      @Nullable final String user,
      final String schemaVersion,
      final Collection<String> nodeIds,
      final String... migrations)
  {
    super(user, schemaVersion, migrations);
    this.nodeIds = checkNotNull(nodeIds);
  }

  /**
   * Return the nodes who participated in the quorum
   */
  public Collection<String> getNodeIds() {
    return nodeIds;
  }

  public void setNodeIds(final Collection<String> nodeIds) {
    this.nodeIds = nodeIds;
  }
}
