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
package org.sonatype.nexus.datastore.mybatis;

import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;

/**
 * Comprehensive list of placeholder types used in DAO schemas.
 *
 * Defaults are provided for H2 and PostgreSQL.
 *
 * We can configure further database-specific types in {@code $install-dir/etc/fabric/config-store-mybatis.xml} and
 * {@code content-store-mybatis.xml} using keys in the form of {@code <placeholder_type>.<databaseId>}, for example:
 *
 * <pre>
 * <property name="UUID_TYPE.MySQL" value="VARCHAR(36)"/>
 * </pre>
 *
 * @since 3.20
 */
enum PlaceholderTypes
{
  UUID_TYPE(of("H2", "UUID", "PostgreSQL", "UUID")),

  JSON_TYPE(of("H2", "JSON", "PostgreSQL", "JSONB")),

  BINARY_TYPE(of("H2", "BYTEA", "PostgreSQL", "BYTEA"));

  private static final Logger log = LoggerFactory.getLogger(PlaceholderTypes.class);

  private static final Pattern TYPE_PATTERN = Pattern.compile("[A-Za-z]+(?: [A-Za-z]+)?(?: ?\\([1-9][0-9]*\\))?");

  private final Map<String, String> defaults;

  private PlaceholderTypes(Map<String, String> defaults) {
    this.defaults = checkNotNull(defaults);
  }

  /**
   * Configures concrete types for all the known placeholder types.
   *
   * If this is not H2 or PostgreSQL then we lookup the configured types and turn on lenient support.
   */
  public static boolean configurePlaceholderTypes(final Configuration config) {
    String databaseId = config.getDatabaseId();
    Properties variables = config.getVariables();

    boolean lenient = false;
    for (PlaceholderTypes placeholder : values()) {
      String name = placeholder.name();

      String key = name + '.' + databaseId;
      String type = placeholder.defaults.get(databaseId);

      if (type == null) {
        // no built-in default, check config
        type = variables.getProperty(key);
        lenient = true;
      }

      if (isNullOrEmpty(type)) {
        log.warn("No type configured for {}", key);
        throw new IllegalArgumentException(format("No database type configured for %s", key));
      }
      else if (!TYPE_PATTERN.matcher(type).matches()) {
        log.warn("Invalid type {} configured for {}", type, key);
        throw new IllegalArgumentException(format("Invalid database type %s configured for %s", type, key));
      }

      variables.put(name, type);
    }

    return lenient;
  }
}
