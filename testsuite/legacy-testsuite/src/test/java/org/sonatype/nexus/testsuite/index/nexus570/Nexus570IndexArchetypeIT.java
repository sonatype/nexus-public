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
package org.sonatype.nexus.testsuite.index.nexus570;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.integrationtests.TestContainer;
import org.sonatype.nexus.rest.model.NexusArtifact;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class Nexus570IndexArchetypeIT
    extends AbstractNexusIntegrationTest
{

  @BeforeClass
  public static void setSecureTest() {
    TestContainer.getInstance().getTestContext().setSecureTest(true);
  }

  @Test
  public void searchForArchetype() throws Exception {
    // Short explanation what (might) happen here
    // 1) for start, indexing of deployed stuff is done async
    // 2) second, superclass of this IT deploys using
    // deployArtifacts( File project, String wagonHint, String deployUrl, Model model ) method
    // that does not wait for async event handlers to calm down, but is important to
    // state that deploy happens in "maven order", POM then JAR (will become important later)
    // 3) third, the "simple-archetype" being deployed has POM that does not state packaging,
    // hence, default "jar" packaging is assumed (this is maven "rule").
    // 4) fourth, the JAR that is deployed 2nd, does have HETA-INF/archetype.xml, present in archetypes only
    // 5) fifth, due to "known problems" for problem "how to detect is an artifact and archetype or not",
    // maven indexer contains a "bit" of heuristics. Maven Archetypes were recognized in many ways,
    // but until recently they had no proper "packaging" set to "maven-archetype". Older archetypes
    // had JAR packaging in POM and only the aforementioned XML was telling they are archetypes and not "plain JARs"
    // actually. Also, there is a mess with "partial archetypes", etc.
    // 6) Finally, sixth, as Nexus receives deploys (POM then JAR), it will kick indexer at every of them
    // but the actually indexed data will "incrementally" become correct: at POM deploy JAR will be not
    // present, so Indexer will create "partial" context based on POM only (hence, packaging will be JAR
    // and for example no classnames will be indexed due to the lack of JAR). On 2nd deploy of JAR,
    // the created context will be complete (as now bot POM and JAR will be present), and packaging
    // will be properly set to "maven-archetype".
    //
    // See the "heuristic" here (MI 5.1.0):
    // https://github.com/apache/maven-indexer/blob/91147a9311b7aef2dce50e581f4fafad710486f3/indexer-core/src/main/java/org/apache/maven/index/creator/MavenArchetypeArtifactInfoIndexCreator.java#L37
    // https://github.com/apache/maven-indexer/blob/91147a9311b7aef2dce50e581f4fafad710486f3/indexer-core/src/main/java/org/apache/maven/index/creator/MinimalArtifactInfoIndexCreator.java#L50
    //
    // So what happened, is that the assertion below got a "hit" from Indexer, but the hit was actually
    // returned from "partially" indexed artifact, and due to JAR not yet indexed, Maven Indexer
    // did it's best: it assumed packaging is JAR, the information told by POM.
    //
    // Solution(s):
    // a) added wait for calm down (should make everything work more reliable even without b))
    // b) added proper packaging to POM of the resource of this IT
    //

    getEventInspectorsUtil().waitForCalmPeriod();
    Map<String, String> args = new HashMap<String, String>();
    args.put("a", "simple-archetype");
    args.put("g", "nexus570");

    List<NexusArtifact> results = getSearchMessageUtil().searchFor(args);

    Assert.assertEquals(1, results.size());
    Assert.assertEquals("Expected maven-archetype packaging: "
        + results.get(0).getPackaging(), results.get(0).getPackaging(),
        "maven-archetype");

  }

  @Test
  public void searchForjar() throws Exception {
    // unneeded here, as only POM will do for assertion to succeed
    // see above full explanation
    getEventInspectorsUtil().waitForCalmPeriod();
    Map<String, String> args = new HashMap<String, String>();
    args.put("a", "normal");
    args.put("g", "nexus570");

    List<NexusArtifact> results = getSearchMessageUtil().searchFor(args);

    Assert.assertEquals(results.size(), 1);
    Assert.assertEquals("Expected jar packaging: " + results.get(0).getPackaging(), results.get(0).getPackaging(),
        "jar");

  }

}
