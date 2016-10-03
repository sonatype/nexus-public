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
import java.util.Collections;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.security.internal.ContentPermissionCheckerImpl;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;
import org.sonatype.nexus.selector.VariableSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentPermissionCheckerImplTest
    extends TestSupport
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

    config = new SelectorConfiguration();
    config.setName("selector");
    config.setDescription("selector");
    config.setType("jexl");
    config.setAttributes(Collections.singletonMap("expression", "true"));
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

    //just to make sure it was actually called, since returning false is the default behaviour
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

    //just to make sure it was actually called, since returning false is the default behaviour
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
        .anyPermitted(eq(new RepositoryViewPermission("repoFormat", "repoName", Arrays.asList(BreadActions.READ)))))
        .thenReturn(false);

    when(selectorManager.browse()).thenReturn(Arrays.asList(config));

    when(selectorManager.evaluate(any(), any())).thenReturn(true);

    assertThat(impl.isPermitted("repoName", "repoFormat", BreadActions.READ, variableSource), is(false));
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
}
