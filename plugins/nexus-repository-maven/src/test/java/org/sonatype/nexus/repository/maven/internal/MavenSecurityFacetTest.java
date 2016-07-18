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
package org.sonatype.nexus.repository.maven.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpMethods;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
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

public class MavenSecurityFacetTest
    extends TestSupport
{
  @Mock
  Request request;

  @Mock
  Repository repository;

  @Mock
  SelectorConfigurationStore selectorConfigurationStore;

  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  MavenFormatSecurityConfigurationResource mavenFormatSecurityConfigurationResource;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  MavenSecurityFacet mavenSecurityFacet;

  @Before
  public void setupConfig() throws Exception {
    when(request.getPath()).thenReturn("/mygroupid/myartifactid/1.0/myartifactid-1.0.jar");
    when(request.getAction()).thenReturn(HttpMethods.GET);

    when(repository.getFormat()).thenReturn(new Maven2Format());
    when(repository.getName()).thenReturn("MavenSecurityFacetTest");

    when(selectorConfigurationStore.browse())
        .thenReturn(Arrays.asList(getSelectorConfiguration("MavenSecurityFacetTest", "format == 'maven2'")));

    mavenSecurityFacet = new MavenSecurityFacet(mavenFormatSecurityConfigurationResource, selectorConfigurationStore,
        variableResolverAdapter, contentPermissionChecker);

    mavenSecurityFacet.attach(repository);
  }

  @Test
  public void testEnsurePermitted() throws Exception {
    when(contentPermissionChecker
        .isPermitted(eq("MavenSecurityFacetTest"), eq(Maven2Format.NAME), eq(BreadActions.READ), any(), any()))
        .thenReturn(true);

    mavenSecurityFacet.ensurePermitted(request);

    //validation is no AuthorizationException is thrown
  }

  @Test
  public void testEnsurePermitted_notPermitted() throws Exception {
    try {
      mavenSecurityFacet.ensurePermitted(request);
      fail("AuthorizationException should have been thrown");
    }
    catch (AuthorizationException e) {
      //expected
    }

    verify(contentPermissionChecker)
        .isPermitted(eq("MavenSecurityFacetTest"), eq(Maven2Format.NAME), eq(BreadActions.READ), any(), any());
  }

  @Test
  public void testEnsurePermitted_contentSelectorCoordinateGAVEMatches() throws Exception {
    when(contentPermissionChecker
        .isPermitted(eq("MavenSecurityFacetTest"), eq(Maven2Format.NAME), eq(BreadActions.READ), any(), any()))
        .thenReturn(true);

    when(selectorConfigurationStore.browse()).thenReturn(Arrays.asList(
        getSelectorConfiguration("MavenSecurityFacetTest", "coordinate.groupId == 'mygroupid'")));

    mavenSecurityFacet.ensurePermitted(request);

    when(selectorConfigurationStore.browse()).thenReturn(Arrays.asList(
        getSelectorConfiguration("MavenSecurityFacetTest", "coordinate.artifactId == 'myartifactid'")));

    mavenSecurityFacet.ensurePermitted(request);

    when(selectorConfigurationStore.browse()).thenReturn(Arrays.asList(
        getSelectorConfiguration("MavenSecurityFacetTest", "coordinate.version == '1.0'")));

    mavenSecurityFacet.ensurePermitted(request);

    when(selectorConfigurationStore.browse()).thenReturn(Arrays.asList(
        getSelectorConfiguration("MavenSecurityFacetTest", "coordinate.extension == 'jar'")));

    mavenSecurityFacet.ensurePermitted(request);

    when(selectorConfigurationStore.browse()).thenReturn(Arrays.asList(
        getSelectorConfiguration("MavenSecurityFacetTest",
            "coordinate.groupId == 'mygroupid' && coordinate.artifactId == 'myartifactid' && " +
                "coordinate.version == '1.0' && coordinate.extension == 'jar'")));

    mavenSecurityFacet.ensurePermitted(request);
  }

  private SelectorConfiguration getSelectorConfiguration(String name, String expression) {
    SelectorConfiguration config = new SelectorConfiguration();
    config.setName(name);
    config.setDescription(name);
    config.setType("jexl");
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("expression", expression);
    config.setAttributes(attributes);
    return config;
  }
}
