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
package org.sonatype.nexus.repository.apt.datastore.internal.proxy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sonatype.nexus.repository.apt.datastore.AptContentFacet;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Response;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler that validates requests against the configured distribution when requireDistribution is enabled.
 * When enabled, requests for distributions other than the configured one will return 404.
 */
@Component
public class AptDistributionValidationHandler
    implements Handler
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final Pattern DISTS_PATH_PATTERN = Pattern.compile("^/?dists/([^/]+)/");

  @Override
  public Response handle(final Context context) throws Exception {
    AptContentFacet contentFacet = context.getRepository().facet(AptContentFacet.class);

    if (!contentFacet.isEnforceDistribution()) {
      return context.proceed();
    }

    String configuredDistribution = contentFacet.getDistribution();
    if (StringUtils.isBlank(configuredDistribution)) {
      log.error("Enforce Distribution is enabled but no distribution is configured for repository '{}'. " +
          "All requests will be allowed.", context.getRepository().getName());
      return context.proceed();
    }

    String path = context.getRequest().getPath();
    Matcher matcher = DISTS_PATH_PATTERN.matcher(path);

    if (matcher.find()) {
      String requestedDistribution = matcher.group(1);
      if (!configuredDistribution.equals(requestedDistribution)) {
        log.debug("Rejecting request for distribution '{}', configured distribution is '{}'",
            requestedDistribution, configuredDistribution);
        return HttpResponses.notFound("Distribution not available");
      }
    }

    return context.proceed();
  }
}
