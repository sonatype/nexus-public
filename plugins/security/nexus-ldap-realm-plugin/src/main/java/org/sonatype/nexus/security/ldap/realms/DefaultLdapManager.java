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
package org.sonatype.nexus.security.ldap.realms;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.ldap.LdapAuthenticator;
import org.sonatype.security.ldap.dao.LdapGroupDAO;
import org.sonatype.security.ldap.dao.LdapUserDAO;
import org.sonatype.security.ldap.realms.persist.LdapClearCacheEvent;
import org.sonatype.security.ldap.realms.persist.LdapConfiguration;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class DefaultLdapManager
    extends AbstractLdapManager
{
  @Inject
  public DefaultLdapManager(final LdapAuthenticator ldapAuthenticator, final LdapUserDAO ldapUserManager,
      final LdapGroupDAO ldapGroupManager, final LdapConfiguration ldapConfiguration, final EventBus eventBus)
  {
    super(ldapAuthenticator, ldapUserManager, ldapGroupManager, ldapConfiguration);
    checkNotNull(eventBus).register(this);
  }

  @AllowConcurrentEvents
  @Subscribe
  public void onEvent(final LdapClearCacheEvent evt) {
    resetLdapConnector();
  }
}
