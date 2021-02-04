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
package org.sonatype.nexus.common.decorator;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.sonatype.nexus.common.decorator.DecoratorUtils.getDecoratedEntity;

public class DecoratorUtilsTest
{
  @Test
  public void testGetDecoratedEntity() {
    DecoratorTest first = new DefaultDecoratorTest();
    DecoratorTest second = new TestDecorator1(first);
    DecoratorTest third = new TestDecorator2(second);

    assertThat(getDecoratedEntity(first, DefaultDecoratorTest.class), notNullValue());
    assertThat(getDecoratedEntity(first, TestDecorator1.class), nullValue());
    assertThat(getDecoratedEntity(first, TestDecorator2.class), nullValue());

    assertThat(getDecoratedEntity(second, DefaultDecoratorTest.class), notNullValue());
    assertThat(getDecoratedEntity(second, TestDecorator1.class), notNullValue());
    assertThat(getDecoratedEntity(second, TestDecorator1.class).specialMethod1(), equalTo("special1"));
    assertThat(getDecoratedEntity(second, TestDecorator2.class), nullValue());

    assertThat(getDecoratedEntity(third, DefaultDecoratorTest.class), notNullValue());
    assertThat(getDecoratedEntity(third, TestDecorator1.class), notNullValue());
    assertThat(getDecoratedEntity(second, TestDecorator1.class).specialMethod1(), equalTo("special1"));
    assertThat(getDecoratedEntity(third, TestDecorator2.class), notNullValue());
    assertThat(getDecoratedEntity(third, TestDecorator2.class).specialMethod2(), equalTo("special2"));
  }

  // DECORATOR CLASSES

  private interface DecoratorTest
  {
    String getValue();
  }

  private class DefaultDecoratorTest
      implements DecoratorTest
  {
    @Override
    public String getValue() {
      return "default";
    }
  }

  private abstract class DecoratedTest
      implements DecoratorTest, DecoratedObject<DecoratorTest>
  {
    private final DecoratorTest wrapped;

    DecoratedTest(final DecoratorTest wrapped) {
      this.wrapped = wrapped;
    }

    @Override
    public String getValue() {
      return wrapped.getValue();
    }

    @Override
    public DecoratorTest getWrappedObject() {
      return wrapped;
    }
  }

  private final class TestDecorator1
      extends DecoratedTest
  {
    TestDecorator1(final DecoratorTest wrapped) {
      super(wrapped);
    }

    @Override
    public String getValue() {
      return "overridden";
    }

    String specialMethod1() {
      return "special1";
    }
  }

  private final class TestDecorator2
      extends DecoratedTest
  {
    TestDecorator2(final DecoratorTest wrapped) {
      super(wrapped);
    }

    String specialMethod2() {
      return "special2";
    }
  }
}
