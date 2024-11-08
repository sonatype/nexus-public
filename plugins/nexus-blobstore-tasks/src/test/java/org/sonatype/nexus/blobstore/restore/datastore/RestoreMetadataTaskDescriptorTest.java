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
package org.sonatype.nexus.blobstore.restore.datastore;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.app.ApplicationVersion;
import org.sonatype.nexus.formfields.FormField;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class RestoreMetadataTaskDescriptorTest
    extends TestSupport
{
  RestoreMetadataTaskDescriptor underTest;

  @Mock
  ApplicationVersion applicationVersion;

  @Before
  public void setup() {
    when(applicationVersion.getEdition())
        .thenReturn("RPO");

    underTest = new RestoreMetadataTaskDescriptor(true, applicationVersion);
  }

  @Test
  public void testGetFormFields() {
    List<FormField> formField = underTest.getFormFields();
    assertThat(formField, hasSize(6));
  }
}
