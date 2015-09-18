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
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The task configuration backed by plain map. The configuration is persisted by actual underlying scheduler, so it
 * MUST contain strings only (and string encoded primitives). Still, you can circumvent this primitive configuration by
 * storing some custom string as key here, and using that key fetch some custom configuration for your task via some
 * injected component.
 *
 * As this configuration may get persisted, for simplicity's sake there are some HARD requirements against the
 * contents. Those are:
 * For keys: only {@link String}s are accepted, {@code null} keys are NOT accepted.
 * For values: only {@link String}s are accepted, {@code null} keys are NOT accepted. If you must have {@code null} for
 * value, you can use some sentinel value to mark "undefined" state. Still, the best is to not set the mapping at all,
 * as that also might be interpret as "unset". Many of the methods does this: set the key-value is value is non-null,
 * otherwise REMOVE it. Also, many getter method accept "default value", that are returned in case mapping of key
 * is not present in the map.
 *
 * This class is not thread safe.
 *
 * @since 3.0
 */
public final class TaskConfiguration
{
  /**
   * Checks if a property is a private property. Private properties are those properties that start with
   * {@link #PRIVATE_PROP_PREFIX}.
   *
   * @param key property key
   * @return true if the key defines a private property
   */
  public static boolean isPrivateProperty(final String key) {
    return key != null && key.startsWith(PRIVATE_PROP_PREFIX);
  }

  /**
   * Prefix for private properties keys.
   */
  static final String PRIVATE_PROP_PREFIX = ".";

  /**
   * Key of id property (private).
   */
  static final String ID_KEY = PRIVATE_PROP_PREFIX + "id";

  /**
   * Key of name property (private).
   */
  static final String NAME_KEY = PRIVATE_PROP_PREFIX + "name";

  /**
   * Key of type ID property (private).
   */
  static final String TYPE_ID_KEY = PRIVATE_PROP_PREFIX + "typeId";

  /**
   * Key of type name property (private).
   */
  static final String TYPE_NAME_KEY = PRIVATE_PROP_PREFIX + "typeName";

  /**
   * Key of enabled property (private).
   */
  static final String ENABLED_KEY = PRIVATE_PROP_PREFIX + "enabled";

  /**
   * Key of visible property (private).
   */
  static final String VISIBLE_KEY = PRIVATE_PROP_PREFIX + "visible";

  /**
   * Key of alert email property (private).
   */
  static final String ALERT_EMAIL_KEY = PRIVATE_PROP_PREFIX + "alertEmail";

  /**
   * Key of created property (private).
   */
  static final String CREATED_KEY = PRIVATE_PROP_PREFIX + "created";

  /**
   * Key of updated property (private).
   */
  static final String UPDATED_KEY = PRIVATE_PROP_PREFIX + "updated";

  /**
   * Key of message property (private).
   */
  static final String MESSAGE_KEY = PRIVATE_PROP_PREFIX + "message";

  /**
   * Key of repository.
   */
  public static final String REPOSITORY_ID_KEY = "repositoryId";

  /**
   * Key of path.
   */
  public static final String PATH_KEY = "path";

  private final Map<String, String> configuration;

  /**
   * Constructor creating empty configuration.
   */
  public TaskConfiguration()
  {
    this.configuration = Maps.newHashMap();
  }

  /**
   * Copy constructor that creates copy of the passed in configuration. Does not accept {@code null} values
   * and validates the configuration passed in.
   */
  public TaskConfiguration(final TaskConfiguration configuration) throws IllegalArgumentException
  {
    checkNotNull(configuration);
    this.configuration = Maps.newHashMap(configuration.configuration);
    validate();
  }

  /**
   * Performs a "self" validation of the configuration for minimal completeness and correctness.
   */
  public void validate() throws IllegalArgumentException {
    // Minimum requirements
    checkArgument(!Strings.isNullOrEmpty(getId()), "Incomplete task configuration: id");
    checkArgument(!Strings.isNullOrEmpty(getTypeId()), "Incomplete task configuration: typeId");
    for (Entry<?, ?> entry : configuration.entrySet()) {
      checkArgument(
          entry.getKey() instanceof String
              && entry.getValue() instanceof String,
          "Invalid entry in map: %s", configuration);
    }
  }

  /**
   * Returns assembled string to be used for logging and other (non-UI) purposes. Never returns {@code null} or
   * empty strung, result should be used as-is, as it contents might change in future.
   */
  public String getTaskLogName() {
    final String name = Strings.isNullOrEmpty(getName()) ? getTypeName() : getName();
    return String.format("'%s' [%s]", name, getTypeId());
  }

  /**
   * Returns a unique ID of the task instance.
   */
  public String getId() {
    return getString(ID_KEY);
  }

  /**
   * Sets the ID.
   */
  public void setId(final String id) {
    checkNotNull(id);
    configuration.put(ID_KEY, id);
  }

  /**
   * Returns a name of the task instance.
   */
  public String getName() {
    return getString(NAME_KEY);
  }

  /**
   * Sets the task name.
   */
  public void setName(final String name) {
    checkNotNull(name);
    configuration.put(NAME_KEY, name);
  }

  /**
   * Returns a type ID of the task instance.
   */
  public String getTypeId() {
    return getString(TYPE_ID_KEY);
  }

  /**
   * Sets the task type ID.
   */
  public void setTypeId(final String typeId) {
    checkNotNull(typeId);
    configuration.put(TYPE_ID_KEY, typeId);
  }

  /**
   * Returns a type name of the task instance.
   */
  public String getTypeName() {
    return getString(TYPE_NAME_KEY);
  }

  /**
   * Sets the task type name.
   */
  public void setTypeName(final String typeName) {
    checkNotNull(typeName);
    configuration.put(TYPE_NAME_KEY, typeName);
  }

  /**
   * Is task enabled?
   */
  public boolean isEnabled() {
    return getBoolean(ENABLED_KEY, true);
  }

  /**
   * Sets is task enabled.
   */
  public void setEnabled(final boolean enabled) {
    configuration.put(ENABLED_KEY, Boolean.toString(enabled));
  }

  /**
   * Is task while running visible?
   */
  public boolean isVisible() {
    return getBoolean(VISIBLE_KEY, true);
  }

  /**
   * Sets is running task visible.
   */
  public void setVisible(final boolean visible) {
    configuration.put(VISIBLE_KEY, Boolean.toString(visible));
  }

  /**
   * Returns the email where alert should be sent in case of failure.
   */
  public String getAlertEmail() {
    return getString(ALERT_EMAIL_KEY);
  }

  /**
   * Sets or clears the alert email.
   */
  public void setAlertEmail(final String email) {
    if (Strings.isNullOrEmpty(email)) {
      configuration.remove(ALERT_EMAIL_KEY);
    }
    else {
      configuration.put(ALERT_EMAIL_KEY, email);
    }
  }

  /**
   * Gets created.
   */
  public Date getCreated() {
    return getDate(CREATED_KEY, null);
  }

  /**
   * Sets created, {@code date} cannot be {@code null}.
   */
  public void setCreated(final Date date) {
    checkNotNull(date);
    setDate(CREATED_KEY, date);
  }

  /**
   * Gets updated.
   */
  public Date getUpdated() {
    return getDate(UPDATED_KEY, null);
  }

  /**
   * Sets updated, {@code date} cannot be {@code null}.
   */
  public void setUpdated(final Date date) {
    checkNotNull(date);
    setDate(UPDATED_KEY, date);
  }

  /**
   * Returns the message of current or last run of task.
   */
  public String getMessage() {
    return getString(MESSAGE_KEY);
  }

  /**
   * Sets or clears task message of current or last run.
   */
  public void setMessage(final String message) {
    if (Strings.isNullOrEmpty(message)) {
      configuration.remove(MESSAGE_KEY);
    }
    else {
      configuration.put(MESSAGE_KEY, message);
    }
  }

  /**
   * Returns the repository ID that task should target or {@code null} if not set. The latter usually means
   * "all repositories" but the meaning might be different per task.
   */
  public String getRepositoryId() {
    // TODO: this might change?
    final String repoId = getString(REPOSITORY_ID_KEY);
    if (repoId == null || "*".equals(repoId) || "all_repo".equals(repoId)) {
      return null;
    }
    return repoId;
  }

  /**
   * Sets or clears the repository ID.
   */
  public void setRepositoryId(final String repoId) {
    // TODO: this might change?
    if (Strings.isNullOrEmpty(repoId) || "*".equals(repoId) || "all_repo".equals(repoId)) {
      configuration.remove(REPOSITORY_ID_KEY);
    }
    else {
      configuration.put(REPOSITORY_ID_KEY, repoId);
    }
  }

  /**
   * Returns the path under which task should operate, if applicable. Never returns {@code null}.
   */
  public String getPath() {
    return getString(PATH_KEY, "/");
  }

  /**
   * Sets or clears the path.
   */
  public void setPath(final String path) {
    if (Strings.isNullOrEmpty(path)) {
      configuration.remove(PATH_KEY);
    }
    else {
      configuration.put(PATH_KEY, path);
    }
  }

  // ==

  /**
   * Returns date parameter by key.
   */
  public Date getDate(final String key, final Date defaultValue) {
    if (configuration.containsKey(key)) {
      // TODO: will NPE if value is null
      return new DateTime(getString(key)).toDate();
    }
    else {
      return defaultValue;
    }
  }

  /**
   * Sets or clears a date parameter.
   */
  public void setDate(final String key, final Date date) {
    checkNotNull(key);
    if (date == null) {
      configuration.remove(key);
    }
    else {
      configuration.put(key, new DateTime(date).toString());
    }
  }

  /**
   * Returns boolean parameter by key.
   */
  public boolean getBoolean(final String key, final boolean defaultValue) {
    return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
  }

  /**
   * Sets a boolean value.
   */
  public void setBoolean(final String key, final boolean value) {
    checkNotNull(key);
    configuration.put(key, String.valueOf(value));
  }

  /**
   * Returns int parameter by key.
   */
  public int getInteger(final String key, final int defaultValue) {
    return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
  }

  /**
   * Sets' a integer value.
   */
  public void setInteger(final String key, final int value) {
    checkNotNull(key);
    configuration.put(key, String.valueOf(value));
  }

  /**
   * Returns long parameter by key.
   */
  public long getLong(final String key, final long defaultValue) {
    return Long.parseLong(getString(key, String.valueOf(defaultValue)));
  }

  /**
   * Sets' a long value.
   */
  public void setLong(final String key, final long value) {
    checkNotNull(key);
    configuration.put(key, String.valueOf(value));
  }

  /**
   * Returns string parameter by key or {@code null} if no such key mapped.
   */
  public String getString(final String key) {
    return getString(key, null);
  }

  /**
   * Returns string parameter by key or {@code defaultValue} if no such key mapped..
   */
  public String getString(final String key, final String defaultValue) {
    checkNotNull(key);
    if (configuration.containsKey(key)) {
      return configuration.get(key);
    }
    else {
      return defaultValue;
    }
  }

  /**
   * Sets or clears a string value.
   */
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
   * Applies another task configuration on this task configuration.
   */
  public void apply(final TaskConfiguration taskConfiguration) throws IllegalArgumentException {
    checkNotNull(taskConfiguration);
    taskConfiguration.validate();
    for (Entry<String, String> entry : taskConfiguration.configuration.entrySet()) {
      this.configuration.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Returns an immutable copy of this configuration as {@link Map<String,String>}.
   */
  public Map<String, String> asMap() {
    return ImmutableMap.copyOf(configuration);
  }

  // ==

  public String toString() {
    return configuration.toString();
  }
}
