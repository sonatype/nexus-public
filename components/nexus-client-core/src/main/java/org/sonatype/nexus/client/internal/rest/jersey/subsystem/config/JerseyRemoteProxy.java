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

import org.sonatype.nexus.client.core.subsystem.config.RemoteProxy;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.RemoteProxySettingsDTO;

/**
 * @since 2.6
 */
public class JerseyRemoteProxy
    extends JerseySegmentSupport<RemoteProxy, RemoteProxySettingsDTO>
    implements RemoteProxy
{

  public JerseyRemoteProxy(final JerseyNexusClient nexusClient) {
    super(nexusClient);
  }

  @Override
  protected RemoteProxySettingsDTO getSettingsFrom(final GlobalConfigurationResource configuration) {
    return configuration.getRemoteProxySettings();
  }

  @Override
  protected void setSettingsIn(final RemoteProxySettingsDTO settings,
                               final GlobalConfigurationResource configuration)
  {
    configuration.setRemoteProxySettings(settings);
  }

  @Override
  protected RemoteProxySettingsDTO createSettings() {
    return new RemoteProxySettingsDTO();
  }

  @Override
  public RemoteProxy disableHttpProxy() {
    settings().setHttpProxySettings(null);
    return save();
  }

  @Override
  public RemoteProxy disableHttpsProxy() {
    settings().setHttpsProxySettings(null);
    return save();
  }

}
