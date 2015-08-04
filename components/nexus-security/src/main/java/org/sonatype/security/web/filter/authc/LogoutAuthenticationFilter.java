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
package org.sonatype.security.web.filter.authc;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.AuthenticationFilter;

/**
 * Perform sign-out of the {@link HttpSession} and {@link Subject} while leaving the response content up to
 * downstream components.
 * <p />
 * Note: Downstream sign-out related {@link Filter filters} and {@link Servlet servlets} need to expect the session
 * to be invalidated and subject signed out after processing by this filter.
 */
public class LogoutAuthenticationFilter
    extends AuthenticationFilter
{

  /**
   * We are letting everyone in.
   */
  @Override
  protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
    return true;
  }

  /**
   * We are letting the processing chain to continue (must implement it is abstract in superclass but we will never
   * get here).
   */
  @Override
  protected boolean onAccessDenied(ServletRequest request, ServletResponse response)
      throws Exception
  {
    return true;
  }

  /**
   * Signs-out the detected {@link HttpSession} and {@link Subject} in the request.
   *
   * This implementation:
   *
   * <ul>
   * <li>invalidates the existing request session using {@link HttpSession#invalidate}</li>
   * <li>signs-out the current Subject using {@link Subject#logout}</li>
   * </ul>
   *
   * @return true always to allow further handling of response content
   */
  @Override
  public boolean preHandle(ServletRequest request, ServletResponse response)
      throws Exception
  {
    if (HttpServletRequest.class.isAssignableFrom(request.getClass())) {
      HttpSession session = ((HttpServletRequest) request).getSession(false);

      if (session != null) {
        session.invalidate();
      }
    }

    Subject subject = getSubject(request, response);

    if (subject != null) {
      subject.logout();
    }

    return true;
  }
}
