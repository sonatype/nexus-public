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
package org.sonatype.nexus.repository.apt;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.internal.AptFormat;
import org.sonatype.nexus.repository.apt.internal.AptPackageParser;
import org.sonatype.nexus.repository.apt.internal.FacetHelper;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.hosted.AptHostedFacet;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.TempBlob;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.transaction.UnitOfWork;

/**
 * @since 3.17
 */
@Singleton
@Named(AptFormat.NAME)
public class AptUploadHandler
    extends UploadHandlerSupport
{
  private final VariableResolverAdapter variableResolverAdapter;

  private final ContentPermissionChecker contentPermissionChecker;

  private UploadDefinition definition;

  @Inject
  public AptUploadHandler(@Named("simple") final VariableResolverAdapter variableResolverAdapter,
                          final ContentPermissionChecker contentPermissionChecker,
                          final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(uploadDefinitionExtensions);
    this.variableResolverAdapter = variableResolverAdapter;
    this.contentPermissionChecker = contentPermissionChecker;
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    AptHostedFacet hostedFacet = repository.facet(AptHostedFacet.class);
    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    try (TempBlob tempBlob = storageFacet
        .createTempBlob(upload.getAssetUploads().get(0).getPayload(), FacetHelper.hashAlgorithms)) {
      ControlFile controlFile = AptPackageParser.parsePackage(tempBlob);
      if (controlFile == null) {
        throw new IOException("Invalid debian package:  no control file");
      }
      String name = controlFile.getField("Package").map(f -> f.value).get();
      String version = controlFile.getField("Version").map(f -> f.value).get();
      String architecture = controlFile.getField("Architecture").map(f -> f.value).get();
      String assetPath = FacetHelper.buildAssetPath(name, version, architecture);

      doValidation(repository, assetPath);

      UnitOfWork.begin(storageFacet.txSupplier());
      try {
        Asset asset = hostedFacet.ingestAsset(upload.getAssetUploads().get(0).getPayload());
        return new UploadResponse(asset);
      }
      finally {
        UnitOfWork.end();
      }
    }
  }

  private void doValidation(final Repository repository,
                            final String assetPath)
  {
    ensurePermitted(repository.getName(), AptFormat.NAME, assetPath, Collections.emptyMap());
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      definition = getDefinition(AptFormat.NAME, false);
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
