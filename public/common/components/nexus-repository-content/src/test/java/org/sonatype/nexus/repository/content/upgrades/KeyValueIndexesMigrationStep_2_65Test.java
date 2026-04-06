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

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class KeyValueIndexesMigrationStep_2_65Test
    extends TestSupport
{
  @Mock
  private Format format;

  private KeyValueIndexesMigrationStep_2_65 underTest;

  @Before
  public void setup() {
    when(format.getValue()).thenReturn("maven2");
    underTest = new KeyValueIndexesMigrationStep_2_65(List.of(format));
  }

  @Test
  public void testVersion() {
    Optional<String> version = underTest.version();

    assertThat(version).isPresent();
    assertThat(version.get()).isEqualTo("2.65");
  }

  @Test
  public void testConstructorWithEmptyFormats() {
    KeyValueIndexesMigrationStep_2_65 step =
        new KeyValueIndexesMigrationStep_2_65(Collections.emptyList());

    assertThat(step.version()).isPresent();
    assertThat(step.version().get()).isEqualTo("2.65");
  }
}
