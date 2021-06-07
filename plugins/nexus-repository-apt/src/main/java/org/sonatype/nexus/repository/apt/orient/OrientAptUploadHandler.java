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
package org.sonatype.nexus.repository.apt.orient;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.apt.AptUploadHandlerSupport;
import org.sonatype.nexus.repository.apt.internal.AptFacetHelper;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.apt.internal.AptPackageParser;
import org.sonatype.nexus.repository.apt.internal.debian.ControlFile;
import org.sonatype.nexus.repository.apt.orient.internal.hosted.OrientAptHostedFacet;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

/**
 * @since 3.17
 */
@Singleton
@Named(AptFormat.NAME)
public class OrientAptUploadHandler
    extends AptUploadHandlerSupport
{
  @Inject
  public OrientAptUploadHandler(@Named("simple") final VariableResolverAdapter variableResolverAdapter,
                                final ContentPermissionChecker contentPermissionChecker,
                                final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(variableResolverAdapter, contentPermissionChecker, uploadDefinitionExtensions);
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    OrientAptHostedFacet hostedFacet = repository.facet(OrientAptHostedFacet.class);
    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    try (TempBlob tempBlob = storageFacet
        .createTempBlob(upload.getAssetUploads().get(0).getPayload(), AptFacetHelper.hashAlgorithms)) {
      ControlFile controlFile = AptPackageParser.parsePackageInfo(tempBlob).getControlFile();
      String assetPath = AptFacetHelper.buildAssetPath(controlFile);

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
}
