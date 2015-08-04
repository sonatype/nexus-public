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

import org.sonatype.nexus.client.core.spi.SubsystemSupport;
import org.sonatype.nexus.client.core.subsystem.config.Segment;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.GlobalConfigurationResource;
import org.sonatype.nexus.rest.model.GlobalConfigurationResourceResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.beanutils.BeanUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class JerseySegmentSupport<ME extends Segment, S>
    extends SubsystemSupport<JerseyNexusClient>
    implements Segment<ME, S>
{

  private final S settings;

  private GlobalConfigurationResource configuration;

  public JerseySegmentSupport(final JerseyNexusClient nexusClient) {
    super(nexusClient);
    settings = checkNotNull(createSettings());
    refresh();
  }

  @Override
  public S settings() {
    return settings;
  }

  @Override
  public ME refresh() {
    configuration = checkNotNull(getConfiguration());
    copy(getSettingsFrom(configuration), settings);
    return me();
  }

  @Override
  public ME save() {
    setSettingsIn(settings, configuration);
    setConfiguration(configuration);
    return refresh();
  }

  protected abstract S getSettingsFrom(final GlobalConfigurationResource configuration);

  protected abstract void setSettingsIn(final S settings, final GlobalConfigurationResource configuration);

  protected abstract S createSettings();

  private void copy(final S configurationSettings, final S localSettings) {
    S toCopy = configurationSettings;
    if (configurationSettings == null) {
      toCopy = createSettings();
    }
    try {
      BeanUtils.copyProperties(localSettings, toCopy);
    }
    catch (final Exception e) {
      throw Throwables.propagate(e);
    }
  }

  ME me() {
    return (ME) this;
  }

  @VisibleForTesting
  GlobalConfigurationResource getConfiguration() {
    try {
      return getNexusClient()
          .serviceResource("global_settings/current")
          .get(GlobalConfigurationResourceResponse.class)
          .getData();
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

  @VisibleForTesting
  void setConfiguration(final GlobalConfigurationResource configuration) {
    final GlobalConfigurationResourceResponse request = new GlobalConfigurationResourceResponse();
    request.setData(configuration);

    try {
      getNexusClient().serviceResource("global_settings/current").put(request);
    }
    catch (UniformInterfaceException e) {
      throw getNexusClient().convert(e);
    }
    catch (ClientHandlerException e) {
      throw getNexusClient().convert(e);
    }
  }

}
