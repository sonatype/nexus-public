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
package org.sonatype.nexus.repository.conda.internal.security;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.conda.internal.CondaFormat;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.view.Request;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.READ;

/**
 * @since 3.19
 */
public class CondaSecurityFacetTest
    extends TestSupport
{
  private static final String CONDA_SECURITY_FACET_TEST = "CondaSecurityFacetTest";

  @Mock
  Request request;

  @Mock
  Repository repository;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  @Mock
  CondaFormatSecurityContributor securityContributor;

  private CondaSecurityFacet condaSecurityFacet;

  @Before
  public void setupConfig() throws Exception{
    when(request.getPath()).thenReturn("/some/path.txt");
    when(request.getAction()).thenReturn(HttpMethods.GET);

    when(repository.getFormat()).thenReturn(new Format(CondaFormat.NAME) { });
    when(repository.getName()).thenReturn(CONDA_SECURITY_FACET_TEST);

    condaSecurityFacet = new CondaSecurityFacet(securityContributor, variableResolverAdapter, contentPermissionChecker);

    condaSecurityFacet.attach(repository);
  }

  @Test
  public void testEnsurePermittedPermitted() {
    when(contentPermissionChecker.isPermitted(eq(CONDA_SECURITY_FACET_TEST), eq(CondaFormat.NAME), eq(READ), any()))
        .thenReturn(true);
    try {
      condaSecurityFacet.ensurePermitted(request);
    }
    catch (AuthorizationException e) {
      fail("expected operation to be permitted");
    }
  }

  @Test
  public void testEnsurePermittedNotPermitted() {
    when(contentPermissionChecker.isPermitted(eq(CONDA_SECURITY_FACET_TEST), eq(CondaFormat.NAME), eq(READ), any()))
        .thenReturn(false);
    try {
      condaSecurityFacet.ensurePermitted(request);
      fail("AuthorizationException should have been thrown");
    }
    catch (AuthorizationException e) {
      //expected
    }

    verify(contentPermissionChecker).isPermitted(eq(CONDA_SECURITY_FACET_TEST), eq(CondaFormat.NAME), eq(READ), any());
  }
}
