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
package org.sonatype.nexus.coreui.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.coreui.RepositoryXO;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.coreui.internal.RepositoryCleanupAttributesUtil.initializeCleanupAttributes;

public class RepositoryCleanupAttributesUtilTest
    extends TestSupport
{
  private static final String CLEANUP_ATTRIBUTES_KEY = "cleanup";

  private static final String CLEANUP_NAME_KEY = "policyName";

  @Mock
  private RepositoryXO repositoryXO;

  private Map<String, Map<String, Object>> attributes = new HashMap<>();

  private Map<String, Object> cleanup = new HashMap<>();

  @Before
  public void setup() {
    when(repositoryXO.getAttributes()).thenReturn(attributes);
    attributes.put(CLEANUP_ATTRIBUTES_KEY, cleanup);
    cleanup.put(CLEANUP_NAME_KEY, asList("policy1", "policy2"));
  }

  @Test(expected = NullPointerException.class)
  public void when_No_RepositoryXO_Provided_Should_Fail() {
    initializeCleanupAttributes(null);
  }

  @Test
  public void when_Policies_Provided_Should_Convert_Into_Set() {
    initializeCleanupAttributes(repositoryXO);

    assertThat(getCleanupPolicyNames(), is(instanceOf(Set.class)));
  }

  @Test
  public void when_CleanupPolicies_Provided_Should_Return_Same_Policies_In_Order() {
    List<String> listPolicyNames = getCleanupPolicyNamesAsList();
    initializeCleanupAttributes(repositoryXO);

    Set<String> setPolicyNames = getCleanupPolicyNamesAsSet();
    assertThat(setPolicyNames, is(not(empty())));

    int index = 0;
    for (String policyName : getCleanupPolicyNamesAsSet()) {
      assertThat(listPolicyNames.get(index), is(equalTo(policyName)));
      index++;
    }
  }

  @Test
  public void when_Null_Policies_Provided_Should_Remove_Cleanup_Attribute() {
    cleanup.put(CLEANUP_NAME_KEY, null);

    initializeCleanupAttributes(repositoryXO);

    assertThat(getCleanupAttribute(), is(nullValue()));
  }

  @Test
  public void when_Empty_Policies_Provided_Should_Remove_Cleanup_Attribute() {
    cleanup.put(CLEANUP_NAME_KEY, emptyList());

    initializeCleanupAttributes(repositoryXO);

    assertThat(getCleanupAttribute(), is(nullValue()));
  }

  @Test
  public void when_No_CleanupAttribute_Provided_Should_Not_Add_It() {
    attributes.put(CLEANUP_ATTRIBUTES_KEY, null);

    initializeCleanupAttributes(repositoryXO);

    assertThat(getCleanupAttribute(), is(nullValue()));
  }

  private Map<String, Object> getCleanupAttribute() {
    return repositoryXO.getAttributes().get(CLEANUP_ATTRIBUTES_KEY);
  }

  private Object getCleanupPolicyNames() {
    return getCleanupAttribute().get(CLEANUP_NAME_KEY);
  }

  @SuppressWarnings("unchecked")
  private Set<String> getCleanupPolicyNamesAsSet() {
    return (Set<String>) getCleanupPolicyNames();
  }

  @SuppressWarnings("unchecked")
  private List<String> getCleanupPolicyNamesAsList() {
    return (List<String>) getCleanupPolicyNames();
  }
}
