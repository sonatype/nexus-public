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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;

/**
 * Initial {@link AnonymousConfiguration} provider.
 *
 * Provides the initial configuration of anonymous configuration for fresh server installations.
 *
 * @since 3.0
 */
@Named("initial")
@Singleton
public class InitialAnonymousConfigurationProvider
  implements Provider<AnonymousConfiguration>
{
  private final boolean enabled;

  @Inject
  public InitialAnonymousConfigurationProvider(
      @Named("${nexus.security.default.anonymous:-true}") final boolean enabled)
  {
    this.enabled = enabled;
  }

  @Override
  public AnonymousConfiguration get() {
    AnonymousConfiguration model = new AnonymousConfiguration();
    model.setEnabled(enabled);
    model.setUserId(AnonymousConfiguration.DEFAULT_USER_ID);
    model.setRealmName(AnonymousConfiguration.DEFAULT_REALM_NAME);
    return model;
  }
}
