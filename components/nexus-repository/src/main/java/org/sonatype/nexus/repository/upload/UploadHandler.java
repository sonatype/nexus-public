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
import java.util.Map;
import java.util.stream.Stream;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import static java.lang.String.format;

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
  UploadResponse handle(Repository repository, ComponentUpload upload) throws IOException;

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
    boolean dotSegment = Stream.of(path.split("/")).anyMatch(segment -> segment.equals(".") || segment.equals(".."));
    if (dotSegment) {
      throw new ValidationErrorsException(format("Path is not allowed to have '.' or '..' segments: '%s'", path));
    }

    VariableSource variableSource = getVariableResolverAdapter().fromCoordinates(format, path, coordinates);
    if (!contentPermissionChecker().isPermitted(repositoryName, format, BreadActions.EDIT, variableSource)) {
      throw new ValidationErrorsException(format("Not authorized for requested path '%s'", path));
    }
  }

  /**
   * @return a ComponentUpload that can validate it's own fields
   * @since 3.8
   */
  default ValidatingComponentUpload getValidatingComponentUpload(final ComponentUpload componentUpload) {
    return new ValidatingComponentUpload(getDefinition(), componentUpload);
  }
}
