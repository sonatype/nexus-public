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
package org.sonatype.nexus.repository.routing.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.routing.RoutingRuleHelper;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.repository.view.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * A handler which validates that the request is allowed by the RoutingRule assigned to the repository.
 *
 * @since 3.16
 */
@Component
public class RoutingRuleHandler
    implements Handler, org.sonatype.nexus.repository.routing.RoutingRuleHandler
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  static final String PATH_IS_BLOCKED = "Routing rules block the requested item from this repository";

  private final RoutingRuleHelper routingRuleHelper;

  @Autowired
  public RoutingRuleHandler(final RoutingRuleHelper routingRuleHelper) {
    this.routingRuleHelper = checkNotNull(routingRuleHelper);
  }

  @Override
  public Response handle(final Context context) throws Exception {
    Repository repository = context.getRepository();
    String path = path(context.getRequest());

    boolean isRoutingRuleAllowed = routingRuleHelper.isAllowed(repository, path);

    if (!isRoutingRuleAllowed) {
      String repositoryType = repository.getType().getValue();
      String repositoryName = repository.getName();

      log.debug("Routing rules block the requested item for Repository{name='{}', type={}}",
          repositoryName,
          repositoryType);

      return HttpResponses.forbidden(PATH_IS_BLOCKED);
    }

    return context.proceed();
  }

  private String path(final Request request) {
    if (request.getParameters().size() == 0) {
      return request.getPath();
    }
    StringBuilder sb = new StringBuilder();
    request.getParameters()
        .forEach(entry -> sb.append('&').append(entry.getKey()).append('=').append(entry.getValue()));
    sb.replace(0, 1, "?");
    return request.getPath() + sb;
  }
}
