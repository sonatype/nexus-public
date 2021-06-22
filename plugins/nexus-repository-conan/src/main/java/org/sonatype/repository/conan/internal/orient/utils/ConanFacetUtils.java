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
package org.sonatype.repository.conan.internal.orient.utils;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.repository.conan.internal.metadata.ConanCoords;

import static java.util.Collections.singletonList;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;
import static org.sonatype.repository.conan.internal.orient.proxy.v1.OrientConanProxyHelper.getComponentVersion;

/**
 * @since 3.28
 */
public class ConanFacetUtils
{
  public static Asset findAsset(final StorageTx tx, final Bucket bucket, final String assetName) {
    return tx.findAssetWithProperty(P_NAME, assetName, bucket);
  }

  /**
   * Find a component by its name and tag (version)
   *
   * @return found component or null if not found
   */
  @Nullable
  public static Component findComponent(final StorageTx tx,
                                        final Repository repository,
                                        final ConanCoords coords)
  {
    Iterable<Component> components = tx.findComponents(
        Query.builder()
            .where(P_GROUP).eq(coords.getGroup())
            .and(P_NAME).eq(coords.getProject())
            .and(P_VERSION).eq(getComponentVersion(coords))
            .build(),
        singletonList(repository)
    );
    if (components.iterator().hasNext()) {
      return components.iterator().next();
    }
    return null;
  }
}
