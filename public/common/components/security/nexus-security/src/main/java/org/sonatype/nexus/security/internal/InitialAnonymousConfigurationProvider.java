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
package org.sonatype.nexus.security.internal;

import org.springframework.beans.factory.annotation.Autowired;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Initial {@link AnonymousConfiguration} provider.
 *
 * Provides the initial configuration of anonymous configuration for fresh server installations.
 *
 * @since 3.0
 */
@Component
@Qualifier("initial")
public class InitialAnonymousConfigurationProvider
    implements FactoryBean<AnonymousConfiguration>
{
  private final boolean enabled;

  @Autowired
  public InitialAnonymousConfigurationProvider(
      @Value("${nexus.security.default.anonymous:false}") final boolean enabled)
  {
    this.enabled = enabled;
  }

  @Override
  public AnonymousConfiguration getObject() {
    return new InitialAnonymousConfiguration();
  }

  private class InitialAnonymousConfiguration
      implements AnonymousConfiguration
  {
    @Override
    public AnonymousConfiguration copy() {
      return this;
    }

    @Override
    public String getRealmName() {
      return AnonymousConfiguration.DEFAULT_REALM_NAME;
    }

    @Override
    public String getUserId() {
      return AnonymousConfiguration.DEFAULT_USER_ID;
    }

    @Override
    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setRealmName(final String realmName) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setUserId(final String userId) {
      throw new UnsupportedOperationException();
    }
  }

  @Override
  public Class<?> getObjectType() {
    return AnonymousConfiguration.class;
  }
}
