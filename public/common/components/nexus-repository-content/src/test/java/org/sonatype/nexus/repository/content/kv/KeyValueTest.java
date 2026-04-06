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
package org.sonatype.nexus.repository.content.kv;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class KeyValueTest
    extends TestSupport
{
  @Test
  public void testGettersAndSetters() {
    KeyValue underTest = new KeyValue();

    underTest.setCategory("metadata");
    underTest.setKey("version");
    underTest.setValue("1.0.0");
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    underTest.setCreated(now);

    assertThat(underTest.getCategory(), is("metadata"));
    assertThat(underTest.getKey(), is("version"));
    assertThat(underTest.getValue(), is("1.0.0"));
    assertThat(underTest.getCreated(), is(now));
  }

  @Test
  public void testDefaultValuesAreNull() {
    KeyValue underTest = new KeyValue();

    assertThat(underTest.getCategory(), is(nullValue()));
    assertThat(underTest.getKey(), is(nullValue()));
    assertThat(underTest.getValue(), is(nullValue()));
    assertThat(underTest.getCreated(), is(nullValue()));
  }

  @Test
  public void testContinuationTokenIsKey() {
    KeyValue underTest = new KeyValue();
    underTest.setKey("my-key");

    assertThat(underTest.nextContinuationToken(), is("my-key"));
  }
}
