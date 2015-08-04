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
package org.sonatype.nexus.templates.repository.maven;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.proxy.maven.MavenGroupRepository;
import org.sonatype.nexus.proxy.maven.maven1.Maven1ContentClass;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.templates.TemplateManager;
import org.sonatype.nexus.templates.TemplateSet;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MavenRepositoryTemplateTest
    extends NexusAppTestSupport
{
  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    startNx();
  }

  @Test
  public void testAvailableRepositoryTemplateCount() throws Exception {
    TemplateSet templates = lookup(TemplateManager.class).getTemplates().getTemplates(RepositoryTemplate.class);
    assertThat(templates.size(), equalTo(12));
  }

  @Test
  public void testSimpleSelection() throws Exception {
    TemplateSet groups = lookup(TemplateManager.class).getTemplates().getTemplates(RepositoryTemplate.class)
        .getTemplates(MavenGroupRepository.class);
    assertThat(groups.size(), equalTo(2));
    assertThat(groups.getTemplates(new Maven1ContentClass()).size(), equalTo(1));
    assertThat(groups.getTemplates(Maven1ContentClass.class).size(), equalTo(1));
    assertThat(groups.getTemplates(new Maven2ContentClass()).size(), equalTo(1));
    assertThat(groups.getTemplates(Maven2ContentClass.class).size(), equalTo(1));
  }
}
