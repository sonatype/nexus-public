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
package org.sonatype.nexus.proxy.targets;

import org.sonatype.nexus.proxy.repository.Repository;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Simple DefaultTargetRegistry creation test
 */
public class DefaultTargetRegistryTest
    extends AbstractDefaultTargetRegistryTest
{
  @Test
  public void testSimpleM2() {
    // create a dummy
    Repository repository = mock(Repository.class);
    doReturn("dummy").when(repository).getId();
    doReturn(maven2).when(repository).getRepositoryContentClass();

    TargetSet ts =
        targetRegistry.getTargetsForRepositoryPath(repository,
            "/org/apache/maven/maven-core/2.0.9/maven-core-2.0.9.pom");

    assertThat(ts, notNullValue());
    assertThat(ts.getMatches().size(), equalTo(2));
    assertThat(ts.getMatchedRepositoryIds().size(), equalTo(1));
    assertThat(ts.getMatchedRepositoryIds().iterator().next(), equalTo("dummy"));

    TargetSet ts1 =
        targetRegistry.getTargetsForRepositoryPath(repository,
            "/org/apache/maven/maven-core/2.0.9/maven-core-2.0.9-sources.jar");

    assertThat(ts1, notNullValue());
    assertThat(ts1.getMatches().size(), equalTo(1));
    assertThat(ts1.getMatches().iterator().next().getTarget().getId(), equalTo("maven2-with-sources"));

    // adding them
    ts.addTargetSet(ts1);

    assertThat(ts, notNullValue());
    assertThat(ts.getMatches().size(), equalTo(2));
    assertThat(ts.getMatchedRepositoryIds().size(), equalTo(1));
  }

  @Test
  public void testSimpleM1() {
    // create a dummy
    Repository repository = mock(Repository.class);
    doReturn("dummy").when(repository).getId();
    doReturn(maven1).when(repository).getRepositoryContentClass();

    TargetSet ts =
        targetRegistry.getTargetsForRepositoryPath(repository, "/org.apache.maven/jars/maven-model-v3-2.0.jar");

    assertThat(ts, notNullValue());
    assertThat(ts.getMatches().size(), equalTo(1));

    ts =
        targetRegistry.getTargetsForRepositoryPath(repository,
            "/org/apache/maven/maven-core/2.0.9/maven-core-2.0.9-sources.jar");

    assertThat(ts, notNullValue());
    assertThat(ts.getMatches().size(), equalTo(0));
  }

}
