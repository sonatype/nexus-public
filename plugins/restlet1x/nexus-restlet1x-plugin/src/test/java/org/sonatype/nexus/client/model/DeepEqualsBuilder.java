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
package org.sonatype.nexus.client.model;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;

public class DeepEqualsBuilder
    extends EqualsBuilder
{

  private boolean testTransients;

  private String[] excludeFields;

  public DeepEqualsBuilder(boolean testTransients, String[] excludeFields) {
    this.testTransients = testTransients;
    this.excludeFields = excludeFields;
  }

  public static boolean reflectionDeepEquals(Object lhs, Object rhs, boolean testTransients, Class reflectUpToClass,
                                             String[] excludeFields)
  {
    if (lhs == rhs) {
      return true;
    }
    if (lhs == null || rhs == null) {
      return false;
    }
    // Find the leaf class since there may be transients in the leaf
    // class or in classes between the leaf and root.
    // If we are not testing transients or a subclass has no ivars,
    // then a subclass can test equals to a superclass.
    Class testClass = getTestClass(lhs, rhs);
    if (testClass == null) {
      // The two classes are not related.
      return false;
    }
    DeepEqualsBuilder equalsBuilder = new DeepEqualsBuilder(testTransients, excludeFields);
    try {
      reflectionAppend(lhs, rhs, testClass, equalsBuilder, testTransients, excludeFields);
      // while ( testClass.getSuperclass() != null && testClass != reflectUpToClass )
      // {
      // testClass = testClass.getSuperclass();
      // reflectionAppend( lhs, rhs, testClass, equalsBuilder, testTransients, excludeFields );
      // }
    }
    catch (IllegalArgumentException e) {
      // In this case, we tried to test a subclass vs. a superclass and
      // the subclass has ivars or the ivars are transient and
      // we are testing transients.
      // If a subclass has ivars that we are trying to test them, we get an
      // exception and we know that the objects are not equal.
      return false;
    }
    return equalsBuilder.isEquals();
  }

  private static void reflectionAppend(Object lhs, Object rhs, Class clazz, EqualsBuilder builder,
                                       boolean useTransients, String[] excludeFields)
  {
    while (clazz.getSuperclass() != null) {

      Field[] fields = clazz.getDeclaredFields();
      List excludedFieldList = excludeFields != null ? Arrays.asList(excludeFields) : Collections.EMPTY_LIST;
      AccessibleObject.setAccessible(fields, true);
      for (int i = 0; i < fields.length && builder.isEquals(); i++) {
        Field f = fields[i];
        if (!excludedFieldList.contains(f.getName()) && (f.getName().indexOf('$') == -1)
            && (useTransients || !Modifier.isTransient(f.getModifiers()))
            && (!Modifier.isStatic(f.getModifiers()))) {
          try {
            Object lhsChild = f.get(lhs);
            Object rhsChild = f.get(rhs);
            Class testClass = getTestClass(lhsChild, rhsChild);
            boolean hasEqualsMethod = classHasEqualsMethod(testClass);

            if (testClass != null && !hasEqualsMethod) {
              reflectionAppend(lhsChild, rhsChild, testClass, builder, useTransients, excludeFields);
            }
            else {
              builder.append(lhsChild, rhsChild);
            }
          }
          catch (IllegalAccessException e) {
            // this can't happen. Would get a Security exception instead
            // throw a runtime exception in case the impossible happens.
            throw new InternalError("Unexpected IllegalAccessException");
          }
        }
      }

      // now for the parent
      clazz = clazz.getSuperclass();
      reflectionAppend(lhs, rhs, clazz, builder, useTransients, excludeFields);
    }
  }

  private static Class getTestClass(Object lhs, Object rhs) {
    Class testClass = null;
    if (lhs != null && rhs != null) {

      Class lhsClass = lhs.getClass();
      Class rhsClass = rhs.getClass();

      if (lhsClass.isInstance(rhs)) {
        testClass = lhsClass;
        if (!rhsClass.isInstance(lhs)) {
          // rhsClass is a subclass of lhsClass
          testClass = rhsClass;
        }
      }
      else if (rhsClass.isInstance(lhs)) {
        testClass = rhsClass;
        if (!lhsClass.isInstance(rhs)) {
          // lhsClass is a subclass of rhsClass
          testClass = lhsClass;
        }
      }
    }
    return testClass;
  }

  private static boolean classHasEqualsMethod(Class clazz) {
    if (clazz != null) {
      Method[] methods = clazz.getDeclaredMethods();
      for (int ii = 0; ii < methods.length; ii++) {
        Method method = methods[ii];
        if ("equals".equals(method.getName()) && method.getParameterTypes().length == 1
            && method.getParameterTypes()[0].equals(Object.class)) {
          return true;
        }
      }
    }
    return false;
  }

}
