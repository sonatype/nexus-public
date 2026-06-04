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
package org.sonatype.nexus.bootstrap.entrypoint.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.BASEDIR_SYS_PROP;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.DATADIR_SYS_PROP;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.getBasePath;
import static org.sonatype.nexus.bootstrap.entrypoint.configuration.NexusDirectoryConfiguration.getDataPath;

@Lazy
@Component
public class NexusProperties
    extends PropertySource<String>
{
  private static final String INTERNAL_DEFAULT_PATH = "/org/sonatype/nexus/bootstrap/application/default.properties";

  private static final File EXTERNAL_DEFAULT_NEXUS_PROPERTIES_FILEPATH =
      getBasePath("etc", "nexus-default.properties").toFile();

  private static final File EXTERNAL_NEXUS_PROPERTIES_FILEPATH = getDataPath("etc", "nexus.properties").toFile();

  private static final File EXTERNAL_NODENAME_FILEPATH = getDataPath("etc", "nexus-nodename.properties").toFile();

  private static final Logger LOG = LoggerFactory.getLogger(NexusProperties.class);

  private static PropertyMap nexusProperties;

  private final NexusPropertiesVerifier nexusPropertiesVerifier = new NexusPropertiesVerifier();

  public NexusProperties() {
    super("nexus-properties");
    init();
  }

  NexusProperties(final PropertyMap propertiesForTesting) {
    super("nexus-properties");
    nexusProperties = propertiesForTesting;
  }

  @Override
  public String getProperty(final String propertyName) {
    try {
      Interpolator interpolator = new StringSearchInterpolator();
      interpolator.addValueSource(new MapBasedValueSource(nexusProperties));
      interpolator.addValueSource(new MapBasedValueSource(System.getProperties()));
      interpolator.addValueSource(new EnvarBasedValueSource());

      String propertyValue = nexusProperties.get(propertyName);

      if (propertyValue != null) {
        propertyValue = interpolator.interpolate(propertyValue);
      }
      return propertyValue;
    }
    catch (IOException | InterpolationException e) {
      throw new RuntimeException("Failed to interpolate nexus.properties entry: " + propertyName, e);
    }
  }

  void put(final String key, final String value) {
    nexusProperties.put(key, value);
  }

  /**
   * Enforces analytics to be enabled for Community Edition.
   * This method should only be called by ApplicationLauncher when running in CE mode.
   */
  public void enforceCommunityEditionAnalytics() {
    nexusProperties.put("nexus.analytics.enabled", "true");
    // Sync to System properties since init() has already completed and won't sync this change automatically
    System.setProperty("nexus.analytics.enabled", "true");
  }

  String get(final String property, final String defaultValue) {
    String value = getProperty(property);

    return value != null ? value : defaultValue;
  }

  public Map<String, String> get() {
    return Map.copyOf(nexusProperties);
  }

  private void init() {
    if (nexusProperties != null) {
      return;
    }

    try {
      maybeCopyDefaults();

      nexusProperties = new PropertyMap();
      applyClasspathProperties(nexusProperties);
      applyProperties(nexusProperties, EXTERNAL_DEFAULT_NEXUS_PROPERTIES_FILEPATH, true, emptySet());
      applyProperties(nexusProperties, EXTERNAL_NEXUS_PROPERTIES_FILEPATH, false, emptySet());
      applyProperties(nexusProperties, EXTERNAL_NODENAME_FILEPATH, false, singleton("nexus.clustered.nodeName"));
      applySystemProperties(nexusProperties);

      interpolate();

      canonicalize(nexusProperties, BASEDIR_SYS_PROP);
      canonicalize(nexusProperties, "karaf.etc");
      canonicalize(nexusProperties, DATADIR_SYS_PROP);

      nexusPropertiesVerifier.verify(this);

      nexusProperties.forEach(System::setProperty);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void applyClasspathProperties(final PropertyMap nexusProperties) throws IOException {
    URL resource = getClass().getResource(INTERNAL_DEFAULT_PATH);
    if (resource != null) {
      Properties properties = new Properties();
      try (InputStream inputStream = resource.openStream()) {
        properties.load(inputStream);
      }
      nexusProperties.putAll(properties);
      LOG.debug("nexus.properties loaded from classpath {}", INTERNAL_DEFAULT_PATH);
    }
  }

  private static void applyProperties(
      final PropertyMap nexusProperties,
      final File externalFilepath,
      final boolean required,
      final Set<String> keysToWatch) throws IOException
  {
    PropertyMap props = new PropertyMap();
    try {
      props.load(externalFilepath.toURI().toURL());
      if (keysToWatch == null || keysToWatch.isEmpty()) {
        nexusProperties.putAll(props);
        LOG.debug("nexus.properties loaded from file {}", externalFilepath.getAbsolutePath());
      }
      else {
        for (Entry<String, String> entry : props.entrySet()) {
          String key = entry.getKey();
          String value = entry.getValue();
          if (keysToWatch.contains(key)) {
            nexusProperties.put(key, value);
            LOG.debug("Overriding key {} from file {}", key, externalFilepath);
          }
        }
      }
    }
    catch (IOException e) {
      if (required) {
        throw e;
      }

      LOG.debug("Failed to load properties from non-required file: {}", externalFilepath);
    }
  }

  private static void applySystemProperties(final PropertyMap nexusProperties) {
    for (Entry<Object, Object> entry : System.getProperties().entrySet()) {
      String key = entry.getKey().toString();
      nexusProperties.put(key, entry.getValue().toString());
      LOG.debug("Overriding key {} from System.properties", key);
    }
  }

  private static void maybeCopyDefaults() throws IOException {
    if (EXTERNAL_DEFAULT_NEXUS_PROPERTIES_FILEPATH.exists() && !EXTERNAL_NEXUS_PROPERTIES_FILEPATH.exists()) {
      File parentDir = EXTERNAL_NEXUS_PROPERTIES_FILEPATH.getParentFile();
      if (parentDir != null && !parentDir.isDirectory()) {
        Files.createDirectories(parentDir.toPath());
      }

      // Get list of default properties, commented out
      List<String> defaultProperties =
          getDefaultPropertiesCommentedOut(EXTERNAL_DEFAULT_NEXUS_PROPERTIES_FILEPATH.toPath());

      Files.write(EXTERNAL_NEXUS_PROPERTIES_FILEPATH.toPath(), defaultProperties, ISO_8859_1);
    }
  }

  private static List<String> getDefaultPropertiesCommentedOut(final Path defaultPropertiesPath) throws IOException {
    return Files.readAllLines(defaultPropertiesPath, ISO_8859_1)
        .stream()
        .filter(l -> !l.startsWith("##"))
        .map(l -> l.startsWith("#") || l.isEmpty() ? l : "# " + l)
        .collect(Collectors.toList());
  }

  private static void canonicalize(final PropertyMap nexuProperties, final String name) throws IOException {
    String value = nexuProperties.get(name);
    if (value == null) {
      LOG.warn("Unable to canonicalize null entry: {}", name);
      return;
    }
    File file = new File(value).getCanonicalFile();
    nexuProperties.put(name, file.getPath());
  }

  private void interpolate() throws IOException {
    Interpolator interpolator = new StringSearchInterpolator();
    interpolator.addValueSource(new MapBasedValueSource(nexusProperties));
    interpolator.addValueSource(new MapBasedValueSource(System.getProperties()));
    interpolator.addValueSource(new EnvarBasedValueSource());

    for (Entry<String, String> entry : nexusProperties.entrySet()) {
      try {
        nexusProperties.put(entry.getKey(), interpolator.interpolate(entry.getValue()));
      }
      catch (InterpolationException e) {
        throw new RuntimeException(
            "Failed to interpolate nexus.properties entry: " + entry.getKey() + "/" + entry.getValue(), e);
      }
    }
  }
}
