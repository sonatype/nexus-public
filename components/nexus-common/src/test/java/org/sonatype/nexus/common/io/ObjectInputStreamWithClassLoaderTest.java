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
package org.sonatype.nexus.common.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.io.ObjectInputStreamWithClassLoader.LoadingFunction;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ObjectInputStreamWithClassLoaderTest
    extends TestSupport
{
  private static final String OBJECT_TO_SERIALIZE = "test";

  private final TestClassLoader classLoader = new TestClassLoader();

  @Mock
  private LoadingFunction loadingFunction;

  @Mock
  private ObjectStreamClass classDescription;

  @Test(expected = NullPointerException.class)
  public void failFastWhenClassLoaderNull() throws Exception {
    try (ObjectInputStreamWithClassLoader in = new ObjectInputStreamWithClassLoader(
        serialize(OBJECT_TO_SERIALIZE), (ClassLoader) null)) {
      // exception expected
    }
  }

  @Test
  public void useCustomClassLoaderToResolveClass() throws Exception {
    String name = "testClassName";
    when(classDescription.getName()).thenReturn(name);
    try (ObjectInputStreamWithClassLoader underTest = new ObjectInputStreamWithClassLoader(
        serialize(OBJECT_TO_SERIALIZE), classLoader)) {
      underTest.resolveClass(classDescription);
    }
    catch (Exception e) {
      // no-op
    }
    assertThat(classLoader.isLoaded(name), is(true));
  }

  @Test
  public void deserializeUsingCustomClassLoader() throws Exception {
    String contents = "contents";
    TestFixture deserialized;
    try (ObjectInputStream objects = new ObjectInputStreamWithClassLoader(
        serialize(new TestFixture(contents)), classLoader)) {
      deserialized = (TestFixture) objects.readObject();
    }
    assertThat(deserialized.contents, is(equalTo(contents)));
  }

  @Test(expected = NullPointerException.class)
  public void failFastWhenLoadingFunctionNull() throws Exception {
    try (ObjectInputStreamWithClassLoader in = new ObjectInputStreamWithClassLoader(
        serialize(OBJECT_TO_SERIALIZE), (LoadingFunction) null)) {
      // exception expected
    }
  }

  @Test
  public void useCustomLoadingFunctionToResolveClass() throws Exception {
    String name = "testClassName";
    when(classDescription.getName()).thenReturn(name);
    doReturn(getClass()).when(loadingFunction).loadClass(anyString());
    try (ObjectInputStreamWithClassLoader underTest = new ObjectInputStreamWithClassLoader(
        serialize(OBJECT_TO_SERIALIZE), loadingFunction)) {
      underTest.resolveClass(classDescription);
    }
    catch (Exception e) {
      // no-op
    }
    verify(loadingFunction).loadClass(name);
  }

  @Test
  public void deserializeUsingCustomLoadingFunction() throws Exception {
    String contents = "contents";
    TestFixture deserialized;
    doReturn(TestFixture.class).when(loadingFunction).loadClass(anyString());
    try (ObjectInputStream objects = new ObjectInputStreamWithClassLoader(
        serialize(new TestFixture(contents)), loadingFunction)) {
      deserialized = (TestFixture) objects.readObject();
    }
    assertThat(deserialized.contents, is(equalTo(contents)));
  }

  private InputStream serialize(final Object o) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
      out.writeObject(o);
    }
    return new ByteArrayInputStream(bos.toByteArray());
  }

  @SuppressWarnings("serial")
  private static class TestFixture
      implements Serializable
  {
    String contents;

    public TestFixture(final String contents) {
      this.contents = contents;
    }
  }

  private static class TestClassLoader
      extends ClassLoader
  {

    private final Map<String, Class<?>> classes = new HashMap<>();

    @Override
    public Class<?> loadClass(final String name) throws ClassNotFoundException {
      if (name.contains("TestFixture")) {
        classes.put(name, TestFixture.class);
        return TestFixture.class;
      }
      else {
        classes.put(name, ObjectInputStreamWithClassLoaderTest.class);
        return ObjectInputStreamWithClassLoaderTest.class;
      }
    }

    public boolean isLoaded(final String name) {
      return classes.containsKey(name);
    }
  }
}
