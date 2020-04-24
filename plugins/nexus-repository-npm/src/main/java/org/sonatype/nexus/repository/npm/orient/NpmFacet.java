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
package org.sonatype.nexus.repository.npm.orient;

import java.io.IOException;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;

/**
 * NPM facet, present on all NPM repositories.
 *
 * @since 3.6.1
 */
@Facet.Exposed
public interface NpmFacet
    extends Facet
{
  @Nullable
  Asset findRepositoryRootAsset();

  @Nullable
  Asset putRepositoryRoot(AssetBlob assetBlob, @Nullable AttributesMap contentAttributes) throws IOException;


  @Nullable
  Asset findPackageRootAsset(String packageId);

  @Nullable
  Asset putPackageRoot(String packageId, AssetBlob assetBlob, @Nullable AttributesMap contentAttributes)
      throws IOException;

  @Nullable
  Asset findTarballAsset(String packageId,
                         String tarballName);

  @Nullable
  Asset putTarball(String packageId,
                   String tarballName,
                   AssetBlob assetBlob,
                   @Nullable AttributesMap contentAttributes) throws IOException;
}
