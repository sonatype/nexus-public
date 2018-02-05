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
package org.sonatype.nexus.internal.security.anonymous;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.SingletonEntityAdapter;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * {@link AnonymousConfiguration} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class AnonymousConfigurationEntityAdapter
    extends SingletonEntityAdapter<AnonymousConfiguration>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("anonymous")
      .build();

  private static final String P_ENABLED = "enabled";

  private static final String P_USER_ID = "user_id";

  private static final String P_REALM_NAME = "realm_name";

  public AnonymousConfigurationEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_ENABLED, OType.BOOLEAN)
        .setMandatory(true)
        .setNotNull(true);
    type.createProperty(P_USER_ID, OType.STRING)
        .setNotNull(true);
    type.createProperty(P_REALM_NAME, OType.STRING)
        .setNotNull(true);
  }

  @Override
  protected AnonymousConfiguration newEntity() {
    return new AnonymousConfiguration();
  }

  @Override
  protected void readFields(final ODocument document, final AnonymousConfiguration entity) {
    boolean enabled = document.field(P_ENABLED, OType.BOOLEAN);
    String userId = document.field(P_USER_ID, OType.STRING);
    String realmName = document.field(P_REALM_NAME, OType.STRING);

    entity.setEnabled(enabled);
    entity.setUserId(userId);
    entity.setRealmName(realmName);
  }

  @Override
  protected void writeFields(final ODocument document, final AnonymousConfiguration entity) {
    document.field(P_ENABLED, entity.isEnabled());
    document.field(P_USER_ID, entity.getUserId());
    document.field(P_REALM_NAME, entity.getRealmName());
  }

  @Override
  @Nullable
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind)  {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);
    log.debug("Emitted {} event with metadata {}", eventKind, metadata);
    switch (eventKind) {
      case CREATE:
        return new AnonymousConfigurationCreatedEvent(metadata);
      case UPDATE:
        return new AnonymousConfigurationUpdatedEvent(metadata);
      case DELETE:
        return new AnonymousConfigurationDeletedEvent(metadata);
      default:
        return null;
    }
  }
}
