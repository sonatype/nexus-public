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

import java.util.Arrays;
import java.util.Collections;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ReflectionException;
import javax.management.ServiceNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class MBeanTest
{
  private static final String ATTRIBUTE_NAME = "attr";

  private static final String OPERATION_NAME = "op";

  @Mock
  private MBeanAttribute attribute;

  @Mock
  private MBeanAttribute otherAttribute;

  @Mock
  private MBeanOperation operation;

  private MBeanInfo info;

  private MBean underTest;

  @Before
  public void setUp() {
    info = new MBeanInfo("x", "d", new MBeanAttributeInfo[0], null, new MBeanOperationInfo[0], null);

    when(attribute.getName()).thenReturn(ATTRIBUTE_NAME);
    when(operation.getKey()).thenReturn(new OperationKey(OPERATION_NAME, new String[0]));

    underTest = new MBean(info, Collections.singletonList(attribute), Collections.singletonList(operation));
  }

  @Test
  public void constructorRejectsNullInfo() throws Exception {
    assertThrows(NullPointerException.class,
        () -> new MBean(null, Collections.emptyList(), Collections.emptyList()));
  }

  @Test
  public void getMBeanInfoReturnsInfo() throws Exception {
    assertThat(underTest.getMBeanInfo(), sameInstance(info));
  }

  @Test
  public void getAttributesReturnsSuppliedAttributes() throws Exception {
    assertThat(underTest.getAttributes(), contains(attribute));
  }

  @Test
  public void getOperationsReturnsSuppliedOperations() throws Exception {
    assertThat(underTest.getOperations(), contains(operation));
  }

  @Test
  public void getAttributeReturnsValue() throws Exception {
    when(attribute.getValue()).thenReturn("value");

    assertThat(underTest.getAttribute(ATTRIBUTE_NAME), is((Object) "value"));
  }

  @Test
  public void getAttributeUnknownNameThrowsAttributeNotFound() throws Exception {
    assertThrows(AttributeNotFoundException.class, () -> underTest.getAttribute("missing"));
  }

  @Test
  public void getAttributeWrapsGenericExceptionInMBeanException() throws Exception {
    Exception cause = new Exception("boom");
    when(attribute.getValue()).thenThrow(cause);

    MBeanException thrown = assertThrows(MBeanException.class, () -> underTest.getAttribute(ATTRIBUTE_NAME));
    assertThat(thrown.getCause(), sameInstance((Throwable) cause));
  }

  @Test
  public void getAttributePropagatesAttributeNotFoundException() throws Exception {
    AttributeNotFoundException cause = new AttributeNotFoundException("nope");
    when(attribute.getValue()).thenThrow(cause);

    AttributeNotFoundException thrown =
        assertThrows(AttributeNotFoundException.class, () -> underTest.getAttribute(ATTRIBUTE_NAME));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void getAttributePropagatesReflectionException() throws Exception {
    ReflectionException cause = new ReflectionException(new Exception("r"));
    when(attribute.getValue()).thenThrow(cause);

    ReflectionException thrown =
        assertThrows(ReflectionException.class, () -> underTest.getAttribute(ATTRIBUTE_NAME));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void getAttributePropagatesMBeanException() throws Exception {
    MBeanException cause = new MBeanException(new Exception("inner"));
    when(attribute.getValue()).thenThrow(cause);

    MBeanException thrown =
        assertThrows(MBeanException.class, () -> underTest.getAttribute(ATTRIBUTE_NAME));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void getAttributeNullNameThrowsRawNpe() {
    // attribute(name) calls checkNotNull(name) inside the try; the resulting NullPointerException is a
    // RuntimeException, which Throwables.propagateIfPossible rethrows as-is rather than wrapping in MBeanException.
    assertThrows(NullPointerException.class, () -> underTest.getAttribute(null));
  }

  @Test
  public void setAttributeCallsSetValue() throws Exception {
    underTest.setAttribute(new Attribute(ATTRIBUTE_NAME, "v"));

    verify(attribute).setValue("v");
  }

  @Test
  public void setAttributeUnknownNameThrowsAttributeNotFound() throws Exception {
    assertThrows(AttributeNotFoundException.class, () -> underTest.setAttribute(new Attribute("missing", "v")));
  }

  @Test
  public void setAttributePropagatesInvalidAttributeValueException() throws Exception {
    InvalidAttributeValueException cause = new InvalidAttributeValueException("bad");
    doThrow(cause).when(attribute).setValue(any());

    InvalidAttributeValueException thrown = assertThrows(InvalidAttributeValueException.class,
        () -> underTest.setAttribute(new Attribute(ATTRIBUTE_NAME, "v")));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void setAttributeWrapsGenericExceptionInMBeanException() throws Exception {
    Exception cause = new Exception("boom");
    doThrow(cause).when(attribute).setValue(any());

    MBeanException thrown =
        assertThrows(MBeanException.class, () -> underTest.setAttribute(new Attribute(ATTRIBUTE_NAME, "v")));
    assertThat(thrown.getCause(), sameInstance((Throwable) cause));
  }

  @Test
  public void setAttributePropagatesAttributeNotFoundException() throws Exception {
    AttributeNotFoundException cause = new AttributeNotFoundException("nope");
    doThrow(cause).when(attribute).setValue(any());

    AttributeNotFoundException thrown = assertThrows(AttributeNotFoundException.class,
        () -> underTest.setAttribute(new Attribute(ATTRIBUTE_NAME, "v")));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void setAttributePropagatesMBeanException() throws Exception {
    MBeanException cause = new MBeanException(new Exception("inner"));
    doThrow(cause).when(attribute).setValue(any());

    MBeanException thrown = assertThrows(MBeanException.class,
        () -> underTest.setAttribute(new Attribute(ATTRIBUTE_NAME, "v")));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void setAttributePropagatesReflectionException() throws Exception {
    ReflectionException cause = new ReflectionException(new Exception("r"));
    doThrow(cause).when(attribute).setValue(any());

    ReflectionException thrown = assertThrows(ReflectionException.class,
        () -> underTest.setAttribute(new Attribute(ATTRIBUTE_NAME, "v")));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void setAttributeNullAttributeThrowsRawNpe() {
    // attribute.getName() NPEs inside the try; RuntimeExceptions are rethrown as-is (not wrapped in MBeanException).
    assertThrows(NullPointerException.class, () -> underTest.setAttribute(null));
  }

  @Test
  public void getAttributesArrayReturnsKnownValues() throws Exception {
    when(attribute.getValue()).thenReturn("value");

    AttributeList list = underTest.getAttributes(new String[]{ATTRIBUTE_NAME});

    assertThat(list.size(), is(1));
    Attribute result = (Attribute) list.get(0);
    assertThat(result.getName(), is(ATTRIBUTE_NAME));
    assertThat(result.getValue(), is((Object) "value"));
  }

  @Test
  public void getAttributesArraySkipsFailingAttribute() throws Exception {
    when(attribute.getValue()).thenReturn("value");
    when(otherAttribute.getName()).thenReturn("attr2");
    when(otherAttribute.getValue()).thenThrow(new Exception("boom"));

    MBean mbean = new MBean(info, Arrays.asList(attribute, otherAttribute), Collections.emptyList());

    AttributeList list = mbean.getAttributes(new String[]{ATTRIBUTE_NAME, "attr2"});

    assertThat(list.size(), is(1));
    Attribute result = (Attribute) list.get(0);
    assertThat(result.getName(), is(ATTRIBUTE_NAME));
    assertThat(result.getValue(), is((Object) "value"));
  }

  @Test
  public void getAttributesNullNamesReturnsEmpty() throws Exception {
    AttributeList list = underTest.getAttributes((String[]) null);

    assertThat(list, empty());
  }

  @Test
  public void getAttributesEmptyArrayReturnsEmpty() throws Exception {
    AttributeList list = underTest.getAttributes(new String[0]);

    assertThat(list, empty());
  }

  @Test
  public void setAttributesSetsAll() throws Exception {
    AttributeList input = new AttributeList();
    input.add(new Attribute(ATTRIBUTE_NAME, "v"));

    AttributeList result = underTest.setAttributes(input);

    verify(attribute).setValue("v");
    assertThat(result.size(), is(1));
    Attribute kept = (Attribute) result.get(0);
    assertThat(kept.getName(), is(ATTRIBUTE_NAME));
    assertThat(kept.getValue(), is((Object) "v"));
  }

  @Test
  public void setAttributesNullReturnsEmpty() throws Exception {
    AttributeList result = underTest.setAttributes(null);

    assertThat(result, empty());
  }

  @Test
  public void setAttributesSkipsFailingAttribute() throws Exception {
    AttributeList input = new AttributeList();
    input.add(new Attribute(ATTRIBUTE_NAME, "v"));
    input.add(new Attribute("missing", "x"));

    AttributeList result = underTest.setAttributes(input);

    assertThat(result.size(), is(1));
    Attribute kept = (Attribute) result.get(0);
    assertThat(kept.getName(), is(ATTRIBUTE_NAME));
  }

  @Test
  public void invokeReturnsOperationResult() throws Exception {
    when(operation.invoke(any())).thenReturn("result");

    Object result = underTest.invoke(OPERATION_NAME, new Object[0], new String[0]);

    assertThat(result, is((Object) "result"));
  }

  @Test
  public void invokeHandlesNullParamsAndTypes() throws Exception {
    when(operation.invoke(any())).thenReturn("ok");

    Object result = underTest.invoke(OPERATION_NAME, null, null);

    assertThat(result, is((Object) "ok"));

    ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
    verify(operation).invoke(captor.capture());
    assertThat(captor.getValue().length, is(0));
  }

  @Test
  public void invokePassesSuppliedParamsToOperation() throws Exception {
    Object[] params = new Object[]{"a", 42};
    when(operation.invoke(any())).thenReturn("ok");

    underTest.invoke(OPERATION_NAME, params, new String[0]);

    ArgumentCaptor<Object[]> captor = ArgumentCaptor.forClass(Object[].class);
    verify(operation).invoke(captor.capture());
    assertThat(captor.getValue(), sameInstance(params));
  }

  @Test
  public void invokeNullNameThrowsNpeNotWrapped() throws Exception {
    assertThrows(NullPointerException.class, () -> underTest.invoke(null, new Object[0], new String[0]));
  }

  @Test
  public void invokeUnknownOperationWrappedInMBeanException() throws Exception {
    MBeanException thrown =
        assertThrows(MBeanException.class, () -> underTest.invoke("missing", new Object[0], new String[0]));
    assertThat(thrown.getCause(), instanceOf(ServiceNotFoundException.class));
    assertThat(thrown.getCause().getMessage(), containsString("Missing operation"));
  }

  @Test
  public void invokePropagatesMBeanException() throws Exception {
    MBeanException cause = new MBeanException(new Exception("inner"));
    when(operation.invoke(any())).thenThrow(cause);

    MBeanException thrown =
        assertThrows(MBeanException.class, () -> underTest.invoke(OPERATION_NAME, new Object[0], new String[0]));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void invokePropagatesReflectionException() throws Exception {
    ReflectionException cause = new ReflectionException(new Exception("r"));
    when(operation.invoke(any())).thenThrow(cause);

    ReflectionException thrown =
        assertThrows(ReflectionException.class, () -> underTest.invoke(OPERATION_NAME, new Object[0], new String[0]));
    assertThat(thrown, sameInstance(cause));
  }

  @Test
  public void invokeWrapsGenericExceptionInMBeanException() throws Exception {
    Exception cause = new Exception("boom");
    when(operation.invoke(any())).thenThrow(cause);

    MBeanException thrown =
        assertThrows(MBeanException.class, () -> underTest.invoke(OPERATION_NAME, new Object[0], new String[0]));
    assertThat(thrown.getCause(), sameInstance((Throwable) cause));
  }
}
