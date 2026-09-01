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
package org.sonatype.nexus.script;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class ScriptResultXOTest
{
  @Test
  public void defaultConstructorLeavesFieldsNull() {
    ScriptResultXO underTest = new ScriptResultXO();

    assertThat(underTest.getName(), is(nullValue()));
    assertThat(underTest.getResult(), is(nullValue()));
  }

  @Test
  public void allArgsConstructorSetsFields() {
    ScriptResultXO underTest = new ScriptResultXO("myScript", "myResult");

    assertThat(underTest.getName(), is("myScript"));
    assertThat(underTest.getResult(), is("myResult"));
  }

  @Test
  public void allArgsConstructorAcceptsNulls() {
    ScriptResultXO underTest = new ScriptResultXO(null, null);

    assertThat(underTest.getName(), is(nullValue()));
    assertThat(underTest.getResult(), is(nullValue()));
  }

  @Test
  public void setNameUpdatesName() {
    ScriptResultXO underTest = new ScriptResultXO();

    underTest.setName("updatedName");

    assertThat(underTest.getName(), is("updatedName"));
  }

  @Test
  public void setNameAcceptsNull() {
    ScriptResultXO underTest = new ScriptResultXO("initial", "result");

    underTest.setName(null);

    assertThat(underTest.getName(), is(nullValue()));
  }

  @Test
  public void setResultUpdatesResult() {
    ScriptResultXO underTest = new ScriptResultXO();

    underTest.setResult("updatedResult");

    assertThat(underTest.getResult(), is("updatedResult"));
  }

  @Test
  public void setResultAcceptsNull() {
    ScriptResultXO underTest = new ScriptResultXO("name", "initial");

    underTest.setResult(null);

    assertThat(underTest.getResult(), is(nullValue()));
  }

  @Test
  public void equalsIsReflexive() {
    ScriptResultXO underTest = new ScriptResultXO("name", "result");

    assertThat(underTest.equals(underTest), is(true));
  }

  @Test
  public void equalsReturnsFalseForNull() {
    ScriptResultXO underTest = new ScriptResultXO("name", "result");

    assertThat(underTest.equals(null), is(false));
  }

  @Test
  public void equalsReturnsFalseForDifferentType() {
    ScriptResultXO underTest = new ScriptResultXO("name", "result");

    assertThat(underTest.equals("not a ScriptResultXO"), is(false));
  }

  @Test
  public void equalsReturnsTrueForSameValues() {
    ScriptResultXO first = new ScriptResultXO("name", "result");
    ScriptResultXO second = new ScriptResultXO("name", "result");

    assertThat(first.equals(second), is(true));
  }

  @Test
  public void equalsReturnsTrueWhenBothFieldsNull() {
    ScriptResultXO first = new ScriptResultXO();
    ScriptResultXO second = new ScriptResultXO();

    assertThat(first.equals(second), is(true));
  }

  @Test
  public void equalsReturnsFalseForDifferentName() {
    ScriptResultXO first = new ScriptResultXO("name", "result");
    ScriptResultXO second = new ScriptResultXO("other", "result");

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void equalsReturnsFalseForDifferentResult() {
    ScriptResultXO first = new ScriptResultXO("name", "result");
    ScriptResultXO second = new ScriptResultXO("name", "other");

    assertThat(first.equals(second), is(false));
  }

  @Test
  public void hashCodeIsEqualForEqualObjects() {
    ScriptResultXO first = new ScriptResultXO("name", "result");
    ScriptResultXO second = new ScriptResultXO("name", "result");

    assertThat(first.hashCode(), is(second.hashCode()));
  }

  @Test
  public void hashCodeMatchesObjectsHash() {
    ScriptResultXO underTest = new ScriptResultXO("name", "result");

    assertThat(underTest.hashCode(), is(java.util.Objects.hash("name", "result")));
  }

  @Test
  public void hashCodeDiffersForDifferentValues() {
    ScriptResultXO first = new ScriptResultXO("name", "result");
    ScriptResultXO second = new ScriptResultXO("other", "value");

    assertThat(first.hashCode(), is(not(second.hashCode())));
  }

  @Test
  public void toStringRendersFieldValues() {
    ScriptResultXO underTest = new ScriptResultXO("myScript", "myResult");

    assertThat(underTest.toString(), is("ScriptResultXO{name='myScript', result='myResult'}"));
  }

  @Test
  public void toStringRendersNullFields() {
    ScriptResultXO underTest = new ScriptResultXO();

    assertThat(underTest.toString(), is("ScriptResultXO{name='null', result='null'}"));
  }

  @Test
  public void equalsComparesValuesNotReferences() {
    // distinct String instances with equal values to prove value-based (not reference) equality
    ScriptResultXO first = new ScriptResultXO(new String("name"), new String("result"));
    ScriptResultXO second = new ScriptResultXO(new String("name"), new String("result"));

    assertThat(first.equals(second), is(true));
  }

  @Test
  public void equalsIsSymmetric() {
    ScriptResultXO first = new ScriptResultXO("name", "result");
    ScriptResultXO second = new ScriptResultXO("name", "result");

    assertThat(first.equals(second), is(true));
    assertThat(second.equals(first), is(true));
  }

  @Test
  public void equalsReturnsFalseWhenOnlyNameIsNull() {
    ScriptResultXO withNullName = new ScriptResultXO(null, "result");
    ScriptResultXO withName = new ScriptResultXO("name", "result");

    assertThat(withNullName.equals(withName), is(false));
    assertThat(withName.equals(withNullName), is(false));
  }

  @Test
  public void equalsReturnsFalseWhenOnlyResultIsNull() {
    ScriptResultXO withNullResult = new ScriptResultXO("name", null);
    ScriptResultXO withResult = new ScriptResultXO("name", "result");

    assertThat(withNullResult.equals(withResult), is(false));
    assertThat(withResult.equals(withNullResult), is(false));
  }

  @Test
  public void hashCodeMatchesObjectsHashForNullFields() {
    ScriptResultXO underTest = new ScriptResultXO();

    assertThat(underTest.hashCode(), is(java.util.Objects.hash(null, null)));
  }
}
