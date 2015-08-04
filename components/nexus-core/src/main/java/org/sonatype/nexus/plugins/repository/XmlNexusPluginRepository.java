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
package org.sonatype.nexus.plugins.repository;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.PluginMetadata;

import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * Simple xml-based plugin repository meant to work with IDE workspace dependency resolution, but will likely be useful
 * in other scenarios too. The contents of the repository is configured using xml file passed with
 * -Dnexus.xml-plugin-repository=<path> system property. Below is an example repository configuration file (yes, I know
 * it will not render properly in javadoc, but I want this to be readable in the sources).
 *
 * <pre>
 * <plugin-repository>
 *   <artifacts>
 *     <artifact>
 *       <location>/workspaces/nexus-dev/nexus/nexus/plugins/indexer/nexus-indexer-lucene-plugin/target/classes</location>
 *       <groupId>org.sonatype.nexus.plugins</groupId>
 *       <artifactId>nexus-indexer-lucene-plugin</artifactId>
 *       <version>2.3-SNAPSHOT</version>
 *       <type>nexus-plugin</type>
 *     </artifact>
 *     <artifact>
 *       <location>/Users/igor/.m2/repository/org/apache/maven/indexer/indexer-core/5.0.0/indexer-core-5.0.0.jar</location>
 *       <groupId>org.apache.maven.indexer</groupId>
 *       <artifactId>indexer-core</artifactId>
 *       <version>5.0.0</version>
 *       <type>jar</type>
 *     </artifact>
 *   </artifacts>
 * </plugin-repository>
 * </pre>
 *
 * @since 2.3
 */
@Named(XmlNexusPluginRepository.ID)
@Singleton
@Deprecated
class XmlNexusPluginRepository
    extends AbstractNexusPluginRepository
{
  static final String ID = "xml";

  private final Map<GAVCoordinate, PluginMetadata> plugins;

  private final Map<GAVCoordinate, PluginRepositoryArtifact> artifacts;

  @Inject
  public XmlNexusPluginRepository(@Named("${xml-plugin-repository}") @Nullable File pluginRepositoryXml)
      throws IOException, XmlPullParserException
  {
    final Map<GAVCoordinate, PluginMetadata> plugins = new LinkedHashMap<GAVCoordinate, PluginMetadata>();
    final Map<GAVCoordinate, PluginRepositoryArtifact> artifacts =
        new LinkedHashMap<GAVCoordinate, PluginRepositoryArtifact>();

    if (pluginRepositoryXml != null) {
      Xpp3Dom dom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(pluginRepositoryXml));

      Xpp3Dom artifactsDom = dom.getChild("artifacts");

      for (Xpp3Dom artifactDom : artifactsDom.getChildren("artifact")) {
        String type = getChildText(artifactDom, "type");

        boolean plugin;
        if ("nexus-plugin".equals(type)) {
          plugin = true;

          // apparently plugin manager expects plugin artifact gavs to have type==null
          type = null;
        }
        else {
          plugin = false;
        }

        String location = artifactDom.getChild("location").getValue();
        String groupId = artifactDom.getChild("groupId").getValue();
        String artifactId = artifactDom.getChild("artifactId").getValue();
        String version = artifactDom.getChild("version").getValue();
        String classifier = getChildText(artifactDom, "classifier");

        GAVCoordinate gav = new GAVCoordinate(groupId, artifactId, version, classifier, type);

        File file = new File(location).getCanonicalFile();

        artifacts.put(gav, new PluginRepositoryArtifact(gav, file, this));

        if (plugin) {
          try {
            PluginMetadata metadata =
                getPluginMetadata(new File(file, "META-INF/nexus/plugin.xml").toURI().toURL());

            plugins.put(new GAVCoordinate(groupId, artifactId, version, classifier, type), metadata);
          }
          catch (IOException e) {
            // it was not meant to be
          }
        }
      }
    }

    this.plugins = Collections.unmodifiableMap(plugins);
    this.artifacts = Collections.unmodifiableMap(artifacts);
  }

  private String getChildText(Xpp3Dom dom, String childName) {
    Xpp3Dom child = dom.getChild(childName);
    return child != null ? child.getValue() : null;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public int getPriority() {
    return 0; // this is meant for IDE integration and should be consulted
    // before anything else
  }

  @Override
  public Map<GAVCoordinate, PluginMetadata> findAvailablePlugins() {
    return plugins;
  }

  @Override
  public PluginRepositoryArtifact resolveArtifact(GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    PluginRepositoryArtifact artifact = artifacts.get(gav);
    if (artifact == null) {
      throw new NoSuchPluginRepositoryArtifactException(this, gav);
    }
    return artifact;
  }

  @Override
  public PluginRepositoryArtifact resolveDependencyArtifact(PluginRepositoryArtifact plugin, GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    return resolveArtifact(gav);
  }

  @Override
  public PluginMetadata getPluginMetadata(GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    PluginMetadata metadata = plugins.get(gav);
    if (metadata == null) {
      throw new NoSuchPluginRepositoryArtifactException(this, gav);
    }
    return metadata;
  }

}
