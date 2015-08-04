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
package org.sonatype.nexus.proxy.maven.metadata.operations;

import java.util.Collections;

import org.sonatype.nexus.proxy.maven.metadata.operations.ModelVersionUtility.Version;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class AddPluginOperationTest
{
  @Test
  public void testAddingPluginsDoesNotDuplicatesEntries()
      throws Exception
  {
    final Metadata md = new Metadata();
    {
      final Plugin plugin = new Plugin();
      plugin.setArtifactId("bar-maven-plugin");
      plugin.setPrefix("bar");
      plugin.setName("Bar plugin");
      md.addPlugin(plugin);
    }

    // new plugin addition
    {
      final Plugin plugin1 = new Plugin();
      plugin1.setPrefix("foo");
      plugin1.setArtifactId("foo-maven-plugin");
      plugin1.setName("Foo plugin");
      MetadataBuilder.changeMetadata(md, Collections.<MetadataOperation>singletonList(new AddPluginOperation(
          new PluginOperand(Version.V110, plugin1))));
    }
    assertThat("New plugin should be added!", md.getPlugins(), hasSize(2));
    assertThat(md.getPlugins().get(0).getPrefix(), equalTo("bar"));
    assertThat(md.getPlugins().get(0).getName(), equalTo("Bar plugin"));
    assertThat(md.getPlugins().get(1).getPrefix(), equalTo("foo"));
    assertThat(md.getPlugins().get(1).getName(), equalTo("Foo plugin"));

    // existing plugin addition
    {
      final Plugin plugin1 = new Plugin();
      plugin1.setPrefix("foo");
      plugin1.setArtifactId("foo-maven-plugin");
      plugin1.setName("The new Foo plugin");
      MetadataBuilder.changeMetadata(md, Collections.<MetadataOperation>singletonList(new AddPluginOperation(
          new PluginOperand(Version.V110, plugin1))));
    }
    assertThat("No new plugin should be added!", md.getPlugins(), hasSize(2));
    assertThat(md.getPlugins().get(0).getPrefix(), equalTo("bar"));
    assertThat(md.getPlugins().get(0).getName(), equalTo("Bar plugin"));
    assertThat(md.getPlugins().get(1).getPrefix(), equalTo("foo"));
    assertThat(md.getPlugins().get(1).getName(), equalTo("The new Foo plugin"));

    // existing plugin addition
    {
      final Plugin plugin1 = new Plugin();
      plugin1.setPrefix("bar");
      plugin1.setArtifactId("bar-maven-plugin");
      plugin1.setName("The new Bar plugin");
      MetadataBuilder.changeMetadata(md, Collections.<MetadataOperation>singletonList(new AddPluginOperation(
          new PluginOperand(Version.V110, plugin1))));
    }
    assertThat("No new plugin should be added!", md.getPlugins(), hasSize(2));
    assertThat(md.getPlugins().get(0).getPrefix(), equalTo("bar"));
    assertThat(md.getPlugins().get(0).getName(), equalTo("The new Bar plugin"));
    assertThat(md.getPlugins().get(1).getPrefix(), equalTo("foo"));
    assertThat(md.getPlugins().get(1).getName(), equalTo("The new Foo plugin"));

    // new plugin addition wrt plugin order, plugins are ordered by ArtifactID
    {
      final Plugin plugin1 = new Plugin();
      plugin1.setPrefix("alpha");
      plugin1.setArtifactId("alpha-maven-plugin");
      plugin1.setName("Alpha plugin");
      MetadataBuilder.changeMetadata(md, Collections.<MetadataOperation>singletonList(new AddPluginOperation(
          new PluginOperand(Version.V110, plugin1))));
    }
    assertThat("New plugin should be added!", md.getPlugins(), hasSize(3));
    assertThat(md.getPlugins().get(0).getPrefix(), equalTo("alpha"));
    assertThat(md.getPlugins().get(0).getName(), equalTo("Alpha plugin"));
    assertThat(md.getPlugins().get(1).getPrefix(), equalTo("bar"));
    assertThat(md.getPlugins().get(1).getName(), equalTo("The new Bar plugin"));
    assertThat(md.getPlugins().get(2).getPrefix(), equalTo("foo"));
    assertThat(md.getPlugins().get(2).getName(), equalTo("The new Foo plugin"));
  }
}
