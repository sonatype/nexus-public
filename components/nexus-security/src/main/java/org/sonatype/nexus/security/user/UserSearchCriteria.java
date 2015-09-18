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
package org.sonatype.nexus.security.user;

import java.util.HashSet;
import java.util.Set;

/**
 * A defines searchable fields.
 *
 * Null or empty fields will be ignored.
 */
public class UserSearchCriteria
{
  private String userId;

  private Set<String> oneOfRoleIds = new HashSet<>();

  private String source;

  private String email;

  public UserSearchCriteria() {
  }

  public UserSearchCriteria(final String userId) {
    this.userId = userId;
  }

  public UserSearchCriteria(final String userId, final Set<String> oneOfRoleIds, final String source) {
    this.userId = userId;
    this.oneOfRoleIds = oneOfRoleIds;
    this.source = source;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(final String userId) {
    this.userId = userId;
  }

  public Set<String> getOneOfRoleIds() {
    return oneOfRoleIds;
  }

  public void setOneOfRoleIds(final Set<String> oneOfRoleIds) {
    this.oneOfRoleIds = oneOfRoleIds;
  }

  public String getSource() {
    return source;
  }

  public void setSource(final String source) {
    this.source = source;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }
}
