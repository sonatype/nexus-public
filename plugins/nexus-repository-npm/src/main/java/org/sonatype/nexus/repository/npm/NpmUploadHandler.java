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
package org.sonatype.nexus.repository.npm;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.npm.internal.NpmAttributes;
import org.sonatype.nexus.repository.npm.internal.NpmFacetUtils;
import org.sonatype.nexus.repository.npm.internal.NpmFormat;
import org.sonatype.nexus.repository.npm.internal.NpmHostedFacet;
import org.sonatype.nexus.repository.npm.internal.NpmMetadataUtils;
import org.sonatype.nexus.repository.npm.internal.NpmPackageId;
import org.sonatype.nexus.repository.npm.internal.NpmPackageParser;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for uploading components via UI & API
 *
 * @since 3.7
 */
@Singleton
@Named(NpmFormat.NAME)
public class NpmUploadHandler
    extends UploadHandlerSupport
{
  private UploadDefinition definition;

  private final ContentPermissionChecker contentPermissionChecker;

  private final VariableResolverAdapter variableResolverAdapter;

  private final NpmPackageParser npmPackageParser;

  @Inject
  public NpmUploadHandler(final ContentPermissionChecker contentPermissionChecker,
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
      final String name = (String) packageJson.get(NpmAttributes.P_NAME);
      final String version =(String) packageJson.get(NpmAttributes.P_VERSION);
      final String path = NpmMetadataUtils.createRepositoryPath(name, version);
      final Map<String, String> coordinates = toCoordinates(packageJson);

      ensurePermitted(repository.getName(), NpmFormat.NAME, path, coordinates);

      UnitOfWork.begin(storageFacet.txSupplier());
      try {
        return new UploadResponse(facet.putPackage(packageJson, tempBlob));
      }
      finally {
        UnitOfWork.end();
      }
    }
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
}
