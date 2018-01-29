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
package org.sonatype.nexus.repository.upload;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import static java.lang.String.format;
import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * @since 3.7
 */
public interface UploadHandler
{

  /**
   * Adds a component to the repository at the described location. May fail or have unexpected behavior if the
   * repository format does not match this upload instance.
   *
   * @param repository the {@link Repository} to add the component to
   * @param upload the upload
   * @return the {@link Asset Assets} created by the operation
   */
  Collection<String> handle(Repository repository, ComponentUpload upload) throws IOException;

  /**
   * The {@link UploadDefinition} used by this format.
   */
  UploadDefinition getDefinition();

  /**
   * The <code>VariableResolverAdapter</code> to use for <code>ensurePermitted</code>
   */
  VariableResolverAdapter getVariableResolverAdapter();

  /**
   * The <code>ContentPermissionChecker</code> to use for <code>ensurePermitted</code>
   */
  ContentPermissionChecker contentPermissionChecker();

  /**
   * Use the <code>ContentPermissionChecker</code> to verify the current user has EDIT permission for the repository,
   * path and coordinates. An <code>AuthorizationException</code> will be thrown if the action is not permitted.
   *
   * @param repositoryName the name of the repository the asset is being uploaded to
   * @param format the format name
   * @param path the path within the repository that will represent the asset (should not be prefixed with a slash)
   * @param coordinates a map containing the coordinate fields and their values
   */
  default void ensurePermitted(final String repositoryName,
                               final String format,
                               final String path,
                               final Map<String, String> coordinates)
  {
    VariableSource variableSource = getVariableResolverAdapter().fromCoordinates(format, path, coordinates);
    if (!contentPermissionChecker().isPermitted(repositoryName, format, BreadActions.EDIT, variableSource)) {
      throw new ValidationErrorsException(format("Not authorized for requested path '%s'", path));
    }
  }

  /**
   * Simple validation of upload which ensures that non-optional fields are not missing.
   */
  default void validate(final ComponentUpload componentUpload) {
    ValidationErrorsException exception = new ValidationErrorsException();

    if (componentUpload.getAssetUploads().isEmpty()) {
      exception.withError("No assets found in upload");
    }

    getDefinition().getComponentFields().stream()
        .filter(field -> !field.isOptional())
        .filter(field -> isBlank(componentUpload.getField(field.getName())))
        .forEach(field -> exception.withError(field.getName(),
            format("Missing required component field '%s'", field.getDisplayName())));

    AtomicInteger assetCounter = new AtomicInteger();
    componentUpload.getAssetUploads().stream()
        .forEachOrdered(asset -> {
          int assetCount = assetCounter.incrementAndGet();

          if (asset.getPayload() == null) {
            exception.withError("file", format("Missing file on asset '%s'", assetCount));
          }

          getDefinition().getAssetFields().stream()
              .filter(field -> !field.isOptional())
              .filter(field -> isBlank(asset.getField(field.getName())))
              .forEach(field -> exception.withError(field.getName(),
                  format("Missing required asset field '%s' on '%s'", field.getDisplayName(), assetCount)));
        });

    int i = 1;
    int length = componentUpload.getAssetUploads().size();
    for (AssetUpload assetUpload : componentUpload.getAssetUploads()) {
      int otherIndex = i;
      for (AssetUpload other : componentUpload.getAssetUploads().subList(i++, length)) {
        if (assetUpload.getFields().equals(other.getFields())) {
          exception.withError(String.format("The assets %s and %s have identical coordinates", i - 1, otherIndex + 1));
        }
        otherIndex++;
      }
    }

    if (!exception.getValidationErrors().isEmpty()) {
      throw exception;
    }
  }
}
