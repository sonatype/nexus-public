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

import java.lang.reflect.Method;
import java.util.function.Supplier;

import javax.management.MBeanAttributeInfo;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

public class ReflectionMBeanAttributeTest
{
  private Fixture fixture;

  private Supplier<Fixture> supplier;

  private Method getName;

  private Method setName;

  private Method isActive;

  @Before
  public void setUp() throws Exception {
    fixture = new Fixture();
    supplier = () -> fixture;
    getName = Fixture.class.getMethod("getName");
    setName = Fixture.class.getMethod("setName", String.class);
    isActive = Fixture.class.getMethod("isActive");
  }

  @Test
  public void buildGetterOnly() throws Exception {
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .target(supplier)
        .getter(getName)
        .build();

    MBeanAttributeInfo info = attribute.getInfo();
    assertThat(info.isReadable(), is(true));
    assertThat(info.isWritable(), is(false));
    assertThat(info.getType(), equalTo(String.class.getName()));
    assertThat(info.isIs(), is(false));

    // getter-only attribute reads the value from the target
    assertThat(attribute.getValue(), equalTo("test-name"));

    // the value is read live from the SAME target instance, not cached
    fixture.setName("mutated");
    assertThat(attribute.getValue(), equalTo("mutated"));

    // setter is null, so setting must fail
    assertThrows(IllegalStateException.class, () -> attribute.setValue("other"));
  }

  @Test
  public void buildSetterOnly() throws Exception {
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .target(supplier)
        .setter(setName)
        .build();

    MBeanAttributeInfo info = attribute.getInfo();
    assertThat(info.isWritable(), is(true));
    assertThat(info.isReadable(), is(false));
    assertThat(info.getType(), equalTo(String.class.getName()));
    assertThat(info.isIs(), is(false));

    // setter-only attribute writes the value to the target
    attribute.setValue("updated");
    assertThat(fixture.getName(), equalTo("updated"));

    // getter is null, so reading must fail
    assertThrows(IllegalStateException.class, attribute::getValue);
  }

  @Test
  public void buildGetterAndSetter() throws Exception {
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .target(supplier)
        .getter(getName)
        .setter(setName)
        .build();

    MBeanAttributeInfo info = attribute.getInfo();
    assertThat(info.isReadable(), is(true));
    assertThat(info.isWritable(), is(true));
    // both present: type resolves from the getter return type, and this is not an 'is' form
    assertThat(info.getType(), equalTo(String.class.getName()));
    assertThat(info.isIs(), is(false));

    // getValue invokes the getter against the SAME target instance the supplier returns
    fixture.setName("direct-set");
    assertThat(attribute.getValue(), equalTo("direct-set"));

    // setValue invokes the setter against that SAME target instance (visible mutation)
    attribute.setValue("round-trip");
    assertThat(fixture.getName(), equalTo("round-trip"));

    // round-trip through the attribute observes the same mutation
    assertThat(attribute.getValue(), equalTo("round-trip"));
  }

  @Test
  public void buildIsGetter() throws Exception {
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("active")
        .target(supplier)
        .getter(isActive)
        .build();

    MBeanAttributeInfo info = attribute.getInfo();
    assertThat(info.isReadable(), is(true));
    assertThat(info.isWritable(), is(false));
    assertThat(info.isIs(), is(true));
    assertThat(info.getType(), equalTo(boolean.class.getName()));
    assertThat(attribute.getValue(), equalTo(true));
  }

  @Test
  public void buildFailsWhenNameNull() {
    ReflectionMBeanAttribute.Builder builder = new ReflectionMBeanAttribute.Builder()
        .target(supplier)
        .getter(getName);
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void buildFailsWhenTargetNull() {
    ReflectionMBeanAttribute.Builder builder = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .getter(getName);
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void buildFailsWhenNoGetterOrSetter() {
    ReflectionMBeanAttribute.Builder builder = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .target(supplier);
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void getValueFailsWhenTargetSuppliesNull() {
    // getter is present, so the getter guard passes and the target IS consulted before failing
    CountingSupplier countingSupplier = new CountingSupplier(null);
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .target(countingSupplier)
        .getter(getName)
        .build();

    assertThrows(IllegalStateException.class, attribute::getValue);
    // count == 1 proves the failure came from the target() null-check, not the getter null-check
    assertThat(countingSupplier.count(), is(1));
  }

  @Test
  public void setValueFailsWhenTargetSuppliesNull() {
    // setter is present, so the setter guard passes and the target IS consulted before failing
    CountingSupplier countingSupplier = new CountingSupplier(null);
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .target(countingSupplier)
        .setter(setName)
        .build();

    assertThrows(IllegalStateException.class, () -> attribute.setValue("other"));
    // count == 1 proves the failure came from the target() null-check, not the setter null-check
    assertThat(countingSupplier.count(), is(1));
  }

  @Test
  public void getValueGuardFiresBeforeConsultingTarget() {
    // setter-only attribute: getter is null, so getValue must fail on the getter guard
    CountingSupplier countingSupplier = new CountingSupplier(fixture);
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .target(countingSupplier)
        .setter(setName)
        .build();

    assertThrows(IllegalStateException.class, attribute::getValue);
    // count == 0 proves the getter null-check is distinct from the target() null-check
    assertThat(countingSupplier.count(), is(0));
  }

  @Test
  public void setValueGuardFiresBeforeConsultingTarget() {
    // getter-only attribute: setter is null, so setValue must fail on the setter guard
    CountingSupplier countingSupplier = new CountingSupplier(fixture);
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .target(countingSupplier)
        .getter(getName)
        .build();

    assertThrows(IllegalStateException.class, () -> attribute.setValue("other"));
    // count == 0 proves the setter null-check is distinct from the target() null-check
    assertThat(countingSupplier.count(), is(0));
  }

  @Test
  public void accessors() {
    ReflectionMBeanAttribute attribute = new ReflectionMBeanAttribute.Builder()
        .name("name")
        .description("the name attribute")
        .target(supplier)
        .getter(getName)
        .setter(setName)
        .build();

    assertThat(attribute.getInfo(), is(notNullValue()));
    assertThat(attribute.getInfo().getDescription(), equalTo("the name attribute"));
    assertThat(attribute.getName(), equalTo("name"));
    assertThat(attribute.getTarget(), sameInstance((Object) supplier));
    assertThat(attribute.getGetter(), equalTo(getName));
    assertThat(attribute.getSetter(), equalTo(setName));
    assertThat(attribute.toString(), equalTo(ReflectionMBeanAttribute.class.getSimpleName() + "{name='name'}"));
  }

  @Test
  public void constructorRoutesArgumentsToFields() {
    // name is derived from the supplied info, not passed as a separate argument
    MBeanAttributeInfo info =
        new MBeanAttributeInfo("derived", String.class.getName(), "desc", true, true, false);
    ReflectionMBeanAttribute attribute =
        new ReflectionMBeanAttribute(info, supplier, getName, setName);

    assertThat(attribute.getInfo(), sameInstance(info));
    assertThat(attribute.getName(), equalTo("derived"));
    assertThat(attribute.getTarget(), sameInstance((Object) supplier));
    assertThat(attribute.getGetter(), sameInstance(getName));
    assertThat(attribute.getSetter(), sameInstance(setName));
  }

  @Test
  public void constructorRejectsNullInfo() {
    assertThrows(NullPointerException.class,
        () -> new ReflectionMBeanAttribute(null, supplier, getName, setName));
  }

  @Test
  public void constructorRejectsNullTarget() {
    MBeanAttributeInfo info =
        new MBeanAttributeInfo("name", String.class.getName(), null, true, false, false);
    assertThrows(NullPointerException.class,
        () -> new ReflectionMBeanAttribute(info, null, getName, null));
  }

  /**
   * Fixture bean exercised by reflection from the tests.
   */
  public static class Fixture
  {
    private String name = "test-name";

    private boolean active = true;

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public boolean isActive() {
      return active;
    }
  }

  /**
   * Supplier that records how many times it was consulted, used to prove which {@code checkState}
   * guard fired (the method null-check versus the {@code target()} null-check).
   */
  public static class CountingSupplier
      implements Supplier<Object>
  {
    private final Object value;

    private int count;

    public CountingSupplier(final Object value) {
      this.value = value;
    }

    @Override
    public Object get() {
      count++;
      return value;
    }

    public int count() {
      return count;
    }
  }
}
