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
package org.sonatype.security.ldap.realms.persist;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
@Named
public class DefaultLdapConfiguration
    extends AbstractLdapConfiguration
    implements LdapConfiguration
{
  private final EventBus eventBus;

  @Inject
  public DefaultLdapConfiguration(ApplicationConfiguration applicationConfiguration, ConfigurationValidator validator,
      PasswordHelper passwordHelper, EventBus eventBus) throws IOException
  {
    super(applicationConfiguration, validator, passwordHelper);
    this.eventBus = checkNotNull(eventBus);

    if (passwordHelper.foundLegacyEncoding()) {
      log.info("Re-encoding entries using new master phrase");
      save();
    }
  }

  @Override
  public void save() throws IOException {
    super.save();
    // fire clear cache event
    this.eventBus.post(new LdapClearCacheEvent(this));
  }

  @Override
  public void clearCache() {
    super.clearCache();
    // fire clear cache event
    this.eventBus.post(new LdapClearCacheEvent(null));
  }
}
