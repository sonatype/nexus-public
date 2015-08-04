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
package org.sonatype.nexus.security.filter.authz;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.sonatype.nexus.auth.ClientInfo;
import org.sonatype.nexus.auth.NexusAuthorizationEvent;
import org.sonatype.nexus.auth.ResourceInfo;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.web.RemoteIPFinder;
import org.sonatype.security.SecuritySystem;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.HttpMethodPermissionFilter;

/**
 * A filter that maps the action from the HTTP Verb.
 *
 * @author cstamas
 */
public class FailureLoggingHttpMethodPermissionFilter
    extends HttpMethodPermissionFilter
{

  @Inject
  private SecuritySystem securitySystem;

  @Inject
  private EventBus eventBus;

  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response)
      throws IOException
  {
    recordAuthzFailureEvent(request, response);

    request.setAttribute(org.sonatype.nexus.web.Constants.ATTR_KEY_REQUEST_IS_AUTHZ_REJECTED, Boolean.TRUE);

    // NOTE: not calling super which is odd here due to NX anonymous user muck which has to be handled
    // NOTE: specially and adds lots of complication, consider removing the need for this in the future
    //return super.onAccessDenied(request, response);

    return false;
  }

  private void recordAuthzFailureEvent(ServletRequest request, ServletResponse response) {
    Subject subject = getSubject(request, response);

    if (securitySystem.getAnonymousUsername().equals(subject.getPrincipal())) {
      return;
    }

    final Action action = Action.valueOf(getHttpMethodAction(request));

    final ClientInfo clientInfo =
        new ClientInfo(String.valueOf(subject.getPrincipal()),
            RemoteIPFinder.findIP((HttpServletRequest) request), "n/a");
    final ResourceInfo resInfo =
        new ResourceInfo("HTTP", ((HttpServletRequest) request).getMethod(), action,
            ((HttpServletRequest) request).getRequestURI());

    eventBus.post(new NexusAuthorizationEvent(this, clientInfo, resInfo, false));
  }

}
