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
package org.sonatype.nexus.repository.content;

import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.content.AttributeChangeSet.AttributeChange;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class AttributeChangeSetTest
    extends TestSupport
{
  @Test
  public void testEmptyConstructor() {
    AttributeChangeSet changeSet = new AttributeChangeSet();
    assertThat(changeSet.getChanges().isEmpty(), is(true));
  }

  @Test
  public void testSingleChangeConstructor() {
    AttributeChangeSet changeSet = new AttributeChangeSet(AttributeOperation.SET, "key1", "value1");
    List<AttributeChange> changes = changeSet.getChanges();

    assertThat(changes.size(), is(1));
    assertThat(changes.get(0).getOperation(), is(AttributeOperation.SET));
    assertThat(changes.get(0).getKey(), is("key1"));
    assertThat(changes.get(0).getValue(), is("value1"));
  }

  @Test
  public void testFluentAttributesAccumulation() {
    AttributeChangeSet changeSet = new AttributeChangeSet()
        .attributes(AttributeOperation.SET, "key1", "value1")
        .attributes(AttributeOperation.REMOVE, "key2", null)
        .attributes(AttributeOperation.SET, "key3", 42);

    List<AttributeChange> changes = changeSet.getChanges();
    assertThat(changes.size(), is(3));

    assertThat(changes.get(0).getOperation(), is(AttributeOperation.SET));
    assertThat(changes.get(0).getKey(), is("key1"));
    assertThat(changes.get(0).getValue(), is("value1"));

    assertThat(changes.get(1).getOperation(), is(AttributeOperation.REMOVE));
    assertThat(changes.get(1).getKey(), is("key2"));
    assertThat(changes.get(1).getValue(), is(nullValue()));

    assertThat(changes.get(2).getOperation(), is(AttributeOperation.SET));
    assertThat(changes.get(2).getKey(), is("key3"));
    assertThat(changes.get(2).getValue(), is(42));
  }

  @Test
  public void testAttributesReturnsSelf() {
    AttributeChangeSet changeSet = new AttributeChangeSet();
    AttributeChangeSet result = changeSet.attributes(AttributeOperation.SET, "k", "v");
    assertThat(result, is(changeSet));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetChangesReturnsUnmodifiableList() {
    AttributeChangeSet changeSet = new AttributeChangeSet(AttributeOperation.SET, "k", "v");
    changeSet.getChanges().add(null);
  }

  @Test
  public void testAttributeChangeToString() {
    AttributeChangeSet changeSet = new AttributeChangeSet(AttributeOperation.SET, "myKey", "myValue");
    AttributeChange change = changeSet.getChanges().get(0);

    String result = change.toString();
    assertThat(result, containsString("AttributeChange{"));
    assertThat(result, containsString("operation=SET"));
    assertThat(result, containsString("key='myKey'"));
    assertThat(result, containsString("value=myValue"));
  }

  @Test
  public void testCombinedConstructorAndFluent() {
    AttributeChangeSet changeSet = new AttributeChangeSet(AttributeOperation.SET, "initial", "val")
        .attributes(AttributeOperation.SET, "added", "val2");

    assertThat(changeSet.getChanges().size(), is(2));
    assertThat(changeSet.getChanges().get(0).getKey(), is("initial"));
    assertThat(changeSet.getChanges().get(1).getKey(), is("added"));
  }
}
