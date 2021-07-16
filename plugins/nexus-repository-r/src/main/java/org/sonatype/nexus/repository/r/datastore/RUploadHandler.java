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
package org.sonatype.nexus.repository.r.datastore;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.r.RFormat;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.upload.UploadHandlerSupport;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;

import static org.sonatype.nexus.repository.r.internal.util.PackageValidator.validateArchiveUploadPath;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.buildPath;
import static org.sonatype.nexus.repository.r.internal.util.RPathUtils.removeInitialSlashFromPath;

/**
 * Upload handler for R format
 *
 * @since 3.32
 */
@Singleton
@Named(RFormat.NAME)
public class RUploadHandler
    extends UploadHandlerSupport
{
  private static final String PATH_ID = "pathId";

  private static final String PACKAGE_PATH_DISPLAY = "Package Path";

  private final VariableResolverAdapter variableResolverAdapter;

  private final ContentPermissionChecker contentPermissionChecker;

  private UploadDefinition definition;

  @Inject
  public RUploadHandler(
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions,
      @Named("simple") final VariableResolverAdapter variableResolverAdapter,
      final ContentPermissionChecker contentPermissionChecker)
  {
    super(uploadDefinitionExtensions);
    this.variableResolverAdapter = variableResolverAdapter;
    this.contentPermissionChecker = contentPermissionChecker;
  }

  @Override
  public UploadResponse handle(
      final Repository repository, final ComponentUpload upload) throws IOException
  {
    AssetUpload assetUpload = upload.getAssetUploads().get(0);
    PartPayload payload = assetUpload.getPayload();
    Map<String, String> fields = assetUpload.getFields();
    String uploadPath = removeInitialSlashFromPath(fields.get(PATH_ID));
    String assetPath = buildPath(uploadPath, payload.getName());

    ensurePermitted(repository.getName(), RFormat.NAME, assetPath, Collections.emptyMap());
    validateArchiveUploadPath(assetPath);

    FluentAsset fluentAsset = repository
        .facet(RContentFacet.class)
        .putPackage(payload, assetPath);

    Content content = fluentAsset.download();
    return new UploadResponse(
        Collections.singletonList(content),
        Collections.singletonList(fluentAsset.path())
    );
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      List<UploadFieldDefinition> assetField = Collections.singletonList(
          new UploadFieldDefinition(PATH_ID, PACKAGE_PATH_DISPLAY, null, false, Type.STRING, null)
      );
      definition = getDefinition(RFormat.NAME, false, Collections.emptyList(), assetField, null);
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
