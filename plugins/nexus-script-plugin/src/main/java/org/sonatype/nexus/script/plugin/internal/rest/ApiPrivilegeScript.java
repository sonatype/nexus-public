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
package org.sonatype.nexus.script.plugin.internal.rest;

import java.util.Collection;

import org.sonatype.nexus.script.plugin.internal.security.ScriptPrivilegeDescriptor;
import org.sonatype.nexus.security.internal.rest.NexusSecurityApiConstants;
import org.sonatype.nexus.security.privilege.Privilege;
import org.sonatype.nexus.security.privilege.rest.ApiPrivilegeWithActions;
import org.sonatype.nexus.security.privilege.rest.PrivilegeAction;

import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @since 3.19
 */
public class ApiPrivilegeScript
    extends ApiPrivilegeWithActions
{
  public static final String SCRIPT_KEY = "name";

  @NotBlank
  @ApiModelProperty(NexusSecurityApiConstants.PRIVILEGE_SCRIPT_DESCRIPTION)
  private String scriptName;

  /**
   * for deserialization
   */
  private ApiPrivilegeScript() {
    super(ScriptPrivilegeDescriptor.TYPE);
  }

  public ApiPrivilegeScript(final String name,
                            final String description,
                            final boolean readOnly,
                            final String scriptName,
                            final Collection<PrivilegeAction> actions)
  {
    super(ScriptPrivilegeDescriptor.TYPE, name, description, readOnly, actions);
    this.scriptName = scriptName;
  }

  public ApiPrivilegeScript(final Privilege privilege) {
    super(privilege);
    scriptName = privilege.getPrivilegeProperty(SCRIPT_KEY);
  }

  public void setScriptName(final String scriptName) {
    this.scriptName = scriptName;
  }

  public String getScriptName() {
    return scriptName;
  }

  @Override
  protected Privilege doAsPrivilege(final Privilege privilege) {
    super.doAsPrivilege(privilege);
    privilege.addProperty(SCRIPT_KEY, scriptName);
    return privilege;
  }

  @Override
  protected String doAsActionString() {
    return toBreadRunActionString();
  }
}
