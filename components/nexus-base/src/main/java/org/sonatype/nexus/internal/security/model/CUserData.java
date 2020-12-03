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
package org.sonatype.nexus.internal.security.model;

import org.sonatype.nexus.common.entity.HasStringId;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.security.config.CUser;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link CUser} data.
 *
 * @since 3.21
 */
public class CUserData
    implements HasStringId, CUser
{
  private String email;

  private String firstName;

  private String id;

  private String lastName;

  private String password;

  private String status;

  private int version = 1;

  @Override
  public String getEmail() {
    return this.email;
  }

  @Override
  public String getFirstName() {
    return this.firstName;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getLastName() {
    return this.lastName;
  }

  @Override
  public String getPassword() {
    return this.password;
  }

  @Override
  public String getStatus() {
    return this.status;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @JsonIgnore
  @Override
  public boolean isActive() {
    return STATUS_ACTIVE.equals(status) || STATUS_CHANGE_PASSWORD.equals(status);
  }

  @Override
  public void setEmail(final String email) {
    this.email = email;
  }

  @Override
  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  @Override
  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  @Override
  public void setPassword(final String password) {
    this.password = password;
  }

  @Override
  public void setStatus(final String status) {
    this.status = status;
  }

  @Override
  public void setVersion(final int version) {
    this.version = Math.max(version, 1);
  }

  @Override
  public CUserData clone() {
    try {
      return (CUserData) super.clone();
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
