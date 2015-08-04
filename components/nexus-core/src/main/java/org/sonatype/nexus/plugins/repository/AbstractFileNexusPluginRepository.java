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
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.sonatype.nexus.proxy.maven.gav.Gav;
import org.sonatype.nexus.proxy.maven.packaging.ArtifactPackagingMapper;
import org.sonatype.plugin.metadata.GAVCoordinate;
import org.sonatype.plugins.model.PluginMetadata;

import org.codehaus.plexus.util.StringUtils;

/**
 * Abstract {@link NexusPluginRepository} backed by a file-system.
 */
@Deprecated
public abstract class AbstractFileNexusPluginRepository
    extends AbstractNexusPluginRepository
{
  // ----------------------------------------------------------------------
  // Constants
  // ----------------------------------------------------------------------

  private static final String PLUGIN_XML = "META-INF/nexus/plugin.xml";

  // ----------------------------------------------------------------------
  // Implementation fields
  // ----------------------------------------------------------------------

  @Inject
  private ArtifactPackagingMapper packagingMapper;

  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public final Map<GAVCoordinate, PluginMetadata> findAvailablePlugins() {
    final File[] plugins = getPluginFolders();
    if (null == plugins) {
      return Collections.emptyMap();
    }

    final Map<GAVCoordinate, PluginMetadata> installedPlugins =
        new HashMap<GAVCoordinate, PluginMetadata>(plugins.length);

    for (final File f : plugins) {
      if (!f.isDirectory()) {
        continue;
      }
      final File pluginJar = getPluginJar(f);
      if (!pluginJar.isFile()) {
        continue;
      }
      final PluginMetadata md = getPluginMetadata(pluginJar);
      if (null == md) {
        continue;
      }
      installedPlugins.put(new GAVCoordinate(md.getGroupId(), md.getArtifactId(), md.getVersion()), md);
    }

    return installedPlugins;
  }

  public final PluginRepositoryArtifact resolveArtifact(final GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    return new PluginRepositoryArtifact(gav, resolvePluginJar(gav), this);
  }

  public final PluginRepositoryArtifact resolveDependencyArtifact(final PluginRepositoryArtifact plugin,
                                                                  final GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    final File dependencyArtifact = resolveSnapshotOrReleaseDependencyArtifact(plugin, gav);

    if (dependencyArtifact == null || !dependencyArtifact.isFile()) {
      throw new NoSuchPluginRepositoryArtifactException(this, gav);
    }

    return new PluginRepositoryArtifact(gav, dependencyArtifact, this);
  }

  public final PluginMetadata getPluginMetadata(final GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    return getPluginMetadata(resolvePluginJar(gav));
  }

  // ----------------------------------------------------------------------
  // Customizable methods
  // ----------------------------------------------------------------------

  protected abstract File getNexusPluginsDirectory();

  protected File getPluginDependenciesFolder(final PluginRepositoryArtifact plugin) {
    return new File(getPluginFolder(plugin.getCoordinate()), "dependencies");
  }

  private static final String SNAPSHOT_TIMESTAMP_FILE_PATTERN = "([0-9]{8}.[0-9]{6})-([0-9]+)";

  protected File resolveSnapshotOrReleaseDependencyArtifact(final PluginRepositoryArtifact plugin,
                                                            final GAVCoordinate gav)
  {
    // TODO (cstamas): gav has baseVersion, we need to be a bit smarter about resolving it against timestamped too
    // try with baseVersion (-SNAPSHOT) will work if bundle was assembled from stuff in local repository
    // or is part of this same build
    // Also, this part will work with release ones
    final File dependenciesFolder = getPluginDependenciesFolder(plugin);
    File dependencyArtifact = new File(dependenciesFolder, gav.getFinalName(packagingMapper));
    if (dependencyArtifact.isFile()) {
      return dependencyArtifact;
    }

    // for timestamped snapshots, we need another try
    if (Gav.isSnapshot(gav.getVersion())) {
      // is a snapshot, but is a a timestamped one, so let's try to find it
      final StringBuilder buf = new StringBuilder();
      if (StringUtils.isNotEmpty(gav.getClassifier())) {
        buf.append('-').append(gav.getClassifier());
      }
      if (StringUtils.isNotEmpty(gav.getType())) {
        buf.append('.').append(packagingMapper.getExtensionForPackaging(gav.getType()));
      }
      else {
        buf.append(".jar");
      }

      final String versionBaseline =
          gav.getVersion().substring(0, gav.getVersion().length() - "SNAPSHOT".length());
      final Pattern pattern =
          Pattern.compile("^" + Pattern.quote(gav.getArtifactId()) + "-" + Pattern.quote(versionBaseline)
              + SNAPSHOT_TIMESTAMP_FILE_PATTERN + Pattern.quote(buf.toString()) + "$");

      File[] dependencies = dependenciesFolder.listFiles(new FilenameFilter()
      {
        @Override
        public boolean accept(File dir, String name) {
          return pattern.matcher(name).matches();
        }
      });

      if (dependencies != null && dependencies.length == 1 && dependencies[0].isFile()) {
        return dependencies[0];
      }
    }

    return null;
  }

  protected File[] getPluginFolders() {
    return getNexusPluginsDirectory().listFiles();
  }

  protected File getPluginFolder(final GAVCoordinate gav) {
    return new File(getNexusPluginsDirectory(), gav.getArtifactId() + '-' + gav.getVersion());
  }

  // ----------------------------------------------------------------------
  // Implementation methods
  // ----------------------------------------------------------------------

  private static final File getPluginJar(final File pluginFolder) {
    return new File(pluginFolder, pluginFolder.getName() + ".jar");
  }

  private final File resolvePluginJar(final GAVCoordinate gav)
      throws NoSuchPluginRepositoryArtifactException
  {
    final File pluginFolder = getPluginFolder(gav);
    final File pluginJar = getPluginJar(pluginFolder);
    if (pluginJar.isFile()) {
      return pluginJar;
    }
    throw new NoSuchPluginRepositoryArtifactException(this, gav);
  }

  private final PluginMetadata getPluginMetadata(final File file) {
    try {
      return getPluginMetadata(new URL("jar:" + file.toURI() + "!/" + PLUGIN_XML));
    }
    catch (final IOException e) {
      return null;
    }
  }
}
