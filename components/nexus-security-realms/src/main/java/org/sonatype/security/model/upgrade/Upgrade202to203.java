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
import java.util.Iterator;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.configuration.upgrade.UpgradeMessage;
import org.sonatype.security.model.v2_0_2.io.xpp3.SecurityConfigurationXpp3Reader;
import org.sonatype.security.model.v2_0_3.CUser;
import org.sonatype.security.model.v2_0_3.CUserRoleMapping;
import org.sonatype.security.model.v2_0_3.Configuration;
import org.sonatype.security.model.v2_0_3.upgrade.BasicVersionUpgrade;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Singleton
@Typed(SecurityUpgrader.class)
@Named("2.0.2")
public class Upgrade202to203
    extends ComponentSupport
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
    org.sonatype.security.model.v2_0_2.Configuration oldc =
        (org.sonatype.security.model.v2_0_2.Configuration) message.getConfiguration();

    org.sonatype.security.model.v2_0_3.Configuration newc = new BasicVersionUpgrade().upgradeConfiguration(oldc);

    // now strip out all the unused role mappings

    for (Iterator<CUserRoleMapping> iter = newc.getUserRoleMappings().iterator(); iter.hasNext(); ) {
      CUserRoleMapping roleMapping = iter.next();

      if (DEFAULT_SOURCE.equalsIgnoreCase(roleMapping.getSource())
          && !this.hasUser(roleMapping.getUserId(), newc)) {
        log.info("Removing orphaned user role mapping for user: '" + roleMapping.getUserId() + "'.");
        iter.remove();
      }
    }

    newc.setVersion(org.sonatype.security.model.v2_0_3.Configuration.MODEL_VERSION);
    message.setModelVersion(org.sonatype.security.model.v2_0_3.Configuration.MODEL_VERSION);
    message.setConfiguration(newc);
  }

  private boolean hasUser(String userId, Configuration configuration) {
    for (CUser user : configuration.getUsers()) {
      if (user.getId().equals(userId)) {
        return true;
      }
    }
    return false;
  }

}
