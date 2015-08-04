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
package org.sonatype.security.rest.roles;

import java.util.List;
import java.util.regex.Pattern;

import org.sonatype.security.rest.model.RoleAndPrivilegeListFilterResourceRequest;
import org.sonatype.security.rest.model.RoleAndPrivilegeListResource;

import org.apache.commons.lang.StringUtils;

public class FilterRequest
{
  private final boolean showPrivileges;

  private final boolean showRoles;

  private final boolean showExternalRoles;

  private final boolean onlySelected;

  private final String text;

  private final List<String> roleIds;

  private final List<String> privilegeIds;

  private final List<String> hiddenRoleIds;

  private final List<String> hiddenPrivilegeIds;

  private final String userId;

  public FilterRequest(RoleAndPrivilegeListFilterResourceRequest request) {
    this.showPrivileges = !request.getData().isNoPrivileges();
    this.showRoles = !request.getData().isNoRoles();
    this.showExternalRoles = !request.getData().isNoExternalRoles();
    this.onlySelected = request.getData().isOnlySelected();
    this.text = request.getData().getName();
    this.roleIds = request.getData().getSelectedRoleIds();
    this.privilegeIds = request.getData().getSelectedPrivilegeIds();
    this.hiddenRoleIds = request.getData().getHiddenRoleIds();
    this.hiddenPrivilegeIds = request.getData().getHiddenPrivilegeIds();
    this.userId = request.getData().getUserId();
  }

  public boolean isShowPrivileges() {
    return showPrivileges;
  }

  public boolean isShowRoles() {
    return showRoles;
  }

  public boolean isShowExternalRoles() {
    return showExternalRoles;
  }

  public boolean isOnlySelected() {
    return onlySelected;
  }

  public String getText() {
    return text;
  }

  public List<String> getRoleIds() {
    return roleIds;
  }

  public List<String> getPrivilegeIds() {
    return privilegeIds;
  }

  public List<String> getHiddenRoleIds() {
    return hiddenRoleIds;
  }

  public List<String> getHiddenPrivilegeIds() {
    return hiddenPrivilegeIds;
  }

  public String getUserId() {
    return userId;
  }

  public boolean applies(RoleAndPrivilegeListResource resource) {
    if (resource != null) {
      if (resource.getType().equals("role")) {
        if (((isShowRoles() && !resource.isExternal() && !(getUserId() != null && getRoleIds().isEmpty())) ||
            (isShowExternalRoles() && resource.isExternal()))
            && (!getHiddenRoleIds().contains(resource.getId()))
            && (resource.isExternal() ||
            (((getRoleIds().isEmpty() && !isOnlySelected()) || getRoleIds().contains(resource.getId()))))
            && (StringUtils.isEmpty(getText()) || Pattern.compile(Pattern.quote(getText()),
            Pattern.CASE_INSENSITIVE).matcher(resource.getName()).find())) {
          return true;
        }
      }
      else if (resource.getType().equals("privilege")) {
        if (isShowPrivileges()
            && (!getHiddenPrivilegeIds().contains(resource.getId()))
            && ((getPrivilegeIds().isEmpty() && !isOnlySelected()) || getPrivilegeIds().contains(resource.getId()))
            && (StringUtils.isEmpty(getText()) || Pattern.compile(Pattern.quote(getText()),
            Pattern.CASE_INSENSITIVE).matcher(resource.getName()).find())) {
          return true;
        }
      }
    }

    return false;
  }
}
