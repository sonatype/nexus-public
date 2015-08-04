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
package org.sonatype.security.realms.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.configuration.validation.ValidationContext;

public class SecurityValidationContext
    implements ValidationContext
{
  private List<String> existingPrivilegeIds;

  private List<String> existingRoleIds;

  private List<String> existingUserIds;

  private Map<String, String> existingEmailMap;

  private Map<String, List<String>> roleContainmentMap;

  private Map<String, String> existingRoleNameMap;

  private Map<String, List<String>> existingUserRoleMap;

  public void addExistingPrivilegeIds() {
    if (this.existingPrivilegeIds == null) {
      this.existingPrivilegeIds = new ArrayList<String>();
    }
  }

  public void addExistingRoleIds() {
    if (this.existingRoleIds == null) {
      this.existingRoleIds = new ArrayList<String>();
    }

    if (this.roleContainmentMap == null) {
      this.roleContainmentMap = new HashMap<String, List<String>>();
    }

    if (this.existingRoleNameMap == null) {
      this.existingRoleNameMap = new HashMap<String, String>();
    }

    if (this.existingUserRoleMap == null) {
      this.existingUserRoleMap = new HashMap<String, List<String>>();
    }
  }

  public void addExistingUserIds() {
    if (this.existingUserIds == null) {
      this.existingUserIds = new ArrayList<String>();
    }

    if (this.existingEmailMap == null) {
      this.existingEmailMap = new HashMap<String, String>();
    }
  }

  public List<String> getExistingPrivilegeIds() {
    return existingPrivilegeIds;
  }

  public List<String> getExistingRoleIds() {
    return existingRoleIds;
  }

  public List<String> getExistingUserIds() {
    return existingUserIds;
  }

  public Map<String, String> getExistingEmailMap() {
    return existingEmailMap;
  }

  public Map<String, List<String>> getRoleContainmentMap() {
    return roleContainmentMap;
  }

  public Map<String, String> getExistingRoleNameMap() {
    return existingRoleNameMap;
  }

  public Map<String, List<String>> getExistingUserRoleMap() {
    return existingUserRoleMap;
  }

}
