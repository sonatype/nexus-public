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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.management.Descriptor;
import javax.management.DescriptorKey;

import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class DescriptorHelperTest
{

  @TestAuthor("jason")
  public class TestBean
  {

    @TestComments("foo bar baz")
    public void foo() {
      // empty
    }

    @TestInvalidAnnotationValue(@TestComments("foo"))
    public void invalid1() {
      // empty
    }
  }

  @Test
  public void findsAnnotations() {
    TestBean bean = new TestBean();
    List<Annotation> annotations = DescriptorHelper.findAllAnnotations(bean.getClass().getAnnotations());

    // custom annotation should be found
    assertThat(annotations, hasItem(new AnnotationMatcher(TestAuthor.class.getName())));
  }

  @Test
  public void buildDescriptorFromType() {
    TestBean bean = new TestBean();
    Descriptor descriptor = DescriptorHelper.build(bean.getClass());

    // descriptor should have author
    assertThat(descriptor.getFields().length, equalTo(1));
    assertThat(descriptor.getFieldValue("author"), equalTo("jason"));
  }

  @Test
  public void buildDescriptorFromMethod() throws NoSuchMethodException {
    Method method = TestBean.class.getMethod("foo");
    Descriptor descriptor = DescriptorHelper.build(method);

    // descriptor should have comments
    assertThat(descriptor.getFields().length, equalTo(1));
    assertThat(descriptor.getFieldValue("comments"), equalTo("foo bar baz"));
  }

  @Test
  public void buildDescriptorFailsDueToInvalid() throws NoSuchMethodException {
    Method method = TestBean.class.getMethod("invalid1");
    assertThrows(DescriptorHelper.InvalidDescriptorKeyException.class, () -> DescriptorHelper.build(method));
  }

  private static class AnnotationMatcher
      extends CustomTypeSafeMatcher<Annotation>
  {
    private final String annotationName;

    public AnnotationMatcher(final String annotationName) {
      super("Matches: " + annotationName);
      this.annotationName = annotationName;
    }

    @Override
    protected boolean matchesSafely(final Annotation annotation) {
      return annotation.annotationType().getName().equals(annotationName);
    }
  }

  @Test
  public void buildCoercesClassValueToCanonicalName() {
    Descriptor descriptor = DescriptorHelper.build(ClassFixture.class);

    // a Class value is stored as its canonical name
    assertThat(descriptor.getFieldValue("classKey"), equalTo("java.lang.String"));
  }

  @Test
  public void buildCoercesEnumValueToName() {
    Descriptor descriptor = DescriptorHelper.build(EnumFixture.class);

    // an Enum value is stored as its name
    assertThat(descriptor.getFieldValue("enumKey"), equalTo("RED"));
  }

  @Test
  public void buildCoercesClassArrayToStringArray() {
    Descriptor descriptor = DescriptorHelper.build(ClassArrayFixture.class);

    // an array of Class values is converted to a String[] of class names
    Object value = descriptor.getFieldValue("classes");
    assertThat(value, instanceOf(String[].class));
    assertThat(Arrays.asList((String[]) value), equalTo(Arrays.asList("java.lang.String", "java.lang.Integer")));
  }

  @Test
  public void buildStoresPlainStringValue() {
    Descriptor descriptor = DescriptorHelper.build(StringFixture.class);

    // other (non-coerced) types are stored as-is
    assertThat(descriptor.getFieldValue("strKey"), equalTo("hello"));
  }

  @Test
  public void buildStoresPrimitiveValue() {
    Descriptor descriptor = DescriptorHelper.build(IntFixture.class);

    // primitives are valid and stored as-is
    assertThat(descriptor.getFieldValue("intKey"), equalTo(42));
  }

  @Test
  public void buildFailsForSingleAnnotationValue() {
    // a single Annotation value is forbidden
    DescriptorHelper.InvalidDescriptorKeyException e = assertThrows(
        DescriptorHelper.InvalidDescriptorKeyException.class, () -> DescriptorHelper.build(AnnoFixture.class));
    assertThat(e.getMessage(), startsWith("Invalid @DescriptorKey:"));
    assertThat(e.getMessage(), containsString(", method="));
  }

  @Test
  public void buildCoercesEnumArrayToStringArray() {
    // The enum-array branch checks Enum.class.equals(componentType), so it is only reachable when the array's
    // component type is exactly Enum (which a real @interface member cannot express); use a custom Annotation.
    Descriptor descriptor = DescriptorHelper.build(new EnumArrayAnnotationImpl());

    Object value = descriptor.getFieldValue("enums");
    assertThat(value, instanceOf(String[].class));
    assertThat(Arrays.asList((String[]) value), equalTo(Arrays.asList("RED", "GREEN")));
  }

  @Test
  public void buildFailsForAnnotationArrayValue() {
    // The annotation-array branch checks Annotation.class.equals(componentType), so it is only reachable when the
    // array's component type is exactly Annotation; use a custom Annotation to exercise the forbidden path.
    DescriptorHelper.InvalidDescriptorKeyException e =
        assertThrows(DescriptorHelper.InvalidDescriptorKeyException.class,
            () -> DescriptorHelper.build(new AnnotationArrayAnnotationImpl()));
    assertThat(e.getMessage(), startsWith("Invalid @DescriptorKey:"));
    assertThat(e.getMessage(), containsString(", method="));
  }

  @Test
  public void buildDoesNotCoerceConcreteEnumArray() {
    // A real @interface array member has a concrete component type (Color), which is not equal to Enum.class,
    // so the value falls through as a valid "other" type and is stored as-is rather than converted to String[].
    Descriptor descriptor = DescriptorHelper.build(EnumArrayFixture.class);

    Object value = descriptor.getFieldValue("enums");
    assertThat(value, instanceOf(Color[].class));
    // the concrete enum array is preserved element-for-element rather than coerced to String[]
    assertThat(Arrays.asList((Color[]) value), equalTo(Arrays.asList(Color.RED, Color.GREEN)));
  }

  @Test
  public void stringValueReturnsStringWhenCharSequence() {
    Descriptor descriptor = DescriptorHelper.build(StringFixture.class);

    assertThat(DescriptorHelper.stringValue(descriptor, "strKey"), equalTo("hello"));
  }

  @Test
  public void stringValueReturnsNullWhenNotCharSequence() {
    Descriptor descriptor = DescriptorHelper.build(IntFixture.class);

    // the field value is an Integer, not a CharSequence
    assertThat(DescriptorHelper.stringValue(descriptor, "intKey"), is(nullValue()));
  }

  @Test
  public void stringValueReturnsNullWhenFieldAbsent() {
    Descriptor descriptor = DescriptorHelper.build(StringFixture.class);

    assertThat(DescriptorHelper.stringValue(descriptor, "missing"), is(nullValue()));
  }

  @Test
  public void stringValueThrowsWhenDescriptorNull() {
    assertThrows(NullPointerException.class, () -> DescriptorHelper.stringValue(null, "strKey"));
  }

  @Test
  public void stringValueThrowsWhenNameNull() {
    Descriptor descriptor = DescriptorHelper.build(StringFixture.class);

    assertThrows(NullPointerException.class, () -> DescriptorHelper.stringValue(descriptor, null));
  }

  @Test
  public void buildThrowsWhenTypeNull() {
    assertThrows(NullPointerException.class, () -> DescriptorHelper.build((Class<?>) null));
  }

  @Test
  public void buildThrowsWhenMethodNull() {
    assertThrows(NullPointerException.class, () -> DescriptorHelper.build((Method) null));
  }

  @Test
  public void buildThrowsWhenAnnotationsNull() {
    assertThrows(NullPointerException.class, () -> DescriptorHelper.build((Annotation[]) null));
  }

  @Test
  public void buildReturnsEmptyDescriptorWhenNoDescriptorKeys() {
    Descriptor descriptor = DescriptorHelper.build(NoKeyFixture.class);

    // a type with no @DescriptorKey annotations yields a descriptor with no fields
    assertThat(descriptor.getFields().length, equalTo(0));
  }

  @Test
  public void buildSkipsNullDescriptorKeyValue() {
    // when a @DescriptorKey member returns null the field is skipped entirely
    Descriptor descriptor = DescriptorHelper.build(new NullValueAnnotationImpl());

    assertThat(descriptor.getFieldValue("nullKey"), is(nullValue()));
    assertThat(descriptor.getFields().length, equalTo(0));
  }

  @Test
  public void buildCoercesSingleNestedClassUsingCanonicalNameNotBinaryName() {
    // the single-Class branch uses getCanonicalName() (dotted), not getName() (binary, with '$')
    Descriptor descriptor = DescriptorHelper.build(NestedClassFixture.class);

    assertThat(descriptor.getFieldValue("classKey"), equalTo("java.util.Map.Entry"));
  }

  @Test
  public void buildCoercesNestedClassArrayUsingBinaryNameNotCanonicalName() {
    // the Class[] branch uses getName() (binary, with '$'), not getCanonicalName()
    Descriptor descriptor = DescriptorHelper.build(NestedClassArrayFixture.class);

    Object value = descriptor.getFieldValue("classes");
    assertThat(value, instanceOf(String[].class));
    assertThat(Arrays.asList((String[]) value), equalTo(Arrays.asList("java.util.Map$Entry")));
  }

  enum Color
  {
    RED,
    GREEN
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface SomeAnno
  {
    // marker annotation used as an invalid @DescriptorKey value
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface WithClass
  {
    @DescriptorKey("classKey")
    Class<?> value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface WithEnum
  {
    @DescriptorKey("enumKey")
    Color value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface WithClassArray
  {
    @DescriptorKey("classes")
    Class<?>[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface WithEnumArray
  {
    @DescriptorKey("enums")
    Color[] value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface WithAnno
  {
    @DescriptorKey("anno")
    SomeAnno value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface WithString
  {
    @DescriptorKey("strKey")
    String value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface WithInt
  {
    @DescriptorKey("intKey")
    int value();
  }

  @WithClass(String.class)
  static class ClassFixture
  {
    // empty
  }

  @WithEnum(Color.RED)
  static class EnumFixture
  {
    // empty
  }

  @WithClassArray({String.class, Integer.class})
  static class ClassArrayFixture
  {
    // empty
  }

  @WithEnumArray({Color.RED, Color.GREEN})
  static class EnumArrayFixture
  {
    // empty
  }

  @WithAnno(@SomeAnno)
  static class AnnoFixture
  {
    // empty
  }

  @WithString("hello")
  static class StringFixture
  {
    // empty
  }

  @WithInt(42)
  static class IntFixture
  {
    // empty
  }

  @WithClass(Map.Entry.class)
  static class NestedClassFixture
  {
    // empty
  }

  @WithClassArray({Map.Entry.class})
  static class NestedClassArrayFixture
  {
    // empty
  }

  static class NoKeyFixture
  {
    // a type with no annotations at all (no @DescriptorKey)
  }

  /**
   * Custom {@link Annotation} whose {@code @DescriptorKey} member returns {@code null} to exercise the
   * value-skipping branch (a real {@code @interface} member can never be null).
   */
  interface NullValueAnnotation
      extends Annotation
  {
    @DescriptorKey("nullKey")
    Object value();
  }

  static class NullValueAnnotationImpl
      implements NullValueAnnotation
  {
    @Override
    public Object value() {
      return null;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return NullValueAnnotation.class;
    }
  }

  /**
   * Custom {@link Annotation} whose member component type is exactly {@link Enum}, which a real
   * {@code @interface} cannot express; used to exercise the enum-array coercion branch.
   */
  interface EnumArrayAnnotation
      extends Annotation
  {
    @DescriptorKey("enums")
    Enum<?>[] value();
  }

  static class EnumArrayAnnotationImpl
      implements EnumArrayAnnotation
  {
    @Override
    public Enum<?>[] value() {
      return new Enum<?>[]{Color.RED, Color.GREEN};
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return EnumArrayAnnotation.class;
    }
  }

  /**
   * Custom {@link Annotation} whose member component type is exactly {@link Annotation}, which a real
   * {@code @interface} cannot express; used to exercise the forbidden annotation-array branch.
   */
  interface AnnotationArrayAnnotation
      extends Annotation
  {
    @DescriptorKey("annos")
    Annotation[] value();
  }

  static class AnnotationArrayAnnotationImpl
      implements AnnotationArrayAnnotation
  {
    @Override
    public Annotation[] value() {
      return new Annotation[0];
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return AnnotationArrayAnnotation.class;
    }
  }
}
