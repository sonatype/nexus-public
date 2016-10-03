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
package org.sonatype.nexus.repository.view;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;

import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * View router.
 *
 * @since 3.0
 */
public class Router
    extends ComponentSupport
{
  private final List<Route> routes;

  private final DefaultRoute defaultRoute;

  public Router(final List<Route> routes, final DefaultRoute defaultRoute) {
    this.routes = checkNotNull(routes, "Missing routes");
    this.defaultRoute = checkNotNull(defaultRoute, "Missing default route");
  }

  /**
   * Dispatch request to matching route, if supplied will pull attributes from the existingContext.
   */
  public Response dispatch(final Repository repository, final Request request, @Nullable final Context existingContext)
      throws Exception
  {
    checkNotNull(repository);
    checkNotNull(request);

    logRequest(request);

    // Find route and start context
    Context context = maybeCopyContextAttributes(repository, request, existingContext);
    Route route = findRoute(context);
    Response response = context.start(route);
    logResponse(response);
    return response;
  }

  private Context maybeCopyContextAttributes(final Repository repository,
                                             final Request request,
                                             final Context existingContext)
  {
    Context context = new Context(repository, request);

    if (existingContext != null) {
      existingContext.getAttributes().keys()
          .forEach(key -> context.getAttributes().set(key, existingContext.getAttributes().get(key)));
    }

    return context;
  }

  /**
   * Log request details.
   */
  private void logRequest(final Request request) {
    log.debug("Request: {}", request);

    if (log.isTraceEnabled()) {
      if (request.getHeaders().isEmpty()) {
        log.trace("No request headers");
      }
      else {
        log.trace("Request headers:");
        for (Map.Entry<String, String> header : request.getHeaders()) {
          log.trace("  {}: {}", header.getKey(), header.getValue());
        }
      }

      if (request.getAttributes().isEmpty()) {
        log.trace("No request attributes");
      }
      else {
        log.trace("Request attributes:");
        for (Map.Entry<String, Object> entry : request.getAttributes()) {
          log.trace("  {}={}", entry.getKey(), entry.getValue());
        }
      }
    }
  }

  /**
   * Log response details.
   */
  private void logResponse(final Response response) {
    log.debug("Response: {}", response);

    if (log.isTraceEnabled()) {
      if (response.getHeaders().isEmpty()) {
        log.trace("No response headers");
      }
      else {
        log.trace("Response headers:");
        for (Map.Entry<String, String> header : response.getHeaders()) {
          log.trace("  {}: {}", header.getKey(), header.getValue());
        }
      }

      if (response.getAttributes().isEmpty()) {
        log.trace("No response attributes");
      }
      else {
        log.trace("Response attributes:");
        for (Map.Entry<String, Object> entry : response.getAttributes()) {
          log.trace("  {}={}", entry.getKey(), entry.getValue());
        }
      }
    }
  }

  /**
   * Find the first matching route for the given context.
   */
  private Route findRoute(final Context context) {
    for (Route route : routes) {
      if (route.getMatcher().matches(context)) {
        return route;
      }
    }
    return defaultRoute;
  }

  //
  // Builder
  //

  /**
   * View {@link Router} builder.
   */
  public static class Builder
  {
    // TODO: Consider if we want to add route-ids so we can reference defined routes for re-use (maybe builder state only)?
    // TODO: Consider a set of default handlers, as here we have timingHandler on each route?

    private List<Route> routes = Lists.newArrayList();

    private DefaultRoute defaultRoute;

    public Builder route(final Route route) {
      checkNotNull(route);
      routes.add(route);
      return this;
    }

    public Builder defaultHandlers(final Handler... handlers) {
      checkState(this.defaultRoute == null, "Default handlers already configured");
      this.defaultRoute = new DefaultRoute(Arrays.asList(handlers));
      return this;
    }

    public Router create() {
      return new Router(routes, defaultRoute);
    }
  }
}
