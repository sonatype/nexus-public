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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.upload.ComponentUpload;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadHandler;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;
import org.sonatype.nexus.rest.ValidationErrorsException;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * @since 3.7
 */
@Named
@Singleton
public class UploadManagerImpl
    implements UploadManager
{
  private List<UploadDefinition> uploadDefinitions;

  private Map<String, UploadHandler> uploadHandlers;

  @Inject
  public UploadManagerImpl(final Map<String, UploadHandler> uploadHandlers)
  {
    this.uploadHandlers = checkNotNull(uploadHandlers);
    this.uploadDefinitions = Collections
        .unmodifiableList(uploadHandlers.values().stream().map(handler -> handler.getDefinition()).collect(toList()));
  }

  @Override
  public Collection<UploadDefinition> getAvailableDefinitions() {
    return uploadDefinitions;
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload)
      throws IOException
  {
    checkNotNull(repository);
    checkNotNull(upload);

    UploadHandler uploadHandler = getUploadHandler(repository);
    return uploadHandler.handle(repository, uploadHandler.getValidatingComponentUpload(upload).getComponentUpload());
  }

  @Override
  public UploadDefinition getByFormat(final String format) {
    checkNotNull(format);

    UploadHandler handler = uploadHandlers.get(format);
    return handler != null ? handler.getDefinition() : null;
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

}
