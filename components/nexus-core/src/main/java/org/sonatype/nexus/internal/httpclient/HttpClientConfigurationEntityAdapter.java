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
package org.sonatype.nexus.internal.httpclient;

import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.common.Time;
import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.httpclient.config.AuthenticationConfiguration;
import org.sonatype.nexus.httpclient.config.HttpClientConfiguration;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.SingletonEntityAdapter;
import org.sonatype.nexus.security.PasswordHelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link HttpClientConfiguration} entity-adapter.
 *
 * Maps entity directly to document fields via Jackson.
 *
 * @since 3.0
 */
@Named
@Singleton
public class HttpClientConfigurationEntityAdapter
    extends SingletonEntityAdapter<HttpClientConfiguration>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("http_client")
      .build();

  private final ObjectMapper objectMapper;

  @Inject
  public HttpClientConfigurationEntityAdapter(final PasswordHelper passwordHelper) throws Exception {
    super(DB_CLASS);

    this.objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        // register custom serializers and deserializers
        // - goodies Time is our internal Time representation
        // - AuthenticationConfiguration needs a tiny bit of logic for resolving the proper impl and encryption
        .registerModule(
            new SimpleModule()
                .addSerializer(
                    Time.class,
                    new SecondsSerializer()
                )
                .addDeserializer(
                    Time.class,
                    new SecondsDeserializer()
                )
                .addSerializer(
                    AuthenticationConfiguration.class,
                    new AuthenticationConfigurationSerializer(passwordHelper)
                )
                .addDeserializer(
                    AuthenticationConfiguration.class,
                    new AuthenticationConfigurationDeserializer(passwordHelper)
                )
        );
  }

  @Override
  protected void defineType(final OClass type) {
    // no schema
  }

  @Override
  protected HttpClientConfiguration newEntity() {
    return new HttpClientConfiguration();
  }

  // TODO: Sort out if this is what we really want, or if we want to define a EMBEDDEDMAP property

  @Override
  protected void readFields(final ODocument document, final HttpClientConfiguration entity) throws Exception {
    ObjectReader reader = objectMapper.readerForUpdating(entity);
    TokenBuffer buff = new TokenBuffer(objectMapper, false);
    Map<String, Object> fields = document.toMap();

    // strip out id/class synthetics
    fields.remove("@rid");
    fields.remove("@class");

    log.trace("Reading fields: {}", fields);
    objectMapper.writeValue(buff, fields);
    reader.readValue(buff.asParser());
  }

  private static final TypeReference<Map<String, Object>> MAP_STRING_OBJECT =
      new TypeReference<Map<String, Object>>() { };

  @Override
  protected void writeFields(final ODocument document, final HttpClientConfiguration entity) throws Exception {
    Map<String, Object> fields = objectMapper.convertValue(entity, MAP_STRING_OBJECT);
    log.trace("Writing fields: {}", fields);
    document.fromMap(fields);
  }

  @Nullable
  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);
    log.debug("Emitted {} event with metadata {}", eventKind, metadata);
    switch (eventKind) {
      case CREATE:
        return new HttpClientConfigurationCreatedEvent(metadata);
      case UPDATE:
        return new HttpClientConfigurationUpdatedEvent(metadata);
      case DELETE:
        return new HttpClientConfigurationDeletedEvent(metadata);
      default:
        return null;
    }
  }
}
