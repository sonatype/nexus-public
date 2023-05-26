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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.orient.maven.OrientMavenFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.importtask.ImportFileConfiguration;
import org.sonatype.nexus.repository.maven.MavenFacet;
import org.sonatype.nexus.repository.maven.MavenHostedFacet;
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
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.PathPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.hash.HashCode;
import org.joda.time.DateTime;

/**
 * Support for uploading components via UI & API
 *
 * @since 3.7
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
        uploadDefinitionExtensions, false);
  }

  @Override
  protected UploadResponse getUploadResponse(final Repository repository,
                                             final ComponentUpload componentUpload,
                                             final String basePath)
      throws IOException
  {
    ContentAndAssetPathResponseData responseData;

    StorageFacet storageFacet = repository.facet(StorageFacet.class);
    UnitOfWork.begin(storageFacet.txSupplier());
    try {
      responseData = createAssets(repository, basePath, componentUpload.getAssetUploads());

      maybeGeneratePom(repository, componentUpload, basePath, responseData);

      updateMetadata(repository, responseData.getCoordinates());
    }
    finally {
      UnitOfWork.end();
    }
    return responseData.uploadResponse();
  }

  private void updateMetadata(final Repository repository, final Coordinates coordinates) {
    if (coordinates != null) {
      repository.facet(MavenHostedFacet.class)
          .rebuildMetadata(coordinates.getGroupId(), coordinates.getArtifactId(), coordinates.getVersion(), false);
    }
    else {
      log.debug("Not updating metadata.xml files since coordinate could not be retrieved from path");
    }
  }

  @Override
  protected Content doPut(ImportFileConfiguration configuration)
      throws IOException
  {
    OrientMavenFacet mavenFacet = configuration.getRepository().facet(OrientMavenFacet.class);
    MavenPath mavenPath = parser.parsePath(configuration.getAssetName());
    File content = configuration.getFile();
    Path contentPath = content.toPath();

    if (configuration.isHardLinkingEnabled()) {
      final AttributesMap contentAttributes = new AttributesMap();
      contentAttributes.set(Content.CONTENT_LAST_MODIFIED, new DateTime(Files.getLastModifiedTime(contentPath).toMillis()));

      byte[] bytes = Files.readAllBytes(contentPath);
      Map<HashAlgorithm, HashCode> hashes =
          HashType.ALGORITHMS.stream().collect(Collectors.toMap(a -> a, a -> a.function().hashBytes(bytes)));
      return mavenFacet.put(
          mavenPath,
          contentPath,
          configuration.getAssetName(),
          contentAttributes,
          hashes,
          Files.size(contentPath));
    }
    else {
      try (Payload payload = new PathPayload(contentPath, Files.probeContentType(contentPath))) {
        return doPut(configuration.getRepository(), mavenPath, payload);
      }
    }
  }

  @Override
  protected Content doPut(final Repository repository, final MavenPath mavenPath, final Payload payload)
      throws IOException
  {
    OrientMavenFacet mavenFacet = repository.facet(OrientMavenFacet.class);
    Content asset = mavenFacet.put(mavenPath, payload);
    putChecksumFiles(mavenFacet, mavenPath, asset);
    return asset;
  }


  @Override
  protected VersionPolicy getVersionPolicy(final Repository repository) {
    return repository.facet(MavenFacet.class).getVersionPolicy();
  }

  @Override
  protected TempBlob createTempBlob(final Repository repository, final PartPayload payload)
  {
    return repository.facet(StorageFacet.class).createTempBlob(payload, MavenPath.HashType.ALGORITHMS);
  }

  private void putChecksumFiles(final OrientMavenFacet facet, final MavenPath path, final Content content) throws IOException {
    DateTime dateTime = content.getAttributes().require(Content.CONTENT_LAST_MODIFIED, DateTime.class);
    Map<HashAlgorithm, HashCode> hashes = MavenFacetUtils.getHashAlgorithmFromContent(content.getAttributes());
    MavenFacetUtils.addHashes(facet, path, hashes, dateTime);
  }
}
