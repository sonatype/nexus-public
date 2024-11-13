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
package org.sonatype.nexus.datastore.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.transformEntries;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.sonatype.nexus.common.jdbc.JdbcUrlRedactor.redactPassword;

/**
 * {@link DataStore} configuration.
 *
 * @since 3.19
 */
public class DataStoreConfiguration
{
  public static final String REDACTED = "**REDACTED**";

  private static final String JDBC_URL = "jdbcUrl";

  private static final Predicate<String> SENSITIVE_KEYS =
      compile("(?i)(auth|cred|key|pass|secret|sign|token)").asPredicate();

  private String name;

  private String type;

  private String source;

  private Map<String, String> attributes;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = checkNotNull(name);
  }

  public String getType() {
    return type;
  }

  public void setType(final String type) {
    this.type = checkNotNull(type);
  }

  public String getSource() {
    return source;
  }

  public void setSource(final String source) {
    this.source = checkNotNull(source);
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(final Map<String, String> attributes) {
    this.attributes = checkNotNull(attributes);
  }

  /**
   * @since 3.31
   */
  public static boolean isSensitiveKey(final String key) {
    return SENSITIVE_KEYS.test(key);
  }

  @Override
  public String toString() {
    return "{" +
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", source='" + source + '\'' +
        ", attributes=" + maybeRedact(attributes) +
        '}';
  }

  public static Map<String, Map<String, String>> diff(final DataStoreConfiguration left, final DataStoreConfiguration right) {
    Map<String, Map<String, String>> results = new HashMap<>();
    diff("name", left, right, DataStoreConfiguration::getName, results);
    diff("source", left, right, DataStoreConfiguration::getSource, results);
    diff("type", left, right, DataStoreConfiguration::getType, results);

    Stream.concat(left.getAttributes().keySet().stream(), right.getAttributes().keySet().stream())
        .distinct()
        .forEach(attributeKey -> {
          Function<DataStoreConfiguration, String> getAttribute = (config -> config.getAttributes().getOrDefault(attributeKey, null));
          diff("attributes->" + attributeKey, left, right, getAttribute, results);
        });
    return results;
  }

  private static void diff(final String fieldName,
                           final DataStoreConfiguration a,
                           final DataStoreConfiguration b,
                           final Function<DataStoreConfiguration, String> fieldFunction,
                           Map<String, Map<String, String>> results) {
    String aField = fieldFunction.apply(a);
    String bField = fieldFunction.apply(b);

    if (!Objects.equals(aField, bField)) {
      Map<String, String> result = new HashMap<>();
      result.put(a.getName(), maybeRedact(fieldName, aField));
      result.put(b.getName(), maybeRedact(fieldName, bField));
      results.put(fieldName, result);
    }
  }

  /**
   * Redact output using a blacklist of potentially sensitive key patterns.
   */
  protected Map<String, String> maybeRedact(final Map<String, String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return attributes;
    } else {
      return transformEntries(attributes, DataStoreConfiguration::maybeRedact);
    }
  }

  private static String maybeRedact(final String key, final String value) {
    String result = value;
    if (containsIgnoreCase(key, JDBC_URL)) {
      result = redactPassword(value);
    }
    return isSensitiveKey(key) ? REDACTED : result;
  }
}
