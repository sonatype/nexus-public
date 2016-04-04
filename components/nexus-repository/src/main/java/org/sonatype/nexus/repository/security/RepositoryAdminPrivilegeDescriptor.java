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
package org.sonatype.nexus.repository.security;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CPrivilegeBuilder;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.apache.shiro.authz.Permission;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Repository admin {@link PrivilegeDescriptor}.
 *
 * @see RepositoryAdminPermission
 * @since 3.0
 */
@Named(RepositoryAdminPrivilegeDescriptor.TYPE)
@Singleton
public class RepositoryAdminPrivilegeDescriptor
    extends PrivilegeDescriptorSupport
{
  public static final String TYPE = RepositoryAdminPermission.DOMAIN;

  public static final String P_FORMAT = "format";

  public static final String P_REPOSITORY = "repository";

  public static final String P_ACTIONS = "actions";

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Repository Admin")
    String name();

    @DefaultMessage("Format")
    String format();

    @DefaultMessage("The format(s) for the repository")
    String formatHelp();

    @DefaultMessage("Repository")
    String repository();

    @DefaultMessage("The repository name")
    String repositoryHelp();

    @DefaultMessage("Actions")
    String actions();

    @DefaultMessage("The comma-delimited list of actions")
    String actionsHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  public RepositoryAdminPrivilegeDescriptor() {
    super(TYPE);
    this.formFields = ImmutableList.of(
        new StringTextFormField(
            P_FORMAT,
            messages.format(),
            messages.formatHelp(),
            FormField.MANDATORY
        ),
        new RepositoryCombobox(
            P_REPOSITORY,
            messages.repository(),
            messages.repositoryHelp(),
            true
        ).includeAnEntryForAllRepositories(),
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
    String format = readProperty(privilege, P_FORMAT, ALL);
    String name = readProperty(privilege, P_REPOSITORY, ALL);
    List<String> actions = readListProperty(privilege, P_ACTIONS, ALL);
    return new RepositoryAdminPermission(format, name, actions);
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

  public static String id(final String format, final String name, final String... actions) {
    return String.format("nx-%s-%s-%s-%s", TYPE, format, name, Joiner.on(',').join(actions));
  }

  public static CPrivilege privilege(final String format, final String name, final String... actions) {
    checkArgument(actions.length > 0);
    return new CPrivilegeBuilder()
        .type(TYPE)
        .id(id(format, name, actions))
        .description(String.format("%s for %s repository administration", humanizeActions(actions), humanizeName(name, format)))
        .property(P_FORMAT, format)
        .property(P_REPOSITORY, name)
        .property(P_ACTIONS, actions)
        .create();
  }
}
