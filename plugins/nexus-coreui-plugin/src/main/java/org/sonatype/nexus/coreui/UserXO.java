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

import java.util.Set;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.security.role.RolesExist;
import org.sonatype.nexus.security.user.UniqueUserId;
import org.sonatype.nexus.security.user.UserStatus;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

/**
 * User exchange object.
 *
 * @since 3.0
 */
public class UserXO
{
  @NotBlank
  @UniqueUserId(groups = Create.class)
  private String userId;

  @NotBlank(groups = Update.class)
  private String version;

  // Null on create
  private String realm;

  @NotBlank
  private String firstName;

  @NotBlank
  private String lastName;

  @NotBlank
  @Email
  private String email;

  @NotNull
  private UserStatus status;

  @NotBlank(groups = Create.class)
  private String password;

  @NotEmpty
  @RolesExist(groups = {Create.class, Update.class})
  private Set<String> roles;

  private Boolean external;

  // FIXME: Sort out what this is used for
  private Set<String> externalRoles;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getRealm() {
    return realm;
  }

  public void setRealm(String realm) {
    this.realm = realm;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public UserStatus getStatus() {
    return status;
  }

  public void setStatus(UserStatus status) {
    this.status = status;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }

  public Boolean isExternal() {
    return external;
  }

  public void setExternal(Boolean external) {
    this.external = external;
  }

  public Set<String> getExternalRoles() {
    return externalRoles;
  }

  public void setExternalRoles(Set<String> externalRoles) {
    this.externalRoles = externalRoles;
  }

  @Override
  public String toString() {
    return "UserXO{" +
        "userId='" + userId + '\'' +
        ", version='" + version + '\'' +
        ", realm='" + realm + '\'' +
        ", firstName='" + firstName + '\'' +
        ", lastName='" + lastName + '\'' +
        ", email='" + email + '\'' +
        ", status=" + status +
        ", password='" + password + '\'' +
        ", roles=" + roles +
        ", external=" + external +
        ", externalRoles=" + externalRoles +
        '}';
  }
}
