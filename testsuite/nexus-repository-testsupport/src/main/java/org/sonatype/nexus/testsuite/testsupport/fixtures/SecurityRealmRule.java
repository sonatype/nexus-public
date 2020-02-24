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

import javax.inject.Provider;

import org.sonatype.nexus.security.realm.RealmConfiguration;
import org.sonatype.nexus.security.realm.RealmManager;

import org.junit.rules.ExternalResource;

import static java.util.Arrays.asList;

public class SecurityRealmRule
    extends ExternalResource
{
  final Provider<RealmManager> realmManagerProvider;

  public SecurityRealmRule(final Provider<RealmManager> realmManagerProvider) {
    this.realmManagerProvider = realmManagerProvider;
  }

  @Override
  protected void after() {
    RealmConfiguration configuration = realmManagerProvider.get().getConfiguration();
    configuration.setRealmNames(asList("NexusAuthenticatingRealm", "NexusAuthorizingRealm"));
    realmManagerProvider.get().setConfiguration(configuration);
  }

  public void addSecurityRealm(final String realm) {
    RealmConfiguration configuration = realmManagerProvider.get().getConfiguration();
    configuration.getRealmNames().add(realm);
    realmManagerProvider.get().setConfiguration(configuration);
  }

  public void removeSecurityRealm(final String realm) {
    RealmConfiguration configuration = realmManagerProvider.get().getConfiguration();
    configuration.getRealmNames().remove(realm);
    realmManagerProvider.get().setConfiguration(configuration);
  }
}
