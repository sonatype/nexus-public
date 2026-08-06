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
package org.sonatype.nexus.jmx;

import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MBeanBuilderTest
{
  private static final String CLASS_NAME = "org.sonatype.nexus.jmx.TestManagedObject";

  private static final String DESCRIPTION = "test description";

  @Mock
  private MBeanOperation operation;

  @Test
  public void constructorRejectsNullClassName() {
    assertThrows(NullPointerException.class, () -> new MBeanBuilder(null));
  }

  @Test
  public void descriptionReturnsSameBuilderForChaining() {
    MBeanBuilder builder = new MBeanBuilder(CLASS_NAME);
    assertThat(builder.description(DESCRIPTION), sameInstance(builder));
  }

  @Test
  public void descriptorReturnsSameBuilderForChaining() {
    MBeanBuilder builder = new MBeanBuilder(CLASS_NAME);
    Descriptor descriptor = new ImmutableDescriptor("foo=bar");
    assertThat(builder.descriptor(descriptor), sameInstance(builder));
  }

  @Test
  public void attributeRejectsNull() {
    MBeanBuilder builder = new MBeanBuilder(CLASS_NAME);
    assertThrows(NullPointerException.class, () -> builder.attribute(null));
  }

  @Test
  public void operationRejectsNull() {
    MBeanBuilder builder = new MBeanBuilder(CLASS_NAME);
    assertThrows(NullPointerException.class, () -> builder.operation(null));
  }

  @Test
  public void buildCreatesMBeanWithExpectedInfo() {
    Descriptor descriptor = new ImmutableDescriptor("foo=bar");

    SuppliedMBeanAttribute attribute = new SuppliedMBeanAttribute.Builder()
        .name("N")
        .type(String.class)
        .value("v")
        .build();

    MBeanOperationInfo opInfo =
        new MBeanOperationInfo("op", "desc", new MBeanParameterInfo[0], "void", MBeanOperationInfo.UNKNOWN);
    when(operation.getInfo()).thenReturn(opInfo);
    when(operation.getKey()).thenReturn(new OperationKey("op", new String[0]));

    MBeanBuilder builder = new MBeanBuilder(CLASS_NAME);
    builder.description(DESCRIPTION);
    builder.descriptor(descriptor);
    builder.attribute(attribute);
    builder.operation(operation);

    MBean mbean = builder.build();
    assertThat(mbean, notNullValue());

    MBeanInfo info = mbean.getMBeanInfo();
    assertThat(info, notNullValue());
    assertThat(info.getClassName(), equalTo(CLASS_NAME));
    assertThat(info.getDescription(), equalTo(DESCRIPTION));

    // attribute-info must be the exact instance supplied by attribute.getInfo()
    assertThat(info.getAttributes().length, equalTo(1));
    assertThat(info.getAttributes()[0], sameInstance(attribute.getInfo()));
    assertThat(info.getAttributes()[0].getName(), equalTo("N"));
    assertThat(info.getAttributes()[0].getType(), equalTo(String.class.getName()));

    // operation-info must be the exact instance supplied by operation.getInfo()
    assertThat(info.getOperations().length, equalTo(1));
    assertThat(info.getOperations()[0], sameInstance(opInfo));
    assertThat(info.getOperations()[0].getName(), equalTo("op"));
    assertThat(info.getOperations()[0].getReturnType(), equalTo("void"));

    // descriptor must be carried through unchanged
    assertThat(info.getDescriptor(), equalTo(descriptor));

    // builder always supplies empty constructor- and notification-info
    assertThat(info.getConstructors().length, equalTo(0));
    assertThat(info.getNotifications().length, equalTo(0));

    // the supplied attribute and operation themselves are handed to the MBean
    assertThat(mbean.getAttributes(), hasSize(1));
    assertThat(mbean.getAttributes(), hasItem(attribute));
    assertThat(mbean.getOperations(), hasSize(1));
    assertThat(mbean.getOperations(), hasItem(operation));
  }

  @Test
  public void buildWithNoAttributesOrOperationsProducesEmptyInfo() {
    MBean mbean = new MBeanBuilder(CLASS_NAME).build();

    MBeanInfo info = mbean.getMBeanInfo();
    assertThat(info.getClassName(), equalTo(CLASS_NAME));
    assertThat(info.getAttributes().length, equalTo(0));
    assertThat(info.getOperations().length, equalTo(0));
    assertThat(mbean.getAttributes(), hasSize(0));
    assertThat(mbean.getOperations(), hasSize(0));
  }

  @Test
  public void buildPreservesEveryAttributeAndOperationInOrder() {
    SuppliedMBeanAttribute attr1 = new SuppliedMBeanAttribute.Builder()
        .name("alpha")
        .type(String.class)
        .value("a")
        .build();
    SuppliedMBeanAttribute attr2 = new SuppliedMBeanAttribute.Builder()
        .name("beta")
        .type(Integer.class)
        .value(1)
        .build();

    MBeanOperation op1 = mock(MBeanOperation.class);
    MBeanOperationInfo op1Info =
        new MBeanOperationInfo("first", "first-desc", new MBeanParameterInfo[0], "void", MBeanOperationInfo.ACTION);
    when(op1.getInfo()).thenReturn(op1Info);
    when(op1.getKey()).thenReturn(new OperationKey("first", new String[0]));

    MBeanOperation op2 = mock(MBeanOperation.class);
    MBeanOperationInfo op2Info = new MBeanOperationInfo(
        "second",
        "second-desc",
        new MBeanParameterInfo[]{new MBeanParameterInfo("p", String.class.getName(), "param")},
        String.class.getName(),
        MBeanOperationInfo.INFO);
    when(op2.getInfo()).thenReturn(op2Info);
    when(op2.getKey()).thenReturn(new OperationKey("second", new String[]{String.class.getName()}));

    MBeanBuilder builder = new MBeanBuilder(CLASS_NAME);
    builder.attribute(attr1);
    builder.attribute(attr2);
    builder.operation(op1);
    builder.operation(op2);

    MBeanInfo info = builder.build().getMBeanInfo();

    // attributes are carried through in insertion order, each from its own getInfo()
    assertThat(info.getAttributes().length, equalTo(2));
    assertThat(info.getAttributes()[0], sameInstance(attr1.getInfo()));
    assertThat(info.getAttributes()[0].getName(), equalTo("alpha"));
    assertThat(info.getAttributes()[0].getType(), equalTo(String.class.getName()));
    assertThat(info.getAttributes()[1], sameInstance(attr2.getInfo()));
    assertThat(info.getAttributes()[1].getName(), equalTo("beta"));
    assertThat(info.getAttributes()[1].getType(), equalTo(Integer.class.getName()));

    // operations are carried through in insertion order, each from its own getInfo()
    assertThat(info.getOperations().length, equalTo(2));
    assertThat(info.getOperations()[0], sameInstance(op1Info));
    assertThat(info.getOperations()[0].getName(), equalTo("first"));
    assertThat(info.getOperations()[0].getReturnType(), equalTo("void"));
    assertThat(info.getOperations()[1], sameInstance(op2Info));
    assertThat(info.getOperations()[1].getName(), equalTo("second"));
    assertThat(info.getOperations()[1].getReturnType(), equalTo(String.class.getName()));
    assertThat(info.getOperations()[1].getSignature()[0].getName(), equalTo("p"));
    assertThat(info.getOperations()[1].getSignature()[0].getType(), equalTo(String.class.getName()));
  }
}
