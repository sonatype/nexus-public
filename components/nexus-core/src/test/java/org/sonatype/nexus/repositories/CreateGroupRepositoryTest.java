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

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.configuration.model.CRepository;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.templates.repository.maven.Maven2GroupRepositoryTemplate;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Assert;
import org.junit.Test;

public class CreateGroupRepositoryTest
    extends NexusAppTestSupport
{
  private NexusConfiguration nexusConfiguration;

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();
    startNx();
    this.nexusConfiguration = this.lookup(NexusConfiguration.class);
  }

  @Test
  public void testCreateRepo()
      throws Exception
  {

    String groupId = "group-id";

    Maven2GroupRepositoryTemplate template =
        (Maven2GroupRepositoryTemplate) getRepositoryTemplates()
            .getTemplates(Maven2GroupRepositoryTemplate.class).pick();

    template.getConfigurableRepository().setId(groupId);
    template.getConfigurableRepository().setName("group-name");
    // Assert.assertEquals( "group-name", group.getName() );
    template.getConfigurableRepository().setExposed(true);
    template.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);
    template.getExternalConfiguration(true).addMemberRepositoryId("central");

    template.create();

    boolean found = false;
    // verify nexus config in memory
    for (CRepository cRepo : this.nexusConfiguration.getConfigurationModel().getRepositories()) {
      if (groupId.equals(cRepo.getId())) {
        System.out.println("ummmm");
        found = true;
        // make sure something is there, there are already UT, to validate the rest
        Assert.assertEquals("group-name", cRepo.getName());
        // check the members (they are in the external config)
        Xpp3Dom dom = (Xpp3Dom) cRepo.getExternalConfiguration();
        Xpp3Dom memberDom = dom.getChild("memberRepositories");
        Assert.assertEquals(1, memberDom.getChildCount());
        Assert.assertEquals("central", memberDom.getChild(0).getValue());
      }
    }
    Assert.assertTrue("Group Repo is not in memory.", found);

    // reload the config and see if the repo is still there
    this.nexusConfiguration.loadConfiguration(true);

    found = false;
    // verify nexus config in memory
    for (CRepository cRepo : this.nexusConfiguration.getConfigurationModel().getRepositories()) {
      if (groupId.equals(cRepo.getId())) {
        found = true;
        // make sure something is there, there are already UT, to validate the rest
        Assert.assertEquals("group-name", template.getConfigurableRepository().getName());
      }
    }
    Assert.assertTrue("Group Repo is not in file.", found);
  }

  @Test
  public void testCreateRepoWithInvalidMember()
      throws Exception
  {
    String groupId = "group-id";

    Maven2GroupRepositoryTemplate template =
        (Maven2GroupRepositoryTemplate) getRepositoryTemplates()
            .getTemplates(Maven2GroupRepositoryTemplate.class).pick();

    template.getConfigurableRepository().setId(groupId);
    template.getConfigurableRepository().setName("group-name");
    // Assert.assertEquals( "group-name", group.getName() );
    template.getConfigurableRepository().setExposed(true);
    template.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);
    // validation does NOT happen on the fly!
    template.getExternalConfiguration(true).addMemberRepositoryId("INVALID-REPO-ID");

    try {
      template.create();
      Assert.fail("Expected NoSuchRepositoryException");
    }
    catch (ConfigurationException e) {
      // expected
    }

    // verify nexus config in memory
    for (CRepository cRepo : this.nexusConfiguration.getConfigurationModel().getRepositories()) {
      if (groupId.equals(cRepo.getId())) {
        Assert.fail("found Group Repo in memory.");
      }
    }
    // reload the config and see if the repo is still there
    this.nexusConfiguration.loadConfiguration(true);

    // verify nexus config in memory
    for (CRepository cRepo : this.nexusConfiguration.getConfigurationModel().getRepositories()) {
      if (groupId.equals(cRepo.getId())) {
        Assert.fail("found Group Repo in file.");

      }
    }
  }

  @Test
  public void testCreateWithNoId()
      throws Exception
  {
    String groupId = null;

    Maven2GroupRepositoryTemplate template =
        (Maven2GroupRepositoryTemplate) getRepositoryTemplates()
            .getTemplates(Maven2GroupRepositoryTemplate.class).pick();

    template.getConfigurableRepository().setId(groupId);
    template.getConfigurableRepository().setName("group-name");
    // Assert.assertEquals( "group-name", group.getName() );
    template.getConfigurableRepository().setExposed(true);
    template.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);
    template.getExternalConfiguration(true).addMemberRepositoryId("central");

    try {
      template.create();
      Assert.fail("expected ConfigurationException");
    }
    catch (ConfigurationException e) {
      // expected
    }
  }

  @Test
  public void testCreateWithEmptyId()
      throws Exception
  {
    String groupId = "";

    Maven2GroupRepositoryTemplate template =
        (Maven2GroupRepositoryTemplate) getRepositoryTemplates()
            .getTemplates(Maven2GroupRepositoryTemplate.class).pick();

    template.getConfigurableRepository().setId(groupId);
    template.getConfigurableRepository().setName("group-name");
    // Assert.assertEquals( "group-name", group.getName() );
    template.getConfigurableRepository().setExposed(true);
    template.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);
    template.getExternalConfiguration(true).addMemberRepositoryId("central");

    try {
      template.create();
      Assert.fail("expected ConfigurationException");
    }
    catch (ConfigurationException e) {
      // expected
    }
  }

}
