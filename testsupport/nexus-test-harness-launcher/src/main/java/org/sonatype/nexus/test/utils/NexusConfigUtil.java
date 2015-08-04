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
package org.sonatype.nexus.test.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CPathMappingItem;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Reader;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Writer;
import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.proxy.maven.maven2.M2GroupRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2LayoutedM1ShadowRepositoryConfiguration;
import org.sonatype.nexus.proxy.maven.maven2.M2RepositoryConfiguration;

import com.google.common.io.Flushables;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tampers with nexus configuration files, not on the instance! It is meaningful to use it BEFORE nexus is
 * booted (to set it up), or AFTER test is done (to verify config changes did happen), but during test the changes done
 * by this class will be lost! (since nexus keeps config copy in memory, and on next save will simply overwrite it with
 * own copy, or not be aware of the changes at all)
 *
 * @author cstamas
 */
public class NexusConfigUtil
    extends ITUtil
{
  public NexusConfigUtil(AbstractNexusIntegrationTest test) {
    super(test);
  }

  private static Logger log = LoggerFactory.getLogger(NexusConfigUtil.class);

  /**
   * Loads (and upgrades) nexus.xml on the fly. Warning: this method brings up a LOT of Nexus internals! Use it
   * sparingly!
   */
  public Configuration loadAndUpgradeNexusConfiguration()
      throws IOException
  {
    try {
      NexusConfiguration nexusConfiguration;
      nexusConfiguration = getTest().getITPlexusContainer().lookup(NexusConfiguration.class);
      nexusConfiguration.loadConfiguration(true);
      return nexusConfiguration.getConfigurationModel();
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Deprecated
  public Configuration getNexusConfig()
      throws IOException
  {
    return loadNexusConfig();
  }

  public Configuration loadNexusConfig()
      throws IOException
  {
    final File nexusConfigFile = getNexusConfigurationFile();
    final NexusConfigurationXpp3Reader reader = new NexusConfigurationXpp3Reader();

    try (FileInputStream in = new FileInputStream(nexusConfigFile)) {
      return reader.read(in, false);
    }
    catch (XmlPullParserException e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  public void saveNexusConfig(final Configuration config)
      throws IOException
  {
    // save it
    final NexusConfigurationXpp3Writer writer = new NexusConfigurationXpp3Writer();
    try (FileWriter fos = new FileWriter(getSecurityConfigurationFile())) {
      writer.write(fos, config);
      Flushables.flushQuietly(fos);
    }
  }

  @Deprecated
  public static File getNexusFile() {
    return getNexusConfigurationFile();
  }

  public static File getNexusConfigurationFile() {
    return new File(AbstractNexusIntegrationTest.WORK_CONF_DIR, "nexus.xml");
  }

  public static File getSecurityConfigurationFile() {
    return new File(AbstractNexusIntegrationTest.WORK_CONF_DIR, "security-configuration.xml");
  }

  public CPathMappingItem getRoute(String id)
      throws IOException
  {
    List<CPathMappingItem> routes = getNexusConfig().getRepositoryGrouping().getPathMappings();

    for (Iterator<CPathMappingItem> iter = routes.iterator(); iter.hasNext(); ) {
      CPathMappingItem groupsSettingPathMappingItem = iter.next();

      if (groupsSettingPathMappingItem.getId().equals(id)) {
        return groupsSettingPathMappingItem;
      }

    }
    return null;
  }

  public CRepository getRepo(String repoId)
      throws IOException
  {
    List<CRepository> repos = getNexusConfig().getRepositories();

    for (Iterator<CRepository> iter = repos.iterator(); iter.hasNext(); ) {
      CRepository cRepo = iter.next();

      // check id
      if (cRepo.getId().equals(repoId)) {
        return cRepo;
      }
    }
    return null;
  }

  public M2LayoutedM1ShadowRepositoryConfiguration getRepoShadow(String repoId)
      throws IOException
  {
    List<CRepository> repos = getNexusConfig().getRepositories();

    for (Iterator<CRepository> iter = repos.iterator(); iter.hasNext(); ) {
      CRepository cRepo = iter.next();

      // check id
      if (cRepo.getId().equals(repoId)) {
        M2LayoutedM1ShadowRepositoryConfiguration exRepoConf =
            new M2LayoutedM1ShadowRepositoryConfiguration((Xpp3Dom) cRepo.getExternalConfiguration());

        return exRepoConf;
      }
    }

    return null;
  }

  public M2GroupRepositoryConfiguration getGroup(String groupId)
      throws IOException
  {
    List<CRepository> repos = getNexusConfig().getRepositories();

    for (Iterator<CRepository> iter = repos.iterator(); iter.hasNext(); ) {
      CRepository cRepo = iter.next();

      // check id
      if (cRepo.getId().equals(groupId)) {
        M2GroupRepositoryConfiguration exRepoConf =
            new M2GroupRepositoryConfiguration((Xpp3Dom) cRepo.getExternalConfiguration());

        return exRepoConf;
      }
    }

    return null;
  }

  public M2RepositoryConfiguration getM2Repo(String id)
      throws IOException
  {
    List<CRepository> repos = getNexusConfig().getRepositories();

    for (Iterator<CRepository> iter = repos.iterator(); iter.hasNext(); ) {
      CRepository cRepo = iter.next();

      // check id
      if (cRepo.getId().equals(id)) {
        M2RepositoryConfiguration exRepoConf =
            new M2RepositoryConfiguration((Xpp3Dom) cRepo.getExternalConfiguration());

        return exRepoConf;
      }
    }

    return null;
  }
}
