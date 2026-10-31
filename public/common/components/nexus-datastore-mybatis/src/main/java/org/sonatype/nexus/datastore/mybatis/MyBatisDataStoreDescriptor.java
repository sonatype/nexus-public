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
package org.sonatype.nexus.datastore.mybatis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.sonatype.nexus.common.i18n.I18N;
import org.sonatype.nexus.common.i18n.MessageBundle;
import org.sonatype.nexus.datastore.DataStoreDescriptor;
import org.sonatype.nexus.datastore.api.DataStoreConfiguration;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.formfields.TextAreaFormField;
import org.sonatype.nexus.rest.ValidationErrorsException;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.text.Strings2.isBlank;

/**
 * MyBatis {@link DataStoreDescriptor}.
 *
 * @since 3.19
 */
@Component
@Qualifier(MyBatisDataStoreDescriptor.NAME)
public class MyBatisDataStoreDescriptor
    implements DataStoreDescriptor
{
  public static final String NAME = "jdbc";

  public static final String JDBC_URL = "jdbcUrl";

  public static final String USERNAME = "username";

  public static final String PASSWORD = "password";

  public static final String SCHEMA = "schema";

  public static final String ADVANCED = "advanced";

  /*
   * An allow list of HikariCP configuration settings which are safe for security.
   * Full list - https://github.com/brettwooldridge/HikariCP/blob/dev/README.md#gear-configuration-knobs-baby
   */
  private static final Set<String> ALLOWED_ADVANCED_KEYS = Set.of(
      "maximumPoolSize",
      "minimumIdle",
      "connectionTimeout",
      "idleTimeout",
      "maxLifetime",
      "leakDetectionThreshold",
      "validationTimeout",
      "autoCommit",
      "transactionIsolation",
      "poolName",
      "keepaliveTime",
      "initializationFailTimeout");

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("JDBC URL")
    String jdbcUrlLabel();

    @DefaultMessage("Username")
    String usernameLabel();

    @DefaultMessage("Password")
    String passwordLabel();

    @DefaultMessage("Schema")
    String schemaLabel();

    @DefaultMessage("Advanced")
    String advancedLabel();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField<?>> formFields = ImmutableList.of(
      new StringTextFormField(JDBC_URL, messages.jdbcUrlLabel(), null, true),
      new StringTextFormField(USERNAME, messages.usernameLabel(), null, false),
      new PasswordFormField(PASSWORD, messages.passwordLabel(), null, false),
      new StringTextFormField(SCHEMA, messages.schemaLabel(), null, false),
      new TextAreaFormField(ADVANCED, messages.advancedLabel(), null, false));

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<FormField<?>> getFormFields() {
    return formFields;
  }

  @Override
  public void validate(final DataStoreConfiguration configuration) {
    String advanced = configuration.getAttributes().get(ADVANCED);
    if (isBlank(advanced)) {
      return;
    }
    // Parse with the same splitter MyBatisDataStore uses at runtime to inject these properties
    // into HikariCP — reusing it prevents a parser-differential bypass.
    String rejected = MyBatisDataStore.TO_MAP.split(advanced)
        .keySet()
        .stream()
        .filter(key -> !ALLOWED_ADVANCED_KEYS.contains(key))
        .sorted()
        .collect(Collectors.joining(", "));
    if (!rejected.isEmpty()) {
      throw new ValidationErrorsException().withError(
          ADVANCED,
          "The advanced configuration is invalid or contains unknown properties: " + rejected);
    }
  }
}
