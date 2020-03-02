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
package org.sonatype.nexus.repository.view;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nullable;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class PayloadTest
    extends TestSupport
{
  private byte[] TEST_BYTES = "TEST CONTENT".getBytes();

  private ByteArrayInputStream input = new ByteArrayInputStream(TEST_BYTES);

  private ByteArrayOutputStream output;

  private Payload underTest;

  @Before
  public void setup() throws Exception {
    underTest = createTestPayload();
    output = new ByteArrayOutputStream();
  }

  @Test
  public void copy_Uses_Default_ByteCopy() throws IOException {
    underTest.copy(input, output);

    assertThat(output.toByteArray(), equalTo(TEST_BYTES));
  }

  private Payload createTestPayload() {
    return new Payload()
    {
      @Override
      public InputStream openInputStream() throws IOException {
        return null;
      }

      @Override
      public long getSize() {
        return 0;
      }

      @Nullable
      @Override
      public String getContentType() {
        return null;
      }
    };
  }
}
