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
package org.sonatype.nexus.orient.raw;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.mime.MimeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.importtask.ImportFileConfiguration;
import org.sonatype.nexus.repository.raw.RawCoordinatesHelper;
import org.sonatype.nexus.repository.raw.RawUploadHandlerSupport;
import org.sonatype.nexus.repository.raw.internal.RawFormat;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;
import org.sonatype.nexus.repository.view.payloads.StreamPayload;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.Lists;

/**
 * Support for uploading components via UI & API
 *
 * @since 3.7
 */
@Named(RawFormat.NAME)
@Singleton
public class RawUploadHandler
    extends RawUploadHandlerSupport
{
  private final MimeSupport mimeSupport;

  @Inject
  public RawUploadHandler(
      final ContentPermissionChecker contentPermissionChecker,
      @Named("simple") final VariableResolverAdapter variableResolverAdapter,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions,
      final MimeSupport mimeSupport)
  {
    super(contentPermissionChecker, variableResolverAdapter, uploadDefinitionExtensions);
    this.mimeSupport = mimeSupport;
  }

  @Override
  protected List<Content> getResponseContents(final Repository repository, final Map<String, PartPayload> pathToPayload)
      throws IOException
  {
    RawContentFacet facet = repository.facet(RawContentFacet.class);

    List<Content> responseContents = Lists.newArrayList();
    UnitOfWork.begin(repository.facet(StorageFacet.class).txSupplier());
    try {
      for (Entry<String, PartPayload> entry : pathToPayload.entrySet()) {
        String path = entry.getKey();

        Content content = facet.put(path, entry.getValue());

        responseContents.add(content);
      }
    }
    finally {
      UnitOfWork.end();
    }
    return responseContents;
  }

  @Override
  protected Content doPut(final ImportFileConfiguration configuration) throws IOException {
    Repository repository = configuration.getRepository();
    String path = configuration.getAssetName();
    Path contentPath = configuration.getFile().toPath();

    RawContentFacet facet = repository.facet(RawContentFacet.class);
    if (configuration.isHardLinkingEnabled()) {
      Asset asset = facet.getOrCreateAsset(repository, path, RawCoordinatesHelper.getGroup(path), path);
      facet.hardLink(repository, asset, path, contentPath);
      return facet.get(path);
    }
    else {
      return facet.put(path, new StreamPayload(() -> new BufferedInputStream(Files.newInputStream(contentPath)),
          Files.size(contentPath), mimeSupport.detectMimeType(Files.newInputStream(contentPath), path)));
    }
  }
}
