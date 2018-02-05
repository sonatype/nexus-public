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
import org.sonatype.nexus.formfields.SelectorComboFormField;
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
 * Repository selector {@link PrivilegeDescriptor}.
 *
 * @see RepositoryContentSelectorPermission
 * @since 3.1
 */
@Named(RepositoryContentSelectorPrivilegeDescriptor.TYPE)
@Singleton
public class RepositoryContentSelectorPrivilegeDescriptor
    extends PrivilegeDescriptorSupport
{
  public static final String TYPE = RepositoryContentSelectorPermission.DOMAIN;

  public static final String P_CONTENT_SELECTOR = "contentSelector";

  public static final String P_REPOSITORY = "repository";

  public static final String P_ACTIONS = "actions";

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Repository Content Selector")
    String name();

    @DefaultMessage("Content Selector")
    String contentSelector();

    @DefaultMessage("The content selector for the repository")
    String contentSelectorHelp();

    @DefaultMessage("Repository")
    String repository();

    @DefaultMessage("The repository or repositories to grant access")
    String repositoryHelp();

    @DefaultMessage("Actions")
    String actions();

    @DefaultMessage("The comma-delimited list of actions")
    String actionsHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  public RepositoryContentSelectorPrivilegeDescriptor() {
    super(TYPE);
    this.formFields = ImmutableList.of(
        new SelectorComboFormField(
            P_CONTENT_SELECTOR,
            messages.contentSelector(),
            messages.contentSelectorHelp(),
            FormField.MANDATORY
        ),
        new RepositoryCombobox(
            P_REPOSITORY,
            messages.repository(),
            messages.repositoryHelp(),
            true
        ).includeEntriesForAllFormats(),
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
    String contentSelector = readProperty(privilege, P_CONTENT_SELECTOR, ALL);
    String name = readProperty(privilege, P_REPOSITORY, ALL);
    List<String> actions = readListProperty(privilege, P_ACTIONS, ALL);
    RepositorySelector selector = RepositorySelector.fromSelector(name);
    return new RepositoryContentSelectorPermission(contentSelector, selector.getFormat(), selector.getName(), actions);
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

  public static String id(final String contentSelector,
                          final String format,
                          final String name,
                          final String... actions)
  {
    return String.format("nx-%s-%s-%s-%s-%s", TYPE, contentSelector, format, name, Joiner.on(',').join(actions));
  }

  public static CPrivilege privilege(final String contentSelector,
                                     final String format,
                                     final String name,
                                     final String... actions)
  {
    checkArgument(actions.length > 0);
    RepositorySelector selector = RepositorySelector.fromNameAndFormat(name, format);
    return new CPrivilegeBuilder()
        .type(TYPE)
        .id(id(contentSelector, format, name, actions))
        .description(String
            .format("%s for %s repository content selector %s", humanizeActions(actions), selector.humanizeSelector(),
                contentSelector))
        .property(P_CONTENT_SELECTOR, contentSelector)
        .property(P_REPOSITORY, selector.toSelector())
        .property(P_ACTIONS, actions)
        .create();
  }
}
