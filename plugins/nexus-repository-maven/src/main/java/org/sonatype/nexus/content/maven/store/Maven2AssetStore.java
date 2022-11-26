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
package org.sonatype.nexus.content.maven.store;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.store.AssetStore;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED;

public class Maven2AssetStore
    extends AssetStore<Maven2AssetDAO>
{
  @Inject
  public Maven2AssetStore(
      final DataSessionSupplier sessionSupplier,
      @Named(DATASTORE_CLUSTERED_ENABLED_NAMED) final boolean clustered,
      @Assisted final String storeName)
  {
    super(sessionSupplier, clustered, storeName, Maven2AssetDAO.class);
  }

  @Transactional
  public Continuation<Asset> findMavenPluginAssetsForNamespace(final int repositoryId,
      final int limit,
      @Nullable final String continuationToken,
      final String namespace)
  {
    return dao().findMavenPluginAssetsForNamespace(repositoryId, limit, continuationToken, namespace);
  }
}
