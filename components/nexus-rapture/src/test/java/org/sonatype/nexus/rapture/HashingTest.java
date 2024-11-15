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
package org.sonatype.nexus.rapture;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import org.junit.Test;
import org.sonatype.goodies.testsupport.TestSupport;

import com.google.common.hash.Hashing;
import com.google.gson.GsonBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Hashing trials.
 */
public class HashingTest
    extends TestSupport
{

  @Test
  public void testGsonHash() {
    Gson gson = new GsonBuilder().create();
    List<String> data = Arrays.asList("a", "b", "c");
    String hash = Hashing.sha1().hashString(gson.toJson(data), StandardCharsets.UTF_8).toString();
    log(hash);
    assertEquals("e13460afb1e68af030bb9bee8344c274494661fa", hash);
  }

  @Test
  public void testObjectHash() {
    List<String> data = new ArrayList<>(Arrays.asList("a", "b", "c"));
    String initial = String.valueOf(data.hashCode());
    log(initial);
    data.add("d");
    String after = String.valueOf(data.hashCode());
    log(after);
    assertNotEquals(initial, after);
  }
}
