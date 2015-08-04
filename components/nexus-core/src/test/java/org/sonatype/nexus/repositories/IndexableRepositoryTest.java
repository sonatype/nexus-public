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

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.repository.LocalStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven1HostedRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2HostedRepositoryTemplate;

import org.junit.Test;

public class IndexableRepositoryTest
    extends NexusAppTestSupport
{

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();
    startNx();
  }

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Test
  public void testCreateIndexableM1()
      throws Exception
  {
    String repoId = "indexableM1";

    RepositoryTemplate repoTemplate =
        (RepositoryTemplate) getRepositoryTemplates().getTemplates(Maven1HostedRepositoryTemplate.class,
            RepositoryPolicy.RELEASE).pick();

    repoTemplate.getConfigurableRepository().setId(repoId);
    repoTemplate.getConfigurableRepository().setName(repoId + "-name");
    // Assert.assertEquals( "group-name", group.getName() );
    repoTemplate.getConfigurableRepository().setExposed(true);
    repoTemplate.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);

    repoTemplate.getConfigurableRepository().setIndexable(true);

    // will not fail, just create a warning and silently override it
    Repository repository = repoTemplate.create();

    assertFalse("The repository should be non-indexable!", repository.isIndexable());
  }

  @Test
  public void testCreateIndexableM2()
      throws Exception
  {
    String repoId = "indexableM2";

    RepositoryTemplate repoTemplate =
        (RepositoryTemplate) getRepositoryTemplates().getTemplates(Maven2HostedRepositoryTemplate.class)
            .getTemplates(RepositoryPolicy.RELEASE).pick();

    repoTemplate.getConfigurableRepository().setId(repoId);
    repoTemplate.getConfigurableRepository().setName(repoId + "-name");
    // Assert.assertEquals( "group-name", group.getName() );
    repoTemplate.getConfigurableRepository().setExposed(true);
    repoTemplate.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);
    repoTemplate.getConfigurableRepository().setIndexable(true);

    repoTemplate.create();
  }

  @Test
  public void testCreateNonIndexableM2()
      throws Exception
  {
    String repoId = "nonIndexableM2";

    RepositoryTemplate repoTemplate =
        (RepositoryTemplate) getRepositoryTemplates().getTemplates(Maven2HostedRepositoryTemplate.class)
            .getTemplates(RepositoryPolicy.RELEASE).pick();

    repoTemplate.getConfigurableRepository().setId(repoId);
    repoTemplate.getConfigurableRepository().setName(repoId + "-name");
    // Assert.assertEquals( "group-name", group.getName() );
    repoTemplate.getConfigurableRepository().setExposed(true);
    repoTemplate.getConfigurableRepository().setLocalStatus(LocalStatus.IN_SERVICE);
    repoTemplate.getConfigurableRepository().setIndexable(false);

    repoTemplate.create();
  }

}
