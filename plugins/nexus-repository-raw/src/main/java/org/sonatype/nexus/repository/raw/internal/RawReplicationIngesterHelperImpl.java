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
package org.sonatype.nexus.repository.raw.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.blobstore.api.BlobAttributes;
import org.sonatype.nexus.blobstore.api.BlobStore;
import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.AttributeChangeSet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.content.fluent.FluentAssets;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.raw.RawCoordinatesHelper;
import org.sonatype.nexus.repository.raw.RawReplicationIngesterHelper;
import org.sonatype.nexus.repository.replication.ReplicationIngestionException;

import com.google.common.hash.HashCode;
import org.apache.commons.lang.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.blobstore.api.BlobStore.BLOB_NAME_HEADER;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.repository.content.AttributeOperation.SET;

/**
 * @since 3.next
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
public class RawReplicationIngesterHelperImpl
    extends ComponentSupport
    implements RawReplicationIngesterHelper
{
  public static final String CHECKSUM = "checksum";

  private final RepositoryManager repositoryManager;

  private final BlobStoreManager blobStoreManager;

  @Inject
  public RawReplicationIngesterHelperImpl(
      final RepositoryManager repositoryManager,
      final BlobStoreManager blobStoreManager)
  {
    this.repositoryManager = checkNotNull(repositoryManager);
    this.blobStoreManager = checkNotNull(blobStoreManager);
  }

  @Override
  public void replicate(
      final String blobStoreId, final Blob blob,
      final Map<String, Object> assetAttributesMap,
      final String repositoryName, final String blobStoreName)
  {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      throw new ReplicationIngestionException(
          String.format("Can't replicate blob %s, the repository %s doesn't exist", blob.getId().toString(),
              repositoryName));
    }

    BlobStore blobStore = blobStoreManager.get(blobStoreName);
    FluentAssets fluentAssets = repository.facet(ContentFacet.class).assets();
    FluentComponents fluentComponents = repository.facet(ContentFacet.class).components();
    BlobAttributes blobAttributes = blobStore.getBlobAttributes(blob.getId());
    String path = normalizePath(blobAttributes.getHeaders().get(BLOB_NAME_HEADER));

    FluentAsset fluentAsset = fluentAssets.path(path)
        .component(fluentComponents
            .name(path)
            .namespace(RawCoordinatesHelper.getGroup(path))
            .getOrCreate())
        .blob(blob, getChecksumsFromProperties(assetAttributesMap))
        .save();

    AttributeChangeSet changeSet = new AttributeChangeSet();
    for (Entry<String, Object> entry : assetAttributesMap.entrySet()) {
      changeSet.attributes(SET, entry.getKey(), entry.getValue());
    }
    fluentAsset.attributes(changeSet);
  }

  @Override
  public void deleteReplication(final String path, final String repositoryName) {
    Repository repository = repositoryManager.get(repositoryName);
    if (repository == null) {
      throw new ReplicationIngestionException(
          String.format("Can't delete blob in path %s, the repository %s doesn't exist", path, repositoryName));
    }
    ContentMaintenanceFacet componentMaintenance = repository.facet(ContentMaintenanceFacet.class);
    FluentAssets fluentAssets = repository.facet(ContentFacet.class).assets();

    Optional<FluentAsset> result = fluentAssets.path(normalizePath(path)).find();

    if (result.isPresent()) {
      componentMaintenance.deleteAsset(result.get());
    }
  }

  private Map<HashAlgorithm, HashCode> getChecksumsFromProperties(Map<String, Object> attributesMap) {
    Map<HashAlgorithm, HashCode> checksums = new HashMap<>();

    String md5Value = "";
    try {
      md5Value = ((Map<String, Object>) attributesMap.get(CHECKSUM)).get(HashAlgorithm.MD5.name()).toString();
    }
    catch (Exception e) {
      log.warn("Cannot extract md5 checksum from attributes");
    }

    if (StringUtils.isNotEmpty(md5Value)) {
      checksums.put(HashAlgorithm.MD5, HashCode.fromString(md5Value));
    }

    String sha256Value = "";
    try {
      sha256Value = ((Map<String, Object>) attributesMap.get(CHECKSUM)).get(HashAlgorithm.SHA256.name()).toString();
    }
    catch (Exception e) {
      log.warn("Cannot extract sha256 checksum from attributes");
    }

    if (StringUtils.isNotEmpty(sha256Value)) {
      checksums.put(HashAlgorithm.SHA256, HashCode.fromString(sha256Value));
    }

    String sha1Value = "";
    try {
      sha1Value = ((Map<String, Object>) attributesMap.get(CHECKSUM)).get(HashAlgorithm.SHA1.name()).toString();
    }
    catch (Exception e) {
      log.warn("Cannot extract sha1 checksum from attributes");
    }

    if (StringUtils.isNotEmpty(sha1Value)) {
      checksums.put(HashAlgorithm.SHA1, HashCode.fromString(sha1Value));
    }

    String sha512Value = "";
    try {
      sha512Value = ((Map<String, Object>) attributesMap.get(CHECKSUM)).get(HashAlgorithm.SHA512.name()).toString();
    }
    catch (Exception e) {
      log.warn("Cannot extract sha512 checksum from attributes");
    }

    if (StringUtils.isNotEmpty(sha512Value)) {
      checksums.put(HashAlgorithm.SHA512, HashCode.fromString(sha512Value));
    }
    return checksums;
  }

  private String normalizePath(final String path) {
    if (path.startsWith("/")) {
      return path;
    }
    return "/" + path;
  }
}
