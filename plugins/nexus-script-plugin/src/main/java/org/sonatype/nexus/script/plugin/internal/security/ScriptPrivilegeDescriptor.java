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
package org.sonatype.nexus.script.plugin.internal.security;

import com.google.common.base.Joiner;
import org.apache.shiro.authz.Permission;
import org.sonatype.goodies.i18n.I18N;
import org.sonatype.goodies.i18n.MessageBundle;
import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.SetOfCheckboxesFormField;
import org.sonatype.nexus.formfields.StringTextFormField;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.script.ScriptManager;
import org.sonatype.nexus.script.plugin.internal.rest.ApiPrivilegeScript;
import org.sonatype.nexus.script.plugin.internal.rest.ApiPrivilegeScriptRequest;
import org.sonatype.nexus.security.config.CPrivilege;
import org.sonatype.nexus.security.config.CPrivilegeBuilder;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptorSupport;
import org.sonatype.nexus.security.privilege.rest.PrivilegeAction;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.FeatureFlags.REACT_PRIVILEGES_NAMED;

/**
 * Descriptor for {@link ScriptPermission}
 * 
 * @since 3.0
 */
@Named("script")
@Singleton
public class ScriptPrivilegeDescriptor
    extends PrivilegeDescriptorSupport<ApiPrivilegeScript, ApiPrivilegeScriptRequest>
{
  public static final String INVALID_SCRIPT = "\"Invalid script '%s' supplied.\"";

  public static final String TYPE = ScriptPermission.DOMAIN;

  public static final String P_NAME = "name";

  public static final String P_ACTIONS = "actions";

  private interface Messages
      extends MessageBundle
  {
    @DefaultMessage("Script")
    String name();

    @DefaultMessage("Script Name")
    String scriptName();

    @DefaultMessage("The name of the script")
    String scriptNameHelp();

    @DefaultMessage("Actions")
    String actions();

    @DefaultMessage("A comma-delimited list (without whitespace) of actions allowed with this privilege; options include browse, read, edit, add, delete, run, and a wildcard (*) <a href=\"https://links.sonatype.com/products/nxrm3/docs/privileges\" target='_blank'>Help</a>")
    String actionsHelp();

    @DefaultMessage("The actions you wish to allow")
    String actionsCheckboxesHelp();
  }

  private static final Messages messages = I18N.create(Messages.class);

  private final List<FormField> formFields;

  private final ScriptManager scriptManager;

  private static final String P_OPTIONS = "options";

  @Inject
  public ScriptPrivilegeDescriptor(
      final ScriptManager scriptManager,
      @Named(REACT_PRIVILEGES_NAMED) final boolean isReactPrivileges)
  {
    super(TYPE);
    this.formFields = List.of(
        new StringTextFormField(
            P_NAME,
            messages.scriptName(),
            messages.scriptNameHelp(),
            FormField.MANDATORY),
        isReactPrivileges
            ? new SetOfCheckboxesFormField(
                P_ACTIONS,
                messages.actions(),
                messages.actionsCheckboxesHelp(),
                FormField.MANDATORY).withAttribute(P_OPTIONS, PrivilegeAction.getBreadRunActionStrings())
            : new StringTextFormField(
                P_ACTIONS,
                messages.actions(),
                messages.actionsHelp(),
                FormField.MANDATORY,
                "(^(browse|read|edit|add|delete|run)(,(browse|read|edit|add|delete|run)){0,5}$)|(^\\*$)"));
    this.scriptManager = checkNotNull(scriptManager);
  }

  @Override
  public Permission createPermission(final CPrivilege privilege) {
    if (privilege == null) {
      throw new IllegalArgumentException("Privilege cannot be null");
    }
    return new ScriptPermission(readProperty(privilege, P_NAME, ALL), readListProperty(privilege, P_ACTIONS, ALL));
  }

  @Override
  public List<FormField> getFormFields() {
    return formFields;
  }

  @Override
  public String getName() {
    return messages.name();
  }

  public static CPrivilege privilege(final String name, final String... actions) {
    return new CPrivilegeBuilder()
        .type(TYPE)
        .id(id(name, actions))
        .description(humanizeActions(actions) + " for script: " + name)
        .property(P_NAME, name)
        .property(P_ACTIONS, actions)
        .create();
  }

  public static String id(final String name, final String... actions) {
    checkArgument(actions.length > 0);
    return "nx-" + TYPE + "-" + name + "-" + Joiner.on(',').join(actions);
  }

  @Override
  public ApiPrivilegeScript createApiPrivilegeImpl(final Privilege privilege) {
    return new ApiPrivilegeScript(privilege);
  }

  @Override
  public void validate(final ApiPrivilegeScriptRequest apiPrivilege) {
    validateActions(apiPrivilege, PrivilegeAction.getBreadRunActions());
    validateScript(apiPrivilege.getScriptName());
  }

  private void validateScript(final String scriptName) {
    if (scriptManager.get(scriptName) == null) {
      throw new WebApplicationMessageException(Response.Status.BAD_REQUEST,
          String.format(INVALID_SCRIPT, scriptName), MediaType.APPLICATION_JSON);
    }
  }
}
