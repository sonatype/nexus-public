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
package org.sonatype.nexus.repository.sizeassetcount.internal;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.sizeassetcount.SizeAssetCountAttributesFacet;
import org.sonatype.nexus.repository.types.GroupType;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.TaskConfiguration;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SizeAssetCountAttributesCalculatingTaskTest extends TestSupport{


    private static final String MY_REPO_NAME = "my-repo";

    @Mock
    private Repository repository;

    @Mock
    private HostedType hostedType;

    @Mock
    private GroupType groupType;

    @Mock
    private SizeAssetCountAttributesFacet sizeAssetCountAttributesFacet;


    private TaskConfiguration taskConfiguration;


    private SizeAssetCountAttributesCalculatingTask sizeAssetCountAttributesCalculatingTask;

    @Before
    public void setUp() {
        sizeAssetCountAttributesCalculatingTask = new SizeAssetCountAttributesCalculatingTask(hostedType);
        taskConfiguration = new TaskConfiguration();
        taskConfiguration.setId("test");
        taskConfiguration.setTypeId("test");
        taskConfiguration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, MY_REPO_NAME);

        sizeAssetCountAttributesCalculatingTask.configure(taskConfiguration);
    }

    @Test
    public void verify_the_execution_of_the_calcul_of_size_and_asset_count() {

        //Given
        when(repository.getName()).thenReturn(MY_REPO_NAME);
        when(repository.getType()).thenReturn(hostedType);
        when(repository.facet(SizeAssetCountAttributesFacet.class)).thenReturn(sizeAssetCountAttributesFacet);

        //When
        sizeAssetCountAttributesCalculatingTask.execute(repository);

        //Then
        verify(sizeAssetCountAttributesFacet, times(1)).calculateSizeAssetCount();
        assertThat(sizeAssetCountAttributesCalculatingTask.appliesTo(repository)).isTrue();
        assertThat(sizeAssetCountAttributesCalculatingTask.getMessage()).isEqualToIgnoringCase(SizeAssetCountAttributesCalculatingTask.PREFIX_MESSAGE + MY_REPO_NAME);
    }

    @Test
    public void verify_the_task_dont_apply_to_a_repository_type_different_of_hosted() {

        //Given
        when(repository.getName()).thenReturn("my-repo");
        when(repository.getType()).thenReturn(groupType);
        when(repository.facet(SizeAssetCountAttributesFacet.class)).thenReturn(sizeAssetCountAttributesFacet);

        //When
        sizeAssetCountAttributesCalculatingTask.execute(repository);

        //Then
        assertThat(sizeAssetCountAttributesCalculatingTask.appliesTo(repository)).isFalse();
    }

}
