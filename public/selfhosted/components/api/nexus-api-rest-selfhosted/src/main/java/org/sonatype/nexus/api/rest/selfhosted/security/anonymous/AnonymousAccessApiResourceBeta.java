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
package org.sonatype.nexus.api.rest.selfhosted.security.anonymous;

import org.springframework.beans.factory.annotation.Autowired;
import javax.ws.rs.Path;

import org.sonatype.nexus.rest.APIConstants;
import org.sonatype.nexus.security.anonymous.AnonymousManager;
import org.sonatype.nexus.security.internal.rest.SecurityApiConstants;

import io.swagger.annotations.Api;
import org.apache.shiro.mgt.RealmSecurityManager;

import static org.sonatype.nexus.api.rest.selfhosted.security.anonymous.AnonymousAccessApiResourceBeta.RESOURCE_URI;
import org.springframework.stereotype.Component;

/**
 * @since 3.26
 * @deprecated beta prefix is being phased out, prefer starting new APIs with {@link APIConstants#V1_API_PREFIX} instead
 */
@Api(hidden = true)
@Component
@Path(RESOURCE_URI)
@Deprecated
public class AnonymousAccessApiResourceBeta
    extends AnonymousAccessApiResource
{
  static final String RESOURCE_URI = SecurityApiConstants.BETA_RESOURCE_URI + "anonymous";

  @Autowired
  public AnonymousAccessApiResourceBeta(
      final AnonymousManager anonymousManager,
      final RealmSecurityManager realmSecurityManager)
  {
    super(anonymousManager, realmSecurityManager);
  }
}
