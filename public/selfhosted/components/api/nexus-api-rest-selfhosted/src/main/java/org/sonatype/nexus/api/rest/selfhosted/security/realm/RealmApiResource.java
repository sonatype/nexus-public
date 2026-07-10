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
package org.sonatype.nexus.api.rest.selfhosted.security.realm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.sonatype.nexus.api.rest.selfhosted.security.realm.model.RealmApiXO;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.WebApplicationMessageException;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.realm.SecurityRealm;

import com.google.common.base.Predicates;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * REST API for managing NXRM security realms
 *
 * @since 3.20
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RealmApiResource
    implements RealmApiResourceDoc, Resource
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final RealmManager realmManager;

  @Autowired
  public RealmApiResource(final RealmManager realmManager) {
    this.realmManager = checkNotNull(realmManager);
  }

  @Path("available")
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:read")
  @Override
  public List<RealmApiXO> getRealms() {
    return realmManager.getAvailableRealms().stream().map(RealmApiXO::from).collect(Collectors.toList());
  }

  @Path("active")
  @GET
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:read")
  @Override
  public List<String> getActiveRealms() {
    return realmManager.getConfiguredRealmIds();
  }

  @Path("active")
  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Override
  public void setActiveRealms(final List<String> realmIds) {
    Set<String> knownRealmIds = new HashSet<>();
    realmManager.getAvailableRealms().stream().map(SecurityRealm::getId).forEach(knownRealmIds::add);

    List<String> unknownRealms =
        realmIds.stream().filter(Predicates.not(knownRealmIds::contains)).collect(Collectors.toList());
    if (!unknownRealms.isEmpty()) {
      log.debug("Request to set realms with unknown IDs: " + unknownRealms);
      throw new WebApplicationMessageException(Status.BAD_REQUEST, "\"Unknown realmIds: " + unknownRealms + "\"");
    }

    realmManager.setConfiguredRealmIds(realmIds);
  }
}
