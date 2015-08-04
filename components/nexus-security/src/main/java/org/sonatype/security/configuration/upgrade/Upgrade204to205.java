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
package org.sonatype.security.configuration.upgrade;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.configuration.upgrade.UpgradeMessage;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * {@link SecurityConfigurationVersionUpgrader} step for security configuration version 2.0.4 > 2.0.5 upgrade.
 *
 * @since 2.6.3
 */
@Singleton
@Typed(SecurityConfigurationVersionUpgrader.class)
@Named("2.0.4")
public class Upgrade204to205
    implements SecurityConfigurationVersionUpgrader
{
  public Object loadConfiguration(File file)
      throws IOException, ConfigurationIsCorruptedException
  {
    try (Reader r = new BufferedReader(ReaderFactory.newXmlReader(file))) {
      // reading without interpolation to preserve user settings as variables
      return new org.sonatype.security.configuration.model.v2_0_4.io.xpp3.SecurityConfigurationXpp3Reader().read(r);
    }
    catch (XmlPullParserException e) {
      throw new ConfigurationIsCorruptedException(file.getAbsolutePath(), e);
    }
  }

  public void upgrade(UpgradeMessage message)
      throws ConfigurationIsCorruptedException
  {
    org.sonatype.security.configuration.model.v2_0_4.SecurityConfiguration oldc =
        (org.sonatype.security.configuration.model.v2_0_4.SecurityConfiguration) message.getConfiguration();

    org.sonatype.security.configuration.model.v2_0_5.SecurityConfiguration newc =
        new org.sonatype.security.configuration.model.v2_0_5.upgrade.BasicVersionUpgrade()
            .upgradeSecurityConfiguration(oldc);

    newc.setVersion(org.sonatype.security.configuration.model.v2_0_5.SecurityConfiguration.MODEL_VERSION);

    // NEXUS-5828: Security Manager field is gone!

    message.setModelVersion(org.sonatype.security.configuration.model.v2_0_5.SecurityConfiguration.MODEL_VERSION);
    message.setConfiguration(newc);
  }

}
