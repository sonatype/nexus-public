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
package org.sonatype.nexus.repository.maintenance.internal;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.common.entity.EntityMetadata;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.IllegalOperationException;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.security.VariableResolverAdapterManager;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.ComponentMaintenance;
import org.sonatype.nexus.security.BreadActions;
import org.sonatype.nexus.selector.VariableSource;

import org.apache.shiro.authz.AuthorizationException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MaintenanceServiceImplTest
    extends TestSupport
{
  @Mock
  ContentPermissionChecker contentPermissionChecker;

  @Mock
  VariableResolverAdapterManager variableResolverAdapterManager;

  @Mock
  Format format;

  @Mock
  Repository mavenReleases;

  @Mock
  Repository mavenGroup;

  @Mock
  VariableResolverAdapter variableResolverAdapter;

  @Mock
  Asset assetOne;

  @Mock
  VariableSource variableSource;

  @Mock
  EntityMetadata entityMetadata;

  @Mock
  EntityId entityId;

  @Mock
  ComponentMaintenance componentMaintenance;

  MaintenanceServiceImpl underTest;

  @Before
  public void setUp() throws Exception {
    when(format.toString()).thenReturn("maven2");

    when(mavenReleases.getName()).thenReturn("maven-releases");
    when(mavenReleases.getFormat()).thenReturn(format);
    when(mavenReleases.facet(ComponentMaintenance.class)).thenReturn(componentMaintenance);

    when(mavenGroup.getName()).thenReturn("maven-group");
    when(mavenGroup.getFormat()).thenReturn(format);
    doThrow(new MissingFacetException(mavenGroup, ComponentMaintenance.class)).when(mavenGroup)
        .facet(ComponentMaintenance.class);

    when(variableResolverAdapterManager.get("maven2")).thenReturn(variableResolverAdapter);

    when(variableResolverAdapter.fromAsset(assetOne)).thenReturn(variableSource);

    when(assetOne.getEntityMetadata()).thenReturn(entityMetadata);

    when(entityMetadata.getId()).thenReturn(entityId);

    underTest = new MaintenanceServiceImpl(contentPermissionChecker,
        variableResolverAdapterManager);
  }

  @Test
  public void testDeleteAsset() {
    when(contentPermissionChecker.isPermitted("maven-releases", "maven2", BreadActions.DELETE, variableSource))
        .thenReturn(true);

    underTest.deleteAsset(mavenReleases, assetOne);

    verify(componentMaintenance).deleteAsset(entityId);
  }

  @Test(expected = IllegalOperationException.class)
  public void testDeleteAsset_notSupported() {
    when(contentPermissionChecker.isPermitted("maven-group", "maven2", BreadActions.DELETE, variableSource))
        .thenReturn(true);

    underTest.deleteAsset(mavenGroup, assetOne);
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteAsset_notPermitted() {
    when(contentPermissionChecker.isPermitted("maven-releases", "maven2", BreadActions.DELETE, variableSource))
        .thenReturn(false);

    underTest.deleteAsset(mavenReleases, assetOne);
  }
}
