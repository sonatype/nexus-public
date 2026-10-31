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
package org.sonatype.nexus.repository.group.rendered;

import java.util.List;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link GroupRenderedViewCleanupTaskDescriptor}.
 */
public class GroupRenderedViewCleanupTaskDescriptorTest
{
  @Test
  public void descriptor_registeredWithCorrectTypeId() {
    GroupRenderedViewCleanupTaskDescriptor descriptor = new GroupRenderedViewCleanupTaskDescriptor();

    assertThat(descriptor.getId(), equalTo(GroupRenderedViewCleanupTaskDescriptor.TYPE_ID));
  }

  @Test
  public void descriptor_registeredWithCorrectName() {
    GroupRenderedViewCleanupTaskDescriptor descriptor = new GroupRenderedViewCleanupTaskDescriptor();

    assertThat(descriptor.getName(), equalTo("Remove unused rendered group views"));
  }

  @Test
  public void descriptor_hasDaysHorizonFormField() {
    GroupRenderedViewCleanupTaskDescriptor descriptor = new GroupRenderedViewCleanupTaskDescriptor();

    List<FormField> formFields = descriptor.getFormFields();
    assertThat(formFields.size(), equalTo(1));

    FormField<?> field = formFields.get(0);
    assertThat(field, instanceOf(NumberTextFormField.class));
    assertThat(field.getId(), equalTo(GroupRenderedViewCleanupTaskDescriptor.DAYS_HORIZON_FIELD_ID));
  }

  @Test
  public void descriptor_daysHorizonHasDefaultValue() {
    GroupRenderedViewCleanupTaskDescriptor descriptor = new GroupRenderedViewCleanupTaskDescriptor();

    NumberTextFormField field = (NumberTextFormField) descriptor.getFormFields().get(0);
    assertThat(field.getInitialValue(), equalTo(14));
  }

  @Test
  public void descriptor_daysHorizonHasMinimumValue() {
    GroupRenderedViewCleanupTaskDescriptor descriptor = new GroupRenderedViewCleanupTaskDescriptor();

    NumberTextFormField field = (NumberTextFormField) descriptor.getFormFields().get(0);
    // A floor of one day is what stops an operator from entering 0 and turning a storage-bounding
    // sweep into a cache-wipe of every rendered view, however recently downloaded.
    assertThat(field.getMinimumValue(), equalTo(1));
  }

  @Test
  public void descriptor_visibleAndExposed() {
    GroupRenderedViewCleanupTaskDescriptor descriptor = new GroupRenderedViewCleanupTaskDescriptor();

    assertThat(descriptor.isVisible(), equalTo(true));
    assertThat(descriptor.isExposed(), equalTo(true));
  }
}
