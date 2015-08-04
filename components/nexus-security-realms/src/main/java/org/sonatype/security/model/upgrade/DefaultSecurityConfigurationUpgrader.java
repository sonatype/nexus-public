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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.configuration.upgrade.ConfigurationIsCorruptedException;
import org.sonatype.configuration.upgrade.UnsupportedConfigurationVersionException;
import org.sonatype.configuration.upgrade.UpgradeMessage;
import org.sonatype.security.model.Configuration;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Default configuration updater, using versioned Modello models. It tried to detect version signature from existing
 * file and apply apropriate modello io stuff to load configuration. It is also aware of changes across model versions.
 *
 * @author cstamas
 */
@Singleton
@Typed(SecurityConfigurationUpgrader.class)
@Named("default")
public class DefaultSecurityConfigurationUpgrader
    extends ComponentSupport
    implements SecurityConfigurationUpgrader
{
  private final Map<String, SecurityUpgrader> upgraders;

  private final Map<String, SecurityDataUpgrader> dataUpgraders;

  @Inject
  public DefaultSecurityConfigurationUpgrader(Map<String, SecurityUpgrader> upgraders,
                                              Map<String, SecurityDataUpgrader> dataUpgraders)
  {
    this.upgraders = upgraders;
    this.dataUpgraders = dataUpgraders;
  }

  /**
   * This implementation relies to plexus registered upgraders. It will cycle through them until the configuration is
   * the needed (current) model version.
   *
   * @throws IOException if file does not exist or cannot be read
   * @throws ConfigurationIsCorruptedException
   *                     if file is corrupt, or does not need updating
   * @throws UnsupportedConfigurationVersionException
   *                     if file is of a version for which upgrades are not supported
   */
  public Configuration loadOldConfiguration(File file)
      throws IOException, ConfigurationIsCorruptedException, UnsupportedConfigurationVersionException
  {
    // try to find out the model version
    String modelVersion = null;

    try {
      Reader r = new BufferedReader(ReaderFactory.newXmlReader(file));

      Xpp3Dom dom = Xpp3DomBuilder.build(r);

      modelVersion = dom.getChild("version").getValue();
    }
    catch (XmlPullParserException e) {
      throw new ConfigurationIsCorruptedException(file.getAbsolutePath(), e);
    }

    if (Configuration.MODEL_VERSION.equals(modelVersion)) {
      // we have a problem here, model version is OK but we could not load it previously?
      throw new ConfigurationIsCorruptedException(file);
    }

    UpgradeMessage msg = new UpgradeMessage();

    msg.setModelVersion(modelVersion);

    SecurityUpgrader upgrader = upgraders.get(msg.getModelVersion());

    if (upgrader != null) {
      log.info("Upgrading old Security configuration file (version " + msg.getModelVersion() + ") from "
          + file.getAbsolutePath());

      msg.setConfiguration(upgrader.loadConfiguration(file));

      while (!Configuration.MODEL_VERSION.equals(msg.getModelVersion())) {

        // an application might need to upgrade content, that is NOT part of the model
        SecurityDataUpgrader dataUpgrader = this.dataUpgraders.get(msg.getModelVersion());

        if (upgrader != null) {
          upgrader.upgrade(msg);

          if (dataUpgrader != null) {
            dataUpgrader.upgrade(msg.getConfiguration());
          }
        }
        else {
          // we could parse the XML but have no model version? Is this security config at all?
          throw new UnsupportedConfigurationVersionException(modelVersion, file);
        }

        upgrader = upgraders.get(msg.getModelVersion());
      }

      log.info("Security configuration file upgraded to current version " + msg.getModelVersion()
          + " succesfully.");

      return (Configuration) msg.getConfiguration();
    }
    else {
      // we could parse the XML but have no model version? Is this security config at all?
      throw new UnsupportedConfigurationVersionException(modelVersion, file);
    }
  }
}
