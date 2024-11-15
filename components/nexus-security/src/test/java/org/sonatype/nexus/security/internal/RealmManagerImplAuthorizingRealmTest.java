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
package org.sonatype.nexus.security.internal;

import java.util.Collection;
import javax.inject.Inject;

import org.sonatype.nexus.security.AbstractSecurityTest;
import org.sonatype.nexus.security.realm.RealmManager;
import org.sonatype.nexus.security.realm.SecurityRealm;

import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.Realm;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Added this test class in addition to the RealmManagerImplTest class, so I can have different setup that utilizes more
 * of the _real_ system
 */
public class RealmManagerImplAuthorizingRealmTest
    extends AbstractSecurityTest
{
  @Inject
  private RealmSecurityManager realmSecurityManager;

  @Inject
  private RealmManager underTest;

  @Test
  public void testGetAvailableRealms() {
    assertThat(underTest.getAvailableRealms().stream().map(SecurityRealm::getId).collect(toList()),
        is(asList(AuthenticatingRealmImpl.NAME, "MockRealmA", "MockRealmB", "MockRealmC")));
    assertThat(underTest.getAvailableRealms(true).stream().map(SecurityRealm::getId).collect(toList()),
        is(asList(AuthenticatingRealmImpl.NAME, AuthorizingRealmImpl.NAME, "MockRealmA", "MockRealmB", "MockRealmC")));
  }

  @Test
  public void testGetAndSetConfiguredRealmIds() {
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmB", "MockRealmC")));

    underTest.setConfiguredRealmIds(asList("MockRealmA", "InvalidMockRealm"));
    assertThat(underTest.getConfiguredRealmIds(), is(singletonList("MockRealmA")));

    underTest.enableRealm("MockRealmC");
    underTest.enableRealm("MockRealmD");
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmC")));

    underTest.disableRealm("MockRealmA");
    assertThat(underTest.getConfiguredRealmIds(), is(singletonList("MockRealmC")));

    underTest.disableRealm("MockRealmC");
    assertThat(underTest.getConfiguredRealmIds(), is(emptyList()));
  }

  @Test
  public void testGetAndSetConfiguredRealmIdsWithHiddenItems() {
    assertThat(underTest.getConfiguredRealmIds(true),
        is(asList("MockRealmA", "MockRealmB", "MockRealmC", AuthorizingRealmImpl.NAME)));

    underTest.setConfiguredRealmIds(singletonList("MockRealmA"));
    assertThat(underTest.getConfiguredRealmIds(true), is(asList("MockRealmA", AuthorizingRealmImpl.NAME)));

    underTest.enableRealm("MockRealmC");
    underTest.enableRealm("MockRealmD");
    assertThat(underTest.getConfiguredRealmIds(true),
        is(asList("MockRealmA", "MockRealmC", "MockRealmD", AuthorizingRealmImpl.NAME)));

    underTest.disableRealm("MockRealmA");
    underTest.disableRealm("MockRealmD");
    assertThat(underTest.getConfiguredRealmIds(true), is(asList("MockRealmC", AuthorizingRealmImpl.NAME)));

    underTest.disableRealm("MockRealmC");
    assertThat(underTest.getConfiguredRealmIds(true), is(singletonList(AuthorizingRealmImpl.NAME)));
  }

  @Test
  public void testInstallAlwaysIncludesAuthorizingRealmLast() {
    // "initial" from AbstractSecurityTest only lists "MockRealmA" and "MockRealmB"
    assertThat(realmSecurityManager.getRealms().stream().map(Realm::getName).collect(toList()),
        is(asList("MockRealmA", "MockRealmB", "MockRealmC", AuthorizingRealmImpl.NAME)));

    // add another realm last, and watch it magically not actually be last ;)
    underTest.enableRealm("MockRealmC");
    assertThat(realmSecurityManager.getRealms().stream().map(Realm::getName).collect(toList()),
        is(asList("MockRealmA", "MockRealmB", "MockRealmC", AuthorizingRealmImpl.NAME)));

    // try adding the realm in another position and watch it magically go back to the end :)
    underTest.enableRealm(AuthorizingRealmImpl.NAME, 0);
    assertThat(realmSecurityManager.getRealms().stream().map(Realm::getName).collect(toList()),
        is(asList("MockRealmA", "MockRealmB", "MockRealmC", AuthorizingRealmImpl.NAME)));
  }

  @Test
  public void testEnableRealmWithIndex() {
    // default config
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmB", "MockRealmC")));

    // validate adding to beginning of list
    underTest.enableRealm("MockRealmC", 0);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmC", "MockRealmA", "MockRealmB")));

    // validate moving a realm down in the list
    underTest.enableRealm("MockRealmC", 1);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmC", "MockRealmB")));

    // validate moving a realm down to end of list
    underTest.enableRealm("MockRealmC", 2);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmB", "MockRealmC")));

    // validate moving a realm to same position it's already in at end of list
    underTest.enableRealm("MockRealmC", 2);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmB", "MockRealmC")));

    // validate moving a realm to an invalid position
    underTest.enableRealm("MockRealmC", 500);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmB", "MockRealmC")));

    // validate moving a realm up in the list
    underTest.enableRealm("MockRealmC", 1);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmC", "MockRealmB")));

    // validate moving a realm to same position it's already in at middle of list
    underTest.enableRealm("MockRealmC", 1);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmC", "MockRealmB")));

    // validate moving a realm up to beginning of the list
    underTest.enableRealm("MockRealmC", 0);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmC", "MockRealmA", "MockRealmB")));

    // validate moving a realm to same position it's already in at beginning of list
    underTest.enableRealm("MockRealmC", 0);
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmC", "MockRealmA", "MockRealmB")));

    // validating moving realm to invalid position when that realm is only one in list
    underTest.disableRealm("MockRealmB");
    underTest.disableRealm("MockRealmC");
    assertThat(underTest.getConfiguredRealmIds(), is(singletonList("MockRealmA")));
    underTest.enableRealm("MockRealmA", 500);
    assertThat(underTest.getConfiguredRealmIds(), is(singletonList("MockRealmA")));
  }

  @Test
  public void testDisablingAuthorizingRealmImpl() {
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmB", "MockRealmC")));
    underTest.enableRealm(AuthorizingRealmImpl.NAME);

    assertAuthorizingRealmImplEnabled();

    //should do nothing
    underTest.disableRealm(AuthorizingRealmImpl.NAME);

    assertAuthorizingRealmImplEnabled();
  }

  @Test
  public void testGetConfiguredRealmIdsOnlyReturnsValidRealms() {

    underTest.setConfiguredRealmIds(asList("MockRealmA", "MockRealmC", "InvalidMockRealmA", "InvalidMockRealmB"));
    assertThat(underTest.getConfiguredRealmIds(), is(asList("MockRealmA", "MockRealmC")));

    underTest.disableRealm("MockRealmA");
    underTest.enableRealm("MockRealmC");
    underTest.enableRealm("InvalidMockRealmA");
    underTest.enableRealm("InvalidMockRealmX");
    assertThat(underTest.getConfiguredRealmIds(), is(singletonList("MockRealmC")));
  }

  private void assertAuthorizingRealmImplEnabled() {
    Collection<Realm> realms = realmSecurityManager.getRealms();
    assertThat(realms.stream().map(Realm::getName).collect(toList()),
        is(asList("MockRealmA", "MockRealmB", "MockRealmC", AuthorizingRealmImpl.NAME)));
  }
}
