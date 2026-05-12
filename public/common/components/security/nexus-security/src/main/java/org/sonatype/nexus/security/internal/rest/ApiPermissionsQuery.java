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
package org.sonatype.nexus.security.internal.rest;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

/**
 * AND-combined filters for {@link ApiPermissionsResource} query parameters.
 */
public final class ApiPermissionsQuery
{
  private ApiPermissionsQuery() {
  }

  public static List<ApiEndpointPermission> apply(
      final List<ApiEndpointPermission> source,
      @Nullable final String method,
      @Nullable final String pathSubstring,
      @Nullable final String permissionSubstring,
      @Nullable final String tagExact)
  {
    List<ApiEndpointPermission> out = new ArrayList<>();
    for (ApiEndpointPermission row : source) {
      if (StringUtils.isNotBlank(method)
          && !row.getHttpMethod().equalsIgnoreCase(method.trim())) {
        continue;
      }
      if (StringUtils.isNotBlank(pathSubstring)
          && !StringUtils.containsIgnoreCase(row.getPathPattern(), pathSubstring.trim())) {
        continue;
      }
      if (StringUtils.isNotBlank(permissionSubstring) && !matchesPermission(row, permissionSubstring.trim())) {
        continue;
      }
      if (StringUtils.isNotBlank(tagExact)) {
        if (row.getTag() == null
            || !row.getTag().equalsIgnoreCase(tagExact.trim())) {
          continue;
        }
      }
      out.add(row);
    }
    return out;
  }

  private static boolean matchesPermission(
      final ApiEndpointPermission row,
      final String permissionNeedle)
  {
    if (row.getPermissions() == null) {
      return false;
    }
    for (ApiPermissionRequirement req : row.getPermissions()) {
      if (req.getPermission() != null
          && StringUtils.containsIgnoreCase(req.getPermission(), permissionNeedle)) {
        return true;
      }
    }
    return false;
  }
}
