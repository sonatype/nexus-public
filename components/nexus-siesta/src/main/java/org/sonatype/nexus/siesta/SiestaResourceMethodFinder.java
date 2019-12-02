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
package org.sonatype.nexus.siesta;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;

import org.sonatype.nexus.siesta.internal.resteasy.ComponentContainerImpl;

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.servlet.HttpServletInputMessage;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyDeployment;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.jboss.resteasy.plugins.server.servlet.ServletUtil.extractHttpHeaders;
import static org.jboss.resteasy.plugins.server.servlet.ServletUtil.extractUriInfo;
import static org.sonatype.nexus.siesta.SiestaModule.MOUNT_POINT;

/**
 * Resource Method finder for all the resources loaded through the {@link SiestaModule}, {@link SiestaServlet}
 * and {@link ComponentContainer}.
 *
 * @since 3.20
 */
public class SiestaResourceMethodFinder
{
  private ComponentContainerImpl componentContainer;

  private ResteasyDeployment deployment;

  public SiestaResourceMethodFinder(final ComponentContainerImpl componentContainer,
                                    final ResteasyDeployment deployment)
  {
    this.componentContainer = requireNonNull(componentContainer);
    this.deployment = requireNonNull(deployment);
  }

  public String getResourceMethodPath(final HttpServletRequest request,
                                      final HttpServletResponse response)
  {
    StringBuilder buffer = new StringBuilder();
    ResourceMethodInvoker method = getResourceMethod(request, response);

    Path classPath = method.getResourceClass().getAnnotation(Path.class);
    if (nonNull(classPath)) {
      buffer.append(maybePrependWithForwardSlash(classPath.value()));
    }

    Path methodPath = method.getMethod().getDeclaredAnnotation(Path.class);
    if (nonNull(methodPath)) {
      buffer.append(maybePrependWithForwardSlash(methodPath.value()));
    }

    return cleanForwardSlashes(buffer.toString());
  }

  public ResourceMethodInvoker getResourceMethod(final HttpServletRequest request,
                                                 final HttpServletResponse response)
  {
    HttpRequest httpRequest = new HttpServletInputMessage(
        request,
        response,
        request.getServletContext(),
        null,
        extractHttpHeaders(request),
        extractUriInfo(request, MOUNT_POINT),
        request.getMethod(),
        (SynchronousDispatcher) this.componentContainer.getDispatcher());

    return (ResourceMethodInvoker) deployment.getRegistry().getResourceInvoker(httpRequest);
  }

  private String maybePrependWithForwardSlash(final String value) {
    return value.startsWith("/") ? value : "/" + value;
  }

  private String cleanForwardSlashes(final String s) {
    return s.replaceAll("//", "/");
  }
}
