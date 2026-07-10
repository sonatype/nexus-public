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
package org.sonatype.nexus.api.rest.selfhosted.security.eula;

import java.util.Map;
import java.util.Optional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.sonatype.nexus.api.rest.selfhosted.security.eula.model.EulaStatus;
import org.sonatype.nexus.kv.GlobalKeyValueStore;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.rest.Resource;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunityEulaApiResource
    implements Resource, CommunityEulaApiResourceDoc
{
  protected final Logger log = LoggerFactory.getLogger(getClass());

  private final String EULA_KEY = "nexus.community.eula.accepted";

  private final GlobalKeyValueStore globalKeyValueStore;

  public CommunityEulaApiResource(final GlobalKeyValueStore globalKeyValueStore) {
    this.globalKeyValueStore = globalKeyValueStore;
  }

  @Override
  @RequiresPermissions("nexus:*")
  @RequiresAuthentication
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public EulaStatus getCommunityEulaStatus() {
    Optional<NexusKeyValue> eulaStatusOptional = globalKeyValueStore.getKey(EULA_KEY);
    EulaStatus eulaStatus = new EulaStatus();
    if (eulaStatusOptional.isPresent()) {
      NexusKeyValue eulaStatusKeyValue = eulaStatusOptional.get();
      Boolean accepted = (Boolean) eulaStatusKeyValue.value().get("accepted");
      if (accepted != null) {
        eulaStatus.setAccepted(accepted);
      }
    }
    else {
      eulaStatus.setAccepted(false);
    }
    eulaStatus.setDisclaimer(EulaStatus.EXPECTED_DISCLAIMER);
    return eulaStatus;
  }

  @Override
  @POST
  @RequiresAuthentication
  @RequiresPermissions("nexus:*")
  @Consumes(MediaType.APPLICATION_JSON)
  public void setEulaAcceptedCE(EulaStatus eulaStatus) {
    boolean validDisclaimer = eulaStatus.hasExpectedDisclaimer();
    boolean isAlreadyAccepted = getCommunityEulaStatus().isAccepted();
    boolean isSettingFalse = !eulaStatus.isAccepted();

    if (!validDisclaimer) {
      throw new IllegalArgumentException("Invalid EULA disclaimer");
    }
    else if (isSettingFalse) {
      throw new IllegalArgumentException("EULA must be accepted");
    }
    else if (isAlreadyAccepted) {
      log.debug("EULA was already accepted, ignoring request");
      return;
    }

    if (eulaStatus.hasExpectedDisclaimer() && eulaStatus.isAccepted()) {
      NexusKeyValue kv = new NexusKeyValue();
      kv.setKey(EULA_KEY);
      kv.setType(ValueType.OBJECT);
      kv.setValue(Map.of("accepted", eulaStatus.isAccepted()));
      globalKeyValueStore.setKey(kv);
    }
  }
}
