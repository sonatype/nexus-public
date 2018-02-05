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

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.httpbridge.LegacyViewConfiguration;
import org.sonatype.nexus.repository.httpbridge.LegacyViewContributor;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionHelper;
import org.sonatype.nexus.repository.httpbridge.internal.describe.DescriptionRenderer;
import org.sonatype.nexus.repository.manager.RepositoryManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.isNull;
import static org.sonatype.nexus.repository.http.HttpResponses.notFound;

/**
 * Repository view servlet for NX2 urls.
 *
 * @since 3.7
 */
@Named
@Singleton
public class LegacyViewServlet
    extends ViewServlet
{
  private final List<LegacyViewContributor> legacyViewContributors;

  private final RepositoryManager repositoryManager;

  @Inject
  public LegacyViewServlet(final RepositoryManager repositoryManager,
                           final HttpResponseSenderSelector httpResponseSenderSelector,
                           final DescriptionHelper descriptionHelper,
                           final DescriptionRenderer descriptionRenderer,
                           final List<LegacyViewContributor> legacyViewContributors,
                           @Named("${nexus.repository.sandbox.enable:-true}") final boolean sandboxEnabled)
  {
    super(repositoryManager, httpResponseSenderSelector, descriptionHelper, descriptionRenderer, sandboxEnabled);
    this.legacyViewContributors = checkNotNull(legacyViewContributors);
    this.repositoryManager = repositoryManager;
  }

  @Override
  protected void doService(final HttpServletRequest request, final HttpServletResponse response)
      throws Exception
  {
    if (handleFormatSpecificUri(request, response)) {
      return;
    }

    super.doService(request, response);
  }

  private boolean handleFormatSpecificUri(final HttpServletRequest request, final HttpServletResponse response)
      throws ServletException, IOException
  {
    for (LegacyViewContributor legacyViewContributor : legacyViewContributors) {
      LegacyViewConfiguration configuration = legacyViewContributor.contribute();
      if (isFormatSpecific(request, configuration)) {
        RepositoryPath path = RepositoryPath.parse(request.getPathInfo());
        Repository repository = repositoryManager.get(path.getRepositoryName());
        if (repositoryNotFoundOrFormatNotMatched(configuration, repository)) {
          send(null, notFound(REPOSITORY_NOT_FOUND_MESSAGE), response);
          return true;
        }
      }
    }
    return false;
  }

  private boolean repositoryNotFoundOrFormatNotMatched(final LegacyViewConfiguration configuration,
                                                       final Repository repository)
  {
    return isNull(repository) || !Objects.equals(configuration.getFormat(), repository.getFormat().getValue());
  }

  private boolean isFormatSpecific(final HttpServletRequest request, final LegacyViewConfiguration configuration) {
    Matcher matcher = configuration.getRequestPattern().matcher(request.getRequestURI());
    return matcher.matches();
  }
}
