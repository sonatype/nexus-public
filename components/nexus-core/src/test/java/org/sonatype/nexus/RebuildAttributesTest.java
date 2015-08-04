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
package org.sonatype.nexus;

import java.io.IOException;

import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.templates.TemplateManager;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2HostedRepositoryTemplate;

import org.junit.Assert;
import org.junit.Test;

public class RebuildAttributesTest
    extends NexusAppTestSupport
{
  private TemplateManager templateManager;
  
  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();
    startNx();
    this.templateManager = lookup(TemplateManager.class);
  }

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Test
  public void testRepositoryRebuildAttributes()
      throws IOException
  {
    try {
      RepositoryTemplate hostedRepoTemplate =
          (RepositoryTemplate) templateManager.getTemplates()
              .getTemplates(Maven2HostedRepositoryTemplate.class).getTemplates(RepositoryPolicy.RELEASE)
              .pick();

      hostedRepoTemplate.getConfigurableRepository().setId("test");
      hostedRepoTemplate.getConfigurableRepository().setName("Test");
      hostedRepoTemplate.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);

      hostedRepoTemplate.create().recreateAttributes(new ResourceStoreRequest(RepositoryItemUid.PATH_ROOT), null);
    }
    catch (ConfigurationException e) {
      Assert.fail("ConfigurationException creating repository");
    }
  }
}
