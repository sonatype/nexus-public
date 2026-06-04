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
package org.sonatype.nexus.repository.apt.datastore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.AptUploadHandlerSupport;
import org.sonatype.nexus.repository.apt.datastore.internal.hosted.AptHostedFacet;
import org.sonatype.nexus.repository.apt.internal.AptFacetHelper;
import org.sonatype.nexus.repository.apt.internal.AptPackageParser;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.internal.debian.PackageInfo;
import org.sonatype.nexus.repository.importtask.ImportFileConfiguration;
import org.sonatype.nexus.repository.importtask.ImportStreamConfiguration;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.PathPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.repository.view.payloads.TempBlobPayload;

import org.springframework.beans.factory.annotation.Qualifier;

import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Support for uploading an Apt components via UI
 *
 * @since 3.31
 */
@Qualifier(AptFormat.NAME)
@Component
public class AptUploadHandler
    extends AptUploadHandlerSupport
{
  @Autowired
  public AptUploadHandler(
      @Qualifier("simple") final VariableResolverAdapter variableResolverAdapter,
      final ContentPermissionChecker contentPermissionChecker,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(variableResolverAdapter, contentPermissionChecker, uploadDefinitionExtensions);
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    AptContentFacet aptContentFacet = repository.facet(AptContentFacet.class);
    AptHostedFacet hostedFacet = repository.facet(AptHostedFacet.class);
    PartPayload payload = upload.getAssetUploads().get(0).getPayload();
    try (TempBlob tempBlob = aptContentFacet.getTempBlob(payload)) {
      ControlFile controlFile = AptPackageParser
          .parsePackageInfo(tempBlob)
          .getControlFile();
      String assetPath = AptFacetHelper.buildAssetPath(controlFile);
      doValidation(repository, prependIfMissing(assetPath, "/"));
      Content content = hostedFacet
          .put(assetPath, payload, new PackageInfo(controlFile))
          .markAsCached(payload)
          .download();
      return new UploadResponse(Collections.singletonList(content), Collections.singletonList(assetPath));
    }
  }

  @Override
  public Content handle(final Repository repository, final File file, final String path) throws IOException {
    return handle(new ImportFileConfiguration(repository, file, path));
  }

  @Override
  public Content handle(final ImportFileConfiguration configuration) throws IOException {
    Repository repository = configuration.getRepository();
    File file = configuration.getFile();
    AptContentFacet aptContentFacet = repository.facet(AptContentFacet.class);
    AptHostedFacet hostedFacet = repository.facet(AptHostedFacet.class);

    Payload payload = new PathPayload(file.toPath(), Files.probeContentType(file.toPath()));
    try (TempBlob tempBlob = aptContentFacet.getTempBlob(payload)) {
      ControlFile controlFile = AptPackageParser
          .parsePackageInfo(tempBlob)
          .getControlFile();
      String assetPath = AptFacetHelper.buildAssetPath(controlFile);
      doValidation(repository, prependIfMissing(assetPath, "/"));
      return hostedFacet
          .put(assetPath, payload, new PackageInfo(controlFile))
          .markAsCached(payload)
          .download();
    }
  }

  @Override
  public Content handle(final ImportStreamConfiguration configuration) throws IOException {
    Repository repository = configuration.getRepository();
    AptContentFacet aptContentFacet = repository.facet(AptContentFacet.class);
    AptHostedFacet hostedFacet = repository.facet(AptHostedFacet.class);

    try (TempBlob tempBlob = aptContentFacet.getTempBlob(configuration.getInputStream(), null)) {
      ControlFile controlFile = AptPackageParser
          .parsePackageInfo(tempBlob)
          .getControlFile();
      String assetPath = AptFacetHelper.buildAssetPath(controlFile);
      doValidation(repository, prependIfMissing(assetPath, "/"));
      Payload payload = new TempBlobPayload(tempBlob, null);
      return hostedFacet
          .put(assetPath, payload, new PackageInfo(controlFile))
          .markAsCached(payload)
          .download();
    }
  }

  @Override
  public boolean supportsExportImport() {
    return true;
  }
}
