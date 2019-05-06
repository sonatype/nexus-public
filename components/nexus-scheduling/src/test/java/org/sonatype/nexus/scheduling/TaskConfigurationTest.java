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
package org.sonatype.nexus.scheduling;

import java.util.Date;
import java.util.function.Function;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.sonatype.nexus.scheduling.TaskConfiguration.*;

public class TaskConfigurationTest {
  private static final String FOOBAR = "foobar";

  private TaskConfiguration underTest = new TaskConfiguration();

  @Test(expected = IllegalStateException.class)
  public void validateMissingId() {
    underTest.setTypeId(FOOBAR);
    underTest.validate();
  }

  @Test(expected = IllegalStateException.class)
  public void validateMissingTypeId() {
    underTest.setId(FOOBAR);
    underTest.validate();
  }

  @Test
  public void setGetId() {
    underTest.setId(FOOBAR);

    assertThat(underTest.getId(), equalTo(FOOBAR));

    underTest.setString(ID_KEY, null);

    assertThat(underTest.getId(), nullValue());

    underTest.setId("");

    assertThat(underTest.getId(), nullValue());
  }

  @Test(expected = NullPointerException.class)
  public void setIdNull() {
    underTest.setId(null);
  }

  @Test
  public void setGetName() {
    underTest.setName(FOOBAR);

    assertThat(underTest.getName(), equalTo(FOOBAR));

    underTest.setString(NAME_KEY, null);

    assertThat(underTest.getName(), nullValue());

    underTest.setName("");

    assertThat(underTest.getName(), nullValue());
  }

  @Test(expected = NullPointerException.class)
  public void setNameNull() {
    underTest.setName(null);
  }

  @Test
  public void setGetTypeId() {
    underTest.setTypeId(FOOBAR);

    assertThat(underTest.getTypeId(), equalTo(FOOBAR));

    underTest.setString(TYPE_ID_KEY, null);

    assertThat(underTest.getTypeId(), nullValue());

    underTest.setTypeId("");

    assertThat(underTest.getTypeId(), nullValue());
  }

  @Test(expected = NullPointerException.class)
  public void setTypeIdNull() {
    underTest.setTypeId(null);
  }

  @Test
  public void setGetTypeName() {
    underTest.setTypeName(FOOBAR);

    assertThat(underTest.getTypeName(), equalTo(FOOBAR));

    underTest.setString(TYPE_NAME_KEY, null);

    assertThat(underTest.getTypeName(), nullValue());

    underTest.setTypeName("");

    assertThat(underTest.getTypeName(), nullValue());
  }

  @Test(expected = NullPointerException.class)
  public void setTypeNameNull() {
    underTest.setTypeName(null);
  }

  @Test
  public void setGetEnabled() {
    underTest.setEnabled(false);

    assertThat(underTest.isEnabled(), equalTo(false));
    assertThat(underTest.getString(ENABLED_KEY), equalTo("false"));

    underTest.setString(ENABLED_KEY, null);

    assertThat(underTest.isEnabled(), equalTo(true));
    assertThat(underTest.getString(ENABLED_KEY), nullValue());

    underTest.setEnabled(true);

    assertThat(underTest.isEnabled(), equalTo(true));
    assertThat(underTest.getString(ENABLED_KEY), equalTo("true"));
  }

  @Test
  public void setGetVisible() {
    underTest.setVisible(false);

    assertThat(underTest.isVisible(), equalTo(false));
    assertThat(underTest.getString(VISIBLE_KEY), equalTo("false"));

    underTest.setString(VISIBLE_KEY, null);

    assertThat(underTest.isVisible(), equalTo(true));
    assertThat(underTest.getString(VISIBLE_KEY), nullValue());

    underTest.setVisible(true);

    assertThat(underTest.isVisible(), equalTo(true));
    assertThat(underTest.getString(VISIBLE_KEY), equalTo("true"));
  }

  @Test
  public void setGetAlertEmail() {
    underTest.setAlertEmail(FOOBAR);

    assertThat(underTest.getAlertEmail(), equalTo(FOOBAR));
    assertThat(underTest.getString(ALERT_EMAIL_KEY), equalTo(FOOBAR));

    underTest.setAlertEmail(null);

    assertThat(underTest.getAlertEmail(), nullValue());
    assertThat(underTest.getString(ALERT_EMAIL_KEY), nullValue());

    underTest.setAlertEmail("");

    assertThat(underTest.getAlertEmail(), nullValue());
    assertThat(underTest.getString(ALERT_EMAIL_KEY), nullValue());
  }

  @Test
  public void setGetCreated() {
    Date foobar = DateTime.parse("2000-01-01").toDate();
    underTest.setCreated(foobar);

    assertThat(underTest.getCreated(), equalTo(foobar));

    underTest.setString(CREATED_KEY, null);

    assertThat(underTest.getCreated(), nullValue());
    assertThat(underTest.getString(CREATED_KEY), nullValue());
  }

  @Test(expected = NullPointerException.class)
  public void setCreatedNull() {
    underTest.setCreated(null);
  }

  @Test
  public void setGetUpdated() {
    Date foobar = DateTime.parse("2000-01-01").toDate();
    underTest.setUpdated(foobar);

    assertThat(underTest.getUpdated(), equalTo(foobar));

    underTest.setString(UPDATED_KEY, null);

    assertThat(underTest.getUpdated(), nullValue());
    assertThat(underTest.getString(CREATED_KEY), nullValue());
  }

  @Test(expected = NullPointerException.class)
  public void setUpdatedNull() {
    underTest.setUpdated(null);
  }

  @Test
  public void setGetMessage() {
    underTest.setMessage(FOOBAR);

    assertThat(underTest.getMessage(), equalTo(FOOBAR));
    assertThat(underTest.getString(MESSAGE_KEY), equalTo(FOOBAR));

    underTest.setMessage(null);

    assertThat(underTest.getMessage(), nullValue());
    assertThat(underTest.getString(MESSAGE_KEY), nullValue());

    underTest.setMessage("");

    assertThat(underTest.getMessage(), nullValue());
    assertThat(underTest.getString(MESSAGE_KEY), nullValue());
  }

  @Test
  public void setGetRecoverable() {
    underTest.setRecoverable(false);

    assertThat(underTest.isRecoverable(), equalTo(false));
    assertThat(underTest.getString(RECOVERABLE_KEY), equalTo("false"));

    underTest.setString(RECOVERABLE_KEY, null);

    assertThat(underTest.isRecoverable(), equalTo(false));
    assertThat(underTest.getString(RECOVERABLE_KEY), nullValue());

    underTest.setRecoverable(true);

    assertThat(underTest.isRecoverable(), equalTo(true));
    assertThat(underTest.getString(RECOVERABLE_KEY), equalTo("true"));
  }

  @Test
  public void setGetDate() {
    Date now = DateTime.now().toDate();
    underTest.setDate(FOOBAR, now);

    assertThat(underTest.getDate(FOOBAR, now), equalTo(now));
  }

  @Test
  public void nullDateTest() {
    Date now = DateTime.now().toDate();

    assertThat(underTest.getDate(FOOBAR, now), equalTo(now));
    assertThat(underTest.getString(FOOBAR), nullValue());
  }

  @Test
  public void setGetBoolean() {
    underTest.setBoolean(FOOBAR, true);

    assertThat(underTest.getBoolean(FOOBAR, false), equalTo(true));
    assertThat(underTest.getString(FOOBAR), equalTo("true"));

    underTest.setString(FOOBAR, null);

    assertThat(underTest.getBoolean(FOOBAR, false), equalTo(false));
    assertThat(underTest.getString(FOOBAR), nullValue());
  }

  @Test
  public void setGetInteger() {
    underTest.setInteger(FOOBAR, 1234);

    assertThat(underTest.getInteger(FOOBAR, 42), equalTo(1234));
    assertThat(underTest.getString(FOOBAR), equalTo("1234"));

    underTest.setString(FOOBAR, null);

    assertThat(underTest.getInteger(FOOBAR, 42), equalTo(42));
    assertThat(underTest.getString(FOOBAR), nullValue());
  }

  @Test
  public void setGetLong() {
    underTest.setLong(FOOBAR, 1234L);

    assertThat(underTest.getLong(FOOBAR, 42L), equalTo(1234L));
    assertThat(underTest.getString(FOOBAR), equalTo("1234"));

    underTest.setString(FOOBAR, null);

    assertThat(underTest.getLong(FOOBAR, 42L), equalTo(42L));
    assertThat(underTest.getString(FOOBAR), nullValue());
  }

  @Test
  public void setStringWithToString() {
    // Long
    underTest.setString(FOOBAR, null, (Function<Long, String>) String::valueOf);

    assertThat(underTest.getString(FOOBAR), nullValue());

    underTest.setString(FOOBAR, 1234L, String::valueOf);

    assertThat(underTest.getString(FOOBAR), equalTo("1234"));

    // Integer
    underTest.setString(FOOBAR, null, (Function<Integer, String>) String::valueOf);

    assertThat(underTest.getString(FOOBAR), nullValue());

    underTest.setString(FOOBAR, 4321, String::valueOf);

    assertThat(underTest.getString(FOOBAR), equalTo("4321"));

    // Boolean
    underTest.setString(FOOBAR, null, (Function<Boolean, String>) String::valueOf);

    assertThat(underTest.getString(FOOBAR), nullValue());

    underTest.setString(FOOBAR, true, String::valueOf);

    assertThat(underTest.getString(FOOBAR), equalTo("true"));

    // Date
    Function<Date, String> date2String = d -> new DateTime(d).toString();
    Date date = DateTime.parse("2000-01-01").toDate();

    underTest.setString(FOOBAR, null, date2String);

    assertThat(underTest.getString(FOOBAR), nullValue());

    underTest.setString(FOOBAR, date, date2String);

    assertThat(underTest.getString(FOOBAR), equalTo(date2String.apply(date)));
  }
}
