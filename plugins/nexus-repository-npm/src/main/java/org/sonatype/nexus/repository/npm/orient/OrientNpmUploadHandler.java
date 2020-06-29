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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.npm.NpmUploadHandler;
import org.sonatype.nexus.repository.npm.internal.NpmAttributes;
import org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.orient.NpmHostedFacet;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils.toContent;

/**
 * Support for uploading components via UI & API
 *
 * @since 3.7
 */
@Singleton
@Named(NpmFormat.NAME)
public class OrientNpmUploadHandler
    extends UploadHandlerSupport
    implements NpmUploadHandler
{
  private UploadDefinition definition;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapter variableResolverAdapter;

  private final NpmPackageParser npmPackageParser;

  @Inject
  public OrientNpmUploadHandler(final ContentPermissionChecker contentPermissionChecker,
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
    NpmHostedFacet facet = repository.facet(NpmHostedFacet.class);

    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    try (TempBlob tempBlob = storageFacet.createTempBlob(upload.getAssetUploads().get(0).getPayload(),
        NpmFacetUtils.HASH_ALGORITHMS)) {
      final Map<String, Object> packageJson = npmPackageParser.parsePackageJson(tempBlob);
      ensureNpmPermitted(repository, packageJson);

      UnitOfWork.begin(storageFacet.txSupplier());
      try {
        return new UploadResponse(facet.putPackage(packageJson, tempBlob));
      }
      finally {
        UnitOfWork.end();
      }
    }
  }

  @Override
  public Content handle(
      final Repository repository,
      final File content,
      final String path)
      throws IOException
  {
    NpmHostedFacet npmFacet = repository.facet(NpmHostedFacet.class);
    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    Path contentPath = content.toPath();
    Payload payload =
        new StreamPayload(() -> new FileInputStream(content), content.length(), Files.probeContentType(contentPath));
    TempBlob tempBlob = storageFacet.createTempBlob(payload, NpmFacetUtils.HASH_ALGORITHMS);
    final Map<String, Object> packageJson = npmPackageParser.parsePackageJson(tempBlob);
    ensureNpmPermitted(repository, packageJson);
    StorageTx tx = UnitOfWork.currentTx();
    Asset asset = npmFacet.putPackage(packageJson, tempBlob);
    return toContent(asset, tx.requireBlob(asset.requireBlobRef()));
  }

  private Map<String, Object> ensureNpmPermitted(
      final Repository repository,
      final Map<String, Object> packageJson)
  {
    final String name = (String) packageJson.get(NpmAttributes.P_NAME);
    final String version = (String) packageJson.get(NpmAttributes.P_VERSION);
    final String repositoryPath = NpmMetadataUtils.createRepositoryPath(name, version);
    final Map<String, String> coordinates = toCoordinates(packageJson);

    ensurePermitted(repository.getName(), NpmFormat.NAME, repositoryPath, coordinates);
    return packageJson;
  }

  private Map<String, String> toCoordinates(final Map<String, Object> packageJson) {
    NpmPackageId packageId = NpmPackageId.parse((String) checkNotNull(packageJson.get(NpmAttributes.P_NAME)));
    String version = (String) checkNotNull(packageJson.get(NpmAttributes.P_VERSION));

    if (packageId.scope() != null) {
      return ImmutableMap.of("packageScope", packageId.scope(), "packageName", packageId.name(),
          NpmAttributes.P_VERSION, version);
    }
    else {
      return ImmutableMap.of("packageName", packageId.name(), NpmAttributes.P_VERSION, version);
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

  @Override
  public boolean supportsExportImport() {
    return true;
  }
}
