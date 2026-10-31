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

import javax.management.AttributeNotFoundException;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.MBeanAttributeInfo;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThrows;

public class SuppliedMBeanAttributeTest
{
  private static SuppliedMBeanAttribute.Builder builder() {
    return new SuppliedMBeanAttribute.Builder();
  }

  @Test
  public void buildWithExplicitTypeAndSupplier() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .type("java.lang.String")
        .supplier(() -> "value")
        .build();

    assertThat(attribute.getInfo().getType(), equalTo("java.lang.String"));
    assertThat(attribute.getName(), equalTo("test"));
    assertThat(attribute.getValue(), equalTo("value"));
  }

  @Test
  public void buildWithValueAutoSetsType() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("greeting")
        .value("hello")
        .build();

    assertThat(attribute.getInfo().getType(), equalTo("java.lang.String"));
    assertThat(attribute.getValue(), equalTo("hello"));
  }

  @Test
  public void buildWithSupplierOnlyResolvesTypeFromValue() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("count")
        .supplier(() -> Integer.valueOf(42))
        .build();

    assertThat(attribute.getInfo().getType(), equalTo("java.lang.Integer"));
    assertThat(attribute.getValue(), equalTo(Integer.valueOf(42)));
  }

  @Test
  public void buildWithNullNameThrows() {
    SuppliedMBeanAttribute.Builder builder = builder()
        .supplier(() -> "value");

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void buildWithNullSupplierThrows() {
    SuppliedMBeanAttribute.Builder builder = builder()
        .name("test");

    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void buildWithNullValueAndNoTypeThrows() {
    SuppliedMBeanAttribute.Builder builder = builder()
        .name("test")
        .value(null);

    IllegalStateException exception = assertThrows(IllegalStateException.class, builder::build);
    assertThat(exception.getMessage(), containsString("Can not resolve type"));
  }

  @Test
  public void typeClassSetsClassName() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .type(Integer.class)
        .supplier(() -> Integer.valueOf(1))
        .build();

    assertThat(attribute.getInfo().getType(), equalTo("java.lang.Integer"));
  }

  @Test
  public void descriptorAndDescriptionCarriedIntoInfo() throws Exception {
    Descriptor descriptor = new ImmutableDescriptor("foo=bar");
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .description("a test attribute")
        .value("hello")
        .descriptor(descriptor)
        .build();

    MBeanAttributeInfo info = attribute.getInfo();
    assertThat(info.getDescription(), equalTo("a test attribute"));
    assertThat(info.getDescriptor().getFieldValue("foo"), equalTo((Object) "bar"));
  }

  @Test
  public void instanceMethodsReflectConstruction() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .value("hello")
        .build();

    MBeanAttributeInfo info = attribute.getInfo();
    assertThat(info, sameInstance(attribute.getInfo()));
    assertThat(info.getName(), equalTo("test"));
    assertThat(info.isReadable(), is(true));
    assertThat(info.isWritable(), is(false));

    assertThat(attribute.getName(), equalTo("test"));
    assertThat(attribute.getValue(), equalTo("hello"));
    assertThat(attribute.toString(), startsWith("SuppliedMBeanAttribute{"));
    assertThat(attribute.toString(), containsString("name='test'"));
    assertThat(attribute.toString(), containsString("supplier="));
  }

  @Test
  public void getValueReturnsNullWhenSupplierReturnsNull() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .type("java.lang.String")
        .supplier(() -> null)
        .build();

    assertThat(attribute.getValue(), is(nullValue()));
  }

  @Test
  public void setValueThrowsAttributeNotFoundException() {
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .value("hello")
        .build();

    AttributeNotFoundException exception =
        assertThrows(AttributeNotFoundException.class, () -> attribute.setValue("other"));
    assertThat(exception.getMessage(), containsString("read-only"));
    assertThat(exception.getMessage(), containsString("test"));
  }

  @Test
  public void constructorRejectsNullInfo() {
    assertThrows(NullPointerException.class, () -> new SuppliedMBeanAttribute(null, () -> "value"));
  }

  @Test
  public void constructorRejectsNullSupplier() {
    MBeanAttributeInfo info = new MBeanAttributeInfo("test", "java.lang.String", "desc", true, false, false);
    assertThrows(NullPointerException.class, () -> new SuppliedMBeanAttribute(info, null));
  }

  @Test
  public void constructedAttributeExposesInfoAndSuppliedValue() {
    MBeanAttributeInfo info = new MBeanAttributeInfo("direct", "java.lang.String", "desc", true, false, false);
    SuppliedMBeanAttribute attribute = new SuppliedMBeanAttribute(info, () -> "supplied");

    assertThat(attribute.getInfo(), sameInstance(info));
    assertThat(attribute.getName(), equalTo("direct"));
    assertThat(attribute.getName(), equalTo(info.getName()));
    assertThat(attribute.getValue(), equalTo("supplied"));
  }

  @Test
  public void builderMethodsReturnSameInstanceForChaining() {
    SuppliedMBeanAttribute.Builder builder = builder();
    assertThat(builder.name("test"), sameInstance(builder));
    assertThat(builder.description("desc"), sameInstance(builder));
    assertThat(builder.type("java.lang.String"), sameInstance(builder));
    assertThat(builder.type(Integer.class), sameInstance(builder));
    assertThat(builder.supplier(() -> "value"), sameInstance(builder));
    assertThat(builder.value("value"), sameInstance(builder));
    assertThat(builder.descriptor(new ImmutableDescriptor("foo=bar")), sameInstance(builder));
  }

  @Test
  public void valueOverridesPreviouslyConfiguredType() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .type("java.lang.Integer")
        .value("hello")
        .build();

    // value(non-null) auto-sets the type from the value class, overriding the earlier type(...)
    assertThat(attribute.getInfo().getType(), equalTo("java.lang.String"));
    assertThat(attribute.getValue(), equalTo("hello"));
  }

  @Test
  public void explicitTypeAfterValueWins() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .value("hello")
        .type("java.lang.Integer")
        .build();

    assertThat(attribute.getInfo().getType(), equalTo("java.lang.Integer"));
    assertThat(attribute.getValue(), equalTo("hello"));
  }

  @Test
  public void valueNullWithExplicitTypeBuildsAndReturnsNull() throws Exception {
    SuppliedMBeanAttribute attribute = builder()
        .name("test")
        .type("java.lang.String")
        .value(null)
        .build();

    // value(null) does not auto-set the type, so the explicit type is preserved
    assertThat(attribute.getInfo().getType(), equalTo("java.lang.String"));
    assertThat(attribute.getValue(), is(nullValue()));
  }
}
