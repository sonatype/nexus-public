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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.datastore.DataStoreDescriptor;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.PasswordFormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.formfields.TextAreaFormField;

import com.google.common.collect.ImmutableList;

/**
 * MyBatis {@link DataStoreDescriptor}.
 *
 * @since 3.19
 */
@Singleton
@Named(MyBatisDataStoreDescriptor.NAME)
public class MyBatisDataStoreDescriptor
    implements DataStoreDescriptor
{
  public static final String NAME = "jdbc";

  public static final String JDBC_URL = "jdbcUrl";

  public static final String USERNAME = "username";

  public static final String PASSWORD = "password";

  public static final String SCHEMA = "schema";

  public static final String ADVANCED = "advanced";

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
      new TextAreaFormField(ADVANCED, messages.advancedLabel(), null, false)
  );

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<FormField<?>> getFormFields() {
    return formFields;
  }
}
