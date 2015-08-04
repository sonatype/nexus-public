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

import java.util.Comparator;

import org.sonatype.security.rest.model.RoleAndPrivilegeListResource;

public final class RoleAndPrivilegeListResourceComparator
    implements Comparator<RoleAndPrivilegeListResource>
{
  private final String sort;

  private final String dir;

  public static final String SORT_NAME = "name";

  public static final String SORT_DESCRIPTION = "description";

  public static final String DIR_ASC = "ASC";

  public static final String DIR_DESC = "DESC";

  public RoleAndPrivilegeListResourceComparator(String sort, String dir) {
    this.sort = sort;
    this.dir = dir;
  }

  public int compare(RoleAndPrivilegeListResource o1, RoleAndPrivilegeListResource o2) {
    // always sort by roles first, then privileges
    if (o1.getType().equals("role") && o2.getType().equals("privilege")) {
      return -1;
    }
    else if (o1.getType().equals("privilege") && o2.getType().equals("role")) {
      return 1;
    }

    if (SORT_NAME.equals(sort)) {
      return doCompare(o1.getName(), o2.getName(), dir);
    }
    else if (SORT_DESCRIPTION.equals(sort)) {
      return doCompare(o1.getDescription(), o2.getDescription(), dir);
    }

    return 0;
  }

  private int doCompare(String value1, String value2, String dir) {
    if (DIR_DESC.equals(dir)) {
      return value2.compareToIgnoreCase(value1);
    }
    else {
      return value1.compareToIgnoreCase(value2);
    }
  }
}
