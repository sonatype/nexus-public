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
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CPrivilegeBuilder;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.Permission;

/**
 * Application {@link PrivilegeDescriptor}.
 *
 * @see ApplicationPermission
 * @since 3.0
 */
@Named(ApplicationPrivilegeDescriptor.TYPE)
@Singleton
public class ApplicationPrivilegeDescriptor
    extends PrivilegeDescriptorSupport
{
  public static final String TYPE = "application";

  public static final String P_DOMAIN = "domain";

  public static final String P_ACTIONS = "actions";

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Application")
    String name();

    @DefaultMessage("Domain")
    String domain();

    @DefaultMessage("The domain for the privilege")
    String domainHelp();

    @DefaultMessage("Actions")
    String actions();

    @DefaultMessage("The comma-delimited list of actions")
    String actionsHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  public ApplicationPrivilegeDescriptor() {
    super(TYPE);
    this.formFields = ImmutableList.of(
        new StringTextFormField(
            P_DOMAIN,
            messages.domain(),
            messages.domainHelp(),
            FormField.MANDATORY
        ),
        new StringTextFormField(
            P_ACTIONS,
            messages.actions(),
            messages.actionsHelp(),
            FormField.MANDATORY
        )
    );
  }

  @Override
  public Permission createPermission(final CPrivilege privilege) {
    assert privilege != null;
    String domain = readProperty(privilege, P_DOMAIN, ALL);
    List<String> actions = readListProperty(privilege, P_ACTIONS, ALL);
    return new ApplicationPermission(domain, actions);
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

  public static String id(final String domain, final String... actions) {
    return String.format("%s-%s", domain, Joiner.on(',').join(actions));
  }

  public static CPrivilege privilege(final String domain, final String... actions) {
    return new CPrivilegeBuilder()
        .type(TYPE)
        .id(id(domain, actions))
        .property(P_DOMAIN, domain)
        .property(P_ACTIONS, actions)
        .create();
  }
}
