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
package org.sonatype.security.rest.privileges;

import java.util.Map.Entry;

import org.sonatype.security.authorization.Privilege;
import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.rest.model.PrivilegeProperty;
import org.sonatype.security.rest.model.PrivilegeStatusResource;

import org.restlet.data.Request;

public abstract class AbstractPrivilegePlexusResource
    extends AbstractSecurityPlexusResource
{
  public static final String PRIVILEGE_ID_KEY = "privilegeId";

  public PrivilegeStatusResource securityToRestModel(Privilege privilege, Request request, boolean appendResourceId) {
    PrivilegeStatusResource resource = new PrivilegeStatusResource();

    for (Entry<String, String> prop : privilege.getProperties().entrySet()) {
      PrivilegeProperty privProp = new PrivilegeProperty();
      privProp.setKey(prop.getKey());
      privProp.setValue(prop.getValue());

      resource.addProperty(privProp);
    }

    resource.setType(privilege.getType());
    resource.setId(privilege.getId());
    resource.setName(privilege.getName());
    resource.setDescription(privilege.getDescription());

    String resourceId = "";
    if (appendResourceId) {
      resourceId = resource.getId();
    }
    resource.setResourceURI(this.createChildReference(request, resourceId).toString());

    resource.setUserManaged(!privilege.isReadOnly());

    return resource;
  }

}
