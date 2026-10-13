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
package org.sonatype.nexus.repository.group.rendered;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.proxy.ProxyFacet;
import org.sonatype.nexus.repository.proxy.ProxyRepositoryConfiguration;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link GroupRenderedViewSupport}.
 */
public class GroupRenderedViewSupportTest
{
  @Test
  public void mixedMembers_returnsMinMetadataMaxAge() {
    Repository proxy1 = mockProxyRepository(1440); // 1440 seconds
    Repository proxy2 = mockProxyRepository(60); // 60 seconds
    Repository hosted = mockHostedRepository();

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Arrays.asList(proxy1, proxy2, hosted));

    assertThat(result, equalTo(60));
  }

  @Test
  public void noProxyMembers_returnsFiniteDefault() {
    Repository hosted1 = mockHostedRepository();
    Repository hosted2 = mockHostedRepository();

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Arrays.asList(hosted1, hosted2));

    // A hosted-only group has no proxy metadataMaxAge to derive a bound from. It must still get
    // a finite bound, otherwise a materialised rendered view would never expire on time alone.
    assertThat(result, equalTo(GroupRenderedViewSupport.NO_PROXY_MEMBER_MAX_AGE_SECONDS));
  }

  @Test
  public void noProxyMembers_finiteDefaultIsPositiveAndMatchesTwentyFourHours() {
    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Collections.singletonList(mockHostedRepository()));

    // Guards the property the TTL-overlay consumer depends on: the value must pass a >= 0 check
    // so the staleness branch is actually taken for hosted-only groups.
    assertThat(result >= 0, equalTo(true));
    assertThat(result, equalTo((int) Duration.ofHours(24).toSeconds()));
  }

  @Test
  public void emptyMemberList_returnsFiniteDefault() {
    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Collections.emptyList());

    assertThat(result, equalTo(GroupRenderedViewSupport.NO_PROXY_MEMBER_MAX_AGE_SECONDS));
  }

  @Test
  public void negativeMaxAge_filteredOut() {
    Repository proxy1 = mockProxyRepository(-1);
    Repository proxy2 = mockProxyRepository(100);

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Arrays.asList(proxy1, proxy2));

    assertThat(result, equalTo(100));
  }

  @Test
  public void allNegativeMaxAge_returnsNoTtlBound() {
    Repository proxy1 = mockProxyRepository(-1);
    Repository proxy2 = mockProxyRepository(-5);

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Arrays.asList(proxy1, proxy2));

    // Distinct from the no-proxy-member case above: proxies DO exist here, and a negative
    // metadataMaxAge is an explicit "never expire" choice on the member. We report no bound
    // rather than silently imposing one that is stricter than the operator configured.
    assertThat(result, equalTo(GroupRenderedViewSupport.NO_TTL_BOUND));
  }

  @Test
  public void allNegativeMaxAge_isNotConflatedWithNoProxyMembers() {
    int allNegative = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Collections.singletonList(mockProxyRepository(-1)));
    int noProxies = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Collections.singletonList(mockHostedRepository()));

    assertThat(allNegative, equalTo(GroupRenderedViewSupport.NO_TTL_BOUND));
    assertThat(noProxies, equalTo(GroupRenderedViewSupport.NO_PROXY_MEMBER_MAX_AGE_SECONDS));
    assertThat(allNegative == noProxies, equalTo(false));
  }

  @Test
  public void largeMaxAge_clampedToIntegerMaxValue() {
    // A metadataMaxAge far beyond Integer.MAX_VALUE seconds. Without the Math.min guard in
    // computeMinProxyMetadataMaxAgeSeconds, the narrowing (int) cast would wrap to a negative
    // value, which the >= 0 filter then discards, yielding NO_TTL_BOUND instead of a bound.
    // Divided by 1000 to stay clear of overflow inside Duration itself.
    Repository proxy = mockProxyRepository(Long.MAX_VALUE / 1000);

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Collections.singletonList(proxy));

    assertThat(result, equalTo(Integer.MAX_VALUE));
  }

  @Test
  public void singleProxy_returnsItsMaxAge() {
    Repository proxy = mockProxyRepository(3600);

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Collections.singletonList(proxy));

    assertThat(result, equalTo(3600));
  }

  @Test
  public void zeroMaxAge_includedInMin() {
    Repository proxy1 = mockProxyRepository(0);
    Repository proxy2 = mockProxyRepository(100);

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Arrays.asList(proxy1, proxy2));

    assertThat(result, equalTo(0));
  }

  @Test
  public void noProxyMembers_withCustomDefault_returnsCustomDefault() {
    Repository hosted = mockHostedRepository();

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Collections.singletonList(hosted), 900);

    assertThat(result, equalTo(900));
  }

  @Test
  public void oneArgOverload_stillReturnsBuiltInDefault() {
    // Regression guard: the 1-arg overload must keep delegating to NO_PROXY_MEMBER_MAX_AGE_SECONDS,
    // not silently change behaviour when the 2-arg overload is introduced.
    Repository hosted = mockHostedRepository();

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Collections.singletonList(hosted));

    assertThat(result, equalTo(GroupRenderedViewSupport.NO_PROXY_MEMBER_MAX_AGE_SECONDS));
  }

  @Test
  public void proxyMembersExist_customDefaultDoesNotOverrideMin() {
    // The custom default must only substitute in the no-proxy-members branch.
    Repository proxy1 = mockProxyRepository(1440);
    Repository proxy2 = mockProxyRepository(60);

    int result = GroupRenderedViewSupport.computeMinProxyMetadataMaxAgeSeconds(
        Arrays.asList(proxy1, proxy2), 900);

    assertThat(result, equalTo(60));
  }

  // Helper methods

  private Repository mockProxyRepository(long metadataMaxAgeSeconds) {
    Repository repo = mock(Repository.class);
    ProxyFacet proxyFacet = mock(ProxyFacet.class);
    ProxyRepositoryConfiguration config = mock(ProxyRepositoryConfiguration.class);

    when(config.getMetadataMaxAge()).thenReturn(Duration.ofSeconds(metadataMaxAgeSeconds));
    when(proxyFacet.getConfiguration()).thenReturn(config);
    when(repo.optionalFacet(ProxyFacet.class)).thenReturn(Optional.of(proxyFacet));

    return repo;
  }

  private Repository mockHostedRepository() {
    Repository repo = mock(Repository.class);
    when(repo.optionalFacet(ProxyFacet.class)).thenReturn(Optional.empty());
    return repo;
  }
}
