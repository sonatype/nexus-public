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

import org.sonatype.nexus.configuration.application.ApplicationConfiguration;

public class CGlobalRemoteConnectionSettingsCoreConfiguration
    extends AbstractCoreConfiguration<CRemoteConnectionSettings>
{
  public CGlobalRemoteConnectionSettingsCoreConfiguration(ApplicationConfiguration applicationConfiguration) {
    super(applicationConfiguration);
  }

  @Override
  public CRemoteConnectionSettings getConfiguration(boolean forWrite) {
    if (getOriginalConfiguration() == null) {
      // create default
      CRemoteConnectionSettings newConn = new CRemoteConnectionSettings();
      newConn.setConnectionTimeout(20000);
      newConn.setRetrievalRetryCount(3);
      getApplicationConfiguration().getConfigurationModel().setGlobalConnectionSettings(newConn);
      setOriginalConfiguration(newConn);
    }
    return super.getConfiguration(forWrite);
  }

  @Override
  protected CRemoteConnectionSettings extractConfiguration(Configuration configuration) {
    return configuration.getGlobalConnectionSettings();
  }

  @Override
  protected void copyTransients(final CRemoteConnectionSettings source, final CRemoteConnectionSettings destination) {
    super.copyTransients(source, destination);

    if (((CRemoteConnectionSettings) source).getQueryString() == null) {
      ((CRemoteConnectionSettings) destination).setQueryString(null);
    }
    if (((CRemoteConnectionSettings) source).getUserAgentCustomizationString() == null) {
      ((CRemoteConnectionSettings) destination).setUserAgentCustomizationString(null);
    }
  }

}
