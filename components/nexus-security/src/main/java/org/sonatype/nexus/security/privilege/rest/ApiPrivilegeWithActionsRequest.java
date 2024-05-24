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
package org.sonatype.nexus.security.privilege.rest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.sonatype.nexus.security.internal.rest.NexusSecurityApiConstants;
import org.sonatype.nexus.security.privilege.Privilege;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotEmpty;

/**
 * @since 3.19
 */
public abstract class ApiPrivilegeWithActionsRequest
    extends ApiPrivilegeRequest
{
  public static final String ACTIONS_KEY = "actions";

  @NotEmpty
  @ApiModelProperty(NexusSecurityApiConstants.PRIVILEGE_ACTION_DESCRIPTION)
  private Collection<PrivilegeAction> actions;

  public ApiPrivilegeWithActionsRequest() {
    super();
  }

  public ApiPrivilegeWithActionsRequest(final String name,
                                        final String description,
                                        final Collection<PrivilegeAction> actions)
  {
    super(name, description);
    this.actions = actions;
  }

  public ApiPrivilegeWithActionsRequest(final Privilege privilege) {
    super(privilege);
    String[] parts = privilege.getPrivilegeProperty(ACTIONS_KEY).split(",");
    actions = Arrays.stream(parts).map(PrivilegeAction::fromAction).filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public void setActions(Collection<PrivilegeAction> actions) {
    this.actions = actions;
  }

  public Collection<PrivilegeAction> getActions() {
    return actions;
  }

  @Override
  protected Privilege doAsPrivilege(final Privilege privilege) {
    privilege.addProperty(ACTIONS_KEY, asActionString());
    return privilege;
  }

  private String asActionString() {
    return doAsActionString();
  }

  protected abstract String doAsActionString();

  protected String toCrudActionString() {
    if (actions == null || actions.isEmpty()) {
      return null;
    }

    List<String> actionList = actions.stream().map(PrivilegeAction::getCrudAction).filter(Objects::nonNull)
        .collect(Collectors.toList());

    return String.join(",", actionList);
  }

  protected String toCrudTaskActionString() {
    if (actions == null || actions.isEmpty()) {
      return null;
    }

    List<String> actionList = actions.stream().map(PrivilegeAction::getCrudTaskActions).filter(Objects::nonNull)
        .collect(Collectors.toList());

    return String.join(",", actionList);
  }

  protected String toBreadActionString() {
    if (actions == null || actions.isEmpty()) {
      return null;
    }

    List<String> actionList = actions.stream().map(PrivilegeAction::getBreadAction).collect(Collectors.toList());

    return String.join(",", actionList);
  }

  protected String toBreadRunActionString() {
    if (actions == null || actions.isEmpty()) {
      return null;
    }

    List<String> actionList = actions.stream().map(PrivilegeAction::getBreadRunAction).collect(Collectors.toList());

    return String.join(",", actionList);
  }
}
