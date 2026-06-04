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

import org.sonatype.nexus.common.io.ByteSize.ByteUnit;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.sonatype.nexus.common.io.ByteSize.gigaBytes;
import static org.sonatype.nexus.common.io.ByteSize.kiloBytes;
import static org.sonatype.nexus.common.io.ByteSize.megaBytes;
import static org.sonatype.nexus.common.io.ByteSize.teraBytes;

/**
 * Tests for {@link ByteSize}.
 */
public class ByteSizeTest
{
  @Test
  public void parse_nM() throws Exception {
    ByteSize size = ByteSize.parse("100m");
    assertThat(size, equalTo(ByteSize.megaBytes(100)));
  }

  @Test
  public void parse_n_M() throws Exception {
    ByteSize size = ByteSize.parse("100 m");
    assertThat(size, equalTo(ByteSize.megaBytes(100)));
  }

  @Test
  public void parse__n_M_() throws Exception {
    ByteSize size = ByteSize.parse(" 100 m ");
    assertThat(size, equalTo(ByteSize.megaBytes(100)));
  }

  @Test
  public void parse_nMB() throws Exception {
    ByteSize size = ByteSize.parse("100mb");
    assertThat(size, equalTo(ByteSize.megaBytes(100)));
  }

  @Test
  public void toKilosToBytes() throws Exception {
    ByteSize size = ByteSize.kiloBytes(2);
    assertThat(size.value(), equalTo((long) 2));
    assertThat(size.toKiloBytes(), equalTo((long) 2));
    assertThat(size.toBytes(), equalTo((long) 2048));
  }

  @Test
  public void bytes() throws Exception {
    ByteSize size = ByteSize.bytes(1);
  }

  @Test
  public void kilo() throws Exception {
    ByteSize size = ByteSize.kiloBytes(1);
  }

  @Test
  public void mega() throws Exception {
    ByteSize size = ByteSize.megaBytes(1);
  }

  @Test
  public void giga() throws Exception {
    ByteSize size = ByteSize.gigaBytes(1);
  }

  @Test
  public void tera() throws Exception {
    ByteSize size = ByteSize.teraBytes(1);
  }

  @Test
  public void asKiloBytes() {
    ByteSize size = ByteSize.bytes(1024).asKiloBytes();
    assertThat(size.unit(), equalTo(ByteUnit.KILOBYTES));
    assertThat(size.value(), equalTo(1L));
  }

  // Upward conversions (no division involved — regression guard)

  @Test
  public void megaBytesToBytes() {
    assertThat(megaBytes(1).toBytes(), equalTo(1_048_576L));
  }

  @Test
  public void gigaBytesToBytes() {
    assertThat(gigaBytes(1).toBytes(), equalTo(1_073_741_824L));
  }

  @Test
  public void teraBytesToBytes() {
    assertThat(teraBytes(1).toBytes(), equalTo(1_099_511_627_776L));
  }

  @Test
  public void gigaBytesToMegaBytes() {
    assertThat(gigaBytes(1).toMegaBytes(), equalTo(1024L));
  }

  @Test
  public void teraBytesToMegaBytes() {
    assertThat(teraBytes(1).toMegaBytes(), equalTo(1_048_576L));
  }

  @Test
  public void teraBytesToGigaBytes() {
    assertThat(teraBytes(1).toGigaBytes(), equalTo(1024L));
  }

  // Downward conversions (the 6 previously buggy paths)

  @Test
  public void bytesToMegaBytes() {
    assertThat(ByteSize.bytes(1_048_576L).toMegaBytes(), equalTo(1L));
  }

  @Test
  public void bytesToGigaBytes() {
    assertThat(ByteSize.bytes(1_073_741_824L).toGigaBytes(), equalTo(1L));
  }

  @Test
  public void kiloBytesToGigaBytes() {
    assertThat(kiloBytes(1_048_576L).toGigaBytes(), equalTo(1L));
  }

  @Test
  public void bytesToTeraBytes() {
    assertThat(ByteSize.bytes(1_099_511_627_776L).toTeraBytes(), equalTo(1L));
  }

  @Test
  public void kiloBytesToTeraBytes() {
    assertThat(kiloBytes(1_073_741_824L).toTeraBytes(), equalTo(1L));
  }

  @Test
  public void megaBytesToTeraBytes() {
    assertThat(megaBytes(1_048_576L).toTeraBytes(), equalTo(1L));
  }
}
