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
package org.sonatype.nexus.repository.pypi.datastore.internal;

import java.util.Set;

import javax.inject.Named;

import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.content.maintenance.LastAssetMaintenanceFacet;
import org.sonatype.nexus.repository.pypi.internal.AssetKind;
import org.sonatype.nexus.repository.pypi.internal.PyPiFormat;
import org.sonatype.nexus.repository.pypi.internal.PyPiIndexFacet;

import static org.sonatype.nexus.repository.pypi.internal.PyPiAttributes.P_NAME;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * {@link ContentMaintenanceFacet} for PyPI where components should be deleted along with their last asset
 * and associated index file.
 *
 * @since 3.29
 */
@Named
public class PyPiLastAssetMaintenanceFacet
    extends LastAssetMaintenanceFacet
{
  @Override
  public Set<String> deleteAsset(final Asset asset) {
    PyPiIndexFacet facet = getRepository().facet(PyPiIndexFacet.class);
    facet.deleteRootIndex();
    if (assetKindIsPackage(asset)) {
      // We are deleting a package and therefore need to remove the associated index as it is no longer valid
      facet.deleteIndex(asset.attributes(PyPiFormat.NAME).get(P_NAME, String.class));
    }
    return super.deleteAsset(asset);
  }

  @Override
  public Set<String> deleteComponent(final Component component) {
    PyPiIndexFacet facet = getRepository().facet(PyPiIndexFacet.class);
    facet.deleteRootIndex();
    facet.deleteIndex(component.name());

    return super.deleteComponent(component);
  }

  private boolean assetKindIsPackage(final Asset asset) {
    return AssetKind.PACKAGE.name().equals(asset.attributes(PyPiFormat.NAME).get(P_ASSET_KIND));
  }
}
