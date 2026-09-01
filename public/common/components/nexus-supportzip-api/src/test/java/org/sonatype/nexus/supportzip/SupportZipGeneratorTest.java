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
package org.sonatype.nexus.supportzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;
import org.sonatype.nexus.supportzip.SupportZipGenerator.Result;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for {@link SupportZipGenerator.Result}
 */
public class SupportZipGeneratorTest
{
  @Test
  public void testGettersReturnConstructorValues() {
    Result result = new Result(true, "support.zip", "/var/log/support.zip", 1024L);
    assertThat(result.isTruncated(), is(true));
    assertThat(result.getFilename(), is("support.zip"));
    assertThat(result.getLocalPath(), is("/var/log/support.zip"));
    assertThat(result.getSize(), is(1024L));
  }

  @Test
  public void testGettersReturnConstructorValuesWhenNotTruncated() {
    Result result = new Result(false, "archive.zip", "/tmp/archive.zip", 0L);
    assertThat(result.isTruncated(), is(false));
    assertThat(result.getFilename(), is("archive.zip"));
    assertThat(result.getLocalPath(), is("/tmp/archive.zip"));
    assertThat(result.getSize(), is(0L));
  }

  @Test
  public void testGettersAllowNullStrings() {
    Result result = new Result(false, null, null, -1L);
    assertThat(result.isTruncated(), is(false));
    assertThat(result.getFilename(), is((String) null));
    assertThat(result.getLocalPath(), is((String) null));
    assertThat(result.getSize(), is(-1L));
  }

  @Test
  public void testToStringContainsClassNameAndFieldValues() {
    Result result = new Result(true, "support.zip", "/var/log/support.zip", 2048L);
    String text = result.toString();
    assertThat(text, containsString("Result"));
    assertThat(text, containsString("truncated=true"));
    assertThat(text, containsString("filename=support.zip"));
    assertThat(text, containsString("localPath=/var/log/support.zip"));
    assertThat(text, containsString("size=2048"));
  }

  @Test
  public void testToStringWhenNotTruncated() {
    Result result = new Result(false, "archive.zip", "/tmp/archive.zip", 512L);
    String text = result.toString();
    assertThat(text, containsString("Result"));
    assertThat(text, containsString("truncated=false"));
    assertThat(text, containsString("filename=archive.zip"));
    assertThat(text, containsString("localPath=/tmp/archive.zip"));
    assertThat(text, containsString("size=512"));
  }

  @Test
  public void testToStringExactFormat() {
    Result result = new Result(true, "support.zip", "/var/log/support.zip", 100L);
    assertThat(result.toString(),
        is("Result{truncated=true, filename=support.zip, localPath=/var/log/support.zip, size=100}"));
  }

  @Test
  public void testToStringRendersNullStringsAsNull() {
    Result result = new Result(false, null, null, -1L);
    assertThat(result.toString(),
        is("Result{truncated=false, filename=null, localPath=null, size=-1}"));
  }

  @Test
  public void testSerializationPreservesAllFields() throws Exception {
    Result original = new Result(true, "support.zip", "/var/log/support.zip", 4096L);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }

    Result restored;
    try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      restored = (Result) in.readObject();
    }

    assertThat(restored.isTruncated(), is(true));
    assertThat(restored.getFilename(), is("support.zip"));
    assertThat(restored.getLocalPath(), is("/var/log/support.zip"));
    assertThat(restored.getSize(), is(4096L));
  }
}
