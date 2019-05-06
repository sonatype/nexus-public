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
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.sonatype.nexus.logging.task.TaskLogInfo;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.base.AbstractInstant;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
 * Many of the methods do this: set the key-value if value is non-null, otherwise REMOVE it.
 * Many getters accept a "default value" that is returned when the key is not present in the map.
 *
 * This class is not thread safe.
 *
 * @since 3.0
 */
public class TaskConfiguration
    implements TaskLogInfo
{
  static final String ID_KEY = ".id";

  static final String NAME_KEY = ".name";

  static final String TYPE_ID_KEY = ".typeId";

  static final String TYPE_NAME_KEY = ".typeName";

  static final String ENABLED_KEY = ".enabled";

  static final String VISIBLE_KEY = ".visible";

  static final String ALERT_EMAIL_KEY = ".alertEmail";

  static final String CREATED_KEY = ".created";

  static final String UPDATED_KEY = ".updated";

  static final String MESSAGE_KEY = ".message";

  static final String RECOVERABLE_KEY = ".recoverable";

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
    checkState(!Strings.isNullOrEmpty(getId()), "Incomplete task configuration: id");
    checkState(!Strings.isNullOrEmpty(getTypeId()), "Incomplete task configuration: typeId");
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

  public String getId() {
    return getString(ID_KEY);
  }

  public void setId(final String id) {
    checkNotNull(id);
    setString(ID_KEY, id);
  }

  public String getName() {
    return getString(NAME_KEY);
  }

  public void setName(final String name) {
    checkNotNull(name);
    setString(NAME_KEY, name);
  }

  public String getTypeId() {
    return getString(TYPE_ID_KEY);
  }

  public void setTypeId(final String typeId) {
    checkNotNull(typeId);
    setString(TYPE_ID_KEY, typeId);
  }

  public String getTypeName() {
    return getString(TYPE_NAME_KEY);
  }

  public void setTypeName(final String typeName) {
    checkNotNull(typeName);
    setString(TYPE_NAME_KEY, typeName);
  }

  public boolean isEnabled() {
    return getBoolean(ENABLED_KEY, true);
  }

  public void setEnabled(final boolean enabled) {
    setBoolean(ENABLED_KEY, enabled);
  }

  public boolean isVisible() {
    return getBoolean(VISIBLE_KEY, true);
  }


  public void setVisible(final boolean visible) {
    setBoolean(VISIBLE_KEY, visible);
  }

  @Nullable
  public String getAlertEmail() {
    return getString(ALERT_EMAIL_KEY);
  }

  public void setAlertEmail(final String email) {
    setString(ALERT_EMAIL_KEY, email);
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
    setString(MESSAGE_KEY, message);
  }

  public boolean isRecoverable() {
    return getBoolean(RECOVERABLE_KEY, false);
  }

  public void setRecoverable(final boolean requestRecovery) {
    setBoolean(RECOVERABLE_KEY, requestRecovery);
  }

  //
  // Typed configuration helpers
  //

  public Date getDate(final String key, final Date defaultValue) {
    return Optional.ofNullable(key)
        .map(this::getString)
        .map(DateTime::new)
        .map(AbstractInstant::toDate)
        .orElse(defaultValue);
  }

  public void setDate(final String key, final Date date) {
    setString(key, date, d -> new DateTime(d).toString());
  }

  public boolean getBoolean(final String key, final boolean defaultValue) {
    return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
  }

  public void setBoolean(final String key, final boolean value) {
    setString(key, value, String::valueOf);
  }

  public int getInteger(final String key, final int defaultValue) {
    return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
  }

  public void setInteger(final String key, final int value) {
    setString(key, value, String::valueOf);
  }

  public long getLong(final String key, final long defaultValue) {
    return Long.parseLong(getString(key, String.valueOf(defaultValue)));
  }

  public void setLong(final String key, final long value) {
    setString(key, value, String::valueOf);
  }

  @Nullable
  public String getString(final String key) {
    return getString(key, null);
  }

  public String getString(final String key, final String defaultValue) {
    checkNotNull(key);
    return configuration.getOrDefault(key, defaultValue);
  }

  public void setString(final String key, final String value) {
    setString(key, value, Function.identity());
  }

  <T> void setString(final String key, final T value, Function<T, String> f) {
    checkNotNull(key);
    if (value == null || Strings.isNullOrEmpty(f.apply(value))) {
      configuration.remove(key);
    }
    else {
      configuration.put(key, f.apply(value));
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
