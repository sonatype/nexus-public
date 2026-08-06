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
package org.sonatype.nexus.jmx.reflect;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;

import org.sonatype.nexus.jmx.MBean;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

public class ReflectionMBeanBuilderTest
{
  @Test
  public void discoverWithoutTargetThrowsNullPointerException() {
    ReflectionMBeanBuilder builder = new ReflectionMBeanBuilder(ExampleManagedObject.class);

    assertThrows(NullPointerException.class, () -> builder.discover());
  }

  @Test
  public void discoverWithoutManagedObjectAnnotationThrowsNullPointerException() {
    ReflectionMBeanBuilder builder = new ReflectionMBeanBuilder(Unmanaged.class)
        .target(Unmanaged::new);

    // @ManagedObject is absent on Unmanaged, so DescriptorHelper-driven discovery sees a null descriptor and
    // discover()'s checkNotNull(managedDescriptor) fires; the NPE is the actual (pinned) behavior, not a defect.
    assertThrows(NullPointerException.class, () -> builder.discover());
  }

  @Test
  public void targetIsChainable() {
    ReflectionMBeanBuilder builder = new ReflectionMBeanBuilder(ExampleManagedObject.class);

    assertThat(builder.target(ExampleManagedObject::new), sameInstance(builder));
  }

  @Test
  public void discoverExampleManagedObject() throws Exception {
    ReflectionMBeanBuilder builder = new ReflectionMBeanBuilder(ExampleManagedObject.class)
        .target(ExampleManagedObject::new);

    // discover() is chainable and returns the same builder
    assertThat(builder.discover(), sameInstance(builder));

    MBean mbean = builder.build();
    assertThat(mbean, notNullValue());

    MBeanInfo info = mbean.getMBeanInfo();
    assertThat(info, notNullValue());
    assertThat(info.getClassName(), equalTo(ExampleManagedObject.class.getName()));

    // descriptor is always present
    Descriptor descriptor = info.getDescriptor();
    assertThat(descriptor, notNullValue());

    // R/W attribute, derived name 'Name' from getName/setName
    MBeanAttributeInfo nameAttr = findAttribute(info, "Name");
    assertThat(nameAttr, notNullValue());
    assertThat(nameAttr.isReadable(), is(true));
    assertThat(nameAttr.isWritable(), is(true));
    assertThat(nameAttr.isIs(), is(false));
    assertThat(nameAttr.getType(), equalTo(String.class.getName()));
    // no description was provided on getName/setName, so the attribute description is null
    assertThat(nameAttr.getDescription(), nullValue());

    // W-only attribute; getPassword is not annotated so the attribute is writable only
    MBeanAttributeInfo passwordAttr = findAttribute(info, "Password");
    assertThat(passwordAttr, notNullValue());
    assertThat(passwordAttr.isReadable(), is(false));
    assertThat(passwordAttr.isWritable(), is(true));
    assertThat(passwordAttr.isIs(), is(false));
    // type is derived from the setter parameter when there is no getter
    assertThat(passwordAttr.getType(), equalTo(String.class.getName()));
    assertThat(passwordAttr.getDescription(), equalTo("Set password"));

    // operation, default name from method
    MBeanOperationInfo resetOp = findOperation(info, "resetName");
    assertThat(resetOp, notNullValue());
    assertThat(resetOp.getDescription(), equalTo("Reset name"));
    // operation takes no arguments, returns void and uses the default UNKNOWN impact
    assertThat(resetOp.getSignature().length, is(0));
    assertThat(resetOp.getReturnType(), equalTo("void"));
    assertThat(resetOp.getImpact(), is(MBeanOperationInfo.UNKNOWN));

    // getName/setName correlate into a single R/W attribute, and the unannotated getPassword
    // does not contribute a readable Password; inherited Object methods are not exposed
    assertThat(info.getAttributes().length, is(2));
    assertThat(info.getOperations().length, is(1));
  }

  @Test
  public void discoverProcessesAllAttributeAndOperationBranches() throws Exception {
    ReflectionMBeanBuilder builder = new ReflectionMBeanBuilder(AnnotatedFixture.class)
        .target(AnnotatedFixture::new);

    MBean mbean = builder.discover().build();
    MBeanInfo info = mbean.getMBeanInfo();

    // explicit attribute name branch (@ManagedAttribute(name = "Custom"))
    MBeanAttributeInfo custom = findAttribute(info, "Custom");
    assertThat(custom, notNullValue());
    assertThat(custom.isReadable(), is(true));
    assertThat(custom.isWritable(), is(false));
    assertThat(custom.isIs(), is(false));
    assertThat(custom.getType(), equalTo(String.class.getName()));
    // the explicit @ManagedAttribute(name = "Custom") fully replaces the derived 'Something' name
    assertThat(findAttribute(info, "Something"), nullValue());

    // 'is' getter branch (boolean isActive -> attribute 'Active')
    MBeanAttributeInfo active = findAttribute(info, "Active");
    assertThat(active, notNullValue());
    assertThat(active.isReadable(), is(true));
    assertThat(active.isWritable(), is(false));
    assertThat(active.isIs(), is(true));
    assertThat(active.getType(), equalTo(boolean.class.getName()));

    // method marked as both attribute and operation is skipped (warn-and-continue branch)
    assertThat(findAttribute(info, "Confused"), nullValue());
    assertThat(findOperation(info, "getConfused"), nullValue());

    // attribute annotation on a method that is neither getter nor setter is skipped
    assertThat(findAttribute(info, "DoStuff"), nullValue());

    // explicit operation name + description branch
    MBeanOperationInfo customOp = findOperation(info, "customOp");
    assertThat(customOp, notNullValue());
    assertThat(customOp.getDescription(), equalTo("d"));
    assertThat(customOp.getReturnType(), equalTo("void"));
    assertThat(customOp.getImpact(), is(MBeanOperationInfo.UNKNOWN));
    // the explicit operation name replaces the method name 'doOp'
    assertThat(findOperation(info, "doOp"), nullValue());

    // plain @ManagedOperation: default name from the method, null description, default impact
    MBeanOperationInfo ping = findOperation(info, "ping");
    assertThat(ping, notNullValue());
    assertThat(ping.getDescription(), nullValue());
    assertThat(ping.getImpact(), is(MBeanOperationInfo.UNKNOWN));

    // exactly two attributes (Custom, Active) and two operations (customOp, ping) survive
    assertThat(info.getAttributes().length, is(2));
    assertThat(info.getOperations().length, is(2));
  }

  private static MBeanAttributeInfo findAttribute(final MBeanInfo info, final String name) {
    for (MBeanAttributeInfo attribute : info.getAttributes()) {
      if (attribute.getName().equals(name)) {
        return attribute;
      }
    }
    return null;
  }

  private static MBeanOperationInfo findOperation(final MBeanInfo info, final String name) {
    for (MBeanOperationInfo operation : info.getOperations()) {
      if (operation.getName().equals(name)) {
        return operation;
      }
    }
    return null;
  }

  /**
   * Fixture without {@link ManagedObject} to exercise the missing-descriptor check in discover().
   */
  public static class Unmanaged
  {
    // empty
  }

  /**
   * Fixture exercising the remaining discover() branches. Implements {@link Comparable} so the
   * compiler generates a synthetic bridge method, covering the bridge/synthetic skip branch.
   */
  @ManagedObject(domain = "org.sonatype.nexus.jmx")
  public static class AnnotatedFixture
      implements Comparable<AnnotatedFixture>
  {
    @ManagedAttribute(name = "Custom")
    public String getSomething() {
      return "value";
    }

    @ManagedAttribute
    public boolean isActive() {
      return true;
    }

    // marked as both attribute and operation -> skipped
    @ManagedAttribute
    @ManagedOperation
    public String getConfused() {
      return "confused";
    }

    // attribute annotation on a method that is neither a getter nor a setter -> skipped
    @ManagedAttribute
    public void doStuff() {
      // empty
    }

    @ManagedOperation(name = "customOp", description = "d")
    public void doOp() {
      // empty
    }

    // plain operation: default name from method and no description
    @ManagedOperation
    public void ping() {
      // empty
    }

    @Override
    public int compareTo(final AnnotatedFixture o) {
      return 0;
    }
  }
}
