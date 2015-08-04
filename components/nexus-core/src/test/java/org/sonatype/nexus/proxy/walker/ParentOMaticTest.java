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
package org.sonatype.nexus.proxy.walker;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * Tests for ParentOMatic class.
 *
 * @author cstamas
 */
public class ParentOMaticTest
    extends TestSupport
{
  protected void printListPerLine(final List<String> strings) {
    for (String string : strings) {
      print(string);
    }
  }

  /**
   * Flip this to false if you want to see output on System.out instead do actual comparison. Useful if behaviour
   * changes, and you need to generate text files that you can save (after you verifies it's correctness) to assert
   * against it. With having COMPARE=false this test will never fail!
   */
  private final boolean COMPARE = true;

  private StringBuilder stringBuilder;

  protected void print(final String str) {
    if (COMPARE) {
      stringBuilder.append(str).append("\n");
    }
    else {
      System.out.println(str);
    }
  }

  protected void doAssert()
      throws Exception
  {
    if (COMPARE) {
      final String callerName = getCallerMethodName();
      final String actualClasspathName = getClass().getSimpleName() + "-" + callerName + ".txt";
      final InputStream actualInputStream = getClass().getResourceAsStream(actualClasspathName);
      final String actual = IOUtils.toString(actualInputStream).replace("\r\n", "\n");

      assertThat(actual, Matchers.equalTo(stringBuilder.toString()));
    }
  }

  protected String getCallerMethodName() {
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    return stackTrace[3].getMethodName();
  }

  /**
   * Simple "naive" case. Just adding a bunch of paths.
   */
  @Test
  public void exampleCase()
      throws Exception
  {
    stringBuilder = new StringBuilder();
    final ParentOMatic cn = new ParentOMatic();

    print("Example case");
    print("");
    cn.addAndMarkPath("/foo/bam/car2");
    cn.addAndMarkPath("/foo/baz");
    cn.addAndMarkPath("/foo/baz/foo");
    cn.addAndMarkPath("/foo/bar");
    cn.addAndMarkPath("/foo/bar/car1");
    cn.addAndMarkPath("/foo/bar/car3");
    print(cn.dump());
    print("");
    print("Maven MD recreate would run against paths:");
    printListPerLine(cn.getMarkedPaths());
    doAssert();
  }

  /**
   * "Peter's case" as Peter did actually implement this and realized that snapshot removal (main work) takes 3
   * minutes, and all the "bookkeeping" takes 20 minutes. This is kinda "generated" repository and snapshot removals
   * are equally spread out.
   */
  @Test
  public void petersCase()
      throws Exception
  {
    stringBuilder = new StringBuilder();
    final ParentOMatic cn = new ParentOMatic();

    print("Peter's case");
    print("");
    cn.addAndMarkPath("/g1/a1/v1");
    cn.addAndMarkPath("/g1/a1/v2");
    cn.addAndMarkPath("/g1/a1/v3");
    cn.addAndMarkPath("/g1/a2/v1");
    cn.addAndMarkPath("/g1/a2/v2");
    cn.addAndMarkPath("/g1/a2/v3");
    cn.addAndMarkPath("/g1/a3/v1");
    cn.addAndMarkPath("/g1/a3/v2");
    cn.addAndMarkPath("/g1/a3/v3");
    print(cn.dump());
    print("");
    print("Maven MD recreate would run against paths:");
    printListPerLine(cn.getMarkedPaths());
    doAssert();
  }

  /**
   * WL merge case: when merge of two WL's happens, and both of them contains a prefix (like "/a/b" in one and
   * "/a/b/c" in other), the least specific must win, as otherwise if both left in merged list (if we would check
   * entry equality only), most specific would win as least would be just a parent and not a leaf, hence, not matched
   * when WL used in proxy.
   */
  @Test
  public void wlMergeCase()
      throws Exception
  {
    final ParentOMatic cn = new ParentOMatic(true, true, false);
    cn.addAndMarkPath("/g1/a1");
    cn.addAndMarkPath("/g1/a1/v2"); // "most specific" addition is not as "least specific" already added
    cn.addAndMarkPath("/g1/a1/v3"); // "most specific" addition is not as "least specific" already added
    cn.addAndMarkPath("/g1/a2/v1");
    cn.addAndMarkPath("/g1/a2/v2");
    cn.addAndMarkPath("/g1/a2"); // adding it later
    cn.addAndMarkPath("/g1/a3/v1");
    cn.addAndMarkPath("/g1/a3/v2");
    cn.addAndMarkPath("/g1/a3/v3");

    final List<String> mergedPaths = cn.getMarkedPaths();
    Collections.sort(mergedPaths);
    assertThat(mergedPaths.size(), is(5));
    assertThat(mergedPaths, contains("/g1/a1", "/g1/a2", "/g1/a3/v1", "/g1/a3/v2", "/g1/a3/v3"));
  }
}
