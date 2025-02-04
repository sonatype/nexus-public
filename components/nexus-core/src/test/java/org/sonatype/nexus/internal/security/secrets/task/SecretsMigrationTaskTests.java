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
package org.sonatype.nexus.internal.security.secrets.task;

import java.util.ArrayList;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.security.secrets.SecretsMigrator;

import org.junit.Test;

import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SecretsMigrationTaskTests
    extends TestSupport
{
  @Test
  public void testTaskMigratesSeveralSources() throws Exception {
    List<SecretsMigrator> migrators = getMigrators();
    SecretsMigrationTask underTest = new SecretsMigrationTask(migrators);

    try {
      underTest.execute();
    }
    catch (Exception e) {
      fail(e.getMessage());
    }

    for (SecretsMigrator migrator : migrators) {
      verify(migrator).migrate();
    }
  }

  private List<SecretsMigrator> getMigrators() {
    List<SecretsMigrator> sources = new ArrayList<>();

    for (int i = 0; i < 5; i++) {
      SecretsMigrator migrationSource = mock(SecretsMigrator.class);
      sources.add(migrationSource);
    }

    return sources;
  }
}
