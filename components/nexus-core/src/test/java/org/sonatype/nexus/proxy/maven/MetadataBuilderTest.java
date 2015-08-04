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
package org.sonatype.nexus.proxy.maven;

import java.util.Arrays;
import java.util.Collection;

import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataBuilder;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataException;
import org.sonatype.nexus.proxy.maven.metadata.operations.MetadataOperation;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class MetadataBuilderTest
    extends TestSupport
{

  /**
   * Tests that changing metadata will work on a clone of metadata and will actually not change the original metadata
   * at all when one of the operations is failing.
   * <p/>
   * It also checks that operations to be performed after the failing operation will not be called.
   */
  @Test
  public void metadataIsNotChangedWhenOperationFails() {
    final Metadata metadata = createMetadata();
    final MetadataOperation notToBeCalledOperation = mock(MetadataOperation.class);

    try {
      MetadataBuilder.changeMetadata(
          metadata,
          Arrays.asList(
              new AddVersion30Operation(),
              new FailAfterChangeOperation(),
              notToBeCalledOperation
          )
      );
      assertThat("Changing metadata was supposed to fail", false, is(true));
    }
    catch (MetadataException e) {
      // do nothing
    }

    assertThat(metadata.getVersion(), equalTo("2.0"));
    assertThat(metadata.getVersioning().getVersions().size(), is(2));
    assertThat(metadata.getModelVersion(), equalTo("1.0"));

    verifyNoMoreInteractions(notToBeCalledOperation);
  }

  /**
   * Tests that changing metadata will work on a clone of metadata and will actually change the metadata only with
   * changes done by operations that does not fail.
   * <p/>
   * It also checks that changing will not fail but return a list of failures from failed operations.
   *
   * @throws MetadataException unexpected
   */
  @Test
  public void failingOperationDoesNotChangeMetadata()
      throws MetadataException
  {
    final Metadata metadata = createMetadata();

    final Collection<MetadataException> metadataExceptions = MetadataBuilder.changeMetadataIgnoringFailures(
        metadata,
        Arrays.asList(
            new AddVersion30Operation(),
            new FailAfterChangeOperation(),
            new SetModelEncodingOperation()
        )
    );

    assertThat(metadata.getVersion(), equalTo("3.0"));
    assertThat(metadata.getVersioning().getVersions().size(), is(3));
    assertThat(metadata.getModelVersion(), equalTo("1.0"));
    assertThat(metadata.getModelEncoding(), equalTo("ISO-8859-1"));
    assertThat(metadataExceptions, is(notNullValue()));
    assertThat(metadataExceptions.size(), is(1));
  }

  private Metadata createMetadata() {
    final Metadata metadata = new Metadata();
    metadata.setGroupId("g");
    metadata.setArtifactId("a");
    metadata.setModelEncoding("UTF-8");
    metadata.setModelVersion("1.0");
    metadata.setVersion("2.0");

    final Versioning versioning = new Versioning();
    versioning.addVersion("1.0");
    versioning.addVersion("2.0");

    metadata.setVersioning(versioning);

    return metadata;
  }

  private static class AddVersion30Operation
      implements MetadataOperation
  {

    @Override
    public boolean perform(final Metadata metadata)
        throws MetadataException
    {
      metadata.getVersioning().addVersion("3.0");
      metadata.setVersion("3.0");

      return true;
    }
  }

  private static class FailAfterChangeOperation
      implements MetadataOperation
  {

    @Override
    public boolean perform(final Metadata metadata)
        throws MetadataException
    {
      metadata.setModelVersion("1.1");
      throw new MetadataException("Wanted failure");
    }
  }

  private static class SetModelEncodingOperation
      implements MetadataOperation
  {

    @Override
    public boolean perform(final Metadata metadata)
        throws MetadataException
    {
      metadata.setModelEncoding("ISO-8859-1");

      return true;
    }
  }

}
