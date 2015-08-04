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
package org.sonatype.nexus.plugins;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.guice.AbstractInterceptorModule;
import org.sonatype.nexus.plugins.repository.NexusPluginRepository;
import org.sonatype.nexus.plugins.repository.NoSuchPluginRepositoryArtifactException;
import org.sonatype.nexus.plugins.repository.PluginRepositoryArtifact;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.PluginDependency;
import org.sonatype.plugins.model.PluginMetadata;
import org.sonatype.sisu.goodies.eventbus.EventBus;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.google.inject.util.Providers;
import org.eclipse.sisu.bean.BeanManager;
import org.eclipse.sisu.inject.MutableBeanLocator;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Bundle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultNexusPluginManager}.
 *
 * @since 2.1
 */
public class DefaultNexusPluginManagerTest
    extends TestSupport
{

  @Mock
  private EventBus eventBus;

  @Mock
  private NexusPluginRepository pluginRepositoryManager;

  @Mock
  private NexusPluginRepository nexusPluginRepository;

  @Mock
  private MutableBeanLocator beanLocator;

  @Mock
  private BeanManager beanManager;

  @Test
  public void pluginDependenciesAreActivatedByGA()
      throws Exception
  {
    final DefaultNexusPluginManager underTest = new DefaultNexusPluginManager(
        eventBus,
        pluginRepositoryManager,
        new HashMap<String, String>(),
        Collections.<AbstractInterceptorModule>emptyList(),
        beanLocator,
        beanManager,
        Providers.<Bundle> of(null)
    )
    {
      @Override
      void createPluginInjector(final PluginRepositoryArtifact plugin, final PluginDescriptor descriptor)
          throws NoSuchPluginRepositoryArtifactException
      {
        // do nothing
      }
    };

    final GAVCoordinate p1 = new GAVCoordinate("g", "p1", "1.0");
    final PluginMetadata p1Meta = pluginMetadata(pluginDependency("g", "p2", "1.0"));

    final GAVCoordinate p2 = new GAVCoordinate("g", "p2", "1.1");
    final PluginMetadata p2Meta = pluginMetadata();

    final PluginRepositoryArtifact pra1 = pluginRepositoryArtifact(p1, p1Meta);
    final PluginRepositoryArtifact pra2 = pluginRepositoryArtifact(p2, p2Meta);

    final Map<GAVCoordinate, PluginMetadata> installedPlugins = new HashMap<GAVCoordinate, PluginMetadata>();
    installedPlugins.put(p1, p1Meta);
    installedPlugins.put(p2, p2Meta);

    when(pluginRepositoryManager.findAvailablePlugins()).thenReturn(installedPlugins);
    when(pluginRepositoryManager.resolveArtifact(p1)).thenReturn(pra1);
    when(pluginRepositoryManager.resolveArtifact(p2)).thenReturn(pra2);

    final Collection<PluginManagerResponse> responses = underTest.activateInstalledPlugins();
    assertThat(responses, is(notNullValue()));
    assertThat(responses.size(), is(2));
    for (PluginManagerResponse response : responses) {
      assertThat(response.isSuccessful(), is(true));
    }
  }

  private PluginRepositoryArtifact pluginRepositoryArtifact(final GAVCoordinate gav, final PluginMetadata meta) {
    final PluginRepositoryArtifact pluginRepositoryArtifact = new PluginRepositoryArtifact();
    pluginRepositoryArtifact.setCoordinate(gav);
    pluginRepositoryArtifact.setNexusPluginRepository(nexusPluginRepository);

    try {
      when(nexusPluginRepository.getPluginMetadata(gav)).thenReturn(meta);
    }
    catch (NoSuchPluginRepositoryArtifactException e) {
      // unexpected
      throw new RuntimeException(e);
    }

    return pluginRepositoryArtifact;
  }

  private PluginMetadata pluginMetadata(final PluginDependency... pluginDependencies) {
    final PluginMetadata pluginMetadata = new PluginMetadata();
    for (PluginDependency pluginDependency : pluginDependencies) {
      pluginMetadata.addPluginDependency(pluginDependency);
    }

    return pluginMetadata;
  }

  private PluginDependency pluginDependency(final String groupId, final String artifactId, final String version) {
    final PluginDependency pluginDependency = new PluginDependency();
    pluginDependency.setGroupId(groupId);
    pluginDependency.setArtifactId(artifactId);
    pluginDependency.setVersion(version);

    return pluginDependency;
  }

}
