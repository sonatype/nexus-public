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
package org.sonatype.nexus.repository.npm.repair;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmHostedFacet;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.repair.RepairMetadataComponent;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.AssetEntityAdapter;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.hash.HashCode;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.StreamSupport.stream;
import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA512;
import static org.sonatype.nexus.common.hash.Hashes.hash;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.AssetKind.TARBALL;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.P_NAME;
import static org.sonatype.nexus.repository.npm.internal.NpmAttributes.P_VERSION;
import static org.sonatype.nexus.repository.npm.internal.NpmPackageRootMetadataUtils.createFullPackageMetadata;
import static org.sonatype.nexus.repository.npm.internal.NpmPackageRootMetadataUtils.getPackageRoot;
import static org.sonatype.nexus.repository.npm.internal.NpmVersionComparator.extractPackageRootVersionUnlessEmpty;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;

/**
 * Reprocesses each package and updates the package root. This fixes a problem where checksums were incorrectly updated
 * because of the following bug in NXRM https://issues.sonatype.org/browse/NEXUS-15425
 *
 * @since 3.10
 */
@Named
@Singleton
public class NpmRepairPackageRootComponent
    extends RepairMetadataComponent
{
  private static final String SHASUM = "shasum";

  private static final String DIST = "dist";

  private static final String VERSIONS = "versions";

  private static final String INTEGRITY = "integrity";

  private final NpmPackageParser npmPackageParser;

  @Inject
  public NpmRepairPackageRootComponent(final RepositoryManager repositoryManager,
                                       final AssetEntityAdapter assetEntityAdapter,
                                       final NpmPackageParser npmPackageParser,
                                       @Named(HostedType.NAME) final Type hostedType,
                                       @Named(NpmFormat.NAME) final Format npmFormat)
  {
    super(repositoryManager, assetEntityAdapter, hostedType, npmFormat);
    this.npmPackageParser = checkNotNull(npmPackageParser);
  }

  public void repair() {
    log.info("Beginning processing all npm packages for repair");

    stream(repositoryManager.browse().spliterator(), false)
        .filter(this::shouldRepairRepository)
        .forEach(this::doRepairRepository);
  }

  @Override
  public void updateAsset(final Repository repository, final StorageTx tx, final Asset asset) {
    if (TARBALL.name().equals(asset.formatAttributes().get(P_ASSET_KIND))) {
      Blob blob = tx.getBlob(asset.blobRef());
      if (blob != null) {
        maybeUpdateAsset(repository, asset, blob);
      }
    }
  }

  private void maybeUpdateAsset(final Repository repository, final Asset asset, final Blob blob) {
    Map<String, Object> packageJson = npmPackageParser.parsePackageJson(blob::getInputStream);

    NestedAttributesMap updatedMetadata = createFullPackageMetadata(
        new NestedAttributesMap("metadata", packageJson),
        repository.getName(),
        blob.getMetrics().getSha1Hash(),
        repository,
        extractPackageRootVersionUnlessEmpty);

    updatePackageRootIfShaIncorrect(repository, asset, blob, updatedMetadata,
        NpmPackageId.parse((String) packageJson.get(P_NAME)), (String) packageJson.get(P_VERSION));
  }

  private void updatePackageRootIfShaIncorrect(final Repository repository,
                                               final Asset asset,
                                               final Blob blob,
                                               final NestedAttributesMap newPackageRoot,
                                               final NpmPackageId packageId,
                                               final String packageVersion)
  {
    NpmHostedFacet hostedFacet = repository.facet(NpmHostedFacet.class);
    try {
      NestedAttributesMap oldPackageRoot = getPackageRoot(UnitOfWork.currentTx(), repository, packageId);
      if (oldPackageRoot != null) {
        String oldSha = extractShasum(oldPackageRoot, packageVersion);
        String newSha = extractShasum(newPackageRoot, packageVersion);

        if (!Objects.equals(oldSha, newSha)) {
          maybeUpdateIntegrity(asset, blob, packageVersion, oldPackageRoot, newPackageRoot);

          hostedFacet.putPackageRoot(packageId, null, newPackageRoot);
        }
      }
    }
    catch (IOException e) {
      log.error("Failed to update asset {}", asset.name(), e);
    }
  }

  private String extractShasum(final NestedAttributesMap oldPackageRoot, final String packageVersion) {
    return getDist(packageVersion, oldPackageRoot)
        .get(SHASUM, String.class);
  }

  private void maybeUpdateIntegrity(final Asset asset,
                                    final Blob blob,
                                    final String packageVersion,
                                    final NestedAttributesMap oldPackageRoot,
                                    final NestedAttributesMap metadata)
  {
    String incorrectIntegrity = getDist(packageVersion, oldPackageRoot)
        .get(INTEGRITY, String.class);

    if (!isNullOrEmpty(incorrectIntegrity)) {
      String algorithm = incorrectIntegrity.split("-")[0];

      getDist(packageVersion, metadata)
          .set(INTEGRITY, calculateIntegrity(asset, blob, algorithm));
    }
  }

  private NestedAttributesMap getDist(final String packageVersion, final NestedAttributesMap packageRoot) {
    return packageRoot.child(VERSIONS).child(packageVersion)
        .child(DIST);
  }

  private String calculateIntegrity(final Asset asset, final Blob blob, final String algorithm) {
    try {
      HashCode hash;
      if (algorithm.equalsIgnoreCase(SHA1.name())) {
        hash = hash(SHA1, blob.getInputStream());
      }
      else {
        hash = hash(SHA512, blob.getInputStream());
      }

      return algorithm + "-" + Base64.getEncoder().encodeToString(hash.asBytes());
    }
    catch (IOException e) {
      log.error("Failed to calculate hash for asset {}", asset.name(), e);
    }
    return "";
  }
}
