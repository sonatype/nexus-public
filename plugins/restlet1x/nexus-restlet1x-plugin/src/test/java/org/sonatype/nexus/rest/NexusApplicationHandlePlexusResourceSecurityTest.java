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
package org.sonatype.nexus.rest;

import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;
import org.sonatype.security.web.ProtectedPathManager;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class NexusApplicationHandlePlexusResourceSecurityTest
    extends TestSupport
{
  @Mock
  private PlexusResource mockResource;

  @Mock(name = "protectedPathManager")
  private ProtectedPathManager mockProtectedPathManager;

  private NexusApplication nexusApplication;

  @Before
  public void setUp() throws Exception {
    // HACK: we only need the protected path manager for this test
    nexusApplication = new NexusApplication(
        null,
        mockProtectedPathManager,
        null,
        null
    );
  }

  @Test(expected = IllegalStateException.class)
  public void handlePlexusResourceSecurityWithMismatch() {
    final PathProtectionDescriptor descriptor = new PathProtectionDescriptor("/foo/bar/*", "");
    Mockito.when(mockResource.getResourceProtection()).thenReturn(descriptor);
    Mockito.when(mockResource.getResourceUri()).thenReturn("/foo/baz");
    nexusApplication.handlePlexusResourceSecurity(mockResource);
  }

  @Test
  public void handlePlexusResourceSecurityWithoutMismatch() {
    final PathProtectionDescriptor descriptor = new PathProtectionDescriptor("/foo/bar/*", "");
    Mockito.when(mockResource.getResourceProtection()).thenReturn(descriptor);
    Mockito.when(mockResource.getResourceUri()).thenReturn("/foo/bar/{pattern}");
    nexusApplication.handlePlexusResourceSecurity(mockResource);
  }

  @Test
  public void handlePlexusResourceSecurityWithoutMismatchWithRestletPatterns() {
    final PathProtectionDescriptor descriptor = new PathProtectionDescriptor("/repositories/*", "");
    Mockito.when(mockResource.getResourceProtection()).thenReturn(descriptor);
    Mockito.when(mockResource.getResourceUri()).thenReturn("/repositories/{repoId}");
    nexusApplication.handlePlexusResourceSecurity(mockResource);
  }
}
