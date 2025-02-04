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
package org.sonatype.nexus.internal.kv;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.kv.NexusKeyValue;
import org.sonatype.nexus.kv.ValueType;
import org.sonatype.nexus.testdb.DataSessionRule;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

public class NexusKeyValueDAOTest
    extends TestSupport
{
  @Rule
  public DataSessionRule sessionRule = new DataSessionRule()
      .access(NexusKeyValueDAO.class);

  @Test
  public void testStringValue() {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey("boolean-test");
    kv.setType(ValueType.CHARACTER);
    kv.setValue("test-value");

    assertKeyValueOperations(() -> kv, (existing) -> assertThat(existing.getAsString(), is("test-value")));
  }

  @Test
  public void testIntValue() {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey("int-test");
    kv.setType(ValueType.NUMBER);
    kv.setValue(10);

    assertKeyValueOperations(() -> kv, (existing) -> assertThat(existing.getAsInt(), is(10)));
  }

  @Test
  public void testBooleanValue() {
    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey("boolean-test");
    kv.setType(ValueType.BOOLEAN);
    kv.setValue(false);

    assertKeyValueOperations(() -> kv, (existing) -> assertThat(existing.getAsBoolean(), is(false)));
  }

  @Test
  public void testObjectValue() {
    TestObject objectValue = new TestObject("test", 23, true);

    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey("object-test");
    kv.setType(ValueType.OBJECT);
    kv.setValue(objectValue);

    assertKeyValueOperations(() -> kv, (existing) -> {
      ObjectMapper mapper = new ObjectMapper();
      assertThat(existing.getAsObject(mapper, TestObject.class), equalTo(objectValue));
    });
  }

  @Test
  public void testObjectList() {
    TestObject object1 = new TestObject("object-1", 32, false);
    TestObject object2 = new TestObject("object-2", 65, true);

    NexusKeyValue kv = new NexusKeyValue();
    kv.setKey("object-list-test");
    kv.setType(ValueType.OBJECT);
    kv.setValue(Arrays.asList(object1, object2));

    assertKeyValueOperations(() -> kv, (existing) -> {
      ObjectMapper mapper = new ObjectMapper();
      List<TestObject> objectList = existing.getAsObjectList(mapper, TestObject.class);
      assertThat(objectList, hasSize(2));
      assertThat(objectList.get(0), equalTo(object1));
      assertThat(objectList.get(1), equalTo(object2));
    });
  }

  private void assertKeyValueOperations(
      final Supplier<NexusKeyValue> kvSupplier,
      final Consumer<NexusKeyValue> assertionConsumer)
  {
    NexusKeyValue initial = kvSupplier.get();
    write((dao) -> dao.set(initial));

    Optional<NexusKeyValue> existing = read((dao) -> dao.get(initial.key()));

    // verify kv can be read correctly
    assertTrue(existing.isPresent());
    assertKeyValue(initial, existing.get());
    // pass to consumer with specific kv assertions
    assertionConsumer.accept(existing.get());

    // verify kv can be deleted correctly
    boolean deleted = read((dao) -> dao.remove(initial.key()));
    assertTrue(deleted);
  }

  private void assertKeyValue(final NexusKeyValue a, final NexusKeyValue b) {
    assertThat(a.key(), is(b.key()));
    assertThat(a.type(), is(b.type()));
  }

  private <R> R read(final Function<NexusKeyValueDAO, R> function) {
    try (DataSession<?> dataSession = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      NexusKeyValueDAO dao = dataSession.access(NexusKeyValueDAO.class);
      R result = function.apply(dao);

      dataSession.getTransaction().commit();
      return result;
    }
  }

  private void write(final Consumer<NexusKeyValueDAO> consumer) {
    try (DataSession<?> dataSession = sessionRule.openSession(DEFAULT_DATASTORE_NAME)) {
      NexusKeyValueDAO dao = dataSession.access(NexusKeyValueDAO.class);
      consumer.accept(dao);
      dataSession.getTransaction().commit();
    }
  }

  static class TestObject
  {
    private String name;

    private int age;

    private boolean exclusive;

    public TestObject(final String name, final int age, final boolean exclusive) {
      this.name = name;
      this.age = age;
      this.exclusive = exclusive;
    }

    public TestObject() {

    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public int getAge() {
      return age;
    }

    public void setAge(final int age) {
      this.age = age;
    }

    public boolean isExclusive() {
      return exclusive;
    }

    public void setExclusive(final boolean exclusive) {
      this.exclusive = exclusive;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      TestObject that = (TestObject) o;
      return age == that.age && exclusive == that.exclusive && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, age, exclusive);
    }
  }
}
