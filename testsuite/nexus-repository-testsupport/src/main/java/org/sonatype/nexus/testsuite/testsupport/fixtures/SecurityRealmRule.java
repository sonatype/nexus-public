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
package org.sonatype.nexus.testsuite.testsupport.fixtures;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.security.realm.RealmManager;

import org.junit.rules.ExternalResource;

@Named
@Singleton
public class SecurityRealmRule
    extends ExternalResource
{
  final Provider<RealmManager> realmManagerProvider;

  private List<String> configuredRealms;

  public SecurityRealmRule(final Provider<RealmManager> realmManagerProvider) {
    this.realmManagerProvider = realmManagerProvider;
  }

  @Inject
  public SecurityRealmRule(final RealmManager realmManager) {
    this.realmManagerProvider = () -> realmManager;
  }

  @Override
  public void before() {
    configuredRealms = realmManagerProvider.get().getConfiguredRealmIds();
  }

  @Override
  public void after() {
    realmManagerProvider.get().setConfiguredRealmIds(configuredRealms);
  }

  public void addSecurityRealm(final String realm) {
    realmManagerProvider.get().enableRealm(realm);
  }

  public void removeSecurityRealm(final String realm) {
    realmManagerProvider.get().disableRealm(realm);
  }
}
