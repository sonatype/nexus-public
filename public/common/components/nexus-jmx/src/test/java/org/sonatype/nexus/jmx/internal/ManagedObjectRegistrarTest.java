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
package org.sonatype.nexus.jmx.internal;

import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.sonatype.nexus.jmx.reflect.ExampleManagedObject;
import org.sonatype.nexus.jmx.reflect.ManagedObject;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Named;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ManagedObjectRegistrarTest
{
  @Mock
  private MBeanServer server;

  @Mock
  private ApplicationContext applicationContext;

  @Mock
  private ContextRefreshedEvent event;

  private ManagedObjectRegistrar underTest;

  @Before
  public void setUp() {
    underTest = new ManagedObjectRegistrar(server);
  }

  @Test
  public void constructorRejectsNullServer() {
    assertThrows(NullPointerException.class, () -> new ManagedObjectRegistrar(null));
  }

  @Test
  public void setApplicationContextRegistersManagedObject() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new ExampleManagedObject()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());

    underTest.setApplicationContext(applicationContext);

    ArgumentCaptor<ObjectName> captor = ArgumentCaptor.forClass(ObjectName.class);
    verify(server).registerMBean(any(), captor.capture());

    ObjectName name = captor.getValue();
    assertThat(name.getDomain(), is("org.sonatype.nexus.jmx"));
    assertThat(name.getKeyProperty("foo"), is("bar"));
    assertThat(name.getKeyProperty("type"), is("ExampleManagedObject"));
    assertThat(name.getKeyProperty("name"), nullValue());
  }

  @Test
  public void onApplicationEventRegistersManagedObject() throws Exception {
    when(event.getApplicationContext()).thenReturn(applicationContext);
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new ExampleManagedObject()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());

    underTest.onApplicationEvent(event);

    ArgumentCaptor<ObjectName> captor = ArgumentCaptor.forClass(ObjectName.class);
    verify(server).registerMBean(any(), captor.capture());

    ObjectName name = captor.getValue();
    assertThat(name.getDomain(), is("org.sonatype.nexus.jmx"));
    assertThat(name.getKeyProperty("foo"), is("bar"));
    assertThat(name.getKeyProperty("type"), is("ExampleManagedObject"));
    assertThat(name.getKeyProperty("name"), nullValue());
  }

  @Test
  public void alreadyRegisteredObjectIsNotReRegistered() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new ExampleManagedObject()));
    // getMBeanInfo returns normally -> object is considered already registered
    when(server.getMBeanInfo(any())).thenReturn(mock(MBeanInfo.class));

    underTest.setApplicationContext(applicationContext);

    // the registration check must have run, but registration must be skipped
    verify(server).getMBeanInfo(any());
    verify(server, never()).registerMBean(any(), any());
  }

  @Test
  public void registrationExceptionIsSwallowed() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new ExampleManagedObject()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());
    doThrow(new InstanceAlreadyExistsException("boom")).when(server).registerMBean(any(), any());

    // exception thrown by registerMBean must be caught and logged, not propagated
    underTest.setApplicationContext(applicationContext);

    verify(server).registerMBean(any(), any());
  }

  @Test
  public void objectNameUsesExplicitNameAndType() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new ExplicitNameAndType()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());

    underTest.setApplicationContext(applicationContext);

    ArgumentCaptor<ObjectName> captor = ArgumentCaptor.forClass(ObjectName.class);
    verify(server).registerMBean(any(), captor.capture());

    ObjectName name = captor.getValue();
    assertThat(name.getDomain(), is("org.sonatype.nexus.jmx.internal"));
    assertThat(name.getKeyProperty("type"), is("mytype"));
    assertThat(name.getKeyProperty("name"), is("myname"));
  }

  @Test
  public void objectNameUsesTypeClassSimpleName() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new TypeFromClass()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());

    underTest.setApplicationContext(applicationContext);

    ArgumentCaptor<ObjectName> captor = ArgumentCaptor.forClass(ObjectName.class);
    verify(server).registerMBean(any(), captor.capture());

    ObjectName name = captor.getValue();
    assertThat(name.getDomain(), is("org.sonatype.nexus.jmx.internal"));
    assertThat(name.getKeyProperty("type"), is("Some"));
    // no explicit name and no @Named annotation -> 'name' entry omitted
    assertThat(name.getKeyProperty("name"), nullValue());
  }

  @Test
  public void objectNameUsesNamedAnnotationForName() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new NamedObject()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());

    underTest.setApplicationContext(applicationContext);

    ArgumentCaptor<ObjectName> captor = ArgumentCaptor.forClass(ObjectName.class);
    verify(server).registerMBean(any(), captor.capture());

    ObjectName name = captor.getValue();
    assertThat(name.getDomain(), is("org.sonatype.nexus.jmx.internal"));
    // type defaults to the simple-name of the component
    assertThat(name.getKeyProperty("type"), is("NamedObject"));
    // name derived from @Named value
    assertThat(name.getKeyProperty("name"), is("namedval"));
  }

  @Test
  public void registersMultipleManagedObjects() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of(
            "example", new ExampleManagedObject(),
            "explicit", new ExplicitNameAndType()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());

    underTest.setApplicationContext(applicationContext);

    ArgumentCaptor<ObjectName> captor = ArgumentCaptor.forClass(ObjectName.class);
    verify(server, times(2)).registerMBean(any(), captor.capture());

    List<ObjectName> names = captor.getAllValues();
    assertThat(names, hasSize(2));

    // Look up each ObjectName by its 'type' property rather than by list index: registerMBean is invoked in
    // the bean map's iteration order, which is not part of getBeansWithAnnotation's contract.
    ObjectName example = names.stream()
        .filter(n -> "ExampleManagedObject".equals(n.getKeyProperty("type")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("ExampleManagedObject was not registered"));
    assertThat(example.getDomain(), is("org.sonatype.nexus.jmx"));

    ObjectName explicit = names.stream()
        .filter(n -> "mytype".equals(n.getKeyProperty("type")))
        .findFirst()
        .orElseThrow(() -> new AssertionError("ExplicitNameAndType was not registered"));
    assertThat(explicit.getDomain(), is("org.sonatype.nexus.jmx.internal"));
    assertThat(explicit.getKeyProperty("name"), is("myname"));
  }

  @Test
  public void noManagedObjectsRegistersNothing() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of());

    underTest.setApplicationContext(applicationContext);

    verify(server, never()).getMBeanInfo(any());
    verify(server, never()).registerMBean(any(), any());
  }

  @Test
  public void objectNameUsesQualifierAnnotationForName() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new QualifiedObject()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());

    underTest.setApplicationContext(applicationContext);

    ArgumentCaptor<ObjectName> captor = ArgumentCaptor.forClass(ObjectName.class);
    verify(server).registerMBean(any(), captor.capture());

    ObjectName name = captor.getValue();
    assertThat(name.getDomain(), is("org.sonatype.nexus.jmx.internal"));
    // type defaults to the simple-name of the component
    assertThat(name.getKeyProperty("type"), is("QualifiedObject"));
    // name derived from the @Qualifier value (the first name-source consulted)
    assertThat(name.getKeyProperty("name"), is("qualifiedname"));
  }

  @Test
  public void explicitNameTakesPrecedenceOverNamedAnnotation() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new ExplicitNameWithNamed()));
    when(server.getMBeanInfo(any())).thenThrow(new InstanceNotFoundException());

    underTest.setApplicationContext(applicationContext);

    ArgumentCaptor<ObjectName> captor = ArgumentCaptor.forClass(ObjectName.class);
    verify(server).registerMBean(any(), captor.capture());

    ObjectName name = captor.getValue();
    assertThat(name.getDomain(), is("org.sonatype.nexus.jmx.internal"));
    assertThat(name.getKeyProperty("type"), is("ExplicitNameWithNamed"));
    // explicit @ManagedObject#name wins over the @Named value
    assertThat(name.getKeyProperty("name"), is("explicitname"));
  }

  @Test
  public void registrationCheckExceptionIsSwallowed() throws Exception {
    when(applicationContext.getBeansWithAnnotation(ManagedObject.class))
        .thenReturn(ImmutableMap.<String, Object>of("bean", new ExampleManagedObject()));
    // a non-InstanceNotFoundException from getMBeanInfo propagates out of the registration check
    when(server.getMBeanInfo(any())).thenThrow(new ReflectionException(new Exception("boom")));

    // exception must be caught and logged, not propagated; registration must be skipped
    underTest.setApplicationContext(applicationContext);

    verify(server).getMBeanInfo(any());
    verify(server, never()).registerMBean(any(), any());
  }

  @ManagedObject(name = "myname", type = "mytype")
  public static class ExplicitNameAndType
  {
    // empty fixture
  }

  @ManagedObject(typeClass = Some.class)
  public static class TypeFromClass
  {
    // empty fixture
  }

  @Named("namedval")
  @ManagedObject
  public static class NamedObject
  {
    // empty fixture
  }

  public static class Some
  {
    // referenced via ManagedObject#typeClass
  }

  @Qualifier("qualifiedname")
  @ManagedObject
  public static class QualifiedObject
  {
    // name derived from @Qualifier value
  }

  @Named("namedval")
  @ManagedObject(name = "explicitname")
  public static class ExplicitNameWithNamed
  {
    // explicit name wins over @Named
  }
}
