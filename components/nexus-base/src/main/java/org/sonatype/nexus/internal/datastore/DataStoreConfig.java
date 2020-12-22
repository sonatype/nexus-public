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
package org.sonatype.nexus.internal.datastore;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.common.property.PropertiesFile;
import org.sonatype.nexus.datastore.DataStoreConfigurationLocalSource;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.datastore.api.DataStoreManager;
import org.sonatype.nexus.supportzip.GeneratedContentSourceSupport;
import org.sonatype.nexus.supportzip.SupportBundle;
import org.sonatype.nexus.supportzip.SupportBundle.ContentSource.Type;
import org.sonatype.nexus.supportzip.SupportBundleCustomizer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.supportzip.PasswordSanitizing.REPLACEMENT;
import static org.sonatype.nexus.supportzip.PasswordSanitizing.SENSITIVE_FIELD_NAMES;

/**
 * Add {@link SupportBundle} to export datastore configuration files.
 *
 * @since 3.next
 */
@Named
@Singleton
public class DataStoreConfig
    extends ComponentSupport
    implements SupportBundleCustomizer
{
  private static final Path PATH = Paths.get("etc/fabric");

  private static final String NAME_KEY = "name";

  private static final String TYPE_KEY = "type";

  private static final String JDBC_URL_KEY = "jdbcUrl";

  private final DataStoreConfigurationLocalSource dsConfigSource;

  @Inject
  public DataStoreConfig(final DataStoreConfigurationLocalSource dsConfigSource) {
    this.dsConfigSource = checkNotNull(dsConfigSource);
  }

  @Override
  public void customize(final SupportBundle supportBundle) {
    File configProps = dsConfigSource.getPropertiesFile(DataStoreManager.CONFIG_DATASTORE_NAME);
    File contentProps = dsConfigSource.getPropertiesFile(DataStoreManager.CONTENT_DATASTORE_NAME);
    supportBundle.add(storeDsPropertyFile(configProps, DataStoreManager.CONFIG_DATASTORE_NAME));
    supportBundle.add(storeDsPropertyFile(contentProps, DataStoreManager.CONTENT_DATASTORE_NAME));
  }

  private GeneratedContentSourceSupport storeDsPropertyFile(final File propertiesFile, final String storeName) {
    String dbPropsPath = PATH.resolve(propertiesFile.getName()).toString();
    return new GeneratedContentSourceSupport(Type.CONFIG, dbPropsPath)
    {
      @Override
      protected void generate(final File file) throws Exception {
        DataStoreConfiguration configDs = dsConfigSource.load(storeName);
        PropertiesFile props = new PropertiesFile(file);
        props.setProperty(NAME_KEY, configDs.getName());
        props.setProperty(TYPE_KEY, configDs.getType());
        Map<String, String> attributes = new HashMap<>(configDs.getAttributes());

        // jdbcUrl may contain sensitive data like a password
        String jdbcUrl = attributes.get(JDBC_URL_KEY);
        attributes.put(JDBC_URL_KEY, getWithoutSensitiveData(jdbcUrl));

        Map<String, String> redactedDsAttributes = configDs.redact(attributes);
        redactedDsAttributes.forEach(props::setProperty);
        props.store();
      }
    };
  }

  private String getWithoutSensitiveData(final String jdbcUrl) {
    String[] urlParts = jdbcUrl.split("\\?");
    StringBuilder result = new StringBuilder(urlParts[0]);
    try {
      if (urlParts.length == 2) {
        result.append("?");
        String[] paramsParts = urlParts[1].split("&");
        for (String param : paramsParts) {
          String[] values = param.split("=");
          String name = values[0];
          result.append(name);
          result.append("=");
          if (SENSITIVE_FIELD_NAMES.contains(name)) {
            result.append(REPLACEMENT);
          }
          else {
            String value = values.length == 2 ? values[1] : "";
            result.append(value);
          }
          result.append("&");
        }
        result.delete(result.length() - 1, result.length());
      }
    }
    catch (Exception e) {
      log.error("Can't parse jdbcUrl: {}", jdbcUrl, e);
      return urlParts[0] + "?cant_parse_parameters";
    }

    return result.toString();
  }
}
