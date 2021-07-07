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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmManager;

import org.junit.rules.ExternalResource;

@Named
@Singleton
public class SecurityRealmRule
    extends ExternalResource
{
  final Provider<RealmManager> realmManagerProvider;

  private RealmConfiguration configuration;

  public SecurityRealmRule(final Provider<RealmManager> realmManagerProvider) {
    this.realmManagerProvider = realmManagerProvider;
  }

  @Inject
  public SecurityRealmRule(final RealmManager realmManager) {
    this.realmManagerProvider = () -> realmManager;
  }

  @Override
  public void before() {
    configuration = realmManagerProvider.get().getConfiguration().copy();
  }

  @Override
  public void after() {
    realmManagerProvider.get().setConfiguration(configuration);
  }

  public void addSecurityRealm(final String realm) {
    RealmConfiguration configuration = realmManagerProvider.get().getConfiguration().copy();
    List<String> realms = new ArrayList<>(configuration.getRealmNames());
    if (!realms.contains(realm)) {
      realms.add(realm);
      configuration.setRealmNames(realms);
      realmManagerProvider.get().setConfiguration(configuration);
    }
  }

  public void removeSecurityRealm(final String realm) {
    RealmConfiguration configuration = realmManagerProvider.get().getConfiguration().copy();
    List<String> realms = new ArrayList<>(configuration.getRealmNames());
    realms.remove(realm);
    configuration.setRealmNames(realms);
    realmManagerProvider.get().setConfiguration(configuration);
  }
}
