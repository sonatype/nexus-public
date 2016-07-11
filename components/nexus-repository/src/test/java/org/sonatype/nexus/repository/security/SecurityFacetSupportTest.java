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
package org.sonatype.nexus.repository.security;

import java.util.Arrays;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.view.Request;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorConfigurationStore;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecurityFacetSupportTest
    extends TestSupport
{
  private static final class TestSecurityFacetSupport
      extends SecurityFacetSupport
  {
    public TestSecurityFacetSupport(final RepositoryFormatSecurityConfigurationResource securityResource,
                                    final SelectorConfigurationStore selectorConfigurationStore,
                                    final VariableResolverAdapter variableResolverAdapter,
                                    final ContentPermissionChecker contentPermissionChecker)
    {
      super(securityResource, selectorConfigurationStore, variableResolverAdapter, contentPermissionChecker);
    }
  }

  @Mock
  Request request;

  @Mock
  Repository repository;

  @Mock
  SelectorConfigurationStore selectorConfigurationStore;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  @Mock
  SelectorConfiguration selectorConfiguration;

  @Mock
  RepositoryFormatSecurityConfigurationResource repositoryFormatSecurityConfigurationResource;

  TestSecurityFacetSupport testSecurityFacetSupport;

  @Before
  public void setupConfig() throws Exception {
    when(request.getPath()).thenReturn("/some/path.txt");
    when(request.getAction()).thenReturn(HttpMethods.GET);

    when(repository.getFormat()).thenReturn(new Format("test") { });
    when(repository.getName()).thenReturn("SecurityFacetSupportTest");

    when(selectorConfigurationStore.browse()).thenReturn(Arrays.asList(selectorConfiguration));

    testSecurityFacetSupport = new TestSecurityFacetSupport(repositoryFormatSecurityConfigurationResource,
        selectorConfigurationStore, variableResolverAdapter, contentPermissionChecker);

    testSecurityFacetSupport.attach(repository);
  }

  @Test
  public void testEnsurePermitted_viewPermitted() throws Exception {
    when(contentPermissionChecker.isViewPermitted("SecurityFacetSupportTest", "test", BreadActions.READ)).thenReturn(true);

    testSecurityFacetSupport.ensurePermitted(request);
  }

  @Test
  public void testEnsurePermitted_viewNotPermittedSelectorPermitted() throws Exception {
    when(contentPermissionChecker
        .isContentPermitted(eq("SecurityFacetSupportTest"), eq("test"), eq(BreadActions.READ), any(), any()))
        .thenReturn(true);
    testSecurityFacetSupport.ensurePermitted(request);

    verify(contentPermissionChecker).isViewPermitted("SecurityFacetSupportTest", "test", BreadActions.READ);
  }

  @Test
  public void testEnsurePermitted_viewNotPermittedSelectorNotPermitted() throws Exception {
    try {
      testSecurityFacetSupport.ensurePermitted(request);
      fail("AuthorizationException should have been thrown");
    }
    catch (AuthorizationException e) {
      //expected
    }

    verify(contentPermissionChecker).isViewPermitted("SecurityFacetSupportTest", "test", BreadActions.READ);
    verify(contentPermissionChecker)
        .isContentPermitted(eq("SecurityFacetSupportTest"), eq("test"), eq(BreadActions.READ), any(), any());
  }
}
