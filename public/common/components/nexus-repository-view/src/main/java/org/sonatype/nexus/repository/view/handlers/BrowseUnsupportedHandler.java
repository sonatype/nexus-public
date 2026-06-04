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
package org.sonatype.nexus.repository.view.handlers;

import java.net.URL;

import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.common.template.TemplateHelper;
import org.sonatype.nexus.common.template.TemplateParameters;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.http.HttpResponses;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Handler;
import org.sonatype.nexus.repository.view.Matcher;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import org.springframework.stereotype.Component;

/**
 * Renders browse not supported page for requests to "/" or "/index.html" for
 * repositories that do not generate this information.
 *
 * @since 3.0
 */
@Component
public class BrowseUnsupportedHandler
    implements Handler, org.sonatype.nexus.repository.BrowseUnsupportedHandler
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private static final String TEMPLATE_RESOURCE = "browseUnsupportedHtml.vm";

  private final TemplateHelper templateHelper;

  private final URL template;

  private final Route route;

  @Autowired
  public BrowseUnsupportedHandler(final TemplateHelper templateHelper, final SecurityHandler securityHandler) {
    this.templateHelper = checkNotNull(templateHelper);
    checkNotNull(securityHandler);
    this.template = getClass().getResource(TEMPLATE_RESOURCE);
    checkNotNull(template);
    this.route = new Route(MATCHER, ImmutableList.of(securityHandler, this));
  }

  @Nonnull
  @Override
  public Response handle(@Nonnull final Context context) throws Exception {
    TemplateParameters params = templateHelper.parameters();
    params.set("repository", context.getRepository());

    String html = templateHelper.render(template, params);
    return HttpResponses.ok(new StringPayload(html, "text/html"));
  }

  @Override
  @Nonnull
  public Route getRoute() {
    return this.route;
  }

  //
  // Matcher
  //

  /**
   * Matches GET request with path ending with one of (ignoring case):
   *
   * "/"
   * "/index.html"
   * "/index.htm"
   */
  private static class MatcherImpl
      implements Matcher
  {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean matches(final Context context) {
      checkNotNull(context);
      String action = context.getRequest().getAction();
      String path = context.getRequest().getPath();
      log.debug("Matching: {} {}", action, path);
      if (HttpMethods.GET.equals(action) || HttpMethods.HEAD.equals(action)) {
        path = Strings2.lower(path);

        return path.endsWith("/") || path.endsWith("/index.html") || path.endsWith("/index.htm");
      }
      return false;
    }
  }

  private static final Matcher MATCHER = new MatcherImpl();
}
