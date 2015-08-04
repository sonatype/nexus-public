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

import java.util.Arrays;
import java.util.List;

import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.templates.repository.maven.Maven1GroupRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven1HostedRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven1ProxyRepositoryTemplate;

import org.junit.Test;

public class GroupUpdateTest
    extends NexusAppTestSupport
{

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    startNx();
  }

  @Test
  public void testUpdateGroup()
      throws Exception
  {
    createM1HostedRepo("m1h");
    createM1ProxyRepo("m1p");
    MavenGroupRepository group = createM1Group("m1g", Arrays.asList("central-m1", "m1h", "m1p"));

    assertTrue(group.getMemberRepositoryIds().contains("m1h"));
    assertTrue(group.getMemberRepositoryIds().contains("m1p"));
    assertTrue(group.getMemberRepositoryIds().contains("central-m1"));
    assertTrue(group.getMemberRepositoryIds().size() == 3);

    // now delete the proxy
    nexusConfiguration().deleteRepository("m1p");

    assertTrue(group.getMemberRepositoryIds().contains("m1h"));
    assertTrue(group.getMemberRepositoryIds().contains("central-m1"));
    assertTrue(group.getMemberRepositoryIds().size() == 2);
  }

  private MavenRepository createM1HostedRepo(String id)
      throws Exception
  {
    Maven1HostedRepositoryTemplate template =
        (Maven1HostedRepositoryTemplate) getRepositoryTemplates()
            .getTemplates(Maven1HostedRepositoryTemplate.class, RepositoryPolicy.RELEASE).pick();

    template.getConfigurableRepository().setIndexable(false);
    template.getConfigurableRepository().setId(id);
    template.getConfigurableRepository().setName(id);

    return template.create();
  }

  private MavenRepository createM1ProxyRepo(String id)
      throws Exception
  {
    Maven1ProxyRepositoryTemplate template =
        (Maven1ProxyRepositoryTemplate) getRepositoryTemplates()
            .getTemplates(Maven1ProxyRepositoryTemplate.class, RepositoryPolicy.RELEASE).pick();

    template.getConfigurableRepository().setIndexable(false);
    template.getConfigurableRepository().setId(id);
    template.getConfigurableRepository().setName(id);

    return template.create();
  }

  private MavenGroupRepository createM1Group(String id, List<String> members)
      throws Exception
  {
    Maven1GroupRepositoryTemplate template =
        (Maven1GroupRepositoryTemplate) getRepositoryTemplates()
            .getTemplates(Maven1GroupRepositoryTemplate.class).pick();

    template.getConfigurableRepository().setId(id);
    template.getConfigurableRepository().setName(id);
    template.getConfigurableRepository().setIndexable(false);

    for (String member : members) {
      template.getExternalConfiguration(true).addMemberRepositoryId(member);
    }

    return (MavenGroupRepository) template.create();
  }
}
