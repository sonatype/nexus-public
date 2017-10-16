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
package org.sonatype.nexus.repository.httpbridge.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionHelper;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionRenderer;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import com.google.common.annotations.VisibleForTesting;

import static java.util.Objects.nonNull;
import static org.sonatype.nexus.repository.http.HttpResponses.notFound;

/**
 * Repository view servlet servicing only Raw repositories.
 *
 * @since 3.7
 */
@Named
@Singleton
public class RawRepositoryViewServlet
    extends ViewServlet
{
  private static final String RAW_FORMAT_NAME = "raw";

  private RepositoryManager repositoryManager;

  @Inject
  public RawRepositoryViewServlet(final RepositoryManager repositoryManager,
                                  final HttpResponseSenderSelector httpResponseSenderSelector,
                                  final DescriptionHelper descriptionHelper,
                                  final DescriptionRenderer descriptionRenderer)
  {
    super(repositoryManager, httpResponseSenderSelector, descriptionHelper, descriptionRenderer);
    this.repositoryManager = repositoryManager;
  }

  @Override
  protected void doService(final HttpServletRequest httpRequest, final HttpServletResponse httpResponse)
      throws Exception
  {
    RepositoryPath path = RepositoryPath.parse(httpRequest.getPathInfo());
    Repository repo = repositoryManager.get(path.getRepositoryName());

    if (nonNull(repo) && !isRawRepo(repo)) {
      send(null, notFound(REPOSITORY_NOT_FOUND_MESSAGE), httpResponse);
      return;
    }

    super.doService(httpRequest, httpResponse);
  }

  @VisibleForTesting
  protected boolean isRawRepo(Repository repository) {
    Format format = repository.getFormat();
    return nonNull(format) && RAW_FORMAT_NAME.equals(format.getValue());
  }
}
