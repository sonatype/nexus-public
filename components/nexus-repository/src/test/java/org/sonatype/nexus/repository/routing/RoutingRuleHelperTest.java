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
package org.sonatype.nexus.repository.routing;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RoutingRuleHelperTest
    extends TestSupport
{
  private RoutingRuleHelper underTest;

  @Mock
  private RoutingRuleStore routingRuleStore;

  @Mock
  private Repository repository;

  @Before
  public void setup() {
    when(routingRuleStore.getById("block")).thenReturn(new RoutingRule("block", "some description", RoutingMode.BLOCK,
        Arrays.asList("^/com/sonatype/.*", ".*foobar.*")));
    when(routingRuleStore.getById("allow")).thenReturn(new RoutingRule("allow", "some description", RoutingMode.ALLOW,
        Arrays.asList(".*foobar.*", "^/org/apache/.*")));
    underTest = new RoutingRuleHelper(routingRuleStore);
  }

  @Test
  public void testHandle_blockMode() throws Exception {
    assertAllowed("block", "/org/apache/tomcat/catalina");

    assertBlocked("block", "/com/sonatype/internal/secrets");
    assertBlocked("block", "/com/foobar/");
  }

  @Test
  public void testHandle_allowMode() throws Exception {
    assertAllowed("allow", "/org/apache/tomcat/catalina");
    assertAllowed("allow", "/com/foobar/");

    assertBlocked("allow", "/com/sonatype/internal/secrets");
  }

  @Test
  public void testHandle_noRuleAssigned() throws Exception {
    configureRepositoryMock(null);

    assertTrue(underTest.isAllowed(repository, "/com/sonatype/internal/secrets"));
  }

  @Test
  public void testHandle_nullRepositoryConfiguration() throws Exception {
    Repository repository = mock(Repository.class);
    Configuration configuration = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(configuration);
    when(configuration.getAttributes()).thenReturn(Collections.emptyMap());

    assertTrue(underTest.isAllowed(repository, "/some/path"));
  }

  private void assertBlocked(final String ruleId, final String path) throws Exception {
    configureRepositoryMock(ruleId);
    assertFalse(underTest.isAllowed(repository, path));
  }

  private void assertAllowed(final String ruleId, final String path) throws Exception {
    configureRepositoryMock(ruleId);
    assertTrue(underTest.isAllowed(repository, path));
  }

  private void configureRepositoryMock(final String repositoryRuleId) {
    Configuration configuration = mock(Configuration.class);
    when(repository.getConfiguration()).thenReturn(configuration);

    Map<String, Map<String, Object>> attributes =
        Collections.singletonMap("routingRules", Collections.singletonMap("routingRuleId", repositoryRuleId));
    when(configuration.getAttributes()).thenReturn(attributes);
  }
}
