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
package org.sonatype.plugin.nexus.testenvironment;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * @author velo
 */
@Mojo(name = "setup-nexus-plugin-environment", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
    requiresDependencyResolution = ResolutionScope.TEST)
public class PluginEnvironmentMojo
    extends AbstractEnvironmentMojo
{

  @Parameter(property = "nexus.version", required = true)
  private String nexusVersion;

  @Override
  protected Artifact resolve(Artifact artifact)
      throws MojoExecutionException
  {
    if (!artifact.isResolved()) {
      if (equivalent(artifact, project.getArtifact())) {
        Artifact da;
        File bundle;
        if ("nexus-plugin".equals(project.getArtifact().getType())) {
          da =
              artifactFactory.createArtifactWithClassifier(project.getArtifact().getGroupId(),
                  project.getArtifact().getArtifactId(),
                  project.getArtifact().getVersion(), "zip",
                  "bundle");
          bundle =
              new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + "-bundle.zip");
        }
        else {
          da =
              artifactFactory.createArtifactWithClassifier(project.getArtifact().getGroupId(),
                  project.getArtifact().getArtifactId(),
                  project.getArtifact().getVersion(),
                  project.getArtifact().getType(),
                  project.getArtifact().getClassifier());
          String classifier =
              project.getArtifact().getClassifier() == null ? "" : "-"
                  + project.getArtifact().getClassifier();
          bundle =
              new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + classifier
                  + "." + project.getArtifact().getType());
        }

        da.setResolved(true);
        bundle = bundle.getAbsoluteFile();
        if (!bundle.exists()) {
          throw new MojoExecutionException("Project bundle doesn't exists " + bundle);
        }
        da.setFile(bundle);
        return da;
      }
      else {
        return super.resolve(artifact);
      }
    }

    return artifact;
  }

  @Override
  protected Artifact getMavenArtifact(MavenArtifact ma)
      throws MojoExecutionException
  {
    try {
      return super.getMavenArtifact(ma);
    }
    catch (AbstractMojoExecutionException e) {
      if (getLog().isDebugEnabled()) {
        getLog().warn("Dependency not found: '" + ma + "', trying with version " + nexusVersion, e);
      }
      else {
        getLog().warn("Dependency not found: '" + ma + "', trying with version " + nexusVersion);
      }

      Artifact artifact =
          artifactFactory.createArtifactWithClassifier(ma.getGroupId(), ma.getArtifactId(), nexusVersion,
              ma.getType(), ma.getClassifier());

      return resolve(artifact);
    }
  }

  private boolean equivalent(Artifact ma, Artifact artifact) {
    if (ma == artifact) {
      return true;
    }
    if (ma == null) {
      return false;
    }

    if (ma.getArtifactId() == null) {
      if (artifact.getArtifactId() != null) {
        return false;
      }
    }
    else if (!ma.getArtifactId().equals(artifact.getArtifactId())) {
      return false;
    }
    if (ma.getGroupId() == null) {
      if (artifact.getGroupId() != null) {
        return false;
      }
    }
    else if (!ma.getGroupId().equals(artifact.getGroupId())) {
      return false;
    }

    if (!("nexus-plugin".equals(ma.getType()) || "nexus-plugin".equals(artifact.getType())
        || ma.getType() == null || artifact.getType() == null)) {
      if (ma.getType() == null) {
        if (!"jar".equals(artifact.getType())) {
          if (artifact.getType() != null) {
            return false;
          }
        }
      }
      else if (!ma.getType().equals(artifact.getType())) {
        return false;
      }

      if (ma.getClassifier() == null) {
        if (artifact.getClassifier() != null) {
          return false;
        }
      }
      else if (!ma.getClassifier().equals(artifact.getClassifier())) {
        return false;
      }
    }
    else {
      if ("nexus-plugin".equals(ma.getType())) {
        if (!"bundle".equals(artifact.getClassifier()) && !"zip".equals(artifact.getType())) {
          return false;
        }
      }
      else {
        if (!"bundle".equals(ma.getClassifier()) && !"zip".equals(ma.getType())) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  protected Collection<Artifact> getNexusPlugins()
      throws MojoExecutionException
  {
    Collection<Artifact> plugins = new LinkedHashSet<Artifact>();
    plugins.add(project.getArtifact());
    plugins.addAll(super.getNexusPlugins());
    return plugins;
  }

  @Override
  protected Collection<MavenArtifact> getNexusPluginsArtifacts()
      throws MojoExecutionException
  {
    Set<MavenArtifact> plugins = new LinkedHashSet<MavenArtifact>();

    if (super.getNexusPluginsArtifacts() != null) {
      plugins.addAll(super.getNexusPluginsArtifacts());
    }

    Collection<Artifact> depPlugins = getNexusPlugins();
    for (Artifact artifact : depPlugins) {
      MavenArtifact ma =
          new MavenArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
              artifact.getType());
      ma.setVersion(artifact.getVersion());
      plugins.add(ma);
    }

    return plugins;
  }

}
