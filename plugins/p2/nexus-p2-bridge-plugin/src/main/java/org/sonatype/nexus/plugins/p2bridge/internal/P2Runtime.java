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
package org.sonatype.nexus.plugins.p2bridge.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.eclipse.bridge.EclipseBridge;
import org.sonatype.eclipse.bridge.EclipseInstance;
import org.sonatype.eclipse.bridge.EclipseLocation;
import org.sonatype.eclipse.bridge.EclipseLocationFactory;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.plugins.repository.NexusPluginRepository;
import org.sonatype.nexus.plugins.repository.NoSuchPluginRepositoryArtifactException;
import org.sonatype.nexus.plugins.repository.PluginRepositoryArtifact;
import org.sonatype.nexus.util.SystemPropertiesHelper;
import org.sonatype.nexus.util.file.DirSupport;
import org.sonatype.plugin.metadata.GAVCoordinate;

import com.google.common.base.Strings;
import org.codehaus.plexus.archiver.UnArchiver;
import org.eclipse.core.runtime.internal.adaptor.EclipseEnvironmentInfo;

@Named
@Singleton
class P2Runtime
{

  /**
   * Flag to completely enable/disable Eclipse logging (equinox).
   */
  private static final boolean LOGGING_ENABLED = SystemPropertiesHelper.getBoolean(
      P2Runtime.class.getName() + ".logging.enabled", false
  );

  private EclipseInstance eclipse;

  private final EclipseLocationFactory eclipseLocationFactory;

  private final EclipseBridge eclipseBridge;

  private final NexusPluginRepository pluginRepositoryManager;

  private final ApplicationConfiguration applicationConfiguration;

  private final UnArchiver unArchiver;

  @Inject
  public P2Runtime(final EclipseBridge eclipseBridge, final EclipseLocationFactory eclipseLocationFactory,
                   final NexusPluginRepository pluginRepositoryManager,
                   final ApplicationConfiguration applicationConfiguration,
                   final @Named("zip") UnArchiver unArchiver)
  {
    this.eclipseBridge = eclipseBridge;
    this.eclipseLocationFactory = eclipseLocationFactory;
    this.pluginRepositoryManager = pluginRepositoryManager;
    this.applicationConfiguration = applicationConfiguration;
    this.unArchiver = unArchiver;
  }

  EclipseInstance get() {
    initialize();
    return eclipse;
  }

  P2Runtime shutdown() {
    return this;
  }

  private synchronized void initialize() {
    if (eclipse != null) {
      return;
    }
    final File p2BridgePluginDir = getP2BridgePluginDir();
    final File p2BridgeRuntimeDir = new File(
        applicationConfiguration.getTemporaryDirectory(), "nexus-p2-bridge-plugin/runtime"
    );
    final File p2BridgeTempDir = new File(
        applicationConfiguration.getTemporaryDirectory(), "nexus-p2-bridge-plugin/tmp"
    );
    final File eclipseDir = new File(p2BridgeRuntimeDir, "eclipse");

    // if temporary plugin directory exists, remove it to avoid the fact that eclipse stores absolute paths to
    // installed bundles (see NXCM-4475)
    try {
      DirSupport.deleteIfExists(p2BridgeRuntimeDir.toPath());
    }
    catch (IOException e) {
      throw new RuntimeException(
          "Cannot delete nexus-p2-bridge temporary directory " + p2BridgeRuntimeDir.getAbsolutePath(), e
      );
    }
    try {
      DirSupport.mkdir(p2BridgeRuntimeDir.toPath());
    }
    catch (IOException e) {
      throw new RuntimeException(
          "Cannot create nexus-p2-bridge temporary directory " + p2BridgeRuntimeDir.getAbsolutePath(), e
      );
    }
    // create a fresh eclipse instance
    try {
      unArchiver.setSourceFile(new File(p2BridgePluginDir, "p2-runtime/eclipse.zip"));
      unArchiver.setDestDirectory(p2BridgeRuntimeDir);
      unArchiver.extract();
    }
    catch (final Exception e) {
      throw new RuntimeException("Cannot unpack Eclipse", e);
    }

    final EclipseLocation eclipseLocation = eclipseLocationFactory.createStaticEclipseLocation(eclipseDir);

    eclipse = eclipseBridge.createInstance(eclipseLocation);
    try {
      {
        // TODO is this really necessary?
        final File secureStorage = new File(
            applicationConfiguration.getConfigurationDirectory(), "eclipse.secure_storage"
        );
        if (secureStorage.exists() && !secureStorage.delete()) {
          throw new RuntimeException(
              "Could not delete Eclipse secure storage " + secureStorage.getAbsolutePath()
          );
        }
        EclipseEnvironmentInfo.setAppArgs(
            new String[]{"-eclipse.keyring", secureStorage.getAbsolutePath()}
        );
      }
      eclipse.start(initParams(p2BridgePluginDir, eclipseDir, p2BridgeTempDir));
      final File[] bundles = new File(p2BridgePluginDir, "p2-runtime/bundles").listFiles(new FilenameFilter()
      {
        @Override
        public boolean accept(final File dir, final String name) {
          return name.endsWith(".jar");
        }
      });
      for (final File bundle : bundles) {
        eclipse.startBundle(eclipse.installBundle(bundle.toURI().toASCIIString()));
      }
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, String> initParams(final File p2BridgePluginDir,
                                         final File eclipseDir,
                                         final File p2BridgeAgentsTempDir)
  {
    final Map<String, String> initParams = new HashMap<String, String>();
    initParams.put("org.eclipse.equinox.simpleconfigurator.exclusiveInstallation", "false");
    initParams.put("osgi.java.profile.bootdelegation", "none");
    final String bridgedPackages = scanBridgedPackages(new File(p2BridgePluginDir, "p2-runtime/bridge"));
    if (!Strings.isNullOrEmpty(bridgedPackages)) {
      initParams.put("org.osgi.framework.system.packages.extra", bridgedPackages);
    }
    if (LOGGING_ENABLED) {
      initParams.put("osgi.debug", new File(eclipseDir, ".options").getAbsolutePath());
    }
    initParams.put(EclipseInstance.TEMPDIR_PROPERTY, p2BridgeAgentsTempDir.getAbsolutePath());
    initParams.put("osgi.framework", findEclipseFramework(new File(p2BridgePluginDir, "dependencies")));
    initParams.put("eclipse.enableStateSaver", "false");
    return initParams;
  }

  private static String findEclipseFramework(final File dir) {
    for (final File f : dir.listFiles()) {
      if (f.getName().startsWith("org.eclipse.osgi")) {
        return f.toURI().toString();
      }
    }
    throw new IllegalStateException("Could not locate org.eclipse.osgi bundle");
  }

  private String scanBridgedPackages(final File dir) {
    final StringBuilder bridgedPackages = new StringBuilder();
    final File[] manifests = dir.listFiles(new FilenameFilter()
    {
      @Override
      public boolean accept(final File dir, final String name) {
        return name.endsWith(".manifest");
      }
    });
    for (final File manifestFile : manifests) {
      try (InputStream is = new FileInputStream(manifestFile)) {
        final Manifest manifest = new Manifest(is);
        final String pkg = manifest.getMainAttributes().getValue("Export-Package");
        if (!Strings.isNullOrEmpty(pkg)) {
          if (bridgedPackages.length() > 0) {
            bridgedPackages.append(",");
          }
          bridgedPackages.append(pkg);
        }
      }
      catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
    return bridgedPackages.toString();
  }

  private File getP2BridgePluginDir() {
    try {
      final GAVCoordinate pluginGav = getP2BridgePluginGAV();
      final PluginRepositoryArtifact pluginArtifact = pluginRepositoryManager.resolveArtifact(pluginGav);
      return pluginArtifact.getFile().getParentFile();
    }
    catch (final NoSuchPluginRepositoryArtifactException e) {
      throw new IllegalStateException("Could not locate nexus-p2-bridge-plugin", e);
    }
  }

  private GAVCoordinate getP2BridgePluginGAV() {
    final Properties props = new Properties();

    final InputStream is =
        P2Runtime.class.getResourceAsStream("/nexus-p2-plugin.properties");

    if (is != null) {
      try {
        props.load(is);
      }
      catch (final IOException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }

    final GAVCoordinate coordinate =
        new GAVCoordinate("org.sonatype.nexus.plugins", "nexus-p2-bridge-plugin", props.getProperty("version"));

    return coordinate;
  }

}
