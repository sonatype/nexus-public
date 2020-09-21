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
package org.sonatype.repository.helm.internal.orient;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.transaction.UnitOfWork;
import org.sonatype.repository.helm.HelmAttributes;
import org.sonatype.repository.helm.HelmUploadHandlerSupport;
import org.sonatype.repository.helm.internal.AssetKind;
import org.sonatype.repository.helm.internal.orient.hosted.HelmHostedFacet;
import org.sonatype.repository.helm.internal.util.HelmAttributeParser;

import org.apache.commons.lang3.StringUtils;

import static org.sonatype.repository.helm.internal.HelmFormat.HASH_ALGORITHMS;
import static org.sonatype.repository.helm.internal.HelmFormat.NAME;

/**
 * Support helm upload for web page
 /**
 * @since 3.next
 */
@Singleton
@Named(NAME)
public class HelmUploadHandler
    extends HelmUploadHandlerSupport
{
  @Inject
  public HelmUploadHandler(
      final ContentPermissionChecker contentPermissionChecker,
      final HelmAttributeParser helmPackageParser,
      @Named("simple") final VariableResolverAdapter variableResolverAdapter,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions)
  {
    super(contentPermissionChecker, helmPackageParser, variableResolverAdapter, uploadDefinitionExtensions);
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    HelmHostedFacet facet = repository.facet(HelmHostedFacet.class);
    StorageFacet storageFacet = repository.facet(StorageFacet.class);

    PartPayload payload = upload.getAssetUploads().get(0).getPayload();

    String fileName = payload.getName() != null ? payload.getName() : StringUtils.EMPTY;
    AssetKind assetKind = AssetKind.getAssetKindByFileName(fileName);

    if (assetKind != AssetKind.HELM_PROVENANCE && assetKind != AssetKind.HELM_PACKAGE) {
      throw new IllegalArgumentException("Unsupported extension. ExtensioIndexYamlAbsoluteUrlRewriterTestn must be .tgz or .tgz.prov");
    }

    try (TempBlob tempBlob = storageFacet.createTempBlob(payload, HASH_ALGORITHMS)) {
      HelmAttributes attributesFromInputStream = helmPackageParser.getAttributes(assetKind, tempBlob.get());
      String extension = assetKind.getExtension();
      String name = attributesFromInputStream.getName();
      String version = attributesFromInputStream.getVersion();

      if (StringUtils.isBlank(name)) {
        throw new ValidationErrorsException("Metadata is missing the name attribute");
      }

      if (StringUtils.isBlank(version)) {
        throw new ValidationErrorsException("Metadata is missing the version attribute");
      }

      String path = String.format("%s-%s%s", name, version, extension);

      ensurePermitted(repository.getName(), NAME, path, Collections.emptyMap());
      try {
        UnitOfWork.begin(storageFacet.txSupplier());
        Asset asset = facet.upload(path, tempBlob, payload, assetKind);
        return new UploadResponse(asset);
      }
      finally {
        UnitOfWork.end();
      }
    }
  }
}
