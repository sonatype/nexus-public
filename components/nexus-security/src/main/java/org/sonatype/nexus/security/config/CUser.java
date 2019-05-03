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
package org.sonatype.nexus.security.config;

import java.io.Serializable;

import org.sonatype.nexus.common.entity.AbstractEntity;
import org.sonatype.nexus.common.text.Strings2;

/**
 * Persistent user.
 */
public class CUser
    extends AbstractEntity
    implements Serializable, Cloneable
{
  public static final String STATUS_DISABLED = "disabled";

  public static final String STATUS_ACTIVE = "active";

  public static final String STATUS_CHANGE_PASSWORD = "changepassword";

  private String id;

  private String firstName;

  private String lastName;

  private String password;

  private String status;

  private String email;

  private String version;

  public String getEmail() {
    return this.email;
  }

  public String getFirstName() {
    return this.firstName;
  }

  public String getId() {
    return this.id;
  }

  public String getLastName() {
    return this.lastName;
  }

  public String getPassword() {
    return this.password;
  }

  public String getStatus() {
    return this.status;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equals(status) || STATUS_CHANGE_PASSWORD.equals(status);
  }

  @Override
  public CUser clone() {
    try {
      return (CUser) super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        ", firstName='" + firstName + '\'' +
        ", lastName='" + lastName + '\'' +
        ", password='" + Strings2.mask(password) + '\'' +
        ", status='" + status + '\'' +
        ", email='" + email + '\'' +
        ", version='" + version + '\'' +
        '}';
  }
}
