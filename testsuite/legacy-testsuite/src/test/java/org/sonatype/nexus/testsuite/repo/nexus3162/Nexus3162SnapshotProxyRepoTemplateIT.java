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
package org.sonatype.nexus.testsuite.repo.nexus3162;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.test.utils.RepositoryTemplateMessageUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author juven
 */
public class Nexus3162SnapshotProxyRepoTemplateIT
    extends AbstractNexusIntegrationTest
{
  protected RepositoryTemplateMessageUtil messageUtil;

  public Nexus3162SnapshotProxyRepoTemplateIT()
      throws Exception
  {
    this.messageUtil = new RepositoryTemplateMessageUtil();
  }

  @Test
  public void getProxySnapshotTemplate()
      throws Exception
  {
    RepositoryBaseResource result = messageUtil.getTemplate(RepositoryTemplateMessageUtil.TEMPLATE_PROXY_SNAPSHOT);

    Assert.assertTrue(result instanceof RepositoryProxyResource);
    Assert.assertEquals(1440, ((RepositoryProxyResource) result).getArtifactMaxAge());
    Assert.assertEquals(1440, ((RepositoryProxyResource) result).getMetadataMaxAge());
    Assert.assertEquals(Integer.valueOf(1440), ((RepositoryProxyResource) result).getItemMaxAge());
  }

  @Test
  public void getProxyReleaseTemplate()
      throws Exception
  {
    RepositoryBaseResource result = messageUtil.getTemplate(RepositoryTemplateMessageUtil.TEMPLATE_PROXY_RELEASE);

    Assert.assertTrue(result instanceof RepositoryProxyResource);
    Assert.assertEquals(-1, ((RepositoryProxyResource) result).getArtifactMaxAge());
    Assert.assertEquals(1440, ((RepositoryProxyResource) result).getMetadataMaxAge());
    Assert.assertEquals(Integer.valueOf(1440), ((RepositoryProxyResource) result).getItemMaxAge());
  }
}
