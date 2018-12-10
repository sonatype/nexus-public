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
package org.sonatype.nexus.coreui.internal.wonderland;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.common.wonderland.AuthTicketService;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Authenticate a user's credentials.
 *
 * @since 2.7
 */
@Named
@Singleton
@Path(AuthenticateResource.RESOURCE_URI)
public class AuthenticateResource
    extends ComponentSupport
    implements Resource
{
  public static final String RESOURCE_URI = "/wonderland/authenticate";

  private final AuthTicketService authTickets;

  @Inject
  public AuthenticateResource(final AuthTicketService authTickets) {
    this.authTickets = checkNotNull(authTickets);
  }

  // FIXME: This may be missing annotation to require user or authentication annotations?

  /**
   * Authenticate a specific user and generate a one-time-use authentication token.
   *
   * @param token User authentication details.
   * @return Authentication ticket.
   */
  @POST
  @Consumes({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
  public AuthTicketXO post(final AuthTokenXO token) {
    checkNotNull(token);

    final String username = Strings2.decodeBase64(token.getU());
    final String password = Strings2.decodeBase64(token.getP());

    // Require current user to be the requested user to authenticate
    final Subject subject = SecurityUtils.getSubject();
    final Object principal = subject.getPrincipal();
    final String principalName = principal == null ? "" : principal.toString();

    if (log.isDebugEnabled()) {
      log.debug("payload username: {}, payload password: {}, principal: {}", username, Strings2.mask(password),
          principalName);
    }

    if (!principalName.equals(username)) {
      log.warn("auth token request denied - authenticated user {} does not match payload user {}",
          principalName, username);
      throw new WebApplicationException("Username mismatch", Status.BAD_REQUEST);
    }

    // Ask the sec-manager to authenticate, this won't alter the current subject
    try {
      SecurityUtils.getSecurityManager().authenticate(new UsernamePasswordToken(username, password));
    }
    catch (AuthenticationException e) {
      log.trace("Authentication failed", e);
      throw new WebApplicationException("Authentication failed", Status.FORBIDDEN);
    }

    // At this point we should be authenticated, return a new ticket
    return new AuthTicketXO().withT(authTickets.createTicket(username));
  }
}
