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
package org.sonatype.nexus.repositories;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Reader;
import org.sonatype.nexus.configuration.model.io.xpp3.NexusConfigurationXpp3Writer;
import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class Nexus2216GroupValidationTest
    extends NexusAppTestSupport
{
  // we need some stuff to prepare
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  // ==

  @Test
  public void testInvertedOrdering()
      throws Exception
  {
    // mangle config
    mangleConfiguration();

    try {
      // lookup nexus, this will do all sort of things, amongst them validate the config
      startNx();

      RepositoryRegistry repositoryRegistry = lookup(RepositoryRegistry.class);

      MavenGroupRepository publicGroup =
          repositoryRegistry.getRepositoryWithFacet("public", MavenGroupRepository.class);

      List<String> memberIds = new ArrayList<String>();
      for (Repository repo : publicGroup.getMemberRepositories()) {
        memberIds.add(repo.getId());
      }

      MatcherAssert.assertThat("Repo object list returned a different set of repos", memberIds,
          Matchers.equalTo(publicGroup.getMemberRepositoryIds()));

      MatcherAssert.assertThat(
          "The config should be 4 reposes, but ids found are: " + publicGroup.getMemberRepositoryIds(),
          publicGroup.getMemberRepositories().size(), Matchers.equalTo(4));
    }
    catch (Exception e) {
      Assert.fail("Should succeed!");
    }
  }

  // ==

  protected void mangleConfiguration()
      throws IOException, XmlPullParserException
  {
    // copy the defaults
    copyDefaultConfigToPlace();

    File configFile = new File(getNexusConfiguration());

    // raw load the config file in place
    FileReader fileReader = new FileReader(configFile);

    NexusConfigurationXpp3Reader reader = new NexusConfigurationXpp3Reader();

    Configuration config = reader.read(fileReader);

    fileReader.close();

    CRepository publicGroup = null;

    // simple put the "public" group (that reference other reposes) as 1st!
    for (CRepository repository : config.getRepositories()) {
      if ("public".equals(repository.getId())) {
        publicGroup = repository;

        break;
      }
    }

    if (publicGroup == null) {
      Assert.fail("Public group not found in default configuration?");
    }

    config.getRepositories().remove(publicGroup);

    config.getRepositories().add(0, publicGroup);

    // raw save the modified config
    FileWriter fileWriter = new FileWriter(configFile);

    NexusConfigurationXpp3Writer writer = new NexusConfigurationXpp3Writer();

    writer.write(fileWriter, config);

    fileWriter.flush();

    fileWriter.close();
  }
}
