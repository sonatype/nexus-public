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
package org.sonatype.security.model.upgrade;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.configuration.upgrade.UpgradeMessage;
import org.sonatype.security.model.v2_0_3.io.xpp3.SecurityConfigurationXpp3Reader;
import org.sonatype.security.model.v2_0_4.CUser;
import org.sonatype.security.model.v2_0_4.upgrade.BasicVersionUpgrade;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Singleton
@Typed(SecurityUpgrader.class)
@Named("2.0.3")
public class Upgrade203to204
    implements SecurityUpgrader
{
  private static String DEFAULT_SOURCE = "default";

  public Object loadConfiguration(File file)
      throws IOException, ConfigurationIsCorruptedException
  {
    FileReader fr = null;

    try {
      // reading without interpolation to preserve user settings as variables
      fr = new FileReader(file);

      SecurityConfigurationXpp3Reader reader = new SecurityConfigurationXpp3Reader();

      return reader.read(fr);
    }
    catch (XmlPullParserException e) {
      throw new ConfigurationIsCorruptedException(file.getAbsolutePath(), e);
    }
    finally {
      if (fr != null) {
        fr.close();
      }
    }
  }

  public void upgrade(UpgradeMessage message)
      throws ConfigurationIsCorruptedException
  {
    org.sonatype.security.model.v2_0_3.Configuration oldc =
        (org.sonatype.security.model.v2_0_3.Configuration) message.getConfiguration();

    org.sonatype.security.model.v2_0_4.Configuration newc =
        new SecurityVersionUpgrade().upgradeConfiguration(oldc);

    newc.setVersion(org.sonatype.security.model.v2_0_4.Configuration.MODEL_VERSION);
    message.setModelVersion(org.sonatype.security.model.v2_0_4.Configuration.MODEL_VERSION);
    message.setConfiguration(newc);
  }

  class SecurityVersionUpgrade
      extends BasicVersionUpgrade
  {

    @Override
    public CUser upgradeCUser(org.sonatype.security.model.v2_0_3.CUser cUser, CUser value) {
      CUser upgradedUser = super.upgradeCUser(cUser, value);

      // get the old users name
      String name = cUser.getName();
      String[] nameParts = name.trim().split(" ", 2);

      // the first name is everything to the left of the first space
      upgradedUser.setFirstName(nameParts[0]);

      // last name is everything else ( if it exists )
      if (nameParts.length > 1) {
        upgradedUser.setLastName(nameParts[1]);
      }

      return upgradedUser;
    }
  }
}
