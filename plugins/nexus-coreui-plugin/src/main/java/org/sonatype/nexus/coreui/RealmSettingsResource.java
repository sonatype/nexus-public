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
package org.sonatype.nexus.coreui;

import java.util.List;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.rest.Resource;

import com.google.inject.Key;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.realm.Realm;
import org.eclipse.sisu.inject.BeanLocator;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @since 3.next
 */
@Named
@Singleton
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(RealmSettingsResource.RESOURCE_PATH)
public class RealmSettingsResource
    extends ComponentSupport
    implements Resource
{
  static final String RESOURCE_PATH = "internal/ui/realms";

  private final BeanLocator beanLocator;

  @Inject
  public RealmSettingsResource(final BeanLocator beanLocator) {
    this.beanLocator = checkNotNull(beanLocator);
  }

  @GET
  @Path("/types")
  @RequiresPermissions("nexus:settings:read")
  public List<ReferenceXO> readRealmTypes() {
    return StreamSupport.stream(beanLocator.locate(Key.get(Realm.class, Named.class)).spliterator(), false)
        .map(entry -> {
          ReferenceXO xo = new ReferenceXO();
          xo.setId(((Named)entry.getKey()).value());
          xo.setName(entry.getDescription());
          return xo;
        })
        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
        .collect(toList());
  }
}
