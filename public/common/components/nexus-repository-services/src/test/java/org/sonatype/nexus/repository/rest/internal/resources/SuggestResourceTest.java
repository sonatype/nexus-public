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

import org.sonatype.nexus.repository.search.SearchService;
import org.sonatype.nexus.testcommon.extensions.AuthenticationExtension;

import org.apache.shiro.authz.UnauthenticatedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith({MockitoExtension.class, AuthenticationExtension.class})
class SuggestResourceTest
{
  @Mock
  SearchService searchService;

  @InjectMocks
  SuggestResource underTest;

  @Test
  public void testSuggest_unauthenticated() {
    assertThrows(UnauthenticatedException.class, () -> underTest.suggest(null, null, 50));
  }
}
