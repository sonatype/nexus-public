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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.role.RoleIdentifier;

/**
 * User.
 */
public class User
    implements Comparable<User>
{
  private String userId;

  private String firstName;

  private String lastName;

  private String emailAddress;

  private String source;

  private UserStatus status;

  private boolean readOnly;

  private String version;

  private Set<RoleIdentifier> roleIdentifiers = new HashSet<RoleIdentifier>();

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getName() {
    String name = this.getFirstName() != null ? this.getFirstName() : "";
    if (Strings2.isNotBlank(this.getLastName())) {
      name += " " + this.getLastName();
    }
    return name;
  }

  public void setName(String name) {
    // deprecated, but attempt to use
    if (Strings2.isNotBlank(name)) {
      String[] nameParts = name.trim().split(" ", 2);
      this.setFirstName(nameParts[0]);
      if (nameParts.length > 1) {
        this.setLastName(nameParts[1]);
      }
    }
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Set<RoleIdentifier> getRoles() {
    return Collections.unmodifiableSet(roleIdentifiers);
  }

  public void addRole(RoleIdentifier roleIdentifier) {
    this.roleIdentifiers.add(roleIdentifier);
  }

  public boolean removeRole(RoleIdentifier roleIdentifier) {
    return this.roleIdentifiers.remove(roleIdentifier);
  }

  public void addAllRoles(Set<RoleIdentifier> roleIdentifiers) {
    this.roleIdentifiers.addAll(roleIdentifiers);
  }

  public void setRoles(Set<RoleIdentifier> roles) {
    this.roleIdentifiers = roles;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public int compareTo(User o) {
    final int before = -1;
    final int equal = 0;
    final int after = 1;

    if (this == o) {
      return equal;
    }

    if (o == null) {
      return after;
    }

    if (getUserId() == null && o.getUserId() != null) {
      return before;
    }
    else if (getUserId() != null && o.getUserId() == null) {
      return after;
    }

    // the userIds are not null
    int result = getUserId().compareTo(o.getUserId());
    if (result != equal) {
      return result;
    }

    if (getSource() == null) {
      return before;
    }

    // if we are all the way to this point, the userIds are equal and this.getSource != null, so just return a
    // compareTo on the source
    return getSource().compareTo(o.getSource());
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    User user = (User) o;

    if (source != null ? !source.equals(user.source) : user.source != null) {
      return false;
    }
    if (userId != null ? !userId.equals(user.userId) : user.userId != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = userId != null ? userId.hashCode() : 0;
    result = 31 * result + (source != null ? source.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "userId='" + userId + '\'' +
        ", firstName='" + firstName + '\'' +
        ", lastName='" + lastName + '\'' +
        ", source='" + source + '\'' +
        '}';
  }
}
