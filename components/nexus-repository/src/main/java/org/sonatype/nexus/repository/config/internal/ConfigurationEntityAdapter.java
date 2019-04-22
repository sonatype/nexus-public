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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityId;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.IterableEntityAdapter;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.routing.RoutingRulesConfiguration;
import org.sonatype.nexus.repository.routing.internal.RoutingRuleEntityAdapter;
import org.sonatype.nexus.security.PasswordHelper;

import com.orientechnologies.orient.core.collate.OCaseInsensitiveCollate;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Configuration} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ConfigurationEntityAdapter
    extends IterableEntityAdapter<Configuration>
{
  public static final String DB_NAME = "repository";

  private static final String DB_CLASS = new OClassNameBuilder()
      .type(DB_NAME)
      .build();

  private static final String P_REPOSITORY_NAME = "repository_name";

  private static final String P_RECIPE_NAME = "recipe_name";

  public static final String P_ROUTING_RULE_ID = "routingRuleId";

  private static final String P_ONLINE = "online";

  private static final String P_ATTRIBUTES = "attributes";

  private static final String I_REPOSITORY_NAME = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_REPOSITORY_NAME)
      .build();

  private final PasswordHelper passwordHelper;

  private final RoutingRulesConfiguration routingRulesConfiguration;

  private final RoutingRuleEntityAdapter routingRuleEntityAdapter;

  @Inject
  public ConfigurationEntityAdapter(final PasswordHelper passwordHelper,
                                    final RoutingRulesConfiguration routingRulesConfiguration,
                                    final RoutingRuleEntityAdapter routingRuleEntityAdapter)
  {
    super(DB_CLASS);
    this.passwordHelper = checkNotNull(passwordHelper);
    this.routingRulesConfiguration = checkNotNull(routingRulesConfiguration);
    this.routingRuleEntityAdapter = checkNotNull(routingRuleEntityAdapter);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_REPOSITORY_NAME, OType.STRING)
        .setCollate(new OCaseInsensitiveCollate())
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_RECIPE_NAME, OType.STRING)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_ROUTING_RULE_ID, OType.LINK);
    type.createProperty(P_ONLINE, OType.BOOLEAN)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_ATTRIBUTES, OType.EMBEDDEDMAP);
    type.createIndex(I_REPOSITORY_NAME, INDEX_TYPE.UNIQUE, P_REPOSITORY_NAME);
  }

  @Override
  protected Configuration newEntity() {
    return new Configuration();
  }

  @Override
  protected void readFields(final ODocument document, final Configuration entity) {
    String recipeName = document.field(P_RECIPE_NAME, OType.STRING);
    String repositoryName = document.field(P_REPOSITORY_NAME, OType.STRING);
    Boolean online = document.field(P_ONLINE, OType.BOOLEAN);
    Map<String, Map<String, Object>> attributes = document.field(P_ATTRIBUTES, OType.EMBEDDEDMAP);
    if (routingRulesConfiguration.isEnabled()) {
      ODocument routingRule = document.field(P_ROUTING_RULE_ID, OType.LINK);
      EntityId routingRuleId =
          routingRule == null ? null : new AttachedEntityId(routingRuleEntityAdapter, routingRule.getIdentity());
      entity.setRoutingRuleId(routingRuleId);
    }

    entity.setRecipeName(recipeName);
    entity.setRepositoryName(repositoryName);
    entity.setOnline(online);
    entity.setAttributes(decrypt(attributes));
  }

  /**
   * Returns a copy of the attributes with sensitive data decrypted.
   */
  private Map<String, Map<String, Object>> decrypt(Map<String, Map<String, Object>> attributes) {
    return process(attributes, passwordHelper::tryDecrypt);
  }

  @Override
  protected void writeFields(final ODocument document, final Configuration entity) {
    Map<String, Map<String, Object>> attributes = entity.getAttributes();

    document.field(P_RECIPE_NAME, entity.getRecipeName());
    document.field(P_REPOSITORY_NAME, entity.getRepositoryName());
    document.field(P_ONLINE, entity.isOnline());
    document.field(P_ATTRIBUTES, encrypt(attributes));

    if (routingRulesConfiguration.isEnabled()) {
      document.field(P_ROUTING_RULE_ID, entity.getRoutingRuleId() != null ?
          getRecordIdObfuscator()
              .decode(routingRuleEntityAdapter.getSchemaType(), entity.getRoutingRuleId().getValue()) :
          null);
    }
  }

  /**
   * Returns a copy of the attributes with sensitive data encrypted.
   */
  private Map<String, Map<String, Object>> encrypt(final Map<String, Map<String, Object>> attributes) {
    return process(attributes, passwordHelper::encrypt);
  }

  /**
   * Returns a detached copy of the attributes with sensitive data transformed using the given function.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  private <V> Map<String, V> process(@Nullable final Map<String, V> map, final Function<String, String> transform) {
    if (map == null) {
      return null;
    }
    Map<String, V> processed = new HashMap<>(map.size());
    for (Entry<String, V> entry : map.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof Map) {
        value = process((Map) value, transform);
      }
      else if (isSensitiveEntry(entry)) {
        value = transform.apply((String) value);
      }
      else {
        value = detach(value); 
      }
      processed.put(entry.getKey(), (V) value);
    }
    return processed;
  }

  /**
   * Returns {@code true} if entry carries sensitive data, that should be encrypted/decrypted on externalization.
   */
  private boolean isSensitiveEntry(final Entry<String, ?> entry) {
    return entry.getKey().toLowerCase(Locale.ENGLISH).endsWith("password") && entry.getValue() instanceof String;
  }

  @Override
  public boolean sendEvents() {
    return true;
  }

  @Nullable
  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    final EntityMetadata metadata = new AttachedEntityMetadata(this, document);
    final String repositoryName = document.field(P_REPOSITORY_NAME);

    log.trace("newEvent: eventKind: {}, repositoryName: {}, metadata: {}", eventKind, repositoryName, metadata);
    switch (eventKind) {
      case CREATE:
        return new ConfigurationCreatedEvent(metadata, repositoryName);
      case UPDATE:
        return new ConfigurationUpdatedEvent(metadata, repositoryName);
      case DELETE:
        return new ConfigurationDeletedEvent(metadata, repositoryName);
      default:
        return null;
    }
  }
}
