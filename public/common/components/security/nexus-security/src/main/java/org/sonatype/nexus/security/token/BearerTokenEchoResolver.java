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
package org.sonatype.nexus.security.token;

import java.util.Optional;

import org.apache.shiro.subject.PrincipalCollection;

/**
 * Resolves whether an incoming request carries a service-account token that should be echoed back
 * rather than minted via the API key service.
 */
public interface BearerTokenEchoResolver
{
  String CREDENTIAL_PREFIX = "sat.";

  /**
   * Returns the token that was presented on the current request if it begins with
   * {@link #CREDENTIAL_PREFIX} <em>and</em> the token is owned by {@code principals}.
   * Returns empty when no SAT is in the header, the SAT fails authentication, or the SAT
   * resolves to a different principal than the one being tokenised.
   *
   * <p>
   * <b>Performance note:</b> the current implementation re-authenticates the SAT against
   * the realm on every call to verify ownership. Because {@code ServiceAccountRealm} disables
   * auth caching (immediate revocation requirement), each invocation incurs a database read
   * even though the filter chain has already authenticated the same credential. A future
   * optimisation (see NEXUS-54481) can eliminate this by storing the verified credential in a
   * request attribute during filter authentication and reading it here.
   */
  Optional<String> presentedToken(PrincipalCollection principals);

  /**
   * Authenticates with the given credentials and, if successful, returns the service-account
   * token to echo back to the caller.
   *
   * @param username account name to authenticate as
   * @param password credential — must begin with {@link #CREDENTIAL_PREFIX}; callers should
   *          pre-filter on {@code password.startsWith(CREDENTIAL_PREFIX)} before invoking
   */
  Optional<String> loginAndEcho(String username, String password);
}
