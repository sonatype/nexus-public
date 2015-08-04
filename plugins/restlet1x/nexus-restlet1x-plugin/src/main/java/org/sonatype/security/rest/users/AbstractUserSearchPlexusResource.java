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
package org.sonatype.security.rest.users;

import java.util.Set;

import org.sonatype.security.rest.AbstractSecurityPlexusResource;
import org.sonatype.security.rest.model.PlexusUserListResourceResponse;
import org.sonatype.security.usermanagement.User;
import org.sonatype.security.usermanagement.UserSearchCriteria;

import org.restlet.data.Request;

public abstract class AbstractUserSearchPlexusResource
    extends AbstractSecurityPlexusResource
{
  public static final String USER_SOURCE_KEY = "userSource";

  protected String getUserSource(Request request) {
    final String source = getRequestAttribute(request, USER_SOURCE_KEY);

    if ("all".equalsIgnoreCase(source)) {
      return null;
    }

    return source;
  }

  protected PlexusUserListResourceResponse search(UserSearchCriteria criteria) {
    PlexusUserListResourceResponse result = new PlexusUserListResourceResponse();

    Set<User> users = this.getSecuritySystem().searchUsers(criteria);
    result.setData(this.securityToRestModel(users));

    return result;
  }

}
