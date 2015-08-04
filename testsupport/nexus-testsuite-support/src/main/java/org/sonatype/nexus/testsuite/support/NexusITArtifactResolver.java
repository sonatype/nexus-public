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
package org.sonatype.nexus.testsuite.support;

import java.io.File;
import java.util.List;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.sisu.maven.bridge.MavenArtifactResolver;
import org.sonatype.sisu.maven.bridge.MavenModelResolver;

import com.google.common.base.Throwables;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.sisu.maven.bridge.support.ArtifactRequestBuilder.request;
import static org.sonatype.sisu.maven.bridge.support.ModelBuildingRequestBuilder.model;

/**
 * Nexus Integration Tests artifact resolver utilities.
 *
 * @since 2.2
 */
public class NexusITArtifactResolver
{

  /**
   * The pom file of project containing the test.
   * Cannot be null.
   */
  private final File testProjectPomFile;

  /**
   * Artifact resolver used to resolve artifacts by Maven coordinates.
   * Cannot be null.
   */
  private final MavenArtifactResolver artifactResolver;

  /**
   * Model resolver used to resolve effective Maven models.
   * Cannot be null.
   */
  private final MavenModelResolver modelResolver;

  public NexusITArtifactResolver(final File testProjectPomFile,
                                 final MavenArtifactResolver artifactResolver,
                                 final MavenModelResolver modelResolver)
  {
    this.testProjectPomFile = checkNotNull(testProjectPomFile);
    this.artifactResolver = checkNotNull(artifactResolver);
    this.modelResolver = checkNotNull(modelResolver);
  }

  /**
   * Resolves an artifact given its Maven coordinates.
   *
   * @param coordinates Maven artifact coordinates
   * @return resolved artifact file
   */
  public File resolveArtifact(final String coordinates)
      throws RuntimeException
  {
    try {
      Artifact artifact = artifactResolver.resolveArtifact(
          request().artifact(coordinates)
      );
      if (artifact == null || artifact.getFile() == null || !artifact.getFile().exists()) {
        throw new RuntimeException(String.format("Artifact %s could not be resolved", coordinates));
      }
      return artifact.getFile();
    }
    catch (ArtifactResolutionException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Resolves a Nexus plugin, given its coordinates, by looking it up in dependency management section of POM in
   * which
   * the test resides.
   *
   * @param groupId    Maven group id of Nexus plugin to be resolved
   * @param artifactId Maven artifact id of Nexus plugin to be resolved
   * @return resolved artifact file
   */
  public File resolvePluginFromDependencyManagement(final String groupId, final String artifactId)
      throws RuntimeException
  {
    return resolveFromDependencyManagement(groupId, artifactId, "nexus-plugin", null, "zip", "bundle");
  }

  /**
   * Resolves a Maven artifact, given its coordinates, by looking it up in dependency management section of POM in
   * which the test resides.
   *
   * @param groupId            Maven group id of artifact to be resolved
   * @param artifactId         Maven artifact id of artifact to be resolved
   * @param type               Maven type of artifact to be resolved. If not specified (null), type is not considered
   *                           while finding the dependency in dependency management
   * @param classifier         Maven classifier of artifact to be resolved. If not specified (null), classifier is
   *                           not
   *                           considered while finding the dependency in dependency management
   * @param overrideType       an optional type to be used to override the type specified in dependency management
   *                           (e.g nexus-plugin -> zip)
   * @param overrideClassifier an optional classifier to override the classifier specified in dependency management
   *                           (e.g (not specified) -> bundle)
   * @return resolved artifact file
   */
  public File resolveFromDependencyManagement(final String groupId,
                                              final String artifactId,
                                              final String type,
                                              final String classifier,
                                              final String overrideType,
                                              final String overrideClassifier)
  {
    try {
      final Model model = modelResolver.resolveModel(model().pom(testProjectPomFile));

      if (model.getDependencyManagement() != null) {
        final List<Dependency> dependencies = model.getDependencyManagement().getDependencies();

        for (Dependency dependency : dependencies) {
          if (!dependency.getGroupId().equalsIgnoreCase(groupId)) {
            continue;
          }
          if (!dependency.getArtifactId().equalsIgnoreCase(artifactId)) {
            continue;
          }
          if (type != null && !dependency.getType().equals(type)) {
            continue;
          }
          if (classifier != null && !dependency.getClassifier().equals(classifier)) {
            continue;
          }

          StringBuilder coordinates = new StringBuilder();
          coordinates.append(dependency.getGroupId());
          coordinates.append(":").append(dependency.getArtifactId());

          String rExtension = dependency.getType();
          if (overrideType != null) {
            rExtension = overrideType;
          }
          if (rExtension != null) {
            coordinates.append(":").append(rExtension);
          }

          String rClassifier = dependency.getClassifier();
          if (overrideClassifier != null) {
            rClassifier = overrideClassifier;
          }
          if (rClassifier != null) {
            coordinates.append(":").append(rClassifier);
          }
          coordinates.append(":").append(dependency.getVersion());
          return resolveArtifact(coordinates.toString());
        }
      }
      throw new RuntimeException(String.format("Dependency %s:%s was not found", groupId, artifactId));
    }
    catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
