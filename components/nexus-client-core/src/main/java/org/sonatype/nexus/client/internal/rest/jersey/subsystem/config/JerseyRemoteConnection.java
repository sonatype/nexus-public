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

import org.sonatype.nexus.client.core.subsystem.config.RemoteConnection;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;

/**
 * @since 2.14
 */
public class JerseyRemoteConnection
    extends JerseySegmentSupport<RemoteConnection, RemoteConnectionSettings>
    implements RemoteConnection
{
  public JerseyRemoteConnection(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  protected RemoteConnectionSettings getSettingsFrom(final GlobalConfigurationResource configuration) {
    return configuration.getGlobalConnectionSettings();
  }

  @Override
  protected void setSettingsIn(final RemoteConnectionSettings settings,
                               final GlobalConfigurationResource configuration)
  {
    configuration.setGlobalConnectionSettings(settings);
  }

  @Override
  protected RemoteConnectionSettings createSettings() {
    return new RemoteConnectionSettings();
  }
}
