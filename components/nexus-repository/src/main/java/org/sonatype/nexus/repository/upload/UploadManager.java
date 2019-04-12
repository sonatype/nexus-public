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

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.storage.Asset;

/**
 * @since 3.7
 */
public interface UploadManager
{
  /**
   * Get the list of {@link UploadDefinition UploadDefinitions} available in this instance.
   */
  Collection<UploadDefinition> getAvailableDefinitions();

  /**
   * Get the {@link UploadDefinition} for the repository format.
   */
  @Nullable
  UploadDefinition getByFormat(String format);

  /**
   * Adds a component to the repository at the appropriate location. Will fail if the repository format does not have an
   * available handler.
   *
   * @since 3.16
   *
   * @param repository the {@link Repository} to add the component to
   * @param request the http request containing the multipart upload
   * @return the {@link Asset Assets} created by the operation
   */
  UploadResponse handle(Repository repository, HttpServletRequest request) throws IOException;
}
