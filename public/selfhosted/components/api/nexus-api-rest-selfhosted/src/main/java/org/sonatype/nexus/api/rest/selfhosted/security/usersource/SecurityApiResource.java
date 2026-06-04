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

package org.sonatype.nexus.api.rest.selfhosted.security.usersource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.sonatype.nexus.api.rest.selfhosted.security.usersource.model.ApiUserSource;
import org.sonatype.nexus.common.QualifierUtil;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.security.user.ConfiguredUsersUserManager;
import org.sonatype.nexus.security.user.UserManager;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 3.17
 */
@Produces(MediaType.APPLICATION_JSON)
public class SecurityApiResource
    implements Resource, SecurityApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final Map<String, UserManager> userManagers;

  @Autowired
  public SecurityApiResource(final List<UserManager> userManagersList) {
    this.userManagers = QualifierUtil.buildQualifierBeanMap(userManagersList);
  }

  @Override
  @GET
  @Path("user-sources")
  @RequiresAuthentication
  @RequiresPermissions("nexus:users:read")
  public List<ApiUserSource> getUserSources() {
    return userManagers.values()
        .stream()
        .filter(um -> !ConfiguredUsersUserManager.SOURCE.equals(um.getSource()))
        .map(um -> new ApiUserSource(um))
        .collect(Collectors.toList());
  }
}
