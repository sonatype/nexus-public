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
package org.sonatype.nexus.security.anonymous.rest;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;
import org.sonatype.nexus.rest.ValidationErrorsException;
import org.sonatype.nexus.security.anonymous.AnonymousConfiguration;
import org.sonatype.nexus.security.anonymous.AnonymousManager;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 3.24
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class AnonymousAccessApiResource
    extends ComponentSupport
    implements Resource, AnonymousAccessApiResourceDoc
{
  private final AnonymousManager anonymousManager;

  private final RealmSecurityManager realmSecurityManager;

  @Inject
  public AnonymousAccessApiResource(final AnonymousManager anonymousManager, final RealmSecurityManager realmSecurityManager) {
    this.anonymousManager = checkNotNull(anonymousManager);
    this.realmSecurityManager = checkNotNull(realmSecurityManager);
  }

  @GET
  @RequiresPermissions("nexus:settings:read")
  @Override
  public AnonymousAccessSettingsXO read() {
    return new AnonymousAccessSettingsXO(anonymousManager.getConfiguration());
  }

  @PUT
  @RequiresAuthentication
  @RequiresPermissions("nexus:settings:update")
  @Override
  public AnonymousAccessSettingsXO update(@Valid final AnonymousAccessSettingsXO anonymousXO) {
    Realm realm = validate(anonymousXO);
    AnonymousConfiguration configuration = anonymousManager.newConfiguration();
    configuration.setEnabled(anonymousXO.isEnabled());
    configuration.setUserId(anonymousXO.getUserId());
    configuration.setRealmName(realm.getName());
    anonymousManager.setConfiguration(configuration);
    return new AnonymousAccessSettingsXO(anonymousManager.getConfiguration());
  }

  Realm validate(AnonymousAccessSettingsXO settings) {
    return realmSecurityManager.getRealms().stream()
        .filter(realm -> realm.getName().equals(settings.getRealmName()))
        .findFirst().orElseThrow(() -> new ValidationErrorsException("realmName", "Realm does not exist"));
  }
}
