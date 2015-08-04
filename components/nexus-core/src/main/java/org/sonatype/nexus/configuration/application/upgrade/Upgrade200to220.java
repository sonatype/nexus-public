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
package org.sonatype.nexus.configuration.application.upgrade;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.configuration.upgrade.SingleVersionUpgrader;
import org.sonatype.configuration.upgrade.UpgradeMessage;
import org.sonatype.nexus.configuration.model.v2_2_0.CRemoteStorage;
import org.sonatype.nexus.configuration.model.v2_2_0.upgrade.BasicVersionUpgrade;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Upgrades configuration model from version 2.0.0 to 2.2.0.
 *
 * @author cstamas
 */
@Singleton
@Named("2.0.0")
public class Upgrade200to220
    extends ComponentSupport
    implements SingleVersionUpgrader
{
  @Override
  public Object loadConfiguration(File file)
      throws IOException, ConfigurationIsCorruptedException
  {
    FileReader fr = null;

    org.sonatype.nexus.configuration.model.v2_0_0.Configuration conf = null;

    try {
      // reading without interpolation to preserve user settings as variables
      fr = new FileReader(file);

      org.sonatype.nexus.configuration.model.v2_0_0.io.xpp3.NexusConfigurationXpp3Reader reader =
          new org.sonatype.nexus.configuration.model.v2_0_0.io.xpp3.NexusConfigurationXpp3Reader();

      conf = reader.read(fr);
    }
    catch (XmlPullParserException e) {
      throw new ConfigurationIsCorruptedException(file.getAbsolutePath(), e);
    }
    finally {
      if (fr != null) {
        fr.close();
      }
    }

    return conf;
  }

  @Override
  public void upgrade(UpgradeMessage message)
      throws ConfigurationIsCorruptedException
  {
    org.sonatype.nexus.configuration.model.v2_0_0.Configuration oldc =
        (org.sonatype.nexus.configuration.model.v2_0_0.Configuration) message.getConfiguration();

    BasicVersionUpgrade versionConverter = new BasicVersionUpgrade()
    {
      @Override
      public CRemoteStorage upgradeCRemoteStorage(
          org.sonatype.nexus.configuration.model.v2_0_0.CRemoteStorage cRemoteStorage)
      {
        final CRemoteStorage remoteStorage = super.upgradeCRemoteStorage(cRemoteStorage);
        if (remoteStorage != null) {
          if (StringUtils.equals(remoteStorage.getProvider(), "apacheHttpClient3x")) {
            // nullify the provider IF: it is set, and is set to the "old" HttpClient3x only
            // as in nullified case, the
            // org.sonatype.nexus.proxy.storage.remote.DefaultRemoteProviderHintFactory will kick in as we
            // want
            remoteStorage.setProvider(null);
          }
        }
        return remoteStorage;
      }
    };

    org.sonatype.nexus.configuration.model.v2_2_0.Configuration newc = versionConverter.upgradeConfiguration(oldc);

    newc.setVersion(org.sonatype.nexus.configuration.model.v2_2_0.Configuration.MODEL_VERSION);
    message.setModelVersion(org.sonatype.nexus.configuration.model.v2_2_0.Configuration.MODEL_VERSION);
    message.setConfiguration(newc);
  }
}
