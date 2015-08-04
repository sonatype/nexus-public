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
package org.sonatype.nexus.client.core.subsystem.config;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * Nexus security settings.
 *
 * @since 2.7
 */
public class SecuritySettings
{

  private boolean anonymousAccessEnabled;

  private String anonymousUsername;

  private String anonymousPassword;

  private java.util.List<String> realms;

  public boolean isAnonymousAccessEnabled() {
    return anonymousAccessEnabled;
  }

  public void setAnonymousAccessEnabled(final boolean anonymousAccessEnabled) {
    this.anonymousAccessEnabled = anonymousAccessEnabled;
  }

  public SecuritySettings withAnonymousAccessEnabled(final boolean anonymousAccessEnabled) {
    setAnonymousAccessEnabled(anonymousAccessEnabled);
    return this;
  }

  public String getAnonymousUsername() {
    return anonymousUsername;
  }

  public void setAnonymousUsername(final String anonymousUsername) {
    this.anonymousUsername = anonymousUsername;
  }

  public SecuritySettings withAnonymousUsername(final String anonymousUsername) {
    setAnonymousUsername(anonymousUsername);
    return this;
  }

  public String getAnonymousPassword() {
    return anonymousPassword;
  }

  public void setAnonymousPassword(final String anonymousPassword) {
    this.anonymousPassword = anonymousPassword;
  }

  public SecuritySettings withAnonymousPassword(final String anonymousPassword) {
    setAnonymousPassword(anonymousPassword);
    return this;
  }

  public List<String> getRealms() {
    return realms;
  }

  public void setRealms(final List<String> realms) {
    if (realms == null) {
      this.realms = Lists.newArrayList();
    }
    else {
      this.realms = Lists.newArrayList(realms);
    }
  }

  public SecuritySettings withRealms(final List<String> realms) {
    setRealms(realms);
    return this;
  }

  public SecuritySettings withRealms(final String... realms) {
    if (realms == null) {
      setRealms(null);
    }
    else {
      setRealms(Arrays.asList(realms));
    }
    return this;
  }

  public SecuritySettings addRealm(final String realm) {
    removeRealm(realm);
    getRealms().add(realm);
    return this;
  }

  public SecuritySettings addRealm(final String realm, final int position) {
    removeRealm(realm);
    getRealms().add(position, realm);
    return this;
  }

  public SecuritySettings removeRealm(final String realm) {
    getRealms().remove(realm);
    return this;
  }

}
