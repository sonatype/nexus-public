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
package org.sonatype.nexus.configuration.model;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;

/**
 * @since 2.6
 */
public class CRemoteProxySettingsCoreConfiguration
    extends AbstractCoreConfiguration<CRemoteProxySettings>
{
  private boolean nullified;

  public CRemoteProxySettingsCoreConfiguration(ApplicationConfiguration applicationConfiguration) {
    super(applicationConfiguration);
  }

  @Override
  protected CRemoteProxySettings extractConfiguration(Configuration configuration) {
    return configuration.getRemoteProxySettings();
  }

  public void initConfig() {
    CRemoteProxySettings newProxy = new CRemoteProxySettings();
    getApplicationConfiguration().getConfigurationModel().setRemoteProxySettings(newProxy);
    setOriginalConfiguration(newProxy);
  }

  public void nullifyConfig() {
    setChangedConfiguration(null);
    setOriginalConfiguration(null);
    nullified = true;
  }

  @Override
  public boolean isDirty() {
    return super.isDirty() || nullified;
  }

  @Override
  public void commitChanges()
      throws ConfigurationException
  {
    if (nullified) {
      // nullified, nothing to validate and the super.commitChanges() will not work
      getApplicationConfiguration().getConfigurationModel().setRemoteProxySettings(null);
    }
    else {
      super.commitChanges();
    }
    nullified = false;
  }

  @Override
  public void rollbackChanges() {
    super.rollbackChanges();
    nullified = false;
  }

  @Override
  protected void copyTransients(CRemoteProxySettings source, CRemoteProxySettings destination) {
    super.copyTransients(source, destination);

    // we need to manually set the http/https to null here, because of flawed overlay, where null objects do NOT
    // overwrite non-null objects
    if (((CRemoteProxySettings) source).getHttpProxySettings() == null) {
      ((CRemoteProxySettings) destination).setHttpProxySettings(null);
    }
    if (((CRemoteProxySettings) source).getHttpsProxySettings() == null) {
      ((CRemoteProxySettings) destination).setHttpsProxySettings(null);
    }
  }

}
