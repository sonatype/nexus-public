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
package org.sonatype.nexus.cleanup.internal.content.search;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.ComponentSet;
import org.sonatype.nexus.repository.content.facet.ContentFacet;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;
import org.sonatype.nexus.repository.content.store.ComponentData;
import org.sonatype.nexus.repository.content.store.ComponentSetData;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class ComponentSetCleanupBrowserTest extends TestSupport {

  private static final String[] TLDS = new String[]{"com", "org", "net", "dev"};
  private static final String[] DOMAINS = new String[]{"example", "sonatype"};
  private static final String[] NAMES = new String[]{"nexus", "iq"};
  @Parameterized.Parameter
  public int browseLimit;
  @Mock
  private Repository mockRepository;
  @Mock
  private CleanupPolicy mockCleanupPolicy;
  @Mock
  private ContentFacet mockContentFacet;
  @Mock
  private FluentComponents mockFluentComponents;
  private ComponentSetCleanupBrowser undertest;

  // We test browse limit with primes to try and catch any 'edges'
  @Parameterized.Parameters
  public static Collection<Integer> data() {
    return Arrays.asList(1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79,
            83, 89, 97, 101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 1000, 9973);
  }

  private static String getValue(String[] choices, int index) {
    return choices[index % choices.length];
  }

  @Before
  public void setup() {
    doReturn(mockContentFacet).when(mockRepository).facet(any());
    doReturn(mockFluentComponents).when(mockContentFacet).components();
    Map<String, String> criteria = new HashMap<>();
    criteria.put("retain", "1");
    criteria.put("sortBy", "version");
    doReturn(criteria).when(mockCleanupPolicy).getCriteria();
    undertest = spy(new ComponentSetCleanupBrowser(mockRepository, mockCleanupPolicy, false));
  }

  @Test
  public void testBrowseSingleSetSingleComponentBySet() {
    doAnswer(a -> generateMockComponentSets(1, a)).when(mockFluentComponents).sets(anyInt(), any());
    doAnswer(a -> generateMockComponents(1, a)).when(mockFluentComponents)
            .byCleanupCriteria(any(), anyMap(), eq(false), anyInt(), any());
    assertThat(simulateBrowseLoop(), is(1));
    verify(undertest, atLeast(1)).processComponentSets(eq(browseLimit), any(), any());
    verify(undertest, atMost(3)).processComponentSets(eq(browseLimit), any(), any());
    verify(undertest, atLeast(1)).processComponents(eq(browseLimit), any(), any(), any());
    verify(undertest, atMost(2)).processComponents(eq(browseLimit), any(), any(), any());

  }

  @Test
  public void testBrowseSingleSetMultipleComponents() {
    doAnswer(a -> generateMockComponentSets(1, a)).when(mockFluentComponents).sets(anyInt(), any());
    doAnswer(a -> generateMockComponents(13, a)).when(mockFluentComponents)
            .byCleanupCriteria(any(), anyMap(), eq(false), anyInt(), any());
    assertThat(simulateBrowseLoop(), is(13));
    verify(undertest, atLeast(13 / browseLimit))
            .processComponents(eq(browseLimit), any(), any(), any());
    verify(undertest, atMost((13 / browseLimit) + 1))
            .processComponents(eq(browseLimit), any(), any(), any());
  }

  @Test
  public void testBrowseMultipleSetsSingleComponent() {
    doAnswer(a -> generateMockComponentSets(13, a)).when(mockFluentComponents).sets(anyInt(), any());
    doAnswer(a -> generateMockComponents(1, a)).when(mockFluentComponents)
            .byCleanupCriteria(any(), anyMap(), eq(false), anyInt(), any());
    assertThat(simulateBrowseLoop(), is(13));
  }

  @Test
  public void testBrowseMultipleSetsMultipleComponents() {
    doAnswer(a -> generateMockComponentSets(13, a)).when(mockFluentComponents).sets(anyInt(), any());
    doAnswer(a -> generateMockComponents(13, a)).when(mockFluentComponents)
            .byCleanupCriteria(any(), anyMap(), eq(false), anyInt(), any());
    assertThat(simulateBrowseLoop(), is(169));
  }

  @Test
  public void testBrowseMultipleComponentsWithoutSet() {
    doReturn(new HashMap<>()).when(mockCleanupPolicy).getCriteria();
    doAnswer(a -> generateMockComponents(10000, a)).when(mockFluentComponents).byCleanupCriteria(any(), anyMap(), eq(false), anyInt(), any());
    assertThat(simulateBrowseLoop(), is(10000));
    verify(undertest, never()).processComponentSets(anyInt(), any(), any());
    verify(undertest, atMost((10000 / browseLimit) + 1)) // 1 extra for 'no more results'
            .processComponents(eq(browseLimit),
                    eq(null), any(), any());
  }

  // This simulates the larger browse 'loop' that the code would be used within, otherwise it would just be testing
  // database access, which isn't really in scope
  int simulateBrowseLoop() {
    String token = null;
    int count = 0;
    String oldToken = null;
    do {
      Continuation<FluentComponent> result = undertest.browse(browseLimit, token);

      oldToken = token;
      token = result.nextContinuationToken();
      if (token != null) {
        assertThat("new token should not match old token", token, not(oldToken));
      }
      count += result.size();
    } while (token != null);
    return count;
  }

  Continuation<ComponentSetData> generateMockComponentSets(int count, InvocationOnMock a) {
    return generateComponentSets(count, a.getArgument(0), a.getArgument(1));
  }

  private Continuation<ComponentSetData> generateComponentSets(final int count, int limit, final String offset) {
    Continuation<ComponentSetData> componentSets = new ContinuationArrayList<>();
    int intOffset = offset == null ? 0 : Integer.parseInt(offset) + 1;
    for (int i = intOffset; i < count; i++) {
      final int index = i;
      ComponentSetData c = new ComponentSetData() {
        @Override
        public String nextContinuationToken() {
          return "" + index;
        }
      };
      c.setNamespace(getValue(TLDS, i) + "." + getValue(DOMAINS, i));
      c.setName(getValue(NAMES, i));
      componentSets.add(c);
      if (componentSets.size() == limit) {
        break;
      }
    }
    return componentSets;
  }

  Continuation<ComponentData> generateMockComponents(int count, InvocationOnMock a) {
    return generateComponents(a.getArgument(0), count, a.getArgument(3), a.getArgument(4));
  }

  private Continuation<ComponentData> generateComponents(final ComponentSet componentSet, final int count,
                                                         final int limit, final String offset) {
    Continuation<ComponentData> components = new ContinuationArrayList<>();
    int intOffset = offset == null ? 0 : Integer.parseInt(offset) + 1;
    for (int i = intOffset; i < count; i++) {
      ComponentData c = new ComponentData();
      c.setNamespace(componentSet == null ? "" : componentSet.namespace());
      c.setName(componentSet == null ? "" : componentSet.name());
      c.setComponentId(i);
      components.add(c);
      if (components.size() == limit) {
        break;
      }
    }
    return components;
  }
}
