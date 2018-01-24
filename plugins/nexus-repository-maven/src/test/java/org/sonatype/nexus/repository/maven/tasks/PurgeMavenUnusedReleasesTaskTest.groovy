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
package org.sonatype.nexus.repository.maven.tasks

import org.junit.Before
import org.junit.Test
import org.mockito.Matchers
import org.sonatype.goodies.testsupport.TestSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.RepositoryTaskSupport
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.maven.PurgeUnusedReleasesFacet
import org.sonatype.nexus.repository.maven.internal.Maven2Format
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.scheduling.TaskConfiguration

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.times
import static org.mockito.Mockito.verify
import static org.mockito.Mockito.when
import static org.sonatype.nexus.repository.maven.tasks.PurgeMavenUnusedReleasesTask.PURGE_UNUSED_MAVEN_RELEASES_MESSAGE

class PurgeMavenUnusedReleasesTaskTest  extends TestSupport {

    public PurgeMavenUnusedReleasesTask task

    private Repository repository

    @Before
    void setup() {
        task = new PurgeMavenUnusedReleasesTask(new HostedType(),
                new Maven2Format())
        TaskConfiguration configuration = new TaskConfiguration()
        configuration.setId(PurgeMavenUnusedReleasesTaskDescriptor.TYPE_ID)
        configuration.setTypeId(PurgeMavenUnusedReleasesTaskDescriptor.TYPE_ID)
        configuration.setString(RepositoryTaskSupport.REPOSITORY_NAME_FIELD_ID, "my-maven-repo")
        task.configure(configuration)

        repository = mock(Repository.class)
        HostedType hostedType = mock(HostedType.class)
        when(repository.getType()).thenReturn(hostedType)
        when(repository.getName()).thenReturn("my-maven-repo")
    }

    @Test
    void "Test message of the task"() {


        when:
        String message = task.getMessage()

        then:
        message == String.format(PURGE_UNUSED_MAVEN_RELEASES_MESSAGE,
                "my-maven-repo")
    }

    @Test
    void "Test verify if the task could be apply on the repository of type"() {

        when:
        Boolean appliesTo = task.appliesTo(repository)

        then:
        appliesTo == false

        and:
        Maven2Format maven2Format = mock(Maven2Format.class)
        when(repository.getFormat()).thenReturn(maven2Format)

        then:
        appliesTo == true
    }

    @Test
     void "Test the execution of the purge when the task is executed"() {

     given:
     RepositoryManager repositoryManager = mock(RepositoryManager.class)
     Maven2Format maven2Format = mock(Maven2Format.class)
     when(repository.getFormat()).thenReturn(maven2Format)
     HostedType hostedType = mock(HostedType.class)
     when(repository.getType()).thenReturn(hostedType)
     when(repositoryManager.get("my-maven-repo")).thenReturn(repository)
     task.install(repositoryManager, new HostedType())
     PurgeUnusedReleasesFacet purgeUnusedReleasesFacet = mock(PurgeUnusedReleasesFacet.class)
     when(repository.facet(PurgeUnusedReleasesFacet.class)).thenReturn(purgeUnusedReleasesFacet)


     when:
     task.execute(repository)

     then:
     verify(purgeUnusedReleasesFacet, times(1)).purgeUnusedReleases( Matchers.anyInt(), Matchers.anyString())

     }


}
