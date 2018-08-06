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
package org.sonatype.nexus.repository.npm.internal;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.ComponentDirector;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.P_NAME;
import static org.sonatype.nexus.repository.npm.internal.NpmPackageRootMetadataUtils.createFullPackageMetadata;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractNewestVersion;

/**
 * @since 3.11
 */
@Named("npm")
@Singleton
public class NpmComponentDirector
    extends ComponentSupport
    implements ComponentDirector
{
  private NpmPackageParser npmPackageParser;

  @Inject
  public NpmComponentDirector(final NpmPackageParser npmPackageParser)
  {
    this.npmPackageParser = npmPackageParser;
  }

  @Override
  public boolean allowMoveTo(final Repository destination) {
    return true;
  }

  @Override
  public boolean allowMoveTo(final Component component, final Repository destination) {
    return true;
  }

  @Override
  public boolean allowMoveFrom(final Repository source) {
    return true;
  }

  @Override
  @Transactional
  public Component afterMove(final Component component, final Repository destination) {
    destination.optionalFacet(NpmHostedFacet.class).ifPresent(f -> {
      final StorageTx tx = UnitOfWork.currentTx();
      tx.browseAssets(component).forEach(asset -> {
        Blob blob = checkNotNull(tx.getBlob(asset.blobRef()));
        final Map<String, Object> packageJson = npmPackageParser.parsePackageJson(blob::getInputStream);
        final NpmPackageId packageId = NpmPackageId.parse((String) packageJson.get(P_NAME));

        try {
          final NestedAttributesMap updatedMetadata = createFullPackageMetadata(
              new NestedAttributesMap("metadata", packageJson),
              destination.getName(),
              blob.getMetrics().getSha1Hash(),
              destination,
              extractNewestVersion);
          f.putPackageRoot(packageId, null, updatedMetadata);
        }
        catch (IOException e) {
          log.error("Failed to update package root, packageId: {}", packageId, e);
        }
      });
    });
    return component;
  }
}
