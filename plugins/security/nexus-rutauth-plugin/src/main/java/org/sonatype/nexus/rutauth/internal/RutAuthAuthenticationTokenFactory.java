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
package org.sonatype.nexus.rutauth.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.validation.InvalidConfigurationException;
import org.sonatype.nexus.security.filter.authc.AuthenticationTokenFactory;
import org.sonatype.nexus.security.filter.authc.HttpHeaderAuthenticationToken;
import org.sonatype.nexus.security.filter.authc.HttpHeaderAuthenticationTokenFactorySupport;
import org.sonatype.security.SecuritySystem;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link AuthenticationTokenFactory} that creates {@link RutAuthAuthenticationToken}s if a configured HTTP header
 * is present.
 *
 * @since 2.7
 */
@Named
@Singleton
public class RutAuthAuthenticationTokenFactory
    extends HttpHeaderAuthenticationTokenFactorySupport
{

  private static final Logger log = LoggerFactory.getLogger(RutAuthAuthenticationTokenFactory.class);

  private final SecuritySystem securitySystem;

  private String headerName;

  @Inject
  public RutAuthAuthenticationTokenFactory(final SecuritySystem securitySystem) {
    this.securitySystem = checkNotNull(securitySystem, "securitySystem");
  }

  @Override
  protected List<String> getHttpHeaderNames() {
    if (headerName != null) {
      return Lists.newArrayList(headerName);
    }
    return Collections.emptyList();
  }

  @Override
  protected HttpHeaderAuthenticationToken createToken(String headerName, String headerValue, final String host) {
    return new RutAuthAuthenticationToken(headerName, headerValue, host);
  }

  public void setHeaderName(String headerName) {
    this.headerName = headerName;
    maybeConfigureRealm();
  }

  private void maybeConfigureRealm() {
    List<String> realms = securitySystem.getRealms();
    if (!realms.contains(RutAuthRealm.ID)) {
      List<String> newRealms = Lists.newArrayList(realms);
      newRealms.add(RutAuthRealm.ID);
      try {
        securitySystem.setRealms(newRealms);
        log.info("Automatically enabled '{}'", RutAuthRealm.DESCRIPTION);
      }
      catch (InvalidConfigurationException e) {
        log.warn("Could not automatically enable '{}'", RutAuthRealm.DESCRIPTION, e);
      }
    }
  }

}
