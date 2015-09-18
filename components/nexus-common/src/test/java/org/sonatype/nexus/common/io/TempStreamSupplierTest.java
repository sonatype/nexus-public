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
package org.sonatype.nexus.common.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.base.Throwables;
import com.google.common.io.CharStreams;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TempStreamSupplierTest
    extends TestSupport
{
  final String content = "foo";

  @Test
  public void testGetTwice() throws Exception {
    TempStreamSupplier underTest = new TempStreamSupplier(content());

    try (InputStream i1 = underTest.get(); InputStream i2 = underTest.get()) {
      assertThat(asString(i1), is(content));
      assertThat(asString(i2), is(content));
    }
  }

  private InputStream content() {
    return new ByteArrayInputStream(content.getBytes());
  }

  private String asString(InputStream in) {
    try (InputStreamReader reader = new InputStreamReader(in)) {
      return CharStreams.toString(reader);
    }
    catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }
}