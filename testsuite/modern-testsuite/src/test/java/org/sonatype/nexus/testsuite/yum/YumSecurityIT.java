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
package org.sonatype.nexus.testsuite.yum;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.sonatype.nexus.client.core.exception.NexusClientResponseException;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.core.subsystem.security.User;
import org.sonatype.nexus.client.core.subsystem.security.Users;
import org.sonatype.nexus.yum.client.Yum;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * ITs related to security.
 *
 * @since 3.0
 */
public class YumSecurityIT
    extends YumITSupport
{

  private static final String ANOTHER_VERSION = "4.3.1";

  private static final String VERSION = "1.2.3";

  private static final String PASSWORD = "yum123";

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  public YumSecurityIT(final String nexusBundleCoordinates) {
    super(nexusBundleCoordinates);
  }

  @Test
  public void shouldNotHaveReadAccessToAliasesForAnonymous()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    final String alias = uniqueName();
    yum().createOrUpdateAlias(repository.id(), alias, VERSION);

    thrown.expect(NexusClientResponseException.class);
    thrown.expectMessage("401");
    createNexusClientForAnonymous(nexus()).getSubsystem(Yum.class).getAlias(repository.id(), alias);
  }

  @Test
  public void shouldNotCreateAliasForAnonymous()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    thrown.expect(NexusClientResponseException.class);
    thrown.expectMessage("401");
    createNexusClientForAnonymous(nexus()).getSubsystem(Yum.class)
        .createOrUpdateAlias(repository.id(), uniqueName(), VERSION);
  }

  @Test
  public void shouldNotHaveUpdateAccessToAliasesForAnonymous()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    final String alias = uniqueName();
    yum().createOrUpdateAlias(repository.id(), alias, VERSION);
    thrown.expect(NexusClientResponseException.class);
    thrown.expectMessage("401");
    createNexusClientForAnonymous(nexus()).getSubsystem(Yum.class)
        .createOrUpdateAlias(repository.id(), alias, "3.2.1");
  }

  @Test
  public void shouldAllowAccessForYumAdmin()
      throws Exception
  {
    final Repository repository = createYumEnabledRepository(repositoryIdForTest());

    final User user = givenYumAdminUser();
    final Yum yum = createNexusClient(nexus(), user.id(), PASSWORD).getSubsystem(Yum.class);
    final String alias = uniqueName();
    yum.createOrUpdateAlias(repository.id(), alias, VERSION);
    assertThat(yum.getAlias(repository.id(), alias), is(VERSION));
    yum.createOrUpdateAlias(repository.id(), alias, ANOTHER_VERSION);
    assertThat(yum.getAlias(repository.id(), alias), is(ANOTHER_VERSION));
  }

  private User givenYumAdminUser() {
    final String username = testMethodName();

    return client().getSubsystem(Users.class).create(username)
        .withEmail(username + "@sonatype.org")
        .withFirstName("bar")
        .withLastName("foo")
        .withPassword(PASSWORD)
        .withRole("anonymous")
        .withRole("nexus-yum-admin")
        .save();
  }

  public static String uniqueName() {
    return "repo_" + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
  }

}
