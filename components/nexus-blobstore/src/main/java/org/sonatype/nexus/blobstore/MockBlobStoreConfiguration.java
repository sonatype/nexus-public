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
package org.sonatype.nexus.blobstore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.BlobStoreConfiguration;
import org.sonatype.nexus.blobstore.api.BlobStoreException;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * POJO {@link BlobStoreConfiguration} for tests only
 *
 * @since 3.20
 *
 */
@VisibleForTesting
public class MockBlobStoreConfiguration
    implements BlobStoreConfiguration
{
  public static final String STATE = "state";

  public static final String WRITABLE = "writable";

  private String name;

  private String type;

  private Map<String, Map<String, Object>> attributes = new HashMap<>();

  public MockBlobStoreConfiguration() {
  }

  public MockBlobStoreConfiguration(final String name, final String type) {
    this.name = name;
    this.type = type;
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
        ", attributes=" + attributes +
        '}';
  }

  private static final ObjectMapper MAPPER = makeObjectMapper();

  private static ObjectMapper makeObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  @Override
  public BlobStoreConfiguration copy(String name) {
    MockBlobStoreConfiguration clone = new MockBlobStoreConfiguration();
    clone.setName(name);
    clone.setType(getType());
    if (attributes != null && attributes.size() > 0) {
      String attribsJson;
      try {
        attribsJson = MAPPER.writer().writeValueAsString(getAttributes());
      }
      catch (JsonProcessingException e) {
        throw new BlobStoreException("failed to marshal blob store configuration attributes to JSON", e, null);
      }
      Map<String, Map<String, Object>> clonedAttributes;
      try {
        clonedAttributes = MAPPER.readValue(attribsJson, new TypeReference<Map<String, Map<String, Object>>>()
        {
        });
      }
      catch (IOException e) {
        throw new BlobStoreException("failed to parse blob store configuration attributes from JSON", e, null);
      }
      clone.setAttributes(clonedAttributes);
    }
    return clone;
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

  public MockBlobStoreConfiguration withName(final String name) {
    setName(name);
    return this;
  }

  public MockBlobStoreConfiguration withType(final String type) {
    setType(type);
    return this;
  }

  public MockBlobStoreConfiguration withAttributes(final Map<String, Map<String, Object>> attributes) {
    setAttributes(attributes);
    return this;
  }
}
