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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.ws.rs.Path;

import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.security.SecuritySystem;
import org.sonatype.nexus.security.internal.rest.SecurityApiConstants;
import org.sonatype.nexus.security.privilege.PrivilegeDescriptor;
import org.springframework.stereotype.Component;

/**
 * @since 3.26
 */
@Component
@Path(RepositoryPrivilegeApiResourceV1.RESOURCE_URI)
public class RepositoryPrivilegeApiResourceV1
    extends RepositoryPrivilegeApiResource
{
  static final String RESOURCE_URI = SecurityApiConstants.V1_RESOURCE_URI + "privileges";

  @Autowired
  public RepositoryPrivilegeApiResourceV1(
      final SecuritySystem securitySystem,
      final SecurityHelper securityHelper,
      final List<PrivilegeDescriptor> privilegeDescriptorsList)
  {
    super(securitySystem, securityHelper, privilegeDescriptorsList);
  }
}
