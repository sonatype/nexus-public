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
package org.sonatype.nexus.plugins.p2.repository.proxy.validator;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.plugins.p2.repository.P2Constants;
import org.sonatype.nexus.plugins.p2.repository.P2ProxyRepository;
import org.sonatype.nexus.plugins.p2.repository.mappings.ArtifactMapping;
import org.sonatype.nexus.plugins.p2.repository.mappings.ArtifactPath;
import org.sonatype.nexus.plugins.p2.repository.proxy.P2ProxyMetadataSource;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.LocalStorageException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.maven.AbstractChecksumContentValidator;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.RemoteHashResponse;
import org.sonatype.nexus.proxy.repository.ItemContentValidator;
import org.sonatype.nexus.proxy.repository.ProxyRepository;

/**
 * P2 checksum content validator.
 */
@Named(P2ChecksumContentValidator.ID)
@Singleton
public class P2ChecksumContentValidator
    extends AbstractChecksumContentValidator
    implements ItemContentValidator
{

  public static final String ID = "P2ChecksumContentValidator";

  @Override
  protected ChecksumPolicy getChecksumPolicy(final ProxyRepository proxy, final AbstractStorageItem item)
      throws LocalStorageException
  {
    if (P2ProxyMetadataSource.isP2MetadataItem(item.getRepositoryItemUid().getPath())) {
      // the checksum is on metadata files
      return ChecksumPolicy.IGNORE;
    }

    if (!proxy.getRepositoryKind().isFacetAvailable(P2ProxyRepository.class)) {
      return ChecksumPolicy.IGNORE;
    }

    final P2ProxyRepository p2repo = proxy.adaptToFacet(P2ProxyRepository.class);

    final ChecksumPolicy checksumPolicy = p2repo.getChecksumPolicy();

    if (checksumPolicy == null || !checksumPolicy.shouldCheckChecksum()
        || !(item instanceof DefaultStorageFileItem)) {
      // there is either no need to validate or we can't validate the item content
      return ChecksumPolicy.IGNORE;
    }

    final ResourceStoreRequest req = new ResourceStoreRequest(P2Constants.ARTIFACT_MAPPINGS_XML);
    req.setRequestLocalOnly(true);
    try {
      p2repo.retrieveItem(true, req);
    }
    catch (final Exception e) {
      // no way to calculate
      log.debug("Unable to find artifact-mapping.xml", e);
      return ChecksumPolicy.IGNORE;
    }

    return checksumPolicy;
  }

  @Override
  protected void cleanup(final ProxyRepository proxy, final RemoteHashResponse remoteHash, final boolean contentValid)
      throws LocalStorageException
  {
    // no know cleanup for p2 repos
  }

  @Override
  protected RemoteHashResponse retrieveRemoteHash(final AbstractStorageItem item, final ProxyRepository proxy,
                                                  final String baseUrl)
      throws LocalStorageException
  {
    final P2ProxyRepository p2repo = proxy.adaptToFacet(P2ProxyRepository.class);

    Map<String, ArtifactPath> paths;
    try {
      final ArtifactMapping artifactMapping = p2repo.getArtifactMappings().get(baseUrl);
      if (artifactMapping == null) {
        log.debug("Unable to retrive remote has for " + item.getPath());
        return null;
      }
      paths = artifactMapping.getArtifactsPath();
    }
    catch (StorageException e) {
      throw new LocalStorageException(e);
    }
    catch (final IllegalOperationException e) {
      log.error("Unable to open artifactsMapping.xml", e);
      return null;
    }
    final String md5 = paths.get(item.getPath()).getMd5();
    if (md5 == null) {
      return null;
    }
    return new RemoteHashResponse(DigestCalculatingInspector.DIGEST_MD5_KEY, md5, null);
  }

}
