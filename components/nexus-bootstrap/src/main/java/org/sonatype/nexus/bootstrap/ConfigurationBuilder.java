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
package org.sonatype.nexus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.MapBasedValueSource;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper to build bootstrap configuration properties.
 *
 * @since 2.8
 */
public class ConfigurationBuilder
{
  private static final Logger log = LoggerFactory.getLogger(ConfigurationBuilder.class);

  private final PropertyMap properties = new PropertyMap();

  public ConfigurationBuilder properties(final Map<String, String> props) {
    if (props == null) {
      throw new NullPointerException();
    }
    if (log.isDebugEnabled()) {
      log.debug("Adding properties:");
      for (Entry<String, String> entry : props.entrySet()) {
        log.debug("  {}='{}'", entry.getKey(), entry.getValue());
      }
    }
    this.properties.putAll(props);
    return this;
  }

  public ConfigurationBuilder properties(final URL url) throws IOException {
    if (url == null) {
      throw new NullPointerException();
    }
    log.debug("Reading properties from: {}", url);
    PropertyMap props = new PropertyMap();
    props.load(url);
    return properties(props);
  }

  public ConfigurationBuilder properties(final String resource, final boolean required) throws IOException {
    return properties(getClass(), resource, required);
  }

  /**
   * @since 3.0
   */
  public ConfigurationBuilder properties(final Class<?> clazz, final String resource, final boolean required) throws IOException {
    URL url = clazz.getResource(resource);
    if (url == null) {
      if (required) {
        throw new IllegalStateException("Missing required resource: " + resource);
      }
      return this;
    }
    return properties(url);
  }

  /**
   * @since 3.0
   */
  public ConfigurationBuilder properties(final File resource, final boolean required) throws IOException {
    if (resource == null || !resource.exists()) {
      if (required) {
        throw new IllegalStateException("Missing required resource: " + resource);
      }
      return this;
    }
    return properties(resource.toURI().toURL());
  }

  public ConfigurationBuilder defaults() throws IOException {
    return properties("default.properties", true);
  }

  public ConfigurationBuilder set(final String name, final String value) {
    if (name == null) {
      throw new NullPointerException();
    }
    if (value == null) {
      throw new NullPointerException();
    }
    log.debug("Set: {}={}", name, value);
    properties.put(name, value);
    return this;
  }

  /**
   * Provides customization of configuration.
   */
  public static interface Customizer
  {
    void apply(ConfigurationBuilder builder) throws Exception;
  }

  public ConfigurationBuilder custom(final Customizer customizer) throws Exception {
    if (customizer == null) {
      throw new NullPointerException();
    }
    log.debug("Customizing: {}", customizer);
    customizer.apply(this);
    return this;
  }

  /**
   * Override any existing properties with values from the given set of overrides.
   *
   * @since 2.8.1
   */
  public ConfigurationBuilder override(final Map<String,String> overrides) {
    if (overrides == null) {
      throw new NullPointerException();
    }
    for (Entry<String,String> entry : overrides.entrySet()) {
      String name = entry.getKey();
      if (properties.containsKey(name)) {
        String value = entry.getValue();
        log.debug("Override: {}={}", name, value);
        properties.put(name, value);
      }
    }
    return this;
  }

  /**
   * @see #override(Map)
   *
   * @since 2.8.1
   */
  public ConfigurationBuilder override(final Properties overrides) {
    return override(new PropertyMap(overrides));
  }

  private void canonicalize(final String name) throws IOException {
    String value = properties.get(name);
    if (value == null) {
      log.warn("Unable to canonicalize null entry: {}", name);
      return;
    }
    File file = new File(value).getCanonicalFile();
    properties.put(name, file.getPath());
  }

  private void interpolate() throws Exception {
    Interpolator interpolator = new StringSearchInterpolator();
    interpolator.addValueSource(new MapBasedValueSource(properties));
    interpolator.addValueSource(new MapBasedValueSource(System.getProperties()));
    interpolator.addValueSource(new EnvarBasedValueSource());

    for (Entry<String, String> entry : properties.entrySet()) {
      properties.put(entry.getKey(), interpolator.interpolate(entry.getValue()));
    }
  }

  public Map<String, String> build() throws Exception {
    if (properties.isEmpty()) {
      throw new IllegalStateException("Not configured");
    }

    interpolate();

    // make some entries canonical
    canonicalize("nexus-work");

    // return copy
    PropertyMap props = new PropertyMap(properties);
    log.info("Properties:");
    for (String key : props.keys()) {
      log.info("  {}='{}'", key, props.get(key));
    }

    return props;
  }
}
