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
package org.sonatype.nexus.proxy.maven.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.configuration.application.NexusConfiguration;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Maps;

/**
 * A very simple artifact packaging mapper, that has everything for quick-start wired in this class. Also, it takes
 * into account the "${nexus-work}/conf/packaging2extension-mapping.properties" file into account if found. To override the
 * "defaults" in this class, simply add lines to properties file with same keys.
 *
 * @author cstamas
 */
@Singleton
@Named
public class DefaultArtifactPackagingMapper
    extends ComponentSupport
    implements ArtifactPackagingMapper
{
  public static final String MAPPING_PROPERTIES_FILE = "packaging2extension-mapping.properties";

  private final static Map<String, String> defaults;

  static {
    defaults = Maps.newHashMapWithExpectedSize(15);
    defaults.put("ejb-client", "jar");
    defaults.put("ejb", "jar");
    defaults.put("rar", "jar");
    defaults.put("par", "jar");
    defaults.put("maven-plugin", "jar");
    defaults.put("maven-archetype", "jar");
    defaults.put("plexus-application", "jar");
    defaults.put("eclipse-plugin", "jar");
    defaults.put("eclipse-feature", "jar");
    defaults.put("eclipse-application", "zip");
    defaults.put("nexus-plugin", "jar");
    defaults.put("java-source", "jar");
    defaults.put("javadoc", "jar");
    defaults.put("test-jar", "jar");
    defaults.put("bundle", "jar");
  }

  private volatile File propertiesFile;

  private volatile Map<String, String> packaging2extensionMapping;

  @Inject
  public DefaultArtifactPackagingMapper(final NexusConfiguration nexusConfiguration) {
    setPropertiesFile(new File(nexusConfiguration.getConfigurationDirectory(), MAPPING_PROPERTIES_FILE));
  }

  @Override
  public void setPropertiesFile(File propertiesFile) {
    this.propertiesFile = propertiesFile;
    this.packaging2extensionMapping = null;
  }

  public Map<String, String> getPackaging2extensionMapping() {
    if (packaging2extensionMapping == null) {
      synchronized (this) {
        if (packaging2extensionMapping == null) {
          packaging2extensionMapping = Maps.newHashMapWithExpectedSize(defaults.size());

          // merge defaults
          packaging2extensionMapping.putAll(defaults);

          if (propertiesFile != null && propertiesFile.exists()) {
            log.info("Found artifact packaging mapping file {}", propertiesFile);

            final Properties userMappings = new Properties();

            try (final FileInputStream fis =new FileInputStream(propertiesFile)) {
              userMappings.load(fis);

              if (userMappings.keySet().size() > 0) {
                for (Object key : userMappings.keySet()) {
                  packaging2extensionMapping.put(key.toString(),
                      userMappings.getProperty(key.toString()));
                }

                log.info("User artifact packaging mapping file contained {} mappings", userMappings.keySet().size());
              }
            }
            catch (IOException e) {
              log.warn(
                  "Got IO exception during read of file: {}", propertiesFile.getAbsolutePath(), e);
            }
          }
          else {
            // make it silent if using defaults
            log.debug(
                "User artifact packaging mappings file not found, will work with defaults...");
          }
        }
      }
    }

    return packaging2extensionMapping;
  }

  public Map<String, String> getDefaults() {
    return defaults;
  }

  @Override
  public String getExtensionForPackaging(String packaging) {
    if (packaging == null) {
      return "jar";
    }

    if (getPackaging2extensionMapping().containsKey(packaging)) {
      return getPackaging2extensionMapping().get(packaging);
    }
    else {
      // default's to packaging name, ie. "jar", "war", "pom", etc.
      return packaging;
    }
  }
}