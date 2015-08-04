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
package org.sonatype.nexus.proxy.maven.routing.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;

import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

public class TextFilePrefixSourceMarshallerTest
    extends TestSupport
{
  @Mock
  StorageFileItem storageFileItem;

  // is state-less, no need for @Before
  final TextFilePrefixSourceMarshaller m = new TextFilePrefixSourceMarshaller(new ConfigImpl());

  final Charset UTF8 = Charset.forName("UTF-8");

  protected String prefixFile1(boolean withComments) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    if (withComments) {
      pw.println("# This is mighty prefix file!");
    }
    pw.println("/org/apache/maven");
    pw.println("/org/sonatype");
    if (withComments) {
      pw.println("# Added later");
    }
    pw.println("/eu/flatwhite");
    return sw.toString();
  }

  protected String prefixFile2(boolean withComments) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    if (withComments) {
      pw.println("# This is mighty prefix file!");
    }
    pw.println("./org/apache/maven");
    pw.println("./org/sonatype");
    if (withComments) {
      pw.println("# Added later");
    }
    pw.println("./eu/flatwhite");
    return sw.toString();
  }

  protected String prefixFile3(boolean withComments) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    if (withComments) {
      pw.println("# This is mighty prefix file!");
    }
    pw.println("/");
    return sw.toString();
  }

  protected String prefixFile4(boolean withComments, boolean fixed) {
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    pw.println(TextFilePrefixSourceMarshaller.MAGIC);
    if (withComments) {
      pw.println("# This is mighty prefix file!");
    }
    if (fixed) {
      pw.println("/foo");
      pw.println("/bar/blah");
      pw.println("/bar/foo");
    }
    else {
      pw.println("foo");
      pw.println("bar\\blah");
      pw.println("\\\\bar////foo");
    }
    return sw.toString();
  }

  protected String withStandardHeaders(String content) {
    final StringBuilder sb = new StringBuilder();

    sb.append(TextFilePrefixSourceMarshaller.MAGIC).append(System.getProperty("line.separator"));
    for (String header : TextFilePrefixSourceMarshaller.HEADERS) {
      sb.append(header).append(System.getProperty("line.separator"));
    }
    return content.replace(TextFilePrefixSourceMarshaller.MAGIC + System.getProperty("line.separator"), sb.toString());
  }

  @Test
  public void roundtrip()
      throws IOException
  {
    when(storageFileItem.getInputStream()).thenReturn(
        new ByteArrayInputStream(prefixFile1(true).getBytes(UTF8)));
    final List<String> entries = m.read(storageFileItem).entries();
    assertThat(entries, is(notNullValue()));
    assertThat(entries.size(), is(3));

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    m.write(entries, outputStream);
    assertThat(outputStream.size(), greaterThan(15));

    final String output = new String(outputStream.toByteArray(), UTF8);
    assertThat(output, equalTo(withStandardHeaders(prefixFile1(false))));
  }

  @Test
  public void roundtrip2()
      throws IOException
  {
    // prefixFile2 is "find created" like, see CENTRAL-515
    when(storageFileItem.getInputStream()).thenReturn(
        new ByteArrayInputStream(prefixFile2(true).getBytes(UTF8)));
    final List<String> entries = m.read(storageFileItem).entries();
    assertThat(entries, is(notNullValue()));
    assertThat(entries.size(), is(3));

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    m.write(entries, outputStream);
    assertThat(outputStream.size(), greaterThan(15));

    final String output = new String(outputStream.toByteArray(), UTF8);
    // once read, the file looses "peculiarities" as dots from start and comments
    // naturally this applies to all nexus-managed files only (hosted + groups) as proxy WLs are
    // passed on as-is (unchanged)
    assertThat(output, equalTo(withStandardHeaders(prefixFile1(false))));
  }

  @Test
  public void roundtrip3()
      throws IOException
  {
    // prefixFile2 is "find created" like, see CENTRAL-515
    when(storageFileItem.getInputStream()).thenReturn(
        new ByteArrayInputStream(prefixFile3(true).getBytes(UTF8)));
    final List<String> entries = m.read(storageFileItem).entries();
    assertThat(entries, is(notNullValue()));
    assertThat(entries.size(), is(1));

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    m.write(entries, outputStream);
    assertThat(outputStream.size(), greaterThan(1));

    final String output = new String(outputStream.toByteArray(), UTF8);
    // once read, the file looses "peculiarities" as dots from start and comments
    // naturally this applies to all nexus-managed files only (hosted + groups) as proxy WLs are
    // passed on as-is (unchanged)
    assertThat(output, equalTo(withStandardHeaders(prefixFile3(false))));
  }

  @Test
  public void roundtrip4()
      throws IOException
  {
    // prefixFile2 is "find created" like, see CENTRAL-515
    when(storageFileItem.getInputStream()).thenReturn(
        new ByteArrayInputStream(prefixFile4(true, false).getBytes(UTF8)));
    final List<String> entries = m.read(storageFileItem).entries();
    assertThat(entries, is(notNullValue()));
    assertThat(entries.size(), is(3));

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    m.write(entries, outputStream);
    assertThat(outputStream.size(), greaterThan(1));

    final String output = new String(outputStream.toByteArray(), UTF8);
    // once read, the file looses "peculiarities" as dots from start and comments
    // naturally this applies to all nexus-managed files only (hosted + groups) as proxy WLs are
    // passed on as-is (unchanged)
    assertThat(output, equalTo(withStandardHeaders(prefixFile4(false, true))));
  }
}
