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
package org.sonatype.nexus.repository.config.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.DetachingList;
import org.sonatype.nexus.common.collect.DetachingMap;
import org.sonatype.nexus.common.collect.DetachingSet;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.HasEntityId;
import org.sonatype.nexus.common.entity.HasName;
import org.sonatype.nexus.common.io.SanitizingJsonOutputStream;
import org.sonatype.nexus.repository.config.Configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.sonatype.nexus.common.text.Strings2.mask;

/**
 * {@link Configuration} data.
 *
 * @since 3.21
 */
public class ConfigurationData
    implements HasEntityId, HasName, Configuration
{
  private static final Logger log = LoggerFactory.getLogger(ConfigurationData.class);

  private static final TypeReference<Map<String, Map<String, Object>>> ATTRIBUTES_TYPE_REF = new TypeReference<Map<String, Map<String, Object>>>() {};

  private static final ObjectWriter ATTRIBUTES_JSON_WRITER = new ObjectMapper().writerFor(ATTRIBUTES_TYPE_REF);

  private static final ObjectReader ATTRIBUTES_JSON_READER = new ObjectMapper().readerFor(ATTRIBUTES_TYPE_REF);

  private EntityId id;

  private String name;

  private String recipeName;

  private boolean online;

  private EntityId routingRuleId;

  private Map<String, Map<String, Object>> attributes;

  @Override
  public EntityId getId() {
    return id;
  }

  @Override
  public void setId(final EntityId id) {
    this.id = id;
  }

  @Override
  public EntityId getRepositoryId() {
    return getId(); // alias repositoryId to id
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public String getRepositoryName() {
    return getName(); // alias repositoryName to name
  }

  @Override
  public void setRepositoryName(final String name) {
    setName(name); // alias repositoryName to name
  }

  @Override
  public String getRecipeName() {
    return recipeName;
  }

  @Override
  public void setRecipeName(final String recipeName) {
    this.recipeName = checkNotNull(recipeName);
  }

  /**
   * @return true, if repository should serve inbound requests
   */
  @Override
  public boolean isOnline() {
    return online;
  }

  /**
   * @param online true, if repository should serve inbound requests
   */
  @Override
  public void setOnline(final boolean online) {
    this.online = online;
  }

  @Override
  public EntityId getRoutingRuleId() {
    return routingRuleId;
  }

  @Override
  public void setRoutingRuleId(final EntityId routingRuleId) {
    this.routingRuleId = routingRuleId;
  }

  @Override
  @Nullable
  public Map<String, Map<String, Object>> getAttributes() {
    return attributes;
  }

  @Override
  public void setAttributes(@Nullable final Map<String, Map<String, Object>> attributes) {
    this.attributes = attributes;
  }

  @Override
  public NestedAttributesMap attributes(final String key) {
    checkNotNull(key);

    if (attributes == null) {
      attributes = Maps.newHashMap();
    }

    Map<String, Object> map = attributes.get(key);
    if (map == null) {
      map = Maps.newHashMap();
      attributes.put(key, map);
    }

    return new NestedAttributesMap(key, map);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "repositoryName='" + name + '\'' +
        ", recipeName='" + recipeName + '\'' +
        ", attributes=" + obfuscatedAttributes() +
        '}';
  }

  /**
   * @return string representation with sensitive data (passwords) obfuscated
   */
  private String obfuscatedAttributes() {
    try (ByteArrayOutputStream obfuscatedAttrs = new ByteArrayOutputStream()) {
      try (SanitizingJsonOutputStream sanitizer = new SanitizingJsonOutputStream(obfuscatedAttrs,
          SENSITIVE_FIELD_NAMES,
          mask("password"))) {
        sanitizer.write(ATTRIBUTES_JSON_WRITER.writeValueAsBytes(attributes));
      }

      Object result = ATTRIBUTES_JSON_READER.readValue(obfuscatedAttrs.toByteArray());
      return result != null ? result.toString() : "";
    }
    catch (IOException e) {
      log.error("Error obfuscating attributes", e);
      return format("<<Unable to obfuscate attributes. Exception was '%s'>>", e.getMessage());
    }
  }

  /**
   * Returns a deeply cloned copy. Note that Entity.entityMetadata is not deep-copied.
   */
  @Override
  public ConfigurationData copy() {
    try {
      ConfigurationData c = (ConfigurationData) clone();
      c.id = null; // don't copy entity id
      c.attributes = copy(attributes);
      return c;
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a lazy copy; for collections the copy is only taken as the content is touched.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private <V> V copy(final V value) {
    Object copy = value;
    if (value instanceof Map) {
      copy = new DetachingMap((Map) value, this::copy);
    }
    else if (value instanceof List) {
      copy = new DetachingList((List) value, this::copy);
    }
    else if (value instanceof Set) {
      copy = new DetachingSet((Set) value, this::copy);
    }
    return (V) copy;
  }

}
