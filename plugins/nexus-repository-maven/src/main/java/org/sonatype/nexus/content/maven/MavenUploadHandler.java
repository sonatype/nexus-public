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
package org.sonatype.nexus.content.maven;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.AssetBlob;
import org.sonatype.nexus.repository.maven.MavenPath;
import org.sonatype.nexus.repository.maven.MavenPath.Coordinates;
import org.sonatype.nexus.repository.maven.MavenPath.HashType;
import org.sonatype.nexus.repository.maven.MavenUploadHandlerSupport;
import org.sonatype.nexus.repository.maven.VersionPolicy;
import org.sonatype.nexus.repository.maven.internal.Maven2Format;
import org.sonatype.nexus.repository.maven.internal.Maven2MavenPathParser;
import org.sonatype.nexus.repository.maven.internal.MavenPomGenerator;
import org.sonatype.nexus.repository.maven.internal.VersionPolicyValidator;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StringPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import org.joda.time.DateTime;

import static org.sonatype.nexus.repository.maven.internal.Constants.CHECKSUM_CONTENT_TYPE;
import static org.sonatype.nexus.repository.view.Content.CONTENT_LAST_MODIFIED;

/**
 * Support for uploading maven components via UI & API
 *
 * @since 3.26
 */
@Named(Maven2Format.NAME)
@Singleton
public class MavenUploadHandler
    extends MavenUploadHandlerSupport
{
  @Inject
  public MavenUploadHandler(
      final Maven2MavenPathParser parser,
      @Named(Maven2Format.NAME) final VariableResolverAdapter variableResolverAdapter,
      final ContentPermissionChecker contentPermissionChecker,
      final VersionPolicyValidator versionPolicyValidator,
      final MavenPomGenerator mavenPomGenerator,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(parser, variableResolverAdapter, contentPermissionChecker, versionPolicyValidator, mavenPomGenerator,
        uploadDefinitionExtensions);
  }

  @Override
  protected ContentAndAssetPathResponseData getResponseContents(final Repository repository,
                                                                final ComponentUpload componentUpload,
                                                                final String basePath) throws IOException
  {
    ContentAndAssetPathResponseData responseData =
        createAssets(repository, basePath, componentUpload.getAssetUploads());
    maybeGeneratePom(repository, componentUpload, basePath, responseData);
    updateMetadata(repository, responseData.getCoordinates());
    return responseData;
  }

  private void updateMetadata(final Repository repository, final Coordinates coordinates) {
    if (coordinates != null) {
      repository.facet(MavenMetadataRebuildFacet.class)
          .rebuildMetadata(coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(),
              false, false);
    }
    else {
      log.debug("Not updating metadata.xml files since coordinate could not be retrieved from path");
    }
  }

  @Override
  protected Content doPut(final Repository repository, final MavenPath mavenPath, final Payload payload)
      throws IOException
  {
    MavenContentFacet mavenFacet = repository.facet(MavenContentFacet.class);
    Content asset = mavenFacet.put(mavenPath, payload);
    putChecksumFiles(mavenFacet, mavenPath, asset);
    return asset;
  }

  @Override
  protected VersionPolicy getVersionPolicy(final Repository repository) {
    return repository.facet(MavenContentFacet.class).getVersionPolicy();
  }

  @Override
  protected TempBlob createTempBlob(final Repository repository, final PartPayload payload) {
    return repository.facet(MavenContentFacet.class).blobs().ingest(payload, MavenPath.HashType.ALGORITHMS);
  }

  private void putChecksumFiles(final MavenContentFacet facet, final MavenPath path, final Content content)
      throws IOException
  {
    DateTime dateTime = content.getAttributes().require(CONTENT_LAST_MODIFIED, DateTime.class);
    for (Entry<String, String> e : getChecksumsFromContent(content).entrySet()) {
      Optional<HashAlgorithm> hashAlgorithm = HashAlgorithm.getHashAlgorithm(e.getKey())
        .filter(HashType.ALGORITHMS::contains);
      if (hashAlgorithm.isPresent()) {
        Content c = new Content(new StringPayload(e.getValue(), CHECKSUM_CONTENT_TYPE));
        c.getAttributes().set(CONTENT_LAST_MODIFIED, dateTime);
        facet.put(path.hash(HashType.valueOf(e.getKey().toUpperCase())), c);
      }
    }
  }

  private Map<String, String> getChecksumsFromContent(final Content content) {
    return Optional.ofNullable(content.getAttributes().get(Asset.class))
          .flatMap(Asset::blob)
          .map(AssetBlob::checksums)
          .orElse(Collections.emptyMap());
  }
}
