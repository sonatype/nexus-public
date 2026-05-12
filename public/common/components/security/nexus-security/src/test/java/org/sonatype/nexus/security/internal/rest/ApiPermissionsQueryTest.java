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

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class ApiPermissionsQueryTest
{
  private final List<ApiEndpointPermission> sample = List.of(
      new ApiEndpointPermission(
          "GET",
          "/service/rest/v1/repositories",
          List.of(new ApiPermissionRequirement("nexus:repository-admin:*:*:read", "AND")),
          "List",
          "Repository Management",
          true),
      new ApiEndpointPermission(
          "DELETE",
          "/service/rest/v1/repositories/x",
          List.of(new ApiPermissionRequirement("nexus:repositories:delete", "AND")),
          "Delete",
          "Repository Management",
          true));

  @Test
  void filtersByMethodAndPathAndPermissionAndCombined() {
    assertThat(ApiPermissionsQuery.apply(sample, "GET", null, null, null), hasSize(1));
    assertThat(ApiPermissionsQuery.apply(sample, null, "repositories", null, null), hasSize(2));
    assertThat(ApiPermissionsQuery.apply(sample, null, null, "delete", null), hasSize(1));
    assertThat(
        ApiPermissionsQuery.apply(sample, "DELETE", "repositories", "nexus:repositories", null),
        hasSize(1));
  }

  @Test
  void filtersByTagExactCaseInsensitive() {
    assertThat(ApiPermissionsQuery.apply(sample, null, null, null, "repository management"), hasSize(2));
    assertThat(ApiPermissionsQuery.apply(sample, null, null, null, "Other"), hasSize(0));
  }
}
