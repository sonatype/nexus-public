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
package org.sonatype.nexus.repository.apt.datastore.internal.hosted;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.inject.Named;

import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.metadata.AptHostedMetadataFacet;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.view.Payload;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Apt hosted facet.
 *
 * @since 3.31
 */
@Named
@Exposed
public class AptHostedFacet
    extends FacetSupport
{
  /**
   * Saves asset to the database and triggers metadata recalculation
   *
   * @param assetPath   - Asset path
   * @param payload     - Request  payload
   * @param packageInfo - Package info
   * @return - Returns Fluent asset after successful save.
   */
  public FluentAsset put(final String assetPath,
                         final Payload payload,
                         final PackageInfo packageInfo) throws IOException
  {
    checkNotNull(assetPath);
    checkNotNull(payload);
    checkNotNull(packageInfo);

    FluentAsset asset = content().put(assetPath, payload, packageInfo);
    metadata().addPackageMetadata(asset);
    metadata().removeInReleaseIndex();
    return asset;
  }

  private AptContentFacet content() {
    return facet(AptContentFacet.class);
  }

  private AptHostedMetadataFacet metadata() {
    return facet(AptHostedMetadataFacet.class);
  }

  /**
   * Method for triggering Apt metadata recalculation.
   */
  public void rebuildMetadata() throws IOException {
    rebuildMetadata(Collections.emptyList());
  }

  /**
   * Method for triggering Apt metadata recalculation with possibility to specify what actually asset was changed
   */
  public void rebuildMetadata(final List<AssetChange> changeList) throws IOException {
    metadata().rebuildMetadata(changeList);
  }
}
