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
import org.sonatype.nexus.configuration.model.CGlobalRestApiCoreConfiguration;
import org.sonatype.nexus.configuration.model.CRestApiSettings;
import org.sonatype.sisu.goodies.eventbus.EventBus;

@Singleton
@Named
public class DefaultGlobalRestApiSettings
    extends AbstractLastingConfigurable<CRestApiSettings>
    implements GlobalRestApiSettings
{
  @Inject
  public DefaultGlobalRestApiSettings(final EventBus eventBus, final ApplicationConfiguration applicationConfiguration) {
    super("Global Rest Api Settings", eventBus, applicationConfiguration);
  }

  @Override
  protected void initializeConfiguration()
      throws ConfigurationException
  {
    if (getApplicationConfiguration().getConfigurationModel() != null) {
      configure(getApplicationConfiguration());
    }
  }

  @Override
  protected CoreConfiguration<CRestApiSettings> wrapConfiguration(Object configuration)
      throws ConfigurationException
  {
    if (configuration instanceof ApplicationConfiguration) {
      return new CGlobalRestApiCoreConfiguration((ApplicationConfiguration) configuration);
    }
    else {
      throw new ConfigurationException("The passed configuration object is of class \""
          + configuration.getClass().getName() + "\" and not the required \""
          + ApplicationConfiguration.class.getName() + "\"!");
    }
  }

  protected void initConfig() {
    ((CGlobalRestApiCoreConfiguration) getCurrentCoreConfiguration()).initConfig();
  }

  // ==

  @Override
  public void disable() {
    ((CGlobalRestApiCoreConfiguration) getCurrentCoreConfiguration()).nullifyConfig();
  }

  @Override
  public boolean isEnabled() {
    return getCurrentConfiguration(false) != null;
  }

  @Override
  public void setForceBaseUrl(boolean forceBaseUrl) {
    if (!isEnabled()) {
      initConfig();
    }

    getCurrentConfiguration(true).setForceBaseUrl(forceBaseUrl);
  }

  @Override
  public boolean isForceBaseUrl() {
    if (!isEnabled()) {
      return false;
    }

    return getCurrentConfiguration(false).isForceBaseUrl();
  }

  @Override
  public void setBaseUrl(String baseUrl) {
    if (!isEnabled()) {
      initConfig();
    }

    getCurrentConfiguration(true).setBaseUrl(baseUrl);
  }

  @Override
  public String getBaseUrl() {
    if (!isEnabled()) {
      return null;
    }

    return getCurrentConfiguration(false).getBaseUrl();
  }

  @Override
  public void setUITimeout(int uiTimeout) {
    if (!isEnabled()) {
      initConfig();
    }

    getCurrentConfiguration(true).setUiTimeout(uiTimeout);
  }

  @Override
  public int getUITimeout() {
    if (!isEnabled()) {
      return 0;
    }

    return getCurrentConfiguration(false).getUiTimeout();
  }

}
