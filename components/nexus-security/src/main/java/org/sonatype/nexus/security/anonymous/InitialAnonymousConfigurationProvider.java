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
package org.sonatype.nexus.security.anonymous;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.security.internal.AuthorizingRealmImpl;

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
  public static final String DEFAULT_USER_ID = "anonymous";

  public static final String DEFAULT_REALM_NAME = AuthorizingRealmImpl.NAME;

  @Override
  public AnonymousConfiguration get() {
    AnonymousConfiguration model = new AnonymousConfiguration();
    model.setEnabled(true);
    model.setUserId(DEFAULT_USER_ID);
    model.setRealmName(DEFAULT_REALM_NAME);
    return model;
  }
}
