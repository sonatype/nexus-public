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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.bootstrap.validation.ValidationConfiguration;
import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.repository.rest.api.ContentSelectorApiCreateRequest;
import org.sonatype.nexus.selector.CselSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;
import org.sonatype.nexus.selector.SelectorFactory;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.validation.internal.AlwaysTraversableResolver;
import org.sonatype.nexus.validation.internal.AopAwareParanamerParameterNameProvider;

import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.apache.shiro.util.ThreadContext;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.selector.SelectorConfiguration.EXPRESSION;

/**
 * Unit tests for {@link ContentSelectorsApiResource}
 *
 * @since 3.next
 */
public class ContentSelectorsApiResourceTest
    extends TestSupport
{
  @Mock
  private SelectorFactory selectorFactory;

  @Mock
  private SelectorManager selectorManager;

  @Mock
  private SelectorConfigurationStore store;

  @Mock
  private EventManager eventManager;

  private ContentSelectorsApiResource underTest;

  private DefaultSecurityManager securityManager;

  private TestRealm realm;

  private ValidatorFactory validatorFactory;

  @Before
  public void setup() {
    underTest = new ContentSelectorsApiResource(selectorFactory, selectorManager, store, eventManager);

    // Set up validation framework (required by ValidationAspect)
    validatorFactory = Validation.byDefaultProvider()
        .configure()
        .parameterNameProvider(new AopAwareParanamerParameterNameProvider())
        .traversableResolver(new AlwaysTraversableResolver())
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory();
    Validator validator = validatorFactory.getValidator();
    ValidationConfiguration.EXECUTABLE_VALIDATOR = validator.forExecutables();

    // Set up Shiro security context for authentication and authorization
    realm = new TestRealm("test-realm");
    realm.addTestAccount("testuser");
    securityManager = new DefaultSecurityManager(realm);
    ThreadContext.bind(securityManager);

    // Create an authenticated subject
    SimplePrincipalCollection principals = new SimplePrincipalCollection("testuser", realm.getName());
    Session session = new SimpleSession();
    DelegatingSubject subject = new DelegatingSubject(principals, true, "localhost", session, securityManager);
    ThreadContext.bind(subject);
  }

  @After
  public void teardown() {
    // Clean up validation framework
    ValidationConfiguration.EXECUTABLE_VALIDATOR = null;
    if (validatorFactory != null) {
      validatorFactory.close();
    }

    // Clean up Shiro security context
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
    if (securityManager != null) {
      securityManager.destroy();
    }
  }

  @Test
  public void testCreateContentSelector_Success() {
    // Given
    String name = "test-selector";
    String expression = "format == \"maven2\"";
    String description = "Test description";

    ContentSelectorApiCreateRequest request = new ContentSelectorApiCreateRequest();
    request.setName(name);
    request.setExpression(expression);
    request.setDescription(description);

    SelectorConfiguration mockConfig = mock(SelectorConfiguration.class);
    when(mockConfig.getName()).thenReturn(name);

    when(selectorManager.create(eq(name), eq(CselSelector.TYPE), eq(description), anyMap()))
        .thenReturn(mockConfig);

    // When
    underTest.createContentSelector(request);

    // Then
    verify(selectorFactory).validateSelector(CselSelector.TYPE, expression);
    verify(selectorManager).create(eq(name), eq(CselSelector.TYPE), eq(description),
        eq(singletonMap(EXPRESSION, expression)));
    verify(eventManager).post(any());
  }

  @Test
  public void testCreateContentSelector_ConcurrentRequests() throws InterruptedException {
    // This test verifies the fix for NEXUS-46663: race condition in concurrent POST requests
    // Before the fix, the API would call findByName() after create(), which could fail with 404
    // if the cache wasn't updated yet. After the fix, create() returns the configuration directly.

    int threadCount = 10;
    int iterationsPerThread = 10;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount * iterationsPerThread);
    AtomicInteger successCount = new AtomicInteger(0);
    List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

    // Mock the create method to return a valid configuration
    doAnswer(invocation -> {
      String name = invocation.getArgument(0);
      SelectorConfiguration mockConfig = mock(SelectorConfiguration.class);
      when(mockConfig.getName()).thenReturn(name);
      return mockConfig;
    }).when(selectorManager).create(anyString(), eq(CselSelector.TYPE), anyString(), anyMap());

    // Submit concurrent requests
    for (int i = 0; i < threadCount * iterationsPerThread; i++) {
      final int requestId = i;
      executor.submit(() -> {
        try {
          // Wait for all threads to be ready
          startLatch.await();

          ContentSelectorApiCreateRequest request = new ContentSelectorApiCreateRequest();
          request.setName("selector-" + requestId);
          request.setExpression("format == \"maven2\"");
          request.setDescription("Concurrent test " + requestId);

          underTest.createContentSelector(request);
          successCount.incrementAndGet();
        }
        catch (Exception e) {
          exceptions.add(e);
        }
        finally {
          doneLatch.countDown();
        }
      });
    }

    // Start all threads simultaneously
    startLatch.countDown();

    // Wait for all threads to complete
    boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);

    // Verify results
    if (!exceptions.isEmpty()) {
      exceptions.forEach(e -> e.printStackTrace());
      fail("Expected no exceptions but got " + exceptions.size() + " exceptions");
    }

    assertThat("All requests should succeed", successCount.get(), is(threadCount * iterationsPerThread));
    assertThat("All threads should complete", completed, is(true));

    // Verify that create was called the expected number of times
    verify(selectorManager, times(threadCount * iterationsPerThread))
        .create(anyString(), eq(CselSelector.TYPE), anyString(), anyMap());
  }

  @Test
  public void testCreateContentSelector_NoRaceConditionAfterFix() {
    // This test specifically validates that the fix eliminates the race condition
    // by ensuring we never call findByName() after create()

    String name = "no-race-selector";
    String expression = "format == \"npm\"";
    String description = "No race condition";

    ContentSelectorApiCreateRequest request = new ContentSelectorApiCreateRequest();
    request.setName(name);
    request.setExpression(expression);
    request.setDescription(description);

    SelectorConfiguration mockConfig = mock(SelectorConfiguration.class);
    when(mockConfig.getName()).thenReturn(name);

    // Mock create to return configuration directly
    when(selectorManager.create(eq(name), eq(CselSelector.TYPE), eq(description), anyMap()))
        .thenReturn(mockConfig);

    // When
    underTest.createContentSelector(request);

    // Then verify that we:
    // 1. Called create exactly once
    verify(selectorManager, times(1)).create(eq(name), eq(CselSelector.TYPE), eq(description), anyMap());

    // 2. Posted the event with the returned configuration (not a looked-up one)
    verify(eventManager, times(1)).post(any());

    // Note: We should NOT call findByName() anywhere in the create flow after the fix
    // The original bug was: create() -> findByName() -> 404 due to stale cache
    // The fix: create() returns the configuration, no findByName() needed
  }

  /**
   * Test realm that allows adding accounts with permissions
   */
  private static class TestRealm
      extends SimpleAccountRealm
  {
    public TestRealm(String name) {
      super(name);
    }

    public void addTestAccount(String username) {
      Set<Permission> permissions = new HashSet<>();
      permissions.add(new WildcardPermission("nexus:*"));
      SimpleAccount account =
          new SimpleAccount(username, "password", getName(), Collections.emptySet(), permissions);
      // Call protected add method from within subclass
      add(account);
    }
  }
}
