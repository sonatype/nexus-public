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
package org.sonatype.nexus.repository.content.tasks;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class CreateAssetBlobIndexTaskDescriptorTest
{
  private CreateAssetBlobIndexTaskDescriptor underTest;

  @Test
  public void testNotExposedByDefault() {
    underTest = new CreateAssetBlobIndexTaskDescriptor(false);

    assertThat("Task should not be exposed by default", underTest.isExposed(), is(false));
    assertThat("Task should be visible", underTest.isVisible(), is(true));
  }

  @Test
  public void testVisibleAndExposedWhenEnabled() {
    underTest = new CreateAssetBlobIndexTaskDescriptor(true);

    assertThat("Task should be visible when enabled", underTest.isVisible(), is(true));
    assertThat("Task should be exposed when enabled", underTest.isExposed(), is(true));
  }

  @Test
  public void testTypeId() {
    underTest = new CreateAssetBlobIndexTaskDescriptor(false);

    assertThat("Type ID should be set correctly",
        underTest.getId(),
        equalTo(CreateAssetBlobIndexTaskDescriptor.TYPE_ID));
    assertThat("Type ID should be repository.asset.blob.index.migration",
        underTest.getId(),
        equalTo("repository.asset.blob.index.migration"));
  }

  @Test
  public void testTaskType() {
    underTest = new CreateAssetBlobIndexTaskDescriptor(false);

    assertThat("Task type should be CreateAssetBlobIndexTask",
        underTest.getType(),
        equalTo(CreateAssetBlobIndexTask.class));
  }

  @Test
  public void testName() {
    underTest = new CreateAssetBlobIndexTaskDescriptor(false);

    assertThat("Task name should be set", underTest.getName(), notNullValue());
    assertThat("Task name should describe the task",
        underTest.getName(),
        equalTo("Create asset blob index for blob_created and asset_blob_id"));
  }
}
