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

import java.util.List;

import org.sonatype.nexus.formfields.FormField;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Support class for {@link TaskDescriptor}s.
 *
 * @since 3.0
 */
public abstract class TaskDescriptorSupport<T extends Task>
    implements TaskDescriptor<T>
{
  private final String id;

  private final String name;

  private final Class<T> type;

  private final boolean visible;

  private final boolean exposed;

  private final List<FormField> formFields;

  private final Predicate<TaskInfo> predicate;

  /**
   * Simplified constructor that will create visible and exposed descriptor without any formField.
   */
  public TaskDescriptorSupport(final Class<T> type, final String name)
  {
    this(type, name, true, true);
  }

  /**
   * Simplified constructor that will create visible and exposed descriptor with formFields.
   */
  public TaskDescriptorSupport(final Class<T> type, final String name, final FormField... formFields)
  {
    this(type, name, true, true, formFields);
  }

  /**
   * Constructor with all bells and whistles making task class' "simple class name" as type ID. Basically this
   * is somewhat backward compatible with old scheduler, where @Named was used with usually simple class name.
   */
  public TaskDescriptorSupport(final Class<T> type,
                               final String name,
                               final boolean visible,
                               final boolean exposed,
                               final FormField... formFields)
  {
    this(type.getSimpleName(), type, name, visible, exposed, formFields);
  }

  /**
   * Constructor with all bells and whistles.
   */
  public TaskDescriptorSupport(final String id,
                               final Class<T> type,
                               final String name,
                               final boolean visible,
                               final boolean exposed,
                               final FormField... formFields)
  {
    checkNotNull(id);
    checkNotNull(type);
    checkNotNull(name);
    checkNotNull(formFields);
    this.id = id;
    this.name = name;
    this.type = type;
    this.visible = visible;
    this.exposed = exposed;
    this.formFields = ImmutableList.copyOf(formFields);
    this.predicate = new Predicate<TaskInfo>()
    {
      @Override
      public boolean apply(final TaskInfo input) {
        return id.equals(input.getConfiguration().getTypeId());
      }
    };
  }

  @Override
  public final String getId() {
    return id;
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final Class<T> getType() {
    return type;
  }

  @Override
  public final boolean isVisible() { return visible; }

  @Override
  public final boolean isExposed() {
    return exposed;
  }

  @Override
  public final List<FormField> formFields() {
    return formFields;
  }

  @Override
  public final Predicate<TaskInfo> predicate() {
    return predicate;
  }

  @Override
  public final List<TaskInfo> filter(final List<TaskInfo> tasks) {
    return newArrayList(Iterables.filter(tasks, predicate()));
  }
}
