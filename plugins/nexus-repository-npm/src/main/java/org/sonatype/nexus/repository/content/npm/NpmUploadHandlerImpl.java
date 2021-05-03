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
package org.sonatype.nexus.repository.content.npm;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.NpmUploadHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;

/**
 * Support for uploading components via UI & API
 *
 * @since 3.next
 */
@Singleton
@Named(NpmFormat.NAME)
public class NpmUploadHandlerImpl
    extends UploadHandlerSupport
    implements NpmUploadHandler
{
  private UploadDefinition definition;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapter variableResolverAdapter;

  private final NpmPackageParser npmPackageParser;

  protected static final List<HashAlgorithm> HASH_ALGORITHMS = Lists.newArrayList(SHA1);

  @Inject
  public NpmUploadHandlerImpl(final ContentPermissionChecker contentPermissionChecker,
                              @Named("simple") final VariableResolverAdapter variableResolverAdapter,
                              final NpmPackageParser npmPackageParser,
                              final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(uploadDefinitionExtensions);
    this.contentPermissionChecker = contentPermissionChecker;
    this.variableResolverAdapter = checkNotNull(variableResolverAdapter);
    this.npmPackageParser = checkNotNull(npmPackageParser);
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    NpmContentFacet contentFacet = repository.facet(NpmContentFacet.class);
    INpmHostedFacet hostedFacet = repository.facet(INpmHostedFacet.class);

    PartPayload payload = upload.getAssetUploads().get(0).getPayload();
    if (!payload.getName().endsWith(".tgz")) {
      throw new IllegalArgumentException("Unsupported extension. Extension must be .tgz");
    }

    try (TempBlob tempBlob = contentFacet.blobs().ingest(payload, HASH_ALGORITHMS)) {
      final Map<String, Object> packageJson = npmPackageParser.parsePackageJson(tempBlob);
      ensureNpmPermitted(repository, packageJson);
      Content content = hostedFacet.putPackage(packageJson, tempBlob);

      NpmPackageId packageId = NpmPackageId.parse((String) checkNotNull(packageJson.get(NpmAttributes.P_NAME)));
      String version = (String) packageJson.get(NpmAttributes.P_VERSION);
      return new UploadResponse(content, singletonList(NpmContentFacet.tarballPath(packageId, version)));
    }
  }

  @Override
  public Content handle(
      final Repository repository,
      final File content,
      final String path)
      throws IOException
  {
    NpmContentFacet contentFacet = repository.facet(NpmContentFacet.class);
    INpmHostedFacet hostedFacet = repository.facet(INpmHostedFacet.class);

    Path contentPath = content.toPath();
    try (Payload payload = new StreamPayload(() -> new BufferedInputStream(Files.newInputStream(content.toPath())),
        content.length(), Files.probeContentType(contentPath));) {
      try (TempBlob tempBlob = contentFacet.blobs().ingest(payload, HASH_ALGORITHMS)) {
        final Map<String, Object> packageJson = npmPackageParser.parsePackageJson(tempBlob);
        ensureNpmPermitted(repository, packageJson);
        return hostedFacet.putPackage(packageJson, tempBlob);
      }
    }
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      definition = getDefinition(NpmFormat.NAME, false);
    }
    return definition;
  }

  @Override
  public VariableResolverAdapter getVariableResolverAdapter() {
    return variableResolverAdapter;
  }

  @Override
  public ContentPermissionChecker contentPermissionChecker() {
    return contentPermissionChecker;
  }
}
