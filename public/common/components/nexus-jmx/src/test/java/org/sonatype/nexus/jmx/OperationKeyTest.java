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

import java.util.List;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class OperationKeyTest
{
  @Test
  public void constructFromNameAndTypes() {
    OperationKey key = new OperationKey("foo", new String[]{"java.lang.String", "int"});

    assertThat(key.getName(), equalTo("foo"));
    assertThat(key.getTypes(), equalTo(List.of("java.lang.String", "int")));
  }

  @Test
  public void constructFromNameAndEmptyTypes() {
    OperationKey key = new OperationKey("foo", new String[0]);

    assertThat(key.getName(), equalTo("foo"));
    assertThat(key.getTypes(), equalTo(List.of()));
  }

  @Test
  public void constructFromNameAndTypesNullNameThrows() {
    assertThrows(NullPointerException.class, () -> new OperationKey(null, new String[0]));
  }

  @Test
  public void constructFromNameAndTypesNullTypesThrows() {
    assertThrows(NullPointerException.class, () -> new OperationKey("foo", null));
  }

  @Test
  public void getTypesIsImmutable() {
    OperationKey key = new OperationKey("foo", new String[]{"java.lang.String"});

    assertThrows(UnsupportedOperationException.class, () -> key.getTypes().add("int"));
  }

  @Test
  public void constructFromOperationInfo() {
    MBeanParameterInfo[] signature = new MBeanParameterInfo[]{
        new MBeanParameterInfo("p1", "java.lang.String", "first parameter"),
        new MBeanParameterInfo("p2", "int", "second parameter")
    };
    MBeanOperationInfo info =
        new MBeanOperationInfo("foo", "the foo operation", signature, "void", MBeanOperationInfo.ACTION);

    OperationKey key = new OperationKey(info);

    assertThat(key.getName(), equalTo("foo"));
    assertThat(key.getTypes(), equalTo(List.of("java.lang.String", "int")));
  }

  @Test
  public void constructFromOperationInfoWithEmptySignature() {
    MBeanOperationInfo info =
        new MBeanOperationInfo("foo", "the foo operation", new MBeanParameterInfo[0], "void", MBeanOperationInfo.INFO);

    OperationKey key = new OperationKey(info);

    assertThat(key.getName(), equalTo("foo"));
    assertThat(key.getTypes(), equalTo(List.of()));
  }

  @Test
  public void constructFromNullOperationInfoThrows() {
    assertThrows(NullPointerException.class, () -> new OperationKey((MBeanOperationInfo) null));
  }

  @Test
  public void equalsReflexive() {
    OperationKey key = new OperationKey("foo", new String[]{"java.lang.String"});

    assertThat(key.equals(key), equalTo(true));
  }

  @Test
  public void equalsAndHashCodeForEqualKeys() {
    OperationKey key1 = new OperationKey("foo", new String[]{"java.lang.String", "int"});
    OperationKey key2 = new OperationKey("foo", new String[]{"java.lang.String", "int"});

    assertThat(key1.equals(key2), equalTo(true));
    assertThat(key2.equals(key1), equalTo(true));
    assertThat(key1.hashCode(), equalTo(key2.hashCode()));
  }

  @Test
  public void notEqualWhenNameDiffers() {
    OperationKey key1 = new OperationKey("foo", new String[]{"java.lang.String"});
    OperationKey key2 = new OperationKey("bar", new String[]{"java.lang.String"});

    assertThat(key1.equals(key2), equalTo(false));
  }

  @Test
  public void notEqualWhenTypesDiffer() {
    OperationKey key1 = new OperationKey("foo", new String[]{"java.lang.String"});
    OperationKey key2 = new OperationKey("foo", new String[]{"int"});

    assertThat(key1.equals(key2), equalTo(false));
  }

  @Test
  public void notEqualToNull() {
    OperationKey key = new OperationKey("foo", new String[]{"java.lang.String"});

    assertThat(key.equals(null), equalTo(false));
  }

  @Test
  public void notEqualToDifferentClass() {
    OperationKey key = new OperationKey("foo", new String[]{"java.lang.String"});

    assertThat(key.equals("foo"), equalTo(false));
  }

  @Test
  public void hashCodeDiffersForDifferentKeys() {
    OperationKey key1 = new OperationKey("foo", new String[]{"java.lang.String"});
    OperationKey key2 = new OperationKey("bar", new String[]{"int"});

    assertThat(key1.hashCode(), not(equalTo(key2.hashCode())));
  }

  @Test
  public void toStringWithZeroTypes() {
    OperationKey key = new OperationKey("name", new String[0]);

    assertThat(key.toString(), equalTo("name()"));
  }

  @Test
  public void toStringWithOneType() {
    OperationKey key = new OperationKey("name", new String[]{"java.lang.String"});

    assertThat(key.toString(), equalTo("name(java.lang.String)"));
  }

  @Test
  public void toStringWithTwoTypes() {
    OperationKey key = new OperationKey("name", new String[]{"java.lang.String", "int"});

    assertThat(key.toString(), equalTo("name(java.lang.String,int)"));
  }
}
