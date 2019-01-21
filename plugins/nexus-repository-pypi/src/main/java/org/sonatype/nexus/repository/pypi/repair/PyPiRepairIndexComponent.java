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
package org.sonatype.nexus.repository.pypi.repair;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.repair.RepairMetadataComponent;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.HostedType;

import static org.sonatype.nexus.repository.pypi.internal.AssetKind.INDEX;
import static org.sonatype.nexus.repository.pypi.internal.AssetKind.ROOT_INDEX;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * @since 3.15
 */
@Named
@Singleton
public class PyPiRepairIndexComponent
    extends RepairMetadataComponent
{
  @Inject
  public PyPiRepairIndexComponent(final RepositoryManager repositoryManager,
                                  final AssetEntityAdapter assetEntityAdapter,
                                  @Named(HostedType.NAME) final Type type,
                                  @Named(PyPiFormat.NAME) final Format format)
  {
    super(repositoryManager, assetEntityAdapter, type, format);
  }

  @Override
  public void updateAsset(final Repository repository, final StorageTx tx, final Asset asset) {
    AssetKind assetKind = AssetKind.valueOf(asset.formatAttributes().get(P_ASSET_KIND, String.class));
    if (ROOT_INDEX.equals(assetKind) || INDEX.equals(assetKind)) {
      tx.deleteAsset(asset);
    }
  }
}
