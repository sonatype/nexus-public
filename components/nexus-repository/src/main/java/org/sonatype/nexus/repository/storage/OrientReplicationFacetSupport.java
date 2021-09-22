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
package org.sonatype.nexus.repository.storage;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.ReplicationMarker;

/**
 * Support for orient {@link ReplicationFacet} implementations.
 *
 * @since 3.31
 */
public abstract class OrientReplicationFacetSupport
    extends FacetSupport
    implements ReplicationFacet {

  @Override
  public void replicate(
      final String path,
      final AssetBlob assetBlob,
      final NestedAttributesMap assetAttributes,
      @Nullable final NestedAttributesMap componentAttributes)
  {
    try {
      ReplicationMarker.set(true);
      doReplicate(path, assetBlob, assetAttributes, componentAttributes);
    }
    finally {
      ReplicationMarker.unset();
    }
  }

  @Override
  public boolean replicateDelete(final String path) {
    try {
      ReplicationMarker.set(true);
      return doReplicateDelete(path);
    }
    finally {
      ReplicationMarker.unset();
    }
  }

  public abstract void doReplicate(
      String path,
      AssetBlob assetBlob,
      NestedAttributesMap assetAttributes,
      @Nullable NestedAttributesMap componentAttributes);

  public abstract boolean doReplicateDelete(String path);
}
