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
package org.sonatype.nexus.coreui.internal;

import java.io.IOException;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.upload.UploadDefinition;
import org.sonatype.nexus.repository.upload.UploadManager;
import org.sonatype.nexus.repository.upload.UploadResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 3.7
 */
@Named
@Singleton
public class UploadService
  extends ComponentSupport
{
  private UploadManager uploadManager;

  private RepositoryManager repositoryManager;

  @Inject
  public UploadService(final RepositoryManager repositoryManager,
                       final UploadManager uploadManager)
  {
    this.uploadManager = checkNotNull(uploadManager);
    this.repositoryManager = checkNotNull(repositoryManager);
  }

  /**
   * Get a list of available definitions for upload.
   */
  public Collection<UploadDefinition> getAvailableDefinitions() {
    return uploadManager.getAvailableDefinitions();
  }

  /**
   * Perform an upload of assets
   *
   * @since 3.16
   *
   * @param repositoryName the repository to upload to
   * @param request a multipart form request
   * @return the query term for results in search (depending on upload could show additional results)
   * @throws IOException
   */
  public String upload(final String repositoryName, final HttpServletRequest request) throws IOException {
    checkNotNull(repositoryName);
    checkNotNull(request);

    Repository repository = checkNotNull(repositoryManager.get(repositoryName), "Specified repository is missing");

    UploadResponse uploadResponse = uploadManager.handle(repository, request);

    return createSearchTerm(uploadResponse.getAssetPaths());
  }

  @VisibleForTesting
  String createSearchTerm(final Collection<String> createdPaths) {
    if (createdPaths.isEmpty()) {
      return null;
    }

    String prefix = Iterables.getFirst(createdPaths, null);

    for (String path : createdPaths) {
      prefix = longestPrefix(prefix, path);
    }

    return prefix;
  }

  private String removeLastSegment(final String path) {
    int index = path.lastIndexOf('/');
    if (index != -1) {
      return path.substring(0, index);
    }
    return path;
  }

  private String longestPrefix(final String prefix, final String path) {
    String result = prefix;
    while (result.length() > 0 && !path.startsWith(result)) {
      result = removeLastSegment(result);
    }
    return result;
  }
}
