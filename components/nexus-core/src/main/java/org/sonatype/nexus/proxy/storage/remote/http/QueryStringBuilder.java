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
package org.sonatype.nexus.proxy.storage.remote.http;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

import org.codehaus.plexus.util.StringUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constructs optional query-string to append to requests.
 *
 * This class is part of Nexus internal implementation and can be changed or removed without prior notice.
 *
 * @since 2.2
 */
@Named
@Singleton
public class QueryStringBuilder
{
  private final List<QueryStringContributor> queryParameterContributors;

  @Inject
  public QueryStringBuilder(final List<QueryStringContributor> queryParameterContributors) {
    this.queryParameterContributors = checkNotNull(queryParameterContributors);
  }

  public String getQueryString(final RemoteStorageContext ctx, final ProxyRepository repository) {
    checkNotNull(ctx);
    checkNotNull(repository);

    final StringBuilder result = new StringBuilder();
    final String configuredQueryString = ctx.getRemoteConnectionSettings().getQueryString();

    if (StringUtils.isNotBlank(configuredQueryString)) {
      result.append(configuredQueryString);
    }

    for (QueryStringContributor contributor : queryParameterContributors) {
      String contributedQueryString = contributor.getQueryString(ctx, repository);
      if (StringUtils.isNotBlank(contributedQueryString)) {
        if (StringUtils.isNotBlank(result.toString())) {
          result.append('&');
        }
        result.append(contributedQueryString);
      }
    }

    final String resultStr = result.toString();

    return StringUtils.isNotBlank(resultStr) ? resultStr : null;
  }

  /**
   * @since 2.3
   */
  public String getQueryString(final ProxyRepository repository) {
    checkNotNull(repository);
    return getQueryString(repository.getRemoteStorageContext(), repository);
  }
}
