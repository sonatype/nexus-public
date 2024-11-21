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
package org.sonatype.nexus.email.internal.secrets.migration;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.crypto.secrets.Secret;
import org.sonatype.nexus.email.EmailConfiguration;
import org.sonatype.nexus.email.EmailManager;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EmailSecretsMigratorTests
    extends TestSupport
{
  @Mock
  private EmailManager emailManager;

  @Mock
  private EmailConfiguration emailConfiguration;

  @InjectMocks
  private EmailSecretsMigrator underTest;

  @Test
  public void testMigrate() {
    Secret notMigrated = getMockSecret("legacy", "legacyPassword");
    when(emailConfiguration.getPassword()).thenReturn(notMigrated);
    when(emailManager.getConfiguration()).thenReturn(emailConfiguration);

    underTest.migrate();
    verify(emailManager).setConfiguration(emailConfiguration, "legacyPassword");
  }

  @Test
  public void testSkipMigration() {
    Secret alreadyMigrated = getMockSecret("_1", "alreadyMigrated");
    when(emailConfiguration.getPassword()).thenReturn(alreadyMigrated);
    when(emailManager.getConfiguration()).thenReturn(emailConfiguration);

    underTest.migrate();
    verify(emailManager, never()).setConfiguration(any(EmailConfiguration.class), anyString());

    when(emailConfiguration.getPassword()).thenReturn(null);
    underTest.migrate();
    verify(emailManager, never()).setConfiguration(any(EmailConfiguration.class), anyString());
  }

  private Secret getMockSecret(String tokenId, String token) {
    Secret secret = mock(Secret.class);
    when(secret.getId()).thenReturn(tokenId);
    when(secret.decrypt()).thenReturn(token.toCharArray());
    return secret;
  }
}
