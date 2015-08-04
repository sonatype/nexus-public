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
package org.sonatype.nexus.configuration.application;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractLastingConfigurable;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.events.GlobalRemoteProxySettingsChangedEvent;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteProxySettings;
import org.sonatype.nexus.configuration.model.CRemoteProxySettingsCoreConfiguration;
import org.sonatype.nexus.proxy.repository.DefaultRemoteHttpProxySettings;
import org.sonatype.nexus.proxy.repository.DefaultRemoteProxySettings;
import org.sonatype.nexus.proxy.repository.RemoteHttpProxySettings;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.base.Throwables;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @since 2.6
 */
@Singleton
@Named
public class DefaultGlobalRemoteProxySettings
    extends AbstractLastingConfigurable<CRemoteProxySettings>
    implements GlobalRemoteProxySettings
{

  private final AuthenticationInfoConverter authenticationInfoConverter;

  @Inject
  public DefaultGlobalRemoteProxySettings(final EventBus eventBus, final ApplicationConfiguration applicationConfiguration, final AuthenticationInfoConverter authenticationInfoConverter) {
    super("Global HTTP Proxy", eventBus, applicationConfiguration);
    this.authenticationInfoConverter = checkNotNull(authenticationInfoConverter);
  }

  @Override
  protected CoreConfiguration<CRemoteProxySettings> wrapConfiguration(Object configuration)
      throws ConfigurationException
  {
    if (configuration instanceof ApplicationConfiguration) {
      return new CRemoteProxySettingsCoreConfiguration((ApplicationConfiguration) configuration);
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  // ==

  @Override
  public RemoteHttpProxySettings getHttpProxySettings() {
    if (isEnabled()) {
      try {
        return convertFromModel(getCurrentConfiguration(false).getHttpProxySettings());
      }
      catch (ConfigurationException e) {
        throw Throwables.propagate(e);
      }
    }

    return null;
  }

  @Override
  public void setHttpProxySettings(final RemoteHttpProxySettings settings) {
    if (!isEnabled()) {
      initConfig();
    }
    getCurrentConfiguration(true).setHttpProxySettings(convertToModel(settings));
  }

  @Override
  public RemoteHttpProxySettings getHttpsProxySettings() {
    if (isEnabled()) {
      try {
        return convertFromModel(getCurrentConfiguration(false).getHttpsProxySettings());
      }
      catch (ConfigurationException e) {
        throw Throwables.propagate(e);
      }
    }

    return null;
  }

  @Override
  public void setHttpsProxySettings(final RemoteHttpProxySettings settings) {
    if (!isEnabled()) {
      initConfig();
    }
    getCurrentConfiguration(true).setHttpsProxySettings(convertToModel(settings));
  }

  @Override
  public Set<String> getNonProxyHosts() {
    if (isEnabled()) {
      return new HashSet<String>(getCurrentConfiguration(false).getNonProxyHosts());
    }

    return Collections.emptySet();
  }

  @Override
  public void setNonProxyHosts(Set<String> nonProxyHosts) {
    if (!isEnabled()) {
      initConfig();
    }

    getCurrentConfiguration(true).setNonProxyHosts(new ArrayList<String>(
        nonProxyHosts == null ? Collections.<String>emptySet() : nonProxyHosts
    ));
  }

  @Override
  public RemoteHttpProxySettings getRemoteHttpProxySettingsFor(final URL url) {
    return DefaultRemoteProxySettings.getRemoteHttpProxySettingsFor(url, this);
  }

  private RemoteHttpProxySettings convertFromModel(CRemoteHttpProxySettings model)
      throws ConfigurationException
  {
    if (model == null) {
      return null;
    }

    final RemoteHttpProxySettings settings = new DefaultRemoteHttpProxySettings();

    settings.setHostname(model.getProxyHostname());
    settings.setPort(model.getProxyPort());
    settings.setProxyAuthentication(
        authenticationInfoConverter.convertAndValidateFromModel(model.getAuthentication())
    );

    return settings;
  }

  public CRemoteHttpProxySettings convertToModel(RemoteHttpProxySettings settings) {
    if (settings == null) {
      return null;
    }

    final CRemoteHttpProxySettings model = new CRemoteHttpProxySettings();

    model.setProxyHostname(settings.getHostname());
    model.setProxyPort(settings.getPort());
    model.setAuthentication(authenticationInfoConverter.convertToModel(settings.getProxyAuthentication()));

    return model;
  }

  // ==

  public void disable() {
    ((CRemoteProxySettingsCoreConfiguration) getCurrentCoreConfiguration()).nullifyConfig();
  }

  public boolean isEnabled() {
    return getCurrentConfiguration(false) != null;
  }

  protected void initConfig() {
    ((CRemoteProxySettingsCoreConfiguration) getCurrentCoreConfiguration()).initConfig();
  }

  @Override
  public boolean commitChanges()
      throws ConfigurationException
  {
    boolean wasDirty = super.commitChanges();

    if (wasDirty) {
      eventBus().post(new GlobalRemoteProxySettingsChangedEvent(this));
    }

    return wasDirty;
  }

}
