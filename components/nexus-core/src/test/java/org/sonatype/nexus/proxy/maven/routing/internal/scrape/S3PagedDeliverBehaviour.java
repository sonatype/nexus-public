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
package org.sonatype.nexus.proxy.maven.routing.internal.scrape;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.tests.http.server.api.Behaviour;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Behaviour} that simulates S3 pages. It holds multiple {@link DeliverBehaviour}s, and sends them back based on
 * parameters.
 */
public class S3PagedDeliverBehaviour
    implements Behaviour
{
  private final DeliverBehaviour parameterlessResponse;

  private final Map<String, DeliverBehaviour> pages;

  public S3PagedDeliverBehaviour(final DeliverBehaviour parameterlessResponse,
                                 final Map<String, DeliverBehaviour> pages)
  {
    this.parameterlessResponse = checkNotNull(parameterlessResponse);
    this.pages = checkNotNull(pages);
  }

  @Override
  public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
      throws Exception
  {
    final String marker = request.getParameter("marker");
    if (Strings.isNullOrEmpty(marker)) {
      return parameterlessResponse.execute(request, response, ctx);
    }
    else {
      final DeliverBehaviour b = pages.get(marker);
      if (b != null) {
        return b.execute(request, response, ctx);
      }
      else {
        response.sendError(500, "Unexpected marker: " + marker);
        return false;
      }
    }
  }
}
