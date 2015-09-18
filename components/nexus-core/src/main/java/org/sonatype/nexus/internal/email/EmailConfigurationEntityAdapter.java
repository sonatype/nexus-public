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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.SingletonEntityAdapter;

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

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
      .type(EmailConfiguration.class)
      .build();

  private static final String P_ENABLED = "enabled";

  private static final String P_HOST = "host";

  private static final String P_PORT = "port";

  private static final String P_USERNAME = "username";

  private static final String P_PASSWORD = "password";

  private static final String P_FROM_ADDRESS = "from_address";

  private static final String P_SUBJECT_PREFIX = "subject_prefix";

  public EmailConfigurationEntityAdapter() {
    super(DB_CLASS);
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

    entity.setEnabled(enabled);
    entity.setHost(host);
    entity.setPort(port);
    entity.setUsername(username);
    entity.setPassword(password);
    entity.setFromAddress(fromAddress);
    entity.setSubjectPrefix(subjectPrefix);
  }

  @Override
  protected void writeFields(final ODocument document, final EmailConfiguration entity) {
    document.field(P_ENABLED, entity.isEnabled());
    document.field(P_HOST, entity.getHost());
    document.field(P_PORT, entity.getPort());
    document.field(P_USERNAME, entity.getUsername());
    document.field(P_PASSWORD, entity.getPassword());
    document.field(P_FROM_ADDRESS, entity.getFromAddress());
    document.field(P_SUBJECT_PREFIX, entity.getSubjectPrefix());
  }
}
