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
package org.sonatype.nexus.repositories.nexus4807;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.templates.TemplateManager;
import org.sonatype.nexus.templates.repository.RepositoryTemplate;

import junit.framework.Assert;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Testing is repository released (from container) when it is removed from Nexus. See NEXUS-4807.
 *
 * @author cstamas
 */
public class Nexus4807Test
    extends NexusAppTestSupport
{
  @Test
  public void testDisposeInvoked()
      throws Exception
  {
    final RepositoryTypeRegistry repositoryTypeRegistry = lookup(RepositoryTypeRegistry.class);
    final RepositoryRegistry repositoryRegistry = lookup(RepositoryRegistry.class);
    final NexusConfiguration nexusConfiguration = lookup(NexusConfiguration.class);
    final TemplateManager templateManager = lookup(TemplateManager.class);

    // register this
    repositoryTypeRegistry.registerRepositoryTypeDescriptors(new RepositoryTypeDescriptor(Repository.class,
        Nexus4807RepositoryImpl.ID, "foo"));

    // load config
    nexusConfiguration.loadConfiguration();

    // assert we have peter not present
    try {
      repositoryRegistry.getRepository("peter");
      Assert.fail("Peter should not be present!");
    }
    catch (NoSuchRepositoryException e) {
      // good, it should be not present
    }

    // create this new repo type
    final RepositoryTemplate template =
        (RepositoryTemplate) templateManager.getTemplates().getTemplateById("nexus4807");
    template.getConfigurableRepository().setId("peter");
    template.getConfigurableRepository().setName("We all love Peter!");
    final Repository repository = template.create();

    // do some simple assertion
    assertThat(repository.getId(), equalTo("peter"));
    assertThat(repository.getName(), equalTo("We all love Peter!"));
    // assert peter is here simply, by having this below not throw any exception and returning non-null
    // note: by interface contract, this method never returns null: either returns value or throws exception
    assertThat(repositoryRegistry.getRepository("peter"), notNullValue());

    // now drop it
    nexusConfiguration.deleteRepository(repository.getId());

    // assert peter left the building
    try {
      repositoryRegistry.getRepository("peter");
      Assert.fail("Peter should not be present, he just left!");
    }
    catch (NoSuchRepositoryException e) {
      // good, he left of main entrance
    }

    // and assert that we really do love Peter
    Nexus4807Repository nexus4807Repository = repository.adaptToFacet(Nexus4807Repository.class);
    assertThat(nexus4807Repository.isDisposeInvoked(), is(true));
  }

}
