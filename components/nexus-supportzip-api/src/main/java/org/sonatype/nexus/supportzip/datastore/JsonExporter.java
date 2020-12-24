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
package org.sonatype.nexus.supportzip.datastore;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.io.SanitizingJsonOutputStream;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.supportzip.PasswordSanitizing.REPLACEMENT;
import static org.sonatype.nexus.supportzip.PasswordSanitizing.SENSITIVE_FIELD_NAMES;

/**
 * Export/Import data to/from the JSON by replacing sensitive data.
 *
 * @since 3.29
 */
@Named
@Singleton
public class JsonExporter
    extends ComponentSupport
{
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static final String EMPTY_JSON = "{}";

  static {
    OBJECT_MAPPER.registerModule(new SimpleModule()
        .addSerializer(Time.class, new SecondsSerializer())
        .addDeserializer(Time.class, new SecondsDeserializer()));
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
    OBJECT_MAPPER.registerModule(new JodaModule());
    OBJECT_MAPPER.registerModule(new Jdk8Module());
    OBJECT_MAPPER.disable(WRITE_DATES_AS_TIMESTAMPS);
    OBJECT_MAPPER.disable(FAIL_ON_EMPTY_BEANS);
  }

  /**
   * Export data to the JSON file and hide sensitive fields.
   *
   * @param objects to be exported.
   * @param file    where to export.
   * @throws IOException for any issue during writing a file.
   */
  public <T> void exportToJson(final List<T> objects, final File file) throws IOException {
    checkNotNull(file);
    if (objects == null || objects.isEmpty()) {
      writeEmptyJson(file);
    }
    else {
      try (ByteArrayInputStream is = new ByteArrayInputStream(OBJECT_MAPPER.writeValueAsBytes(objects));
           OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
           SanitizingJsonOutputStream stream = new SanitizingJsonOutputStream(os, SENSITIVE_FIELD_NAMES, REPLACEMENT)) {
        ByteStreams.copy(is, stream);
      }
    }
  }

  /**
   * Export data to the JSON file and hide sensitive fields.
   *
   * @param object to be exported.
   * @param file   where to export.
   * @throws IOException for any issue during writing a file.
   */
  public <T> void exportObjectToJson(final T object, final File file) throws IOException {
    checkNotNull(file);
    if (object == null) {
      writeEmptyJson(file);
    }
    else {
      try (ByteArrayInputStream is = new ByteArrayInputStream(OBJECT_MAPPER.writeValueAsBytes(object));
           OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
           SanitizingJsonOutputStream stream = new SanitizingJsonOutputStream(os, SENSITIVE_FIELD_NAMES, REPLACEMENT)) {
        ByteStreams.copy(is, stream);
      }
    }
  }

  /**
   * Read JSON data.
   *
   * @param file  file where data will be read.
   * @param clazz the type of imported data.
   * @return the list of {@link T} objects.
   * @throws IOException for any issue during reading a file.
   */
  public <T> List<T> importFromJson(final File file, final Class<T> clazz) throws IOException {
    checkNotNull(file);
    checkNotNull(clazz);
    try (FileInputStream inputStream = new FileInputStream(file)) {
      String jsonData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      if (StringUtils.isNotBlank(jsonData) && !jsonData.equals(EMPTY_JSON)) {
        JavaType type = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
        return OBJECT_MAPPER.readValue(jsonData, type);
      }
    }

    return Collections.emptyList();
  }

  /**
   * Read JSON data.
   *
   * @param file  file where data will be read.
   * @param clazz the type of imported data.
   * @return {@link T} object or {@link Optional#empty} is case of an empty JSON file.
   * @throws IOException for any issue during reading a file.
   */
  public <T> Optional<T> importObjectFromJson(final File file, final Class<T> clazz) throws IOException {
    checkNotNull(file);
    checkNotNull(clazz);
    try (FileInputStream inputStream = new FileInputStream(file)) {
      String jsonData = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      if (StringUtils.isNotBlank(jsonData) && !jsonData.equals(EMPTY_JSON)) {
        return Optional.of(OBJECT_MAPPER.readValue(jsonData, clazz));
      }
    }
    return Optional.empty();
  }

  private void writeEmptyJson(final File file) throws IOException {
    try (FileWriter fileWriter = new FileWriter(file)) {
      fileWriter.write(EMPTY_JSON);
    }
  }
}
