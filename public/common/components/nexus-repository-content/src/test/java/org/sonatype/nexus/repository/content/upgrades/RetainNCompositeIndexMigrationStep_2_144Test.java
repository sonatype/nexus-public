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
package org.sonatype.nexus.repository.content.upgrades;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.repository.Format;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class RetainNCompositeIndexMigrationStep_2_144Test
{
  @Mock
  private Format mavenFormat;

  @Mock
  private Format dockerFormat;

  @Mock
  private Format unsupportedFormat;

  private RetainNCompositeIndexMigrationStep_2_144 underTest;

  @Before
  public void setup() {
    when(mavenFormat.getValue()).thenReturn("maven2");
    when(dockerFormat.getValue()).thenReturn("docker");
    when(unsupportedFormat.getValue()).thenReturn("unsupported-format");
    underTest = new RetainNCompositeIndexMigrationStep_2_144(
        List.of(mavenFormat, dockerFormat, unsupportedFormat));
  }

  @Test
  public void testVersion() {
    Optional<String> version = underTest.version();

    assertThat(version).isPresent();
    assertThat(version.get()).isEqualTo("2.144");
  }

  @Test
  public void testCanExecuteInTransaction() {
    // Returns false because the migration uses CREATE INDEX CONCURRENTLY on PostgreSQL,
    // which cannot run inside a transaction block.
    assertThat(underTest.canExecuteInTransaction()).isFalse();
  }

  @Test
  public void testConstructorWithEmptyFormats() {
    RetainNCompositeIndexMigrationStep_2_144 step =
        new RetainNCompositeIndexMigrationStep_2_144(Collections.emptyList());

    assertThat(step.version()).isPresent();
    assertThat(step.version().get()).isEqualTo("2.144");
  }

  @Test
  public void testConstructorWithSingleFormat() {
    RetainNCompositeIndexMigrationStep_2_144 step =
        new RetainNCompositeIndexMigrationStep_2_144(List.of(mavenFormat));

    assertThat(step.version()).isPresent();
    assertThat(step.version().get()).isEqualTo("2.144");
  }
}
