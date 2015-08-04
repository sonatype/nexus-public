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

import org.sonatype.sisu.goodies.common.FormatTemplate;
import org.sonatype.tests.http.server.api.Behaviour;
import org.sonatype.tests.http.server.jetty.behaviour.Content;
import org.sonatype.tests.http.server.jetty.behaviour.ErrorBehaviour;

/**
 * {@link Behaviour} that combines {@link ErrorBehaviour} and {@link Content} behaviours, by letting specifying
 * response
 * error code and body, thus allowing to simulate error pages too. And it uses {@link FormatTemplate} for body, so body
 * template is evaluated per request.
 */
public class DeliverTemplateBehaviour
    implements Behaviour
{
  private final int code;

  private final String bodyContentType;

  private final FormatTemplate body;

  /**
   * Constructor.
   */
  public DeliverTemplateBehaviour(final int code, final String bodyContentType, final FormatTemplate body) {
    this.code = code;
    this.bodyContentType = bodyContentType;
    this.body = body;
  }

  @Override
  public boolean execute(HttpServletRequest request, HttpServletResponse response, Map<Object, Object> ctx)
      throws Exception
  {
    response.setStatus(code);
    response.setContentType(bodyContentType);
    final byte[] bodyPayload = body.evaluate().getBytes("UTF-8");
    response.setContentLength(bodyPayload.length);
    response.getOutputStream().write(bodyPayload);
    return true;
  }
}
