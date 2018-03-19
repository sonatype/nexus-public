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

import java.util.function.BiFunction;
import java.util.function.Function;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.security.SecurityHelper;
import org.sonatype.nexus.selector.SelectorConfiguration;
import org.sonatype.nexus.selector.SelectorManager;

import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.security.BreadActions.BROWSE;
import static org.sonatype.nexus.security.BreadActions.READ;

public class RepositoryPermissionCheckerTest
    extends TestSupport
{
  private static final String REPOSITORY_NAME = "repositoryName";

  private static final String REPOSITORY_FORMAT = "repositoryFormat";

  private static final String SELECTOR_NAME = "theSelector";

  private static final boolean HAS_REPOSITORY_PERMISSION = true;

  private static final boolean HAS_SELECTOR_PERMISSION = true;

  @Mock
  private Repository repository;

  @Mock
  private Format format;

  @Mock
  private SelectorConfiguration selector;

  @Mock
  private Subject subject;

  @Mock
  private SecurityHelper securityHelper;

  @Mock
  private SelectorManager selectorManager;

  private RepositoryPermissionChecker underTest;

  @Before
  public void setup() {
    when(repository.getName()).thenReturn(REPOSITORY_NAME);
    when(repository.getFormat()).thenReturn(format);
    when(format.getValue()).thenReturn(REPOSITORY_FORMAT);

    when(selector.getName()).thenReturn(SELECTOR_NAME);
    when(selectorManager.browse()).thenReturn(asList(selector));

    when(securityHelper.subject()).thenReturn(subject);

    underTest = new RepositoryPermissionChecker(securityHelper, selectorManager);
  }

  @Test
  public void testUserCanViewRepository() {
    verifyUserAccessOf(underTest::userCanViewRepository, READ);
  }

  @Test
  public void testUserCanBrowseRepository() {
    verifyUserAccessOf(underTest::userCanBrowseRepository, BROWSE);
  }

  private void verifyUserAccessOf(final Function<Repository, Boolean> accessCheck,
                                  final String repositoryPermissionAction)
  {
    BiFunction<Boolean, Boolean, Boolean> userCanAccessRepositoryWhen =
        (hasRepositoryPermission, hasSelectorPermission) -> {
          setUpRepositoryPermission(hasRepositoryPermission, repositoryPermissionAction);
          setUpSelectorPermission(hasSelectorPermission);
          return accessCheck.apply(repository);
        };

    assertTrue(userCanAccessRepositoryWhen.apply(HAS_REPOSITORY_PERMISSION, !HAS_SELECTOR_PERMISSION));
    assertTrue(userCanAccessRepositoryWhen.apply(!HAS_REPOSITORY_PERMISSION, HAS_SELECTOR_PERMISSION));
    assertTrue(userCanAccessRepositoryWhen.apply(HAS_REPOSITORY_PERMISSION, HAS_SELECTOR_PERMISSION));
    assertFalse(userCanAccessRepositoryWhen.apply(!HAS_REPOSITORY_PERMISSION, !HAS_SELECTOR_PERMISSION));
  }

  private void setUpRepositoryPermission(final boolean hasPermission, final String action) {
    when(securityHelper.anyPermitted(new RepositoryViewPermission(REPOSITORY_FORMAT, REPOSITORY_NAME, action)))
        .thenReturn(hasPermission);
  }

  private void setUpSelectorPermission(final boolean hasPermission) {
    when(securityHelper
        .anyPermitted(subject,
            new RepositoryContentSelectorPermission(SELECTOR_NAME, REPOSITORY_FORMAT, REPOSITORY_NAME,
                singletonList(BROWSE))))
        .thenReturn(hasPermission);
  }
}