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

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class PathMatcherTest
{
  protected List<String> entries1 = Arrays.asList("/org/sonatype", "/com/sonatype/nexus",
      "/biz/sonatype/nexus/plugins", "/archetype-metadata.xml");

  protected List<String> entries2 = Arrays.asList("/A/1", "/B/1/2", "/C/1/2/3", "/D/1/2/3/4", "/E/1/2/3/4/5");

  @Test
  public void smoke() {
    final PathMatcher pm = new PathMatcher(entries1, 2);

    assertThat("Should not match!", !pm.matches("/org"));
    assertThat("Should not match!", !pm.matches("/archetype"));
    assertThat("Should not match!", !pm.matches("/archetype-metadata"));

    assertThat("Should match!", pm.matches("/archetype-metadata.xml"));
    assertThat("Should match!", pm.matches("/org/sonatype"));
    assertThat("Should match!", pm.matches("/org/sonatype/"));

    // we constructed WL with depth of 2, so all these below will match
    // even if WL does contain more specific entries, since
    // WL keeps the tree up to 2 level deep only

    // per-prefix
    assertThat("Should match!", pm.matches("/org/sonatype"));
    assertThat("Should match!", pm.matches("/org/sonatype/"));
    assertThat("Should match!", pm.matches("/org/sonatype/nexus"));
    assertThat("Should match!", pm.matches("/org/sonatype/nexus/plugins"));
    assertThat("Should match!", pm.matches("/org/sonatype/barfoo"));
    assertThat("Should match!", pm.matches("/org/sonatype/foobar"));
    // per-prefix
    assertThat("Should match!", pm.matches("/com/sonatype"));
    assertThat("Should match!", pm.matches("/com/sonatype/"));
    assertThat("Should match!", pm.matches("/com/sonatype/nexus"));
    assertThat("Should match!", pm.matches("/com/sonatype/nexus/plugins"));
    assertThat("Should match!", pm.matches("/com/sonatype/barfoo"));
    assertThat("Should match!", pm.matches("/com/sonatype/foobar"));
    // per-prefix
    assertThat("Should match!", pm.matches("/biz/sonatype"));
    assertThat("Should match!", pm.matches("/biz/sonatype/"));
    assertThat("Should match!", pm.matches("/biz/sonatype/nexus"));
    assertThat("Should match!", pm.matches("/biz/sonatype/nexus/plugins"));
    assertThat("Should match!", pm.matches("/biz/sonatype/barfoo"));
    assertThat("Should match!", pm.matches("/biz/sonatype/foobar"));
  }

  protected void check(PathMatcher pm, String path, boolean shouldMatch) {
    if (shouldMatch) {
      assertThat(path + " should match!", pm.matches(path));
    }
    else {
      assertThat(path + " should not match!", !pm.matches(path));
    }
  }

  @Test
  public void testMaxDepth() {
    final PathMatcher pm1 = new PathMatcher(entries2, 2);
    final PathMatcher pm2 = new PathMatcher(entries2, 3);
    final PathMatcher pm3 = new PathMatcher(entries2, 4);

    // wl1 is 2 deep, so whatever is on level 3+ is neglected
    check(pm1, "/A/1/X/3/4/5/6/7/8/9/0", true);
    check(pm1, "/B/1/X/3/4/5/6/7/8/9/0", true);
    check(pm1, "/C/1/X/3/4/5/6/7/8/9/0", true);
    check(pm1, "/D/1/X/3/4/5/6/7/8/9/0", true);
    check(pm1, "/E/1/X/3/4/5/6/7/8/9/0", true);
    check(pm1, "/F/1/X/3/4/5/6/7/8/9/0", false);

    check(pm1, "/A/1/2/X/4/5/6/7/8/9/0", true);
    check(pm1, "/B/1/2/X/4/5/6/7/8/9/0", true);
    check(pm1, "/C/1/2/X/4/5/6/7/8/9/0", true);
    check(pm1, "/D/1/2/X/4/5/6/7/8/9/0", true);
    check(pm1, "/E/1/2/X/4/5/6/7/8/9/0", true);
    check(pm1, "/F/1/2/X/4/5/6/7/8/9/0", false);

    check(pm1, "/A/1/2/3/X/5/6/7/8/9/0", true);
    check(pm1, "/B/1/2/3/X/5/6/7/8/9/0", true);
    check(pm1, "/C/1/2/3/X/5/6/7/8/9/0", true);
    check(pm1, "/D/1/2/3/X/5/6/7/8/9/0", true);
    check(pm1, "/E/1/2/3/X/5/6/7/8/9/0", true);
    check(pm1, "/F/1/2/3/X/5/6/7/8/9/0", false);

    // wl2 is 3 deep
    check(pm2, "/A/1/X/3/4/5/6/7/8/9/0", true);
    check(pm2, "/B/1/X/3/4/5/6/7/8/9/0", false);
    check(pm2, "/C/1/X/3/4/5/6/7/8/9/0", false);
    check(pm2, "/D/1/X/3/4/5/6/7/8/9/0", false);
    check(pm2, "/E/1/X/3/4/5/6/7/8/9/0", false);
    check(pm2, "/F/1/X/3/4/5/6/7/8/9/0", false);

    check(pm2, "/A/1/2/X/4/5/6/7/8/9/0", true);
    check(pm2, "/B/1/2/X/4/5/6/7/8/9/0", true);
    check(pm2, "/C/1/2/X/4/5/6/7/8/9/0", true);
    check(pm2, "/D/1/2/X/4/5/6/7/8/9/0", true);
    check(pm2, "/E/1/2/X/4/5/6/7/8/9/0", true);
    check(pm2, "/F/1/2/X/4/5/6/7/8/9/0", false);

    check(pm2, "/A/1/2/3/X/5/6/7/8/9/0", true);
    check(pm2, "/B/1/2/3/X/5/6/7/8/9/0", true);
    check(pm2, "/C/1/2/3/X/5/6/7/8/9/0", true);
    check(pm2, "/D/1/2/3/X/5/6/7/8/9/0", true);
    check(pm2, "/E/1/2/3/X/5/6/7/8/9/0", true);
    check(pm2, "/F/1/2/3/X/5/6/7/8/9/0", false);

    // wl3 is 4 deep
    check(pm3, "/A/1/X/3/4/5/6/7/8/9/0", true);
    check(pm3, "/B/1/X/3/4/5/6/7/8/9/0", false);
    check(pm3, "/C/1/X/3/4/5/6/7/8/9/0", false);
    check(pm3, "/D/1/X/3/4/5/6/7/8/9/0", false);
    check(pm3, "/E/1/X/3/4/5/6/7/8/9/0", false);
    check(pm3, "/F/1/X/3/4/5/6/7/8/9/0", false);

    check(pm3, "/A/1/2/X/4/5/6/7/8/9/0", true);
    check(pm3, "/B/1/2/X/4/5/6/7/8/9/0", true);
    check(pm3, "/C/1/2/X/4/5/6/7/8/9/0", false);
    check(pm3, "/D/1/2/X/4/5/6/7/8/9/0", false);
    check(pm3, "/E/1/2/X/4/5/6/7/8/9/0", false);
    check(pm3, "/F/1/2/X/4/5/6/7/8/9/0", false);

    check(pm3, "/A/1/2/3/X/5/6/7/8/9/0", true);
    check(pm3, "/B/1/2/3/X/5/6/7/8/9/0", true);
    check(pm3, "/C/1/2/3/X/5/6/7/8/9/0", true);
    check(pm3, "/D/1/2/3/X/5/6/7/8/9/0", true);
    check(pm3, "/E/1/2/3/X/5/6/7/8/9/0", true);
    check(pm3, "/F/1/2/3/X/5/6/7/8/9/0", false);
  }

  @Test
  public void testLeastSpecificWinsMaxDepth3() {
    final PathMatcher pm = new PathMatcher(Arrays.asList("/a/b/c", "/a/b/c/d/e", "/a/b"), 3);
    check(pm, "/a/b/c/d/e", true);
    check(pm, "/a/b/c/d", true);
    check(pm, "/a/b/c", true);
    check(pm, "/a/b", true); // "/a/b" won
    check(pm, "/a", false);
    assertThat(pm.contains("/a"), is(true));
    check(pm, "/a/X", false);
    assertThat(pm.contains("/a/X"), is(false));
  }

  @Test
  public void testLeastSpecificWinsMaxDepth2() {
    final PathMatcher pm = new PathMatcher(Arrays.asList("/a/b/c", "/a/b/c/d/e", "/a/b"), 2);
    check(pm, "/a/b/c/d/e", true);
    check(pm, "/a/b/c/d", true);
    check(pm, "/a/b/c", true);
    check(pm, "/a/b", true); // "/a/b" won
    check(pm, "/a", false);
    assertThat(pm.contains("/a"), is(true));
    check(pm, "/a/c", false);
    assertThat(pm.contains("/a/c"), is(false));
    check(pm, "/X", false);
    assertThat(pm.contains("/X"), is(false));
  }

  @Test
  public void testLeastSpecificWinsMaxDepth2EntriesLongerThenDepth() {
    final PathMatcher pm = new PathMatcher(Arrays.asList("/a/b/c/d/e", "/a/b/c/d/e/f", "/a/b/c"), 2);
    check(pm, "/a/b/c/d/e", true);
    check(pm, "/a/b/c/d", true);
    check(pm, "/a/b/c", true);
    check(pm, "/a/b", true); // "/a/b" won
    check(pm, "/a", false);
    assertThat(pm.contains("/a"), is(true));
    check(pm, "/a/c", false);
    assertThat(pm.contains("/a/c"), is(false));
    check(pm, "/X", false);
    assertThat(pm.contains("/X"), is(false));
  }
}
