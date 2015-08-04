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
package org.sonatype.nexus.web;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Rendering component, to render error pages.
 *
 * @since 2.8
 */
public interface TemplateRenderer
{
  /**
   * Renders a standard error page. Passed in responseCode must be greater or equal to 400. All the parameters
   * must be non-null except for {@code reasonPhrase} and {@code exception}. When this call returns, the
   * {@link HttpServletResponse} will be commited with rendered error page, and response code set.
   *
   * @param request          The servlet request.
   * @param response         The servlet response.
   * @param responseCode     The HTTP error code, must be greater of equal to 400.
   * @param reasonPhrase     The HTTP reason phrase, might be {@code null} (in that case the defaults will be used).
   * @param errorDescription Error description meant for human consumption, will be rendered on the error page.
   * @param exception        The exception (if any) that lead to this error condition, might be {@code null}.
   */
  void renderErrorPage(HttpServletRequest request,
                       HttpServletResponse response,
                       int responseCode,
                       @Nullable String reasonPhrase,
                       String errorDescription,
                       @Nullable Throwable exception)
      throws IOException;

  interface TemplateLocator
  {
    public String name();

    public ClassLoader classloader();
  }

  /**
   * Returns a template locator instance. The template existence in this moment is not checked, only the locator
   * is constructed.
   *
   * @param name        A FQ binary name of the template.
   * @param classloader The ClassLoader to be used to locate the template.
   */
  TemplateLocator template(String name, ClassLoader classloader);

  /**
   * Renders a template to servlet response. After rendering, the response will be commitet. Before calling this
   * method, a proper response code and all the response headers should be set.
   *
   * @throws IllegalArgumentException if the template locator does not points to an existing template.
   */
  void render(TemplateLocator tl, Map<String, Object> dataModel, HttpServletResponse response)
      throws IOException, IllegalArgumentException;
}
