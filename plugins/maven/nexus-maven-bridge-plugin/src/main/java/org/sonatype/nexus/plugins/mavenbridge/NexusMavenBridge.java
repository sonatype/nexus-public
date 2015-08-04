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
package org.sonatype.nexus.plugins.mavenbridge;

import java.util.List;

import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.nexus.proxy.maven.MavenRepository;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelSource;

public interface NexusMavenBridge
{

  Model buildModel(ModelSource pom,
                   List<MavenRepository> repositories,
                   RepositoryListener... listeners)
      throws ModelBuildingException;

  DependencyNode collectDependencies(Dependency dependency,
                                     List<MavenRepository> repositories,
                                     RepositoryListener... listeners)
      throws DependencyCollectionException, ArtifactResolutionException;

  DependencyNode resolveDependencies(Dependency node,
                                     List<MavenRepository> repositories,
                                     RepositoryListener... listeners)
      throws DependencyCollectionException, ArtifactResolutionException;

  DependencyNode collectDependencies(Model model,
                                     List<MavenRepository> repositories,
                                     RepositoryListener... listeners)
      throws DependencyCollectionException, ArtifactResolutionException;

  DependencyNode resolveDependencies(Model model,
                                     List<MavenRepository> repositories,
                                     RepositoryListener... listeners)
      throws DependencyCollectionException, ArtifactResolutionException;

}
