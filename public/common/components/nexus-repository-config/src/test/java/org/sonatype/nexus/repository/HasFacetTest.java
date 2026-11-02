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
package org.sonatype.nexus.repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link HasFacet}.
 */
public class HasFacetTest
{
  interface FirstFacet
      extends Facet
  {
  }

  interface SecondFacet
      extends Facet
  {
  }

  interface ThirdFacet
      extends Facet
  {
  }

  @Test
  public void testIsTrueOnlyForRepositoriesWithTheFacet() {
    Repository withFirst = repositoryWith(FirstFacet.class);
    Repository withSecond = repositoryWith(SecondFacet.class);

    assertThat(new HasFacet(FirstFacet.class).test(withFirst), is(true));
    assertThat(new HasFacet(FirstFacet.class).test(withSecond), is(false));
  }

  @Test
  public void testFactoryMethodCreatesAnEquivalentPredicate() {
    assertThat(HasFacet.hasFacet(FirstFacet.class).test(repositoryWith(FirstFacet.class)), is(true));
    assertThat(HasFacet.hasFacet(FirstFacet.class).test(repositoryWith(SecondFacet.class)), is(false));
  }

  @Test
  public void testComposesWithOtherJdkPredicates() {
    Repository withFirst = repositoryWith(FirstFacet.class);
    Repository withSecond = repositoryWith(SecondFacet.class);
    Repository withThird = repositoryWith(ThirdFacet.class);

    Predicate<Repository> firstOrSecond = new HasFacet(FirstFacet.class).or(new HasFacet(SecondFacet.class));

    List<Repository> matching = Stream.of(withFirst, withSecond, withThird)
        .filter(firstOrSecond)
        .collect(Collectors.toList());

    assertThat(matching, contains(withFirst, withSecond));
  }

  private static Repository repositoryWith(final Class<? extends Facet> facetType) {
    Repository repository = mock(Repository.class);
    when(repository.optionalFacet(facetType)).thenAnswer(invocation -> Optional.of(mock(facetType)));
    return repository;
  }
}
