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
package org.sonatype.nexus.repository.upload.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.importtask.ImportFileConfiguration;
import org.sonatype.nexus.repository.importtask.ImportResult;
import org.sonatype.nexus.repository.rest.ComponentUploadExtension;
import org.sonatype.nexus.repository.rest.internal.resources.ComponentUploadUtils;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.upload.AssetUpload;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandler;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadProcessor;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.rest.ValidationErrorsException;

import org.apache.commons.fileupload.FileUploadException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;

/**
 * {@link UploadManager} implementation.
 *
 * @since 3.24
 */
@FeatureFlag(name = DATASTORE_ENABLED)
@Named
@Singleton
public class UploadManagerImpl
    extends ComponentSupport
    implements UploadManager
{
  private final List<UploadDefinition> uploadDefinitions;

  private final Map<String, UploadHandler> uploadHandlers;

  private final UploadComponentMultipartHelper multipartHelper;

  private final UploadProcessor uploadComponentProcessor;

  private final Set<ComponentUploadExtension> componentUploadExtensions;

  private final EventManager eventManager;

  @Inject
  public UploadManagerImpl(
      final Map<String, UploadHandler> uploadHandlers,
      final UploadComponentMultipartHelper multipartHelper,
      final UploadProcessor uploadComponentProcessor,
      final EventManager eventManager,
      final Set<ComponentUploadExtension> componentsUploadExtensions)
  {
    this.uploadHandlers = checkNotNull(uploadHandlers);
    this.uploadDefinitions = Collections
        .unmodifiableList(uploadHandlers.values().stream()
            .filter(UploadHandler::supportsApiUpload)
            .map(UploadHandler::getDefinition)
            .collect(toList()));
    this.multipartHelper = checkNotNull(multipartHelper);
    this.uploadComponentProcessor = checkNotNull(uploadComponentProcessor);
    this.eventManager = checkNotNull(eventManager);
    this.componentUploadExtensions = checkNotNull(componentsUploadExtensions);
  }

  @Override
  public Collection<UploadDefinition> getAvailableDefinitions() {
    return uploadDefinitions;
  }

  @Override
  public UploadResponse handle(final Repository repository, final HttpServletRequest request) throws IOException {
    checkNotNull(repository);
    checkNotNull(request);

    if (!repository.getConfiguration().isOnline()) {
      throw new ValidationErrorsException("Repository offline");
    }

    UploadHandler uploadHandler = getUploadHandler(repository);
    ComponentUpload upload = create(repository, request);
    logUploadDetails(upload, repository);

    try {
      componentUploadExtensions.forEach(componentUploadExtension -> componentUploadExtension.validate(upload));

      UploadResponse uploadResponse =
          uploadHandler.handle(repository, uploadHandler.getValidatingComponentUpload(upload).getComponentUpload());

      for (ComponentUploadExtension componentUploadExtension : componentUploadExtensions) {
        List<EntityId> componentIds = uploadResponse.getContents().stream()
            .map(uploadComponentProcessor::extractId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList());
        componentUploadExtension.apply(repository, upload, componentIds);
      }

      eventManager.post(new UIUploadEvent(repository,
          uploadResponse.getAssetPaths().stream().map(assetPath -> prependIfMissing(assetPath, "/"))
              .collect(toList())));

      return uploadResponse;
    }
    finally {
      for (AssetUpload assetUpload : upload.getAssetUploads()) {
        assetUpload.getPayload().close();
      }
    }
  }

  @Override
  public UploadDefinition getByFormat(final String format) {
    checkNotNull(format);

    UploadHandler handler = uploadHandlers.get(format);
    return handler != null ? handler.getDefinition() : null;
  }

  @Override
  public Content handle(final ImportFileConfiguration importFileConfiguration)
      throws IOException
  {
    UploadHandler uploadHandler = getUploadHandler(importFileConfiguration.getRepository());

    if (importFileConfiguration.isHardLinkingEnabled()) {
      return uploadHandler.handle(importFileConfiguration);
    }
    else {
      return uploadHandler.handle(
          importFileConfiguration.getRepository(),
          importFileConfiguration.getFile(),
          importFileConfiguration.getAssetName()
      );
    }
  }

  @Override
  public void handleAfterImport(final ImportResult importResult) throws IOException {
    Repository repository = importResult.getRepository();
    UploadHandler uploadHandler = getUploadHandler(repository);
    uploadHandler.handleAfterImport(importResult);
  }

  private ComponentUpload create(final Repository repository, final HttpServletRequest request)
      throws IOException
  {
    try {
      BlobStoreMultipartForm multipartForm = multipartHelper.parse(repository, request);
      return ComponentUploadUtils.createComponentUpload(repository.getFormat().getValue(), multipartForm);
    }
    catch (FileUploadException e) {
      throw new IOException(e);
    }
  }

  private UploadHandler getUploadHandler(final Repository repository)
  {
    if (!(repository.getType() instanceof HostedType)) {
      throw new ValidationErrorsException(
          format("Uploading components to a '%s' type repository is unsupported, must be '%s'",
              repository.getType().getValue(), HostedType.NAME));
    }

    String repositoryFormat = repository.getFormat().toString();
    UploadHandler uploadHandler = uploadHandlers.get(repositoryFormat);

    if (uploadHandler == null) {
      throw new ValidationErrorsException(
          format("Uploading components to '%s' repositories is unsupported", repositoryFormat));
    }

    return uploadHandler;
  }

  private void logUploadDetails(final ComponentUpload componentUpload, final Repository repository) {
    if (log.isInfoEnabled()) {
      Map<String, String> componentFields = componentUpload.getFields();
      List<AssetUpload> assetUploads = componentUpload.getAssetUploads();

      StringBuilder sb = new StringBuilder();
      sb.append("Uploading component with parameters: ").append("repository").append("=\"").append(repository.getName())
          .append("\" ").append("format").append("=\"").append(repository.getFormat().getValue()).append("\" ");
      for (Entry<String, String> entry : componentFields.entrySet()) {
        sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\" ");
      }
      log.info(sb.toString());

      for (AssetUpload assetUpload : assetUploads) {
        sb = new StringBuilder();
        sb.append("Asset with parameters: ");
        sb.append("file=\"").append(assetUpload.getPayload().getName()).append("\" ");
        for (Entry<String, String> entry : assetUpload.getFields().entrySet()) {
          sb.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\" ");
        }
        log.info(sb.toString());
      }
    }
  }
}
