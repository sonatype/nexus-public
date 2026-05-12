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

import org.sonatype.nexus.rest.Resource;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.junit.jupiter.api.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

class EndpointPermissionScannerTest
{
  @Api(tags = "Widget API")
  @Path("v1/widgets")
  public static class SampleResource
      implements Resource
  {
    @GET
    @Path("{id}")
    @RequiresAuthentication
    @RequiresPermissions("nexus:widgets:read")
    @ApiOperation("Get widget")
    public String getWidget(final String id) {
      return id;
    }
  }

  @Path("v1/or")
  public static class OrPermissionsResource
      implements Resource
  {
    @GET
    @RequiresPermissions(value = {"nexus:a:read", "nexus:b:read"}, logical = Logical.OR)
    public String either() {
      return "x";
    }
  }

  @Test
  void scanJoinsClassAndMethodPaths() {
    List<ApiEndpointPermission> rows = EndpointPermissionScanner.scan(List.of(SampleResource.class));

    assertThat(rows, hasSize(1));
    ApiEndpointPermission row = rows.get(0);
    assertThat(row.getHttpMethod(), is("GET"));
    assertThat(row.getPathPattern(), is("/service/rest/v1/widgets/{id}"));
    assertThat(row.getPermissions(), hasSize(1));
    assertThat(row.getPermissions().get(0).getPermission(), is("nexus:widgets:read"));
    assertThat(row.getPermissions().get(0).getLogical(), is("AND"));
    assertThat(row.getDescription(), is("Get widget"));
    assertThat(row.getTag(), is("Widget API"));
    assertThat(row.isAuthenticated(), is(true));
  }

  @Test
  void scanUsesOrLogical() {
    List<ApiEndpointPermission> rows = EndpointPermissionScanner.scan(List.of(OrPermissionsResource.class));

    assertThat(rows, hasSize(1));
    assertThat(rows.get(0).getPermissions().get(0).getLogical(), is("OR"));
    assertThat(rows.get(0).getPermissions().get(1).getLogical(), is("OR"));
  }
}
