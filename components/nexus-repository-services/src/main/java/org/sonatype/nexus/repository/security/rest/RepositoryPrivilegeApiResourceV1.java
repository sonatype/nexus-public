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
package org.sonatype.nexus.repository.security.rest;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;

import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.internal.rest.SecurityApiResourceV1;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;

/**
 * @since 3.26
 */
@Named
@Singleton
@Path(RepositoryPrivilegeApiResourceV1.RESOURCE_URI)
public class RepositoryPrivilegeApiResourceV1
    extends RepositoryPrivilegeApiResource
{
  static final String RESOURCE_URI = SecurityApiResourceV1.V1_RESOURCE_URI + "privileges";

  @Inject
  public RepositoryPrivilegeApiResourceV1(
      final SecuritySystem securitySystem,
      final Map<String, PrivilegeDescriptor> privilegeDescriptors)
  {
    super(securitySystem, privilegeDescriptors);
  }
}
