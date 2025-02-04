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
package org.sonatype.nexus.repository.internal.blobstore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.HasEntityId;
import org.sonatype.nexus.common.entity.HasName;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link BlobStoreConfiguration} data.
 *
 * @since 3.21
 */
public class BlobStoreConfigurationData
    implements HasEntityId, HasName, BlobStoreConfiguration
{

  private static final String STATE = "state";

  private static final String WRITABLE = "writable";

  public static final String ACCESS_KEY_ID = "accessKeyId";

  public static final String SECRET_ACCESS_KEY = "secretAccessKey";

  public static final String S_3 = "s3";

  // do not serialize EntityId, it can be generated on the fly
  @JsonIgnore
  private EntityId id;

  private String name;

  private String type;

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
  public String getName() {
    return name;
  }

  @Override
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public void setType(final String type) {
    this.type = type;
  }

  @Override
  public Map<String, Map<String, Object>> getAttributes() {
    if (attributes == null) {
      attributes = Maps.newHashMap();
    }

    return attributes;
  }

  @Override
  public void setAttributes(final Map<String, Map<String, Object>> attributes) {
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
        "name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", attributes=" + sanitizeAttributes(attributes) +
        '}';
  }

  private Map<String, Map<String, Object>> sanitizeAttributes(Map<String, Map<String, Object>> attributes) {
    if (attributes != null && attributes.containsKey(S_3)) {
      Map<String, Object> cleanedS3 = attributes.get(S_3)
          .entrySet()
          .stream()
          .filter(e -> !ACCESS_KEY_ID.equals(e.getKey()))
          .filter(e -> !SECRET_ACCESS_KEY.equals(e.getKey()))
          .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
      Map<String, Map<String, Object>> newAttributes = new HashMap<>(attributes);
      newAttributes.replace(S_3, cleanedS3);
      return newAttributes;
    }
    return attributes;
  }

  private static final ObjectMapper MAPPER = makeObjectMapper();

  private static ObjectMapper makeObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  @Override
  public BlobStoreConfiguration copy(final String name) {
    BlobStoreConfigurationData clone = new BlobStoreConfigurationData();
    // don't copy entity id
    clone.setName(name);
    clone.setType(getType());
    if (attributes != null && attributes.size() > 0) {
      String attribsJson = getAttribsJson();
      clone.setAttributes(getClonedAttributes(attribsJson));
    }
    return clone;
  }

  private Map<String, Map<String, Object>> getClonedAttributes(final String attribsJson) {
    try {
      return MAPPER.readValue(attribsJson, new TypeReference<Map<String, Map<String, Object>>>()
      {
      });
    }
    catch (IOException e) {
      throw new BlobStoreException("failed to parse blob store configuration attributes from JSON", e, null);
    }
  }

  private String getAttribsJson() {
    try {
      return MAPPER.writer().writeValueAsString(getAttributes());
    }
    catch (JsonProcessingException e) {
      throw new BlobStoreException("failed to marshal blob store configuration attributes to JSON", e, null);
    }
  }

  @Override
  public boolean isWritable() {
    return Optional.ofNullable(attributes)
        .map(a -> a.get(STATE))
        .map(a -> a.get(WRITABLE))
        .map(Boolean.class::cast)
        .orElse(Boolean.TRUE);
  }

  @Override
  public void setWritable(final boolean writable) {
    attributes(STATE).set(WRITABLE, writable);
  }
}
