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
package org.sonatype.nexus.cleanup.internal.content.method;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.maintenance.ContentMaintenanceFacet;
import org.sonatype.nexus.repository.task.DeletionProgress;
import org.sonatype.nexus.scheduling.TaskInterruptedException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteCleanupMethodTest
    extends TestSupport
{
  private static final int BATCH_SIZE = 500;

  @Mock
  private Repository repository;

  @Mock
  private BooleanSupplier cancelledCheck;

  @Mock
  private ContentMaintenanceFacet contentMaintenanceFacet;

  private DeleteCleanupMethod underTest;

  @Before
  public void setUp() {
    System.setProperty("nexus.continuation.browse.limit", String.valueOf(BATCH_SIZE));
    underTest = new DeleteCleanupMethod();
    when(repository.facet(ContentMaintenanceFacet.class)).thenReturn(contentMaintenanceFacet);
  }

  @Test(expected = TaskInterruptedException.class)
  public void testRunFailsIfTaskIsCancelled() {
    when(cancelledCheck.getAsBoolean()).thenReturn(true);
    underTest.run(repository, getRandomStream(1000), cancelledCheck);
  }

  @Test
  public void testRunReBatchStream() {
    when(cancelledCheck.getAsBoolean()).thenReturn(false);
    when(contentMaintenanceFacet.deleteComponents(any(Stream.class)))
        .thenAnswer(invocation -> {
          Stream<FluentComponent> input = invocation.getArgument(0);
          return (int) input.count();
        });

    Stream<FluentComponent> input = getRandomStream(5000);

    DeletionProgress deleted = underTest.run(repository, input, cancelledCheck);

    //validate stream is batched and cancel check is verified for each batch
    verify(contentMaintenanceFacet, times(10)).deleteComponents(any(Stream.class));
    verify(cancelledCheck, times(10)).getAsBoolean();

    assertEquals(5000, deleted.getComponentCount());
  }

  public Stream<FluentComponent> getRandomStream(final int size) {
    List<FluentComponent> resultList = new ArrayList<>(size);

    for (int i = 1; i <= size; i++) {
      FluentComponent fluentComponent = mock(FluentComponent.class);
      when(fluentComponent.namespace()).thenReturn("test");
      when(fluentComponent.name()).thenReturn("random-component-" + i);
      when(fluentComponent.version()).thenReturn(String.format("%s.0.0", i));

      resultList.add(fluentComponent);
    }

    return resultList.stream();
  }
}
