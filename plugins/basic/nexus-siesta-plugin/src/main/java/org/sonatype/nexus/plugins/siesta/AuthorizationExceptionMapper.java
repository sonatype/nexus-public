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
package org.sonatype.nexus.plugins.siesta;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.sonatype.sisu.siesta.common.error.ErrorXO;
import org.sonatype.sisu.siesta.server.ErrorExceptionMapperSupport;

import org.apache.shiro.authz.AuthorizationException;

/**
 * Maps {@link AuthorizationException} to 403 with a {@link ErrorXO} body in case that a user is logged in or to an
 * 401 in case that no user is authenticated.
 *
 * @since 2.4
 */
@Named
@Singleton
public class AuthorizationExceptionMapper
    extends ErrorExceptionMapperSupport<AuthorizationException>
{

  private static final String AUTH_SCHEME_KEY = "auth.scheme";

  public static final String AUTH_REALM_KEY = "auth.realm";

  private static final String ANONYMOUS_LOGIN = "nexus.anonymous";

  private static final String AUTHENTICATE_HEADER = "WWW-Authenticate";

  @Inject
  private Provider<HttpServletRequest> httpServletRequestProvider;

  /**
   * @since 2.5.0
   */
  @Override
  protected Response convert(final AuthorizationException exception, final String id) {
    final Response.ResponseBuilder builder = Response.fromResponse(super.convert(exception, id));

    final HttpServletRequest servletRequest = httpServletRequestProvider.get();
    if (servletRequest.getAttribute(ANONYMOUS_LOGIN) != null) {
      String scheme = (String) servletRequest.getAttribute(AUTH_SCHEME_KEY);
      String realm = (String) servletRequest.getAttribute(AUTH_REALM_KEY);

      builder
          .status(Response.Status.UNAUTHORIZED)
          .header(AUTHENTICATE_HEADER, String.format("%s realm=\"%s\"", scheme, realm));
    }
    return builder.build();
  }

  @Override
  protected int getStatusCode(final AuthorizationException exception) {
    return Response.Status.FORBIDDEN.getStatusCode();
  }

}
