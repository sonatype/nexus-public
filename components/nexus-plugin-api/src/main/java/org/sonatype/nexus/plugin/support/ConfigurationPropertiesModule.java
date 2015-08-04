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
package org.sonatype.nexus.plugin.support;

import java.io.File;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.sisu.goodies.common.Properties2;
import org.sonatype.sisu.goodies.inject.ModuleSupport;

import com.google.common.collect.Maps;
import org.eclipse.sisu.wire.ParameterKeys;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper module to load additional Sisu configuration properties.
 *
 * @since 2.7
 */
public class ConfigurationPropertiesModule
    extends ModuleSupport
{
  private static final String DOT_PROPERTIES = ".properties";

  private final String fileName;

  private final String prefix;

  public ConfigurationPropertiesModule(final @NonNls String fileName, final @Nullable @NonNls String prefix) {
    this.fileName = checkNotNull(fileName);
    this.prefix = prefix;
  }

  /**
   * Equivalent to {@link #ConfigurationPropertiesModule(String, String)} with "<em>id</em>.properties",
   * "<em>id</em>.".
   */
  public ConfigurationPropertiesModule(final @NonNls String id) {
    this(id + DOT_PROPERTIES, id + ".");
  }

  @Override
  protected void configure() {
    CustomProperties properties = new CustomProperties();
    requestInjection(properties); // request early injection
    bind(ParameterKeys.PROPERTIES).toInstance(properties);
  }

  private class CustomProperties
      extends AbstractMap<String, String>
  {
    private Map<String, String> properties = Collections.emptyMap();

    @Inject
    public void loadProperties(@Named("${application-conf}") String configDir) {
      try {
        File file = new File(configDir, fileName).getCanonicalFile();

        if (file.exists()) {
          log.info("Loading properties: {}", file);
          Properties props = Properties2.load(file);

          // If we have a prefix apply it else use as-is
          if (prefix == null) {
            properties = Maps.fromProperties(props);
          }
          else {
            log.debug("Applying prefix to keys: {}", prefix);
            Map<String, String> tmp = Maps.newHashMapWithExpectedSize(props.size());
            for (String key : props.stringPropertyNames()) {
              if (key.startsWith(prefix)) {
                log.warn("Not applying prefix to already prefixed key: {}", key);
                tmp.put(key, props.getProperty(key));
              }
              else {
                tmp.put(String.format("%s%s", prefix, key), props.getProperty(key)); //NON-NLS
              }
            }
            properties = tmp;
          }

          if (log.isDebugEnabled() && !properties.isEmpty()) {
            log.debug("Properties:");
            for (String key : Properties2.sortKeys(properties)) {
              log.debug("  {}={}", key, properties.get(key));
            }
          }
        }
      }
      catch (Exception e) {
        log.warn("Failed to load properties", e);
      }
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
      return properties.entrySet();
    }
  }
}