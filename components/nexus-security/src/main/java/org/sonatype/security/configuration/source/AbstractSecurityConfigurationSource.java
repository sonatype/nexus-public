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
package org.sonatype.security.configuration.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.sonatype.configuration.source.AbstractStreamConfigurationSource;
import org.sonatype.security.configuration.model.SecurityConfiguration;
import org.sonatype.security.configuration.model.io.xpp3.SecurityConfigurationXpp3Reader;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Abstract class that encapsulates Modello model.
 *
 * @author tstevens
 */
public abstract class AbstractSecurityConfigurationSource
    extends AbstractStreamConfigurationSource<SecurityConfiguration>
    implements SecurityConfigurationSource
{

  /**
   * The configuration.
   */
  private SecurityConfiguration configuration;

  public SecurityConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(SecurityConfiguration configuration) {
    this.configuration = configuration;
  }

  /**
   * Called by subclasses when loaded configuration is rejected for some reason.
   */
  protected void rejectConfiguration(String message) {
    this.configuration = null;

    if (message != null) {
      getLogger().warn(message);
    }
  }

  /**
   * Load configuration.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  protected void loadConfiguration(InputStream is)
      throws IOException
  {
    setConfigurationUpgraded(false);

    Reader fr = null;

    try {
      SecurityConfigurationXpp3Reader reader = new SecurityConfigurationXpp3Reader();

      fr = new InputStreamReader(is);

      configuration = reader.read(fr);
    }
    catch (XmlPullParserException e) {
      rejectConfiguration("Security configuration file was not loaded, it has the wrong structure.");

      if (getLogger().isDebugEnabled()) {
        getLogger().debug("security.xml is broken:", e);
      }
    }
    finally {
      if (fr != null) {
        fr.close();
      }
    }

    // check the model version if loaded
    if (configuration != null && !SecurityConfiguration.MODEL_VERSION.equals(configuration.getVersion())) {
      rejectConfiguration("Security configuration file was loaded but discarded, it has the wrong version number.");
    }

    if (getConfiguration() != null) {
      getLogger().debug("Configuration loaded successfully.");
    }
  }
}
