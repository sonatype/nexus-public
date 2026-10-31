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

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import org.sonatype.nexus.jmx.OperationKey;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThrows;

public class ReflectionMBeanOperationTest
{
  private Fixture fixture;

  private Supplier<Fixture> supplier;

  private Method greet;

  private Method doThing;

  private Method concat;

  @Before
  public void setUp() throws Exception {
    fixture = new Fixture();
    supplier = () -> fixture;
    greet = Fixture.class.getMethod("greet", String.class);
    doThing = Fixture.class.getMethod("doThing");
    concat = Fixture.class.getMethod("concat", String.class, int.class);
  }

  @Test
  public void buildWithExplicitProperties() {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .name("doGreet")
        .description("Greets someone")
        .impact(MBeanOperationInfo.ACTION)
        .target(supplier)
        .method(greet)
        .build();

    MBeanOperationInfo info = operation.getInfo();
    assertThat(info.getName(), is("doGreet"));
    assertThat(info.getDescription(), is("Greets someone"));
    assertThat(info.getImpact(), is(MBeanOperationInfo.ACTION));
    assertThat(info.getReturnType(), is(String.class.getName()));

    // signature is derived from the method parameters
    MBeanParameterInfo[] signature = info.getSignature();
    assertThat(signature.length, is(1));
    assertThat(signature[0].getType(), is(String.class.getName()));

    // parameter name is extracted from the method bytecode via paranamer
    assertThat(signature[0].getName(), is("who"));
  }

  @Test
  public void buildDefaultsNameToMethodName() {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .target(supplier)
        .method(doThing)
        .build();

    // name was not provided, so it defaults to the method name
    assertThat(operation.getName(), is("doThing"));
    assertThat(operation.getInfo().getName(), is("doThing"));

    // void method with no parameters and unknown impact by default
    assertThat(operation.getInfo().getReturnType(), is(void.class.getName()));
    assertThat(operation.getInfo().getSignature().length, is(0));
    assertThat(operation.getInfo().getImpact(), is(MBeanOperationInfo.UNKNOWN));
  }

  @Test
  public void buildExtractsMultiParamSignatureInOrder() {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .target(supplier)
        .method(concat)
        .build();

    MBeanParameterInfo[] signature = operation.getInfo().getSignature();
    assertThat(signature.length, is(2));

    // types are extracted in declaration order
    assertThat(signature[0].getType(), is(String.class.getName()));
    assertThat(signature[1].getType(), is(int.class.getName()));

    // names are extracted in declaration order via paranamer
    assertThat(signature[0].getName(), is("first"));
    assertThat(signature[1].getName(), is("second"));

    // return type is carried from the method
    assertThat(operation.getInfo().getReturnType(), is(String.class.getName()));
  }

  @Test
  public void buildFailsWhenTargetNull() {
    ReflectionMBeanOperation.Builder builder = new ReflectionMBeanOperation.Builder()
        .method(doThing);
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void buildFailsWhenMethodNull() {
    ReflectionMBeanOperation.Builder builder = new ReflectionMBeanOperation.Builder()
        .target(supplier);
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void accessors() {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .name("doGreet")
        .description("Greets someone")
        .target(supplier)
        .method(greet)
        .build();

    assertThat(operation.getInfo(), is(notNullValue()));
    assertThat(operation.getInfo().getDescription(), equalTo("Greets someone"));
    assertThat(operation.getName(), equalTo("doGreet"));
    assertThat(operation.getTarget(), sameInstance((Object) supplier));
    assertThat(operation.getMethod(), equalTo(greet));
  }

  @Test
  public void constructorDerivesNameAndKeyFromInfo() {
    // the operation name and key are derived from the supplied info, NOT from the method name
    MBeanParameterInfo[] signature = new MBeanParameterInfo[]{
        new MBeanParameterInfo("who", String.class.getName(), null)
    };
    MBeanOperationInfo info = new MBeanOperationInfo(
        "explicitName",
        "desc",
        signature,
        String.class.getName(),
        MBeanOperationInfo.ACTION);

    ReflectionMBeanOperation operation = new ReflectionMBeanOperation(info, supplier, greet);

    assertThat(operation.getInfo(), sameInstance(info));
    // name comes from info.getName() ("explicitName"), not from the method ("greet")
    assertThat(operation.getName(), is("explicitName"));
    assertThat(operation.getKey(), equalTo(new OperationKey(info)));
    assertThat(operation.getTarget(), sameInstance((Object) supplier));
    assertThat(operation.getMethod(), equalTo(greet));
  }

  @Test
  public void constructorFailsWhenInfoNull() {
    assertThrows(NullPointerException.class,
        () -> new ReflectionMBeanOperation(null, supplier, greet));
  }

  @Test
  public void constructorFailsWhenTargetNull() {
    MBeanOperationInfo info = new MBeanOperationInfo("desc", greet);
    assertThrows(NullPointerException.class,
        () -> new ReflectionMBeanOperation(info, null, greet));
  }

  @Test
  public void constructorFailsWhenMethodNull() {
    MBeanOperationInfo info = new MBeanOperationInfo("desc", greet);
    assertThrows(NullPointerException.class,
        () -> new ReflectionMBeanOperation(info, supplier, null));
  }

  @Test
  public void getKeyMatchesInfo() {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .name("doGreet")
        .target(supplier)
        .method(greet)
        .build();

    OperationKey key = operation.getKey();
    assertThat(key, equalTo(new OperationKey(operation.getInfo())));
    assertThat(key.getName(), is("doGreet"));
    assertThat(key.getTypes(), contains(String.class.getName()));

    // key is the same cached instance across calls
    assertThat(operation.getKey(), sameInstance(operation.getKey()));
  }

  @Test
  public void invokeReturnsResult() throws Exception {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .target(supplier)
        .method(greet)
        .build();

    Object result = operation.invoke(new Object[]{"bob"});
    assertThat(result, equalTo("hi bob"));
  }

  @Test
  public void invokeVoidMethodReturnsNull() throws Exception {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .target(supplier)
        .method(doThing)
        .build();

    Object result = operation.invoke(new Object[0]);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void invokeFailsWhenTargetSuppliesNull() {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .target(() -> null)
        .method(doThing)
        .build();

    assertThrows(IllegalStateException.class, () -> operation.invoke(new Object[0]));
  }

  @Test
  public void toStringContainsNameAndKey() {
    ReflectionMBeanOperation operation = new ReflectionMBeanOperation.Builder()
        .name("doGreet")
        .target(supplier)
        .method(greet)
        .build();

    String string = operation.toString();
    assertThat(string, containsString(ReflectionMBeanOperation.class.getSimpleName()));
    // exact rendered format: name='doGreet' and key=<key>
    assertThat(string, containsString("name='doGreet'"));
    assertThat(string, containsString("key=" + operation.getKey()));
    assertThat(string, containsString(operation.getKey().toString()));
  }

  /**
   * Fixture bean exercised by reflection from the tests.
   */
  public static class Fixture
  {
    public String greet(final String who) {
      return "hi " + who;
    }

    public void doThing() {
      // empty
    }

    public String concat(final String first, final int second) {
      return first + second;
    }
  }
}
