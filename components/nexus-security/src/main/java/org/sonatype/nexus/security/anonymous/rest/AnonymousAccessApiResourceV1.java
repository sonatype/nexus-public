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
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.internal.rest.SecurityApiResourceV1;

import org.apache.shiro.mgt.RealmSecurityManager;

import static org.sonatype.nexus.security.anonymous.rest.AnonymousAccessApiResourceV1.RESOURCE_URI;

/**
 * @since 3.26
 */
@Named
@Singleton
@Path(RESOURCE_URI)
public class AnonymousAccessApiResourceV1
    extends AnonymousAccessApiResource
{
  static final String RESOURCE_URI = SecurityApiResourceV1.V1_RESOURCE_URI + "anonymous";

  @Inject
  public AnonymousAccessApiResourceV1(
      final AnonymousManager anonymousManager,
      final RealmSecurityManager realmSecurityManager)
  {
    super(anonymousManager, realmSecurityManager);
  }
}
