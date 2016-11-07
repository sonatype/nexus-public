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
package org.sonatype.nexus.internal.email;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.entity.EntityEvent;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.AttachedEntityMetadata;
import org.sonatype.nexus.orient.entity.SingletonEntityAdapter;
import org.sonatype.nexus.security.PasswordHelper;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link EmailConfiguration} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class EmailConfigurationEntityAdapter
    extends SingletonEntityAdapter<EmailConfiguration>
{
  private static final String DB_CLASS = new OClassNameBuilder()
      .type("email")
      .build();

  private static final String P_ENABLED = "enabled";

  private static final String P_HOST = "host";

  private static final String P_PORT = "port";

  private static final String P_USERNAME = "username";

  private static final String P_PASSWORD = "password";

  private static final String P_FROM_ADDRESS = "from_address";

  private static final String P_SUBJECT_PREFIX = "subject_prefix";

  private static final String P_START_TLS_ENABLED = "start_tls_enabled";

  private static final String P_START_TLS_REQUIRED = "start_tls_required";

  private static final String P_SSL_ON_CONNECT_ENABLED = "ssl_on_connect_enabled";

  private static final String P_SSL_CHECK_SERVER_IDENTITY_ENABLED = "ssl_check_server_identity_enabled";

  private static final String P_NEXUS_TRUST_STORE_ENABLED = "nexus_trust_store_enabled";

  private final PasswordHelper passwordHelper;

  @Inject
  public EmailConfigurationEntityAdapter(final PasswordHelper passwordHelper) throws Exception {
    super(DB_CLASS);
    this.passwordHelper = checkNotNull(passwordHelper);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_ENABLED, OType.BOOLEAN);
    type.createProperty(P_HOST, OType.STRING);
    type.createProperty(P_PORT, OType.INTEGER);
    type.createProperty(P_USERNAME, OType.STRING);
    type.createProperty(P_PASSWORD, OType.STRING);
    type.createProperty(P_FROM_ADDRESS, OType.STRING);
    type.createProperty(P_SUBJECT_PREFIX, OType.STRING);
    type.createProperty(P_START_TLS_ENABLED, OType.BOOLEAN);
    type.createProperty(P_START_TLS_REQUIRED, OType.BOOLEAN);
    type.createProperty(P_SSL_ON_CONNECT_ENABLED, OType.BOOLEAN);
    type.createProperty(P_SSL_CHECK_SERVER_IDENTITY_ENABLED, OType.BOOLEAN);
    type.createProperty(P_NEXUS_TRUST_STORE_ENABLED, OType.BOOLEAN);
  }

  @Override
  protected EmailConfiguration newEntity() {
    return new EmailConfiguration();
  }

  @Override
  protected void readFields(final ODocument document, final EmailConfiguration entity) {
    boolean enabled = document.field(P_ENABLED, OType.BOOLEAN);
    String host = document.field(P_HOST, OType.STRING);
    int port = document.field(P_PORT, OType.INTEGER);
    String username = document.field(P_USERNAME, OType.STRING);
    String password = document.field(P_PASSWORD, OType.STRING);
    String fromAddress = document.field(P_FROM_ADDRESS, OType.STRING);
    String subjectPrefix = document.field(P_SUBJECT_PREFIX, OType.STRING);
    boolean startTlsEnabled = document.field(P_START_TLS_ENABLED, OType.BOOLEAN);
    boolean startTlsRequired = document.field(P_START_TLS_REQUIRED, OType.BOOLEAN);
    boolean sslOnConnectEnabled = document.field(P_SSL_ON_CONNECT_ENABLED, OType.BOOLEAN);
    boolean sslCheckServerIdentityEnabled = document.field(P_SSL_CHECK_SERVER_IDENTITY_ENABLED, OType.BOOLEAN);
    boolean nexusTrustStoreEnabled = document.field(P_NEXUS_TRUST_STORE_ENABLED, OType.BOOLEAN);

    entity.setEnabled(enabled);
    entity.setHost(host);
    entity.setPort(port);
    entity.setUsername(username);
    entity.setPassword(passwordHelper.decrypt(password));
    entity.setFromAddress(fromAddress);
    entity.setSubjectPrefix(subjectPrefix);
    entity.setStartTlsEnabled(startTlsEnabled);
    entity.setStartTlsRequired(startTlsRequired);
    entity.setSslOnConnectEnabled(sslOnConnectEnabled);
    entity.setSslCheckServerIdentityEnabled(sslCheckServerIdentityEnabled);
    entity.setNexusTrustStoreEnabled(nexusTrustStoreEnabled);
  }

  @Override
  protected void writeFields(final ODocument document, final EmailConfiguration entity) {
    document.field(P_ENABLED, entity.isEnabled());
    document.field(P_HOST, entity.getHost());
    document.field(P_PORT, entity.getPort());
    document.field(P_USERNAME, entity.getUsername());
    document.field(P_PASSWORD, passwordHelper.encrypt(entity.getPassword()));
    document.field(P_FROM_ADDRESS, entity.getFromAddress());
    document.field(P_SUBJECT_PREFIX, entity.getSubjectPrefix());
    document.field(P_START_TLS_ENABLED, entity.isStartTlsEnabled());
    document.field(P_START_TLS_REQUIRED, entity.isStartTlsRequired());
    document.field(P_SSL_ON_CONNECT_ENABLED, entity.isSslOnConnectEnabled());
    document.field(P_SSL_CHECK_SERVER_IDENTITY_ENABLED, entity.isSslCheckServerIdentityEnabled());
    document.field(P_NEXUS_TRUST_STORE_ENABLED, entity.isNexusTrustStoreEnabled());
  }

  @Nullable
  @Override
  public EntityEvent newEvent(final ODocument document, final EventKind eventKind) {
    EntityMetadata metadata = new AttachedEntityMetadata(this, document);
    log.debug("Emitted {} event with metadata {}", eventKind, metadata);
    switch (eventKind) {
      case CREATE:
        return new EmailConfigurationCreatedEvent(metadata);
      case UPDATE:
        return new EmailConfigurationUpdatedEvent(metadata);
      case DELETE:
        return new EmailConfigurationDeletedEvent(metadata);
      default:
        return null;
    }
  }
}
