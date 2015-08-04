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
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.config;

import org.sonatype.nexus.client.core.subsystem.config.Security;
import org.sonatype.nexus.client.core.subsystem.config.SecuritySettings;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;

/**
 * @since 2.7
 */
public class JerseySecurity
    extends JerseySegmentSupport<Security, SecuritySettings>
    implements Security
{

  public JerseySecurity(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  protected SecuritySettings getSettingsFrom(final GlobalConfigurationResource configuration) {
    final SecuritySettings settings = new SecuritySettings();
    settings.setAnonymousAccessEnabled(configuration.isSecurityAnonymousAccessEnabled());
    settings.setAnonymousPassword(configuration.getSecurityAnonymousPassword());
    settings.setAnonymousUsername(configuration.getSecurityAnonymousUsername());
    settings.setRealms(configuration.getSecurityRealms());
    return settings;
  }

  @Override
  protected void setSettingsIn(final SecuritySettings settings,
                               final GlobalConfigurationResource configuration)
  {
    configuration.setSecurityAnonymousAccessEnabled(settings.isAnonymousAccessEnabled());
    configuration.setSecurityAnonymousPassword(settings.getAnonymousPassword());
    configuration.setSecurityAnonymousUsername(settings.getAnonymousUsername());
    configuration.setSecurityRealms(settings.getRealms());
  }

  @Override
  protected SecuritySettings createSettings() {
    return new SecuritySettings();
  }

}
