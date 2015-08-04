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
package org.sonatype.nexus.test.utils;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.sonatype.security.model.CRole;

/**
 * This only works for equals...
 */
public class RoleComparator
    implements Comparator<CRole>
{

  public int compare(CRole role1, CRole role2) {

    // quick outs
    if (role1 == null || role2 == null) {
      return -1;
    }

    if (role1 == role2 || role1.equals(role2)) {
      return 0;
    }

    if (role1.getDescription() == null) {
      if (role2.getDescription() != null) {
        return -1;
      }
    }
    else if (!role1.getDescription().equals(role2.getDescription())) {
      return -1;
    }
    if (role1.getId() == null) {
      if (role2.getId() != null) {
        return -1;
      }
    }
    else if (!role1.getId().equals(role2.getId())) {
      return -1;
    }
        /*
         * if ( role1.getModelEncoding() == null ) { if ( role2.getModelEncoding() != null ) return -1; } else if (
         * !role1.getModelEncoding().equals( role2.getModelEncoding() ) ) return -1;
         */
    if (role1.getName() == null) {
      if (role2.getName() != null) {
        return -1;
      }
    }
    else if (!role1.getName().equals(role2.getName())) {
      return -1;
    }
    if (role1.getPrivileges() == null) {
      if (role2.getPrivileges() != null) {
        return -1;
      }
    }

    Set<String> role1Privileges = new HashSet<String>(role1.getPrivileges());
    Set<String> role2Privileges = new HashSet<String>(role2.getPrivileges());

    if (!(role1Privileges.size() == role2Privileges.size() && role1Privileges.containsAll(role2Privileges))) {
      return -1;
    }
    if (role1.getRoles() == null || role1.getRoles().isEmpty()) {
      if (role2.getRoles() == null || role2.getRoles().isEmpty()) {
        return 0;
      }
      else {
        return -1;
      }
    }

    Set<String> role1Roles = new HashSet<String>(role1.getRoles());
    Set<String> role2Roles = new HashSet<String>(role2.getRoles());

    if (!(role1Roles.size() == role2Roles.size() && role1Roles.containsAll(role2Roles))) {
      return -1;
    }
    return 0;
  }
}
