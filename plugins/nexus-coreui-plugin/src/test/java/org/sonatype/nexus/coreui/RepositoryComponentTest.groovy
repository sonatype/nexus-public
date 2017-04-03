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
package org.sonatype.nexus.coreui

import java.security.Permission

import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.common.event.EventManager
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Filter
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.manager.internal.RepositoryImpl
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.security.SecurityHelper
import org.sonatype.nexus.selector.SelectorManager

import org.junit.Before
import org.junit.Test
import org.mockito.Mock

import static org.mockito.Matchers.any
import static org.mockito.Mockito.never
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when

/**
 * Test for {@link RepositoryComponent}
 */
class RepositoryComponentTest
    extends TestSupport
{
  @Mock
  SecurityHelper securityHelper

  @Mock
  Format format

  @Mock
  EventManager eventManager

  @Mock
  RepositoryManager repositoryManager

  @Mock
  SelectorManager selectorManager

  RepositoryComponent underTest

  @Before
  void setup() {
    when(format.getValue()).thenReturn('format')
    when(repositoryManager.browse()).thenReturn([repository()])

    underTest = new RepositoryComponent()
    underTest.securityHelper = securityHelper
    underTest.repositoryManager = repositoryManager
    underTest.selectorManager = selectorManager
  }

  @Test
  void checkUserPermissionsOnFilter() {
    underTest.filter(applyPermissions(true))
    verify(securityHelper).anyPermitted(any(Permission.class))
  }

  @Test
  void doNotCheckPermissionsWhenNotApplied() {
    underTest.filter(applyPermissions(false))
    verify(securityHelper, never()).anyPermitted(any(Permission.class))
  }

  StoreLoadParameters applyPermissions(boolean apply) {
    new StoreLoadParameters(
        filter: [
            new Filter(
                property: 'applyPermissions',
                value: Boolean.toString(apply)
            )
        ]
    )
  }

  Repository repository() {
    def repository = new RepositoryImpl(eventManager, new HostedType(), format)
    repository.name = 'repository'
    repository
  }
}
