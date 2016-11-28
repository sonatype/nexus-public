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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

// FIXME: Revisit this overly complex configuration container class

/**
 * The task configuration backed by plain map.
 *
 * The configuration is persisted by actual underlying scheduler, so it MUST contain strings only
 * (and string encoded primitives). Still, you can circumvent this primitive configuration by storing some custom
 * string as key here, and using that key fetch some custom configuration for your task via some injected component.
 *
 * As this configuration may get persisted, for simplicity's sake there are some HARD requirements against the
 * contents:
 *
 * For keys: only {@link String}s are accepted, {@code null} keys are NOT accepted.
 *
 * For values: only {@link String}s are accepted, {@code null} keys are NOT accepted. If you must have {@code null} for
 * value, you can use some sentinel value to mark "undefined" state. Still, the best is to not set the mapping at all,
 * as that also might be interpret as "unset".
 *
 * Many of the methods does this: set the key-value is value is non-null, otherwise REMOVE it.
 * Also, many getter method accept "default value", that are returned in case mapping of key is not present in the map.
 *
 * This class is not thread safe.
 *
 * @since 3.0
 */
public final class TaskConfiguration
{
  // TODO: keys which start with "." are considered "private" for some strange reason

  private static final String ID_KEY = ".id";

  private static final String NAME_KEY = ".name";

  private static final String TYPE_ID_KEY = ".typeId";

  private static final String TYPE_NAME_KEY = ".typeName";

  private static final String ENABLED_KEY = ".enabled";

  private static final String VISIBLE_KEY = ".visible";

  private static final String ALERT_EMAIL_KEY = ".alertEmail";

  private static final String CREATED_KEY = ".created";

  private static final String UPDATED_KEY = ".updated";

  private static final String MESSAGE_KEY = ".message";

  private final Map<String, String> configuration;

  public TaskConfiguration() {
    this.configuration = new HashMap<>();
  }

  public TaskConfiguration(final TaskConfiguration configuration) {
    checkNotNull(configuration);
    this.configuration = new HashMap<>(configuration.configuration);
    validate();
  }

  public void validate() {
    // FIXME: These are state-checks not argument checks!
    checkArgument(!Strings.isNullOrEmpty(getId()), "Incomplete task configuration: id");
    checkArgument(!Strings.isNullOrEmpty(getTypeId()), "Incomplete task configuration: typeId");
    for (Entry<?, ?> entry : configuration.entrySet()) {
      checkArgument(entry.getKey() instanceof String && entry.getValue() instanceof String,
          "Invalid entry in map: %s", configuration);
    }
  }

  public String getTaskLogName() {
    final String name = Strings.isNullOrEmpty(getName()) ? getTypeName() : getName();
    return String.format("'%s' [%s]", name, getTypeId());
  }

  /**
   * Copy configuration from given to self.
   */
  public TaskConfiguration apply(final TaskConfiguration from) {
    checkNotNull(from);
    from.validate();
    configuration.putAll(from.configuration);
    return this;
  }

  public Map<String, String> asMap() {
    return ImmutableMap.copyOf(configuration);
  }

  public String toString() {
    return configuration.toString();
  }

  //
  // Core properties
  //

  // FIXME: Some of this screams out for a builer pattern, as we expect things like id to be non-null
  // FIXME: and this correctness is only enforced via validate helper

  public String getId() {
    return getString(ID_KEY);
  }

  public void setId(final String id) {
    checkNotNull(id);
    configuration.put(ID_KEY, id);
  }

  public String getName() {
    return getString(NAME_KEY);
  }

  public void setName(final String name) {
    checkNotNull(name);
    configuration.put(NAME_KEY, name);
  }

  public String getTypeId() {
    return getString(TYPE_ID_KEY);
  }

  public void setTypeId(final String typeId) {
    checkNotNull(typeId);
    configuration.put(TYPE_ID_KEY, typeId);
  }

  public String getTypeName() {
    return getString(TYPE_NAME_KEY);
  }

  public void setTypeName(final String typeName) {
    checkNotNull(typeName);
    configuration.put(TYPE_NAME_KEY, typeName);
  }

  public boolean isEnabled() {
    return getBoolean(ENABLED_KEY, true);
  }

  public void setEnabled(final boolean enabled) {
    configuration.put(ENABLED_KEY, Boolean.toString(enabled));
  }

  public boolean isVisible() {
    return getBoolean(VISIBLE_KEY, true);
  }


  public void setVisible(final boolean visible) {
    configuration.put(VISIBLE_KEY, Boolean.toString(visible));
  }

  @Nullable
  public String getAlertEmail() {
    return getString(ALERT_EMAIL_KEY);
  }

  public void setAlertEmail(final String email) {
    if (Strings.isNullOrEmpty(email)) {
      configuration.remove(ALERT_EMAIL_KEY);
    }
    else {
      configuration.put(ALERT_EMAIL_KEY, email);
    }
  }

  @Nullable
  public Date getCreated() {
    return getDate(CREATED_KEY, null);
  }

  public void setCreated(final Date date) {
    checkNotNull(date);
    setDate(CREATED_KEY, date);
  }

  @Nullable
  public Date getUpdated() {
    return getDate(UPDATED_KEY, null);
  }

  public void setUpdated(final Date date) {
    checkNotNull(date);
    setDate(UPDATED_KEY, date);
  }

  @Nullable
  public String getMessage() {
    return getString(MESSAGE_KEY);
  }

  public void setMessage(final String message) {
    if (Strings.isNullOrEmpty(message)) {
      configuration.remove(MESSAGE_KEY);
    }
    else {
      configuration.put(MESSAGE_KEY, message);
    }
  }

  //
  // Typed configuration helpers
  //

  // FIXME: Consider changing set null to remove sematics, this could lead to confusing results

  public Date getDate(final String key, final Date defaultValue) {
    if (configuration.containsKey(key)) {
      // TODO: will NPE if value is null
      return new DateTime(getString(key)).toDate();
    }
    else {
      return defaultValue;
    }
  }

  public void setDate(final String key, final Date date) {
    checkNotNull(key);
    if (date == null) {
      configuration.remove(key);
    }
    else {
      configuration.put(key, new DateTime(date).toString());
    }
  }

  public boolean getBoolean(final String key, final boolean defaultValue) {
    return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
  }

  public void setBoolean(final String key, final boolean value) {
    checkNotNull(key);
    configuration.put(key, String.valueOf(value));
  }

  public int getInteger(final String key, final int defaultValue) {
    return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
  }

  public void setInteger(final String key, final int value) {
    checkNotNull(key);
    configuration.put(key, String.valueOf(value));
  }

  public long getLong(final String key, final long defaultValue) {
    return Long.parseLong(getString(key, String.valueOf(defaultValue)));
  }

  public void setLong(final String key, final long value) {
    checkNotNull(key);
    configuration.put(key, String.valueOf(value));
  }

  @Nullable
  public String getString(final String key) {
    return getString(key, null);
  }

  public String getString(final String key, final String defaultValue) {
    checkNotNull(key);
    if (configuration.containsKey(key)) {
      return configuration.get(key);
    }
    else {
      return defaultValue;
    }
  }

  public void setString(final String key, final String value) {
    checkNotNull(key);
    if (value == null) {
      configuration.remove(key);
    }
    else {
      configuration.put(key, value);
    }
  }

  /**
   * @since 3.2
   */
  public boolean containsKey(final String key) {
    checkNotNull(key);
    return configuration.containsKey(key);
  }
}
