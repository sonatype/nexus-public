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
package org.sonatype.nexus.repository.content.replication;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.ReplicationMarker;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;

/**
 * Support for {@link ReplicationFacet} implementations.
 *
 * @since 3.31
 */
public abstract class ReplicationFacetSupport
    extends FacetSupport
    implements ReplicationFacet
{
  @Override
  public void replicate(final String path,
                 final Blob blob,
                 final Map<String, Object> assetAttributes,
                 final @Nullable Map<String, Object> componentAttributes) {
    try {
      ReplicationMarker.set(true);
      doReplicate(path, blob, assetAttributes, componentAttributes);
    } finally {
      ReplicationMarker.unset();
    }
  }

  @Override
  public boolean replicateDelete(final String path) {
    try {
      ReplicationMarker.set(true);
      return doReplicateDelete(path);
    } finally {
      ReplicationMarker.unset();
    }
  }

  public abstract void doReplicate(
      String path,
      Blob blob,
      Map<String, Object> assetAttributes,
      @Nullable Map<String, Object> componentAttributes);

  public boolean doReplicateDelete(final String path) {
    ContentFacet contentFacet = facet(ContentFacet.class);
    ContentMaintenanceFacet componentMaintenance = facet(ContentMaintenanceFacet.class);
    FluentAssets fluentAssets = contentFacet.assets();
    Optional<FluentAsset> result = fluentAssets.path(path).find();
    result.ifPresent(componentMaintenance::deleteAsset);
    return result.isPresent();
  }
}
