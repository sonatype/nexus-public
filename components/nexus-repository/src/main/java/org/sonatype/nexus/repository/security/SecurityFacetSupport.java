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
package org.sonatype.nexus.repository.security;

import javax.inject.Inject;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.security.SecurityHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Support for {@link SecurityFacet} implementations.
 *
 * @since 3.0
 */
public class SecurityFacetSupport
    extends FacetSupport
    implements SecurityFacet
{
  private final SecurityHelper securityHelper;

  private final RepositoryFormatSecurityConfigurationResource securityResource;

  @Inject
  public SecurityFacetSupport(final SecurityHelper securityHelper,
                              final RepositoryFormatSecurityConfigurationResource securityResource)
  {
    this.securityHelper = checkNotNull(securityHelper);
    this.securityResource = checkNotNull(securityResource);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    securityResource.add(getRepository());
  }

  @Override
  protected void doDestroy() throws Exception {
    securityResource.remove(getRepository());
  }

  @Override
  public void ensurePermitted(final Request request) {
    checkNotNull(request);

    // determine permission action from request
    String action = action(request);

    securityHelper.ensurePermitted(new RepositoryViewPermission(getRepository(), action));
  }

  /**
   * Returns BREAD action for request action.
   */
  private String action(final Request request) {
    switch (request.getAction()) {
      case HttpMethods.OPTIONS:
      case HttpMethods.GET:
      case HttpMethods.HEAD:
      case HttpMethods.TRACE:
        return BreadActions.READ;

      case HttpMethods.POST:
      case HttpMethods.MKCOL:
        return BreadActions.ADD;

      case HttpMethods.PUT:
        return BreadActions.EDIT;

      case HttpMethods.DELETE:
        return BreadActions.DELETE;
    }

    throw new RuntimeException("Unsupported action: " + request.getAction());
  }
}
