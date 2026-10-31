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
package org.sonatype.nexus.repository.content.event.asset;

import org.sonatype.nexus.repository.content.Asset;

/**
 * Event sent whenever an asset's path is changed.
 *
 * @since 3.26
 */
public class AssetPathChangedEvent
    extends AssetUpdatedEvent
{
  private final String oldPath;

  private final String newPath;

  public AssetPathChangedEvent(final Asset asset, final String oldPath, final String newPath) {
    super(asset);
    this.oldPath = oldPath;
    this.newPath = newPath;
  }

  public String getOldPath() {
    return oldPath;
  }

  public String getNewPath() {
    return newPath;
  }

  @Override
  public String toString() {
    return "AssetPathChangedEvent{" +
        "oldPath='" + oldPath + '\'' +
        ", newPath='" + newPath + '\'' +
        "} " + super.toString();
  }
}
