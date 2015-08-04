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
package org.sonatype.nexus.testsuite.support.filters;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.nexus.testsuite.support.Filter;
import org.sonatype.sisu.maven.bridge.MavenModelResolver;

import com.google.common.collect.Maps;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.sisu.Parameters;

/**
 * Replaces version placeholder with version from dependencies management.
 * A value of format "g:a:e:c:${project.dm.version} will trigger a lookup for an dependency g:a:e:c in dependency
 * management section. If found, the ${project.dm.version} will be replaced with found version.
 *
 * @since 2.2
 */
@Named
@Singleton
public class TestProjectDMFilter
    extends TestProjectFilterSupport
    implements Filter
{

  /**
   * Constructor.
   *
   * @param modelResolver Model resolver used to resolve effective model of test project (pom). Cannot be null.
   */
  @Inject
  public TestProjectDMFilter(@Named("remote-model-resolver-using-settings") final MavenModelResolver modelResolver,
                             @Parameters final Map<String, String> properties)
  {
    super(modelResolver, properties);
  }

  /**
   * Returns mappings by looking up version out of dependency management.
   *
   * @param context filtering context. Cannot be null.
   * @param value   value to be filtered. Ignored by this filter.
   * @param model   resolved model of project under test. Cannot be null.
   * @return mappings extracted from project under test model
   */
  @Override
  Map<String, String> mappings(final Map<String, String> context, final String value, final Model model) {
    final Map<String, String> mappings = Maps.newHashMap();

    if (value.contains("${project.dm.") && model.getDependencyManagement() != null) {
      final DefaultArtifact artifact;
      try {
        artifact = new DefaultArtifact(value);

        final List<Dependency> dependencies = model.getDependencyManagement().getDependencies();

        for (Dependency dependency : dependencies) {
          if (!dependency.getGroupId().equalsIgnoreCase(artifact.getGroupId())) {
            continue;
          }
          if (!dependency.getArtifactId().equalsIgnoreCase(artifact.getArtifactId())) {
            continue;
          }
          String extensionToCompare = dependency.getType();
          if (extensionToCompare == null || extensionToCompare.isEmpty()) {
            extensionToCompare = "jar";
          }
          if (!extensionToCompare.equals(artifact.getExtension())) {
            continue;
          }
          String classifierToCompare = dependency.getClassifier();
          if (classifierToCompare == null || extensionToCompare.isEmpty()) {
            classifierToCompare = "";
          }
          if (!classifierToCompare.equals(artifact.getClassifier())) {
            continue;
          }

          mappings.put("project.dm.version", dependency.getVersion());
        }
      }
      catch (IllegalArgumentException e) {
        // TODO log as warning?
      }
    }

    return mappings;
  }

}
