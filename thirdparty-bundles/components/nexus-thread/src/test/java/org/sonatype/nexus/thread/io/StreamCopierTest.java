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
package org.sonatype.nexus.thread.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sonatype.goodies.testsupport.TestSupport;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;

public class StreamCopierTest
    extends TestSupport
{
  private String DEFAULT_READ_OUTPUT = "Test read";

  private StreamCopier<String> underTest;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void when_Read_Simple_Expect_Valid_Output() {
    underTest = new StreamCopier<>(outputStream -> {
    }, inputStream -> DEFAULT_READ_OUTPUT);

    assertThat(underTest.read(), is(equalTo(DEFAULT_READ_OUTPUT)));
  }

  @Test
  public void when_Write_To_OutputStream_Expect_To_Read_Written_Value_Multiple_Times() {
    underTest = new StreamCopier<>(this::writeString, this::readString);

    assertThat(underTest.read(), is(equalTo(DEFAULT_READ_OUTPUT)));
    assertThat(underTest.read(), is(equalTo(DEFAULT_READ_OUTPUT)));
  }

  @Test
  public void when_Write_Throws_Exception_Expect_Fail_To_Read_Value() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Unable to properly read from stream");

    underTest = new StreamCopier<>(outputStream -> {
      throw new RuntimeException("Test witting failure");
    }, this::readString);

    // using a short timeout to make test fail faster
    underTest.read(1000);
  }

  @Test
  public void when_Leaving_Streams_Open_Exception_Expect_Fail_To_Read_Value() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Unable to properly read from stream");

    underTest = new StreamCopier<>(this::writeString, this::readString);
    underTest.afterReadLeaveStreamsOpen();

    // using a short timeout to make test fail faster
    underTest.read(1000);
  }

  @Test
  public void when_Leaving_Streams_Open_And_Closing_Manually_Expect_To_Read_Written_Value_Multiple_Times() {
    underTest = new StreamCopier<>(this::writeStringAndClose, this::readString);
    underTest.afterReadLeaveStreamsOpen();

    assertThat(underTest.read(), is(equalTo(DEFAULT_READ_OUTPUT)));
    assertThat(underTest.read(), is(equalTo(DEFAULT_READ_OUTPUT)));
  }

  @Test
  public void when_Read_Throws_Exception_Expect_Fail_To_Read_Value() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Unable to properly read from stream");

    underTest = new StreamCopier<>(this::writeString, inputStream -> {
      throw new RuntimeException("Test Reading failure");
    });

    // using a short timeout to make test fail faster
    underTest.read(1000);
  }

  private void writeStringAndClose(OutputStream outputStream) {
    try {
      this.writeString(outputStream);

      outputStream.close();
    }
    catch (IOException e) {
      fail(e.getMessage());
    }
  }

  private void writeString(OutputStream outputStream) {
    try {
      outputStream.write(DEFAULT_READ_OUTPUT.getBytes());
    }
    catch (IOException e) {
      fail(e.getMessage());
    }
  }

  private String readString(final InputStream inputStream) {
    try {
      return IOUtils.toString(inputStream);
    }
    catch (IOException e) {
      fail(e.getMessage());
    }
    return null;
  }
}
