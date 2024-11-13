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
package org.sonatype.nexus.repository.browse.node;

import java.time.OffsetDateTime;
import javax.annotation.Nullable;

import org.sonatype.nexus.common.entity.EntityId;

/**
 * Represents a path segment in a tree hierarchy.
 *
 * @since 3.6
 */
public interface BrowseNode
{
  /**
   * @since 3.18
   */
  String getPath();

  /**
   * @since 3.7
   */
  String getName();

  /**
   * @since 3.6.1
   */
  boolean isLeaf();

  @Nullable
  EntityId getComponentId();

  @Nullable
  String getPackageUrl();

  @Nullable
  EntityId getAssetId();

  @Nullable
  Long getAssetCount();

  @Nullable
  OffsetDateTime getLastUpdated();
}
