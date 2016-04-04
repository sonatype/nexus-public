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
package org.sonatype.nexus.security.privilege;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.security.authz.WildcardPermission2;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CPrivilegeBuilder;

import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.Permission;

/**
 * Wildcard {@link PrivilegeDescriptor}.
 *
 * @see WildcardPermission2
 * @since 3.0
 */
@Named(WildcardPrivilegeDescriptor.TYPE)
@Singleton
public class WildcardPrivilegeDescriptor
    extends PrivilegeDescriptorSupport
{
  public static final String TYPE = "wildcard";

  public static final String P_PATTERN = "pattern";

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Wildcard")
    String name();

    @DefaultMessage("Pattern")
    String pattern();

    @DefaultMessage("The regex pattern")
    String patternHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  public WildcardPrivilegeDescriptor() {
    super(TYPE);
    this.formFields = ImmutableList.of(
        new StringTextFormField(
            P_PATTERN,
            messages.pattern(),
            messages.patternHelp(),
            FormField.MANDATORY
        )
    );
  }

  @Override
  public Permission createPermission(final CPrivilege privilege) {
    assert privilege != null;
    String pattern = readProperty(privilege, P_PATTERN);
    return new WildcardPermission2(pattern);
  }

  @Override
  public List<FormField> getFormFields() {
    return formFields;
  }

  @Override
  public String getName() {
    return messages.name();
  }

  //
  // Helpers
  //

  public static String id(final String pattern) {
    return pattern;
  }

  public static CPrivilege privilege(final String pattern) {
    return new CPrivilegeBuilder()
        .type(TYPE)
        .id(id(pattern))
        .property(P_PATTERN, pattern)
        .create();
  }
}
