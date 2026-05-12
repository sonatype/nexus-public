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
package org.sonatype.nexus.repository.security.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.sonatype.nexus.repository.security.RepositoryContentSelectorPermission;
import org.sonatype.nexus.repository.security.RepositoryViewPermission;
import org.sonatype.nexus.repository.security.SelectorEvaluationCache;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.JexlSelector;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorEvaluationException;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.VariableSource;

import com.google.common.collect.Sets;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ContentPermissionCheckerImplTest
{
  @Mock
  SecurityHelper securityHelper;

  @Mock
  SelectorManager selectorManager;

  @Mock
  VariableSource variableSource;

  SelectorConfiguration config;

  ContentPermissionCheckerImpl impl;

  @Before
  public void setup() {
    impl = new ContentPermissionCheckerImpl(securityHelper, selectorManager);

    config = mock(SelectorConfiguration.class);
    when(config.getName()).thenReturn("selector");
    when(config.getDescription()).thenReturn("selector");
    when(config.getType()).thenReturn(JexlSelector.TYPE);
    when(config.getAttributes()).thenReturn(Collections.singletonMap("expression", "true"));

    // Mock security subject for caching
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    // Mock variableSource for existing tests
    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));
  }

  @Test
  public void testIsViewPermitted_permitted() throws Exception {
    when(securityHelper
        .anyPermitted(eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ)))))
            .thenReturn(true);

    assertThat(impl.isViewPermitted("repoName", "repoFormat", BreadActions.READ), is(true));
  }

  @Test
  public void testIsViewPermitted_notPermitted() throws Exception {
    assertThat(impl.isViewPermitted("repoName", "repoFormat", BreadActions.READ), is(false));

    // just to make sure it was actually called, since returning false is the default behaviour
    verify(securityHelper)
        .anyPermitted(eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ))));
  }

  @Test
  public void testIsContentPermitted_permitted() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    when(securityHelper.anyPermitted(eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName",
        Arrays.asList(BreadActions.READ))))).thenReturn(true);

    assertThat(impl.isContentPermitted("repoName", "repoFormat", BreadActions.READ, config, variableSource), is(true));
  }

  @Test
  public void testIsContentPermitted_notPermitted() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    assertThat(impl.isContentPermitted("repoName", "repoFormat", BreadActions.READ, config, variableSource), is(false));

    // just to make sure it was actually called, since returning false is the default behaviour
    verify(securityHelper).anyPermitted(eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName",
        Arrays.asList(BreadActions.READ))));
  }

  @Test
  public void testIsPermitted_viewPermittedContentPermitted() throws Exception {
    when(securityHelper
        .anyPermitted(eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ)))))
            .thenReturn(true);

    when(selectorManager.browse()).thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    assertThat(impl.isPermitted("repoName", "repoFormat", BreadActions.READ, variableSource), is(true));
  }

  @Test
  public void testIsPermitted_viewPermittedContentNotPermitted() throws Exception {
    when(securityHelper
        .anyPermitted(eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ)))))
            .thenReturn(true);

    when(selectorManager.browse()).thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(false);

    assertThat(impl.isPermitted("repoName", "repoFormat", BreadActions.READ, variableSource), is(true));
  }

  @Test
  public void testIsPermitted_viewNotPermittedContentPermitted() throws Exception {
    when(securityHelper
        .anyPermitted(eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName",
            Arrays.asList(BreadActions.READ)))))
                .thenReturn(true);

    when(selectorManager.browse()).thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    assertThat(impl.isPermitted("repoName", "repoFormat", BreadActions.READ, variableSource), is(true));

    // just to validate 'view' permission didn't sneak in and authorize the above call
    verify(securityHelper).anyPermitted(eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName",
        Arrays.asList(BreadActions.READ))));
  }

  @Test
  public void testIsPermitted_viewNotPermittedContentNotPermitted() throws Exception {
    when(securityHelper
        .anyPermitted(eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ)))))
            .thenReturn(false);

    when(selectorManager.browse()).thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(false);

    assertThat(impl.isPermitted("repoName", "repoFormat", BreadActions.READ, variableSource), is(false));
  }

  @Test
  public void testIsViewPermittedMultipleRepositories_permitted() throws Exception {
    when(securityHelper
        .anyPermitted(
            eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ))),
            eq(new RepositoryViewPermission("repoFormat", "repoName2", Arrays.asList(BreadActions.READ)))))
                .thenReturn(true);

    assertThat(impl.isViewPermitted(Sets.newLinkedHashSet(Arrays.asList("repoName", "repoName2")), "repoFormat",
        BreadActions.READ), is(true));
  }

  @Test
  public void testIsViewPermittedMultipleRepositories_notPermitted() throws Exception {
    assertThat(impl.isViewPermitted(Sets.newLinkedHashSet(Arrays.asList("repoName", "repoName2")), "repoFormat",
        BreadActions.READ), is(false));

    // just to make sure it was actually called, since returning false is the default behaviour
    verify(securityHelper)
        .anyPermitted(
            eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ))),
            eq(new RepositoryViewPermission("repoFormat", "repoName2", Arrays.asList(BreadActions.READ))));
  }

  @Test
  public void testIsContentPermittedMultipleRepositories_permitted() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    when(securityHelper
        .anyPermitted(
            eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName",
                Arrays.asList(BreadActions.READ))),
            eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName2",
                Arrays.asList(BreadActions.READ)))))
                    .thenReturn(true);

    assertThat(impl.isContentPermitted(Sets.newLinkedHashSet(Arrays.asList("repoName", "repoName2")), "repoFormat",
        BreadActions.READ, config, variableSource), is(true));
  }

  @Test
  public void testIsContentPermittedMultipleRepositories_notPermitted() throws Exception {
    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    assertThat(impl.isContentPermitted(Sets.newLinkedHashSet(Arrays.asList("repoName", "repoName2")), "repoFormat",
        BreadActions.READ, config, variableSource), is(false));

    // just to make sure it was actually called, since returning false is the default behaviour
    verify(securityHelper)
        .anyPermitted(
            eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName",
                Arrays.asList(BreadActions.READ))),
            eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName2",
                Arrays.asList(BreadActions.READ))));
  }

  @Test
  public void testIsPermittedMultipleRepositories_viewPermittedContentPermitted() throws Exception {
    when(securityHelper
        .anyPermitted(
            eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ))),
            eq(new RepositoryViewPermission("repoFormat", "repoName2", Arrays.asList(BreadActions.READ)))))
                .thenReturn(true);

    when(selectorManager.browse()).thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    assertThat(impl.isPermitted(Sets.newLinkedHashSet(Arrays.asList("repoName", "repoName2")), "repoFormat",
        BreadActions.READ, variableSource), is(true));
  }

  @Test
  public void testIsPermittedMultipleRepositories_viewPermittedContentNotPermitted() throws Exception {
    when(securityHelper
        .anyPermitted(
            eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ))),
            eq(new RepositoryViewPermission("repoFormat", "repoName2", Arrays.asList(BreadActions.READ)))))
                .thenReturn(true);

    when(selectorManager.browse()).thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(false);

    assertThat(impl.isPermitted(Sets.newLinkedHashSet(Arrays.asList("repoName", "repoName2")), "repoFormat",
        BreadActions.READ, variableSource), is(true));
  }

  @Test
  public void testIsPermittedMultipleRepositories_viewNotPermittedContentPermitted() throws Exception {
    when(securityHelper
        .anyPermitted(
            eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName",
                Arrays.asList(BreadActions.READ))),
            eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName2",
                Arrays.asList(BreadActions.READ)))))
                    .thenReturn(true);

    Set<String> repositoryNames = Sets.newLinkedHashSet(Arrays.asList("repoName", "repoName2"));

    when(selectorManager.browseActive(repositoryNames, Collections.singletonList("repoFormat")))
        .thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    assertThat(impl.isPermitted(Sets.newLinkedHashSet(Arrays.asList("repoName", "repoName2")), "repoFormat",
        BreadActions.READ, variableSource), is(true));

    // just to validate 'view' permission didn't sneak in and authorize the above call
    verify(securityHelper).anyPermitted(
        eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName",
            Arrays.asList(BreadActions.READ))),
        eq(new RepositoryContentSelectorPermission("selector", "repoFormat", "repoName2",
            Arrays.asList(BreadActions.READ))));
  }

  @Test
  public void testIsPermittedMultipleRepositories_viewNotPermittedContentNotPermitted() throws Exception {
    when(securityHelper
        .anyPermitted(
            eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ))),
            eq(new RepositoryViewPermission("repoFormat", "repoName2", Arrays.asList(BreadActions.READ)))))
                .thenReturn(false);

    when(selectorManager.browse()).thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(false);

    assertThat(
        impl.isPermitted(Sets.newHashSet("repoName", "repoName2"), "repoFormat", BreadActions.READ, variableSource),
        is(false));
  }

  // ========== Caching Tests (NEXUS-50181) ==========

  @Test
  public void testGenerateCacheKey_withPath() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));

    String cacheKey = impl.generateCacheKey("repoName", "selector1", variableSource);

    assertThat(cacheKey, is("testUser:repoName:selector1:/test/path"));
  }

  @Test
  public void testGenerateCacheKey_withoutPath() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.empty());

    String cacheKey = impl.generateCacheKey("repoName", "selector1", variableSource);

    assertThat(cacheKey, is("testUser:repoName:selector1:"));
  }

  @Test
  public void testGenerateCacheKey_anonymousUser() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn(null);
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));

    String cacheKey = impl.generateCacheKey("repoName", "selector1", variableSource);

    assertThat(cacheKey, is("anonymous:repoName:selector1:/test/path"));
  }

  @Test
  public void testGenerateCacheKey_differentUsersProduceDifferentKeys() throws Exception {
    Subject subject1 = mock(Subject.class);
    when(subject1.getPrincipal()).thenReturn("user1");

    Subject subject2 = mock(Subject.class);
    when(subject2.getPrincipal()).thenReturn("user2");

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));

    when(securityHelper.subject()).thenReturn(subject1);
    String cacheKey1 = impl.generateCacheKey("repoName", "selector1", variableSource);

    when(securityHelper.subject()).thenReturn(subject2);
    String cacheKey2 = impl.generateCacheKey("repoName", "selector1", variableSource);

    assertThat(cacheKey1, not(equalTo(cacheKey2)));
  }

  @Test
  public void testEvaluateSelectorWithCache_cacheMiss() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));
    when(selectorManager.evaluate(config, variableSource)).thenReturn(true);

    boolean result = impl.evaluateSelectorWithCache(config, variableSource, "repoName", new SelectorEvaluationCache());

    assertThat(result, is(true));
    verify(selectorManager, times(1)).evaluate(config, variableSource);
  }

  @Test
  public void testEvaluateSelectorWithCache_cacheHit() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));
    when(selectorManager.evaluate(config, variableSource)).thenReturn(true);

    SelectorEvaluationCache cache = new SelectorEvaluationCache();

    // First call - cache miss
    boolean result1 = impl.evaluateSelectorWithCache(config, variableSource, "repoName", cache);

    // Second call - cache hit (should not call selectorManager again)
    boolean result2 = impl.evaluateSelectorWithCache(config, variableSource, "repoName", cache);

    assertThat(result1, is(true));
    assertThat(result2, is(true));
    // Selector manager should only be called once due to caching
    verify(selectorManager, times(1)).evaluate(config, variableSource);
  }

  @Test
  public void testEvaluateSelectorWithCache_differentPathsCauseCacheMiss() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    VariableSource variableSource1 = mock(VariableSource.class);
    when(variableSource1.get("path")).thenReturn(Optional.of("/path1"));

    VariableSource variableSource2 = mock(VariableSource.class);
    when(variableSource2.get("path")).thenReturn(Optional.of("/path2"));

    when(selectorManager.evaluate(eq(config), any())).thenReturn(true);

    SelectorEvaluationCache cache = new SelectorEvaluationCache();

    // First call with path1
    boolean result1 = impl.evaluateSelectorWithCache(config, variableSource1, "repoName", cache);

    // Second call with path2 - different cache key
    boolean result2 = impl.evaluateSelectorWithCache(config, variableSource2, "repoName", cache);

    assertThat(result1, is(true));
    assertThat(result2, is(true));
    // Selector manager should be called twice - different paths = different cache keys
    verify(selectorManager, times(2)).evaluate(eq(config), any());
  }

  @Test
  public void testEvaluateSelectorWithCache_errorHandling() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));
    when(selectorManager.evaluate(config, variableSource))
        .thenThrow(new SelectorEvaluationException("Test error"));

    // Should return false on error
    boolean result = impl.evaluateSelectorWithCache(config, variableSource, "repoName", new SelectorEvaluationCache());

    assertThat(result, is(false));
    verify(selectorManager, times(1)).evaluate(config, variableSource);
  }

  @Test
  public void testEvaluateSelectorWithCache_errorsNotCached() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));

    // First call throws error
    when(selectorManager.evaluate(config, variableSource))
        .thenThrow(new SelectorEvaluationException("Test error"))
        .thenReturn(true); // Second call succeeds

    SelectorEvaluationCache cache = new SelectorEvaluationCache();

    // First call - error
    boolean result1 = impl.evaluateSelectorWithCache(config, variableSource, "repoName", cache);

    // Second call - should retry (error not cached)
    boolean result2 = impl.evaluateSelectorWithCache(config, variableSource, "repoName", cache);

    assertThat(result1, is(false));
    assertThat(result2, is(true));
    // Should be called twice - errors are not cached
    verify(selectorManager, times(2)).evaluate(config, variableSource);
  }

  @Test
  public void testClearCache() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));
    when(selectorManager.evaluate(config, variableSource)).thenReturn(true);

    SelectorEvaluationCache cache = new SelectorEvaluationCache();

    // First call - cache miss
    impl.evaluateSelectorWithCache(config, variableSource, "repoName", cache);

    // Clear cache
    cache.clear();

    // Second call after clear - should call selectorManager again
    impl.evaluateSelectorWithCache(config, variableSource, "repoName", cache);

    // Should be called twice - once before clear, once after
    verify(selectorManager, times(2)).evaluate(config, variableSource);
  }

  @Test
  public void testIsContentPermitted_usesCaching() throws Exception {
    Subject subject = mock(Subject.class);
    when(subject.getPrincipal()).thenReturn("testUser");
    when(securityHelper.subject()).thenReturn(subject);

    when(variableSource.get("path")).thenReturn(Optional.of("/test/path"));
    when(selectorManager.evaluate(config, variableSource)).thenReturn(true);

    when(securityHelper.anyPermitted(any(RepositoryContentSelectorPermission.class))).thenReturn(true);

    SelectorEvaluationCache cache = new SelectorEvaluationCache();

    // First call
    impl.isContentPermitted("repoName", "repoFormat", BreadActions.READ, config, variableSource, cache);

    // Second call - should use cache
    impl.isContentPermitted("repoName", "repoFormat", BreadActions.READ, config, variableSource, cache);

    // Selector manager should only be called once due to caching
    verify(selectorManager, times(1)).evaluate(config, variableSource);
  }
}
