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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.recipe.RouterBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.repository.view.ViewUtils.copyLocalContextAttributes;

/**
 * View router.
 *
 * @since 3.0
 */
public class Router
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final List<Route> routes;

  private final DefaultRoute defaultRoute;

  public static final String LOCAL_ATTRIBUTE_PREFIX = "local.attribute.";

  public Router(final List<Route> routes, final DefaultRoute defaultRoute) {
    this.routes = requireNonNull(routes, "Missing routes");
    this.defaultRoute = requireNonNull(defaultRoute, "Missing default route");
  }

  /**
   * Dispatch request to matching route, if supplied will pull attributes from the existingContext.
   */
  public Response dispatch(
      final Repository repository,
      final Request request,
      @Nullable final Context existingContext) throws Exception
  {
    requireNonNull(repository);
    requireNonNull(request);

    logRequest(request);

    // Find route and start context
    Context context = maybeCopyContextAttributes(repository, request, existingContext);
    Route route = findRoute(context);
    Response response = context.start(route);
    logResponse(response);
    return response;
  }

  /**
   * Read-only view of the registered routes, in registration order. Exposed for recipe tests
   * that need to inspect the wired route list without reflecting into the private {@code routes}
   * field. Callers must not mutate the returned list; the underlying list is not concurrent and
   * is read on the dispatch hot path.
   */
  @VisibleForTesting
  public List<Route> getRoutes() {
    return Collections.unmodifiableList(routes);
  }

  @VisibleForTesting
  Context maybeCopyContextAttributes(
      final Repository repository,
      final Request request,
      final Context existingContext)
  {
    Context context = new Context(repository, request);

    copyLocalContextAttributes(existingContext, context);

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
      implements RouterBuilder
  {
    // TODO: Consider if we want to add route-ids so we can reference defined routes for re-use (maybe builder state
    // only)?
    // TODO: Consider a set of default handlers, as here we have timingHandler on each route?

    private List<Route> routes = Lists.newArrayList();

    private DefaultRoute defaultRoute;

    public Builder route(final Route route) {
      requireNonNull(route);
      return addRoute(route);
    }

    @Override
    // maps the new bridge interface from repository-config onto the view API
    public Builder route(final org.sonatype.nexus.repository.recipe.Route route) {
      requireNonNull(route);
      if (route instanceof Route) {
        return addRoute((Route) route);
      }
      else {
        // partial mock that can be ignored
        return this;
      }
    }

    private Builder addRoute(final Route route) {
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
