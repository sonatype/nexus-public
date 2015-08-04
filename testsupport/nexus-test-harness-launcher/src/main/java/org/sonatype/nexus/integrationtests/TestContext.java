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
package org.sonatype.nexus.integrationtests;

import java.util.HashMap;

import static com.google.common.base.Preconditions.checkState;

public class TestContext
{
  private boolean secureTest;

  private String username;

  private String password;

  private String adminUsername;

  private String adminPassword;

  private String nexusUrl;

  private final HashMap<String, Object> map;

  public TestContext() {
    map = new HashMap<>();
    // nexusUrl is set only once, it does not change (for now)
    nexusUrl = null;
    reset();
  }

  public void reset() {
    secureTest = false;
    adminUsername = "admin";
    adminPassword = "admin123";
    username = adminUsername;
    password = adminPassword;
    map.clear();
  }

  public void put(final String key, final Object value) {
    this.map.put(key, value);
  }

  public boolean isSecureTest() {
    return secureTest;
  }

  public TestContext setSecureTest(final boolean secureTest) {
    this.secureTest = secureTest;
    return this;
  }

  public String getUsername() {
    return username;
  }

  public TestContext setUsername(final String username) {
    this.username = username;
    return this;
  }

  public String getPassword() {
    return password;
  }

  public TestContext setPassword(final String password) {
    this.password = password;
    return this;
  }

  public String getAdminUsername() {
    return adminUsername;
  }

  public TestContext setAdminUsername(final String adminUsername) {
    this.adminUsername = adminUsername;
    return this;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public TestContext setAdminPassword(final String adminPassword) {
    this.adminPassword = adminPassword;
    return this;
  }

  public TestContext useAdminForRequests() {
    this.username = this.adminUsername;
    this.password = this.adminPassword;
    return this;
  }

  public String getNexusUrl() {
    checkState(nexusUrl != null, "Nexus URL not set");
    return nexusUrl;
  }

  public TestContext setNexusUrl(final String nexusUrl) {
    this.nexusUrl = nexusUrl;
    return this;
  }
}
