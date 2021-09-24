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
package org.sonatype.nexus.repository.maven.internal.orient;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.OrientReplicationFacetSupport;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import static org.sonatype.nexus.repository.maven.internal.orient.MavenFacetUtils.findAsset;
import static org.sonatype.nexus.repository.maven.internal.orient.MavenFacetUtils.findComponent;

/**
 * @since 3.31
 */
@Named
public class OrientMavenReplicationFacet
    extends OrientReplicationFacetSupport
{
  @Override
  @Transactional
  public void doReplicate(final String path,
                        final AssetBlob assetBlob,
                        final NestedAttributesMap assetAttributes,
                        final NestedAttributesMap componentAttributes) {
    try {
      OrientMavenFacet mavenFacet = facet(OrientMavenFacet.class);
      MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
      StorageTx tx = UnitOfWork.currentTx();

      mavenFacet.put(mavenPath, assetBlob, assetAttributes);

      if (mavenPath.getCoordinates() != null) {
        replicateAssetAttributes(tx, mavenPath, assetAttributes);
        replicateComponent(tx, mavenPath, componentAttributes);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void replicateAssetAttributes(final StorageTx tx,
                                        final MavenPath mavenPath,
                                        final NestedAttributesMap assetAttributes) {
    final Bucket bucket = tx.findBucket(getRepository());
    Asset asset = findAsset(tx, bucket, mavenPath);
    asset.attributes(assetAttributes);
    asset.attributes().remove("last_modified");
    tx.saveAsset(asset);
  }

  private void replicateComponent(final StorageTx tx,
                                  final MavenPath mavenPath,
                                  final NestedAttributesMap componentAttributes) {
    Component component = findComponent(tx, getRepository(), mavenPath);
    if (component != null) {
      component.attributes(componentAttributes);
      tx.saveComponent(component);
    }
  }

  @Override
  @Transactional
  public boolean doReplicateDelete(final String path) {
    try {
      OrientMavenFacet mavenFacet = facet(OrientMavenFacet.class);
      MavenPath mavenPath = mavenFacet.getMavenPathParser().parsePath(path);
      return !mavenFacet.delete(mavenPath).isEmpty();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
