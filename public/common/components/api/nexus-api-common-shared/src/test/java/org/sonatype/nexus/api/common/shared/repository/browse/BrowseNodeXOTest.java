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
package org.sonatype.nexus.api.common.shared.repository.browse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Tests {@link BrowseNodeXO}.
 */
class BrowseNodeXOTest
{
  @Test
  void fluentSettersPopulateAllFields() {
    BrowseNodeXO node = new BrowseNodeXO()
        .withId("com/example")
        .withText("example")
        .withType("folder")
        .withLeaf(false)
        .withComponentId("comp-1")
        .withAssetId("asset-1")
        .withPackageUrl("pkg:maven/com.example/example");

    assertThat(node.getId(), is("com/example"));
    assertThat(node.getText(), is("example"));
    assertThat(node.getType(), is("folder"));
    assertThat(node.isLeaf(), is(false));
    assertThat(node.getComponentId(), is("comp-1"));
    assertThat(node.getAssetId(), is("asset-1"));
    assertThat(node.getPackageUrl(), is("pkg:maven/com.example/example"));
  }

  @Test
  void equalityIsBasedOnIdOnly() {
    BrowseNodeXO a = new BrowseNodeXO().withId("same").withText("a").withType("folder");
    BrowseNodeXO b = new BrowseNodeXO().withId("same").withText("b").withType("asset").withLeaf(true);
    BrowseNodeXO c = new BrowseNodeXO().withId("different").withText("a").withType("folder");

    assertThat(a, is(b));
    assertThat(a.hashCode(), is(b.hashCode()));
    assertThat(a, is(not(c)));
  }

  @Test
  void toStringIncludesId() {
    assertThat(new BrowseNodeXO().withId("com/example").toString(), is(not("")));
    assertThat(new BrowseNodeXO().withId("com/example").toString().contains("com/example"), is(true));
  }
}
