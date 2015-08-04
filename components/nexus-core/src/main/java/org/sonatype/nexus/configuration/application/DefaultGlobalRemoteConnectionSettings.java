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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.AbstractLastingConfigurable;
import org.sonatype.nexus.configuration.CoreConfiguration;
import org.sonatype.nexus.configuration.application.events.GlobalRemoteConnectionSettingsChangedEvent;
import org.sonatype.nexus.configuration.model.CGlobalRemoteConnectionSettingsCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.DefaultRemoteConnectionSettings;
import org.sonatype.nexus.proxy.repository.RemoteConnectionSettings;
import org.sonatype.sisu.goodies.eventbus.EventBus;

@Singleton
@Named
public class DefaultGlobalRemoteConnectionSettings
    extends AbstractLastingConfigurable<CRemoteConnectionSettings>
    implements GlobalRemoteConnectionSettings
{
  @Inject
  public DefaultGlobalRemoteConnectionSettings(final EventBus eventBus, final ApplicationConfiguration applicationConfiguration) {
    super("Global Remote Connection Settings", eventBus, applicationConfiguration);
  }

  @Override
  protected CoreConfiguration<CRemoteConnectionSettings> wrapConfiguration(Object configuration)
      throws ConfigurationException
  {
    if (configuration instanceof ApplicationConfiguration) {
      return new CGlobalRemoteConnectionSettingsCoreConfiguration((ApplicationConfiguration) configuration);
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  // ==

  @Override 
  public int getConnectionTimeout() {
    return getCurrentConfiguration(false).getConnectionTimeout();
  }

  @Override 
  public void setConnectionTimeout(int connectionTimeout) {
    getCurrentConfiguration(true).setConnectionTimeout(connectionTimeout);
  }

  @Override 
  public String getQueryString() {
    return getCurrentConfiguration(false).getQueryString();
  }

  @Override 
  public void setQueryString(String queryString) {
    getCurrentConfiguration(true).setQueryString(queryString);
  }

  @Override 
  public int getRetrievalRetryCount() {
    return getCurrentConfiguration(false).getRetrievalRetryCount();
  }

  @Override 
  public void setRetrievalRetryCount(int retrievalRetryCount) {
    getCurrentConfiguration(true).setRetrievalRetryCount(retrievalRetryCount);
  }

  @Override 
  public String getUserAgentCustomizationString() {
    return getCurrentConfiguration(false).getUserAgentCustomizationString();
  }

  @Override 
  public void setUserAgentCustomizationString(String userAgentCustomizationString) {
    getCurrentConfiguration(true).setUserAgentCustomizationString(userAgentCustomizationString);
  }

  // ==

  @Override 
  public RemoteConnectionSettings convertAndValidateFromModel(CRemoteConnectionSettings model)
      throws ConfigurationException
  {
    if (model != null) {
      RemoteConnectionSettings remoteConnectionSettings = new DefaultRemoteConnectionSettings();
      remoteConnectionSettings.setConnectionTimeout(model.getConnectionTimeout());
      remoteConnectionSettings.setQueryString(model.getQueryString());
      remoteConnectionSettings.setRetrievalRetryCount(model.getRetrievalRetryCount());
      remoteConnectionSettings.setUserAgentCustomizationString(model.getUserAgentCustomizationString());
      return remoteConnectionSettings;
    }
    else {
      return null;
    }
  }

  @Override 
  public CRemoteConnectionSettings convertToModel(RemoteConnectionSettings settings) {
    if (settings == null) {
      return null;
    }
    else {
      CRemoteConnectionSettings model = new CRemoteConnectionSettings();
      model.setConnectionTimeout(settings.getConnectionTimeout());
      model.setQueryString(settings.getQueryString());
      model.setRetrievalRetryCount(settings.getRetrievalRetryCount());
      model.setUserAgentCustomizationString(settings.getUserAgentCustomizationString());
      return model;
    }
  }

  @Override
  public boolean commitChanges()
      throws ConfigurationException
  {
    boolean wasDirty = super.commitChanges();
    if (wasDirty) {
      eventBus().post(new GlobalRemoteConnectionSettingsChangedEvent(this));
    }
    return wasDirty;
  }
}
