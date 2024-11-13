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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sonatype.nexus.formfields.CheckboxFormField;
import org.sonatype.nexus.formfields.ComboboxFormField;
import org.sonatype.nexus.formfields.FormField;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;

/**
 * Support for {@link TaskDescriptor} implementations.
 *
 * @since 3.0
 */
public abstract class TaskDescriptorSupport
    implements TaskDescriptor
{
  // Common task form-field identifiers/labels

  public static final String MULTINODE_KEY = "multinode";

  public static final String MULTINODE_LABEL = "Multi node";

  public static final String MULTINODE_HELP = "Run task on all nodes in the cluster.";

  public static final String LIMIT_NODE_KEY = "limitnode";

  public static final String LIMIT_NODE_LABEL = "Select node";

  public static final String LIMIT_NODE_HELP = "Run task on this node in the cluster.";

  // Constants to help document configuration, since these are final and we have no fluent builder ATM

  protected static final boolean VISIBLE = true;

  protected static final boolean NOT_VISIBLE = false;

  protected static final boolean EXPOSED = true;

  protected static final boolean NOT_EXPOSED = false;

  protected static final boolean REQUEST_RECOVERY = true;

  private final String id;

  private final String name;

  private final Class<? extends Task> type;

  private final boolean visible;

  private final boolean exposed;

  private final boolean requestRecovery;

  private final List<FormField> formFields;

  private boolean isReadOnlyUi;

  public TaskDescriptorSupport(final String id,
                               final Class<? extends Task> type,
                               final String name,
                               final boolean visible,
                               final boolean exposed,
                               final FormField... formFields)
  {
    this(id, type, name, visible, exposed, false, formFields);
  }

  public TaskDescriptorSupport(final String id,
                               final Class<? extends Task> type,
                               final String name,
                               final boolean visible,
                               final boolean exposed,
                               final boolean requestRecovery,
                               final FormField... formFields)
  {
    this.id = checkNotNull(id);
    this.type = checkNotNull(type);
    this.name = checkNotNull(name);
    this.visible = visible;
    this.exposed = exposed;
    this.requestRecovery = requestRecovery;

    checkNotNull(formFields);
    this.formFields = Arrays.stream(formFields).filter(Objects::nonNull).collect(toList());
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
  public final Class<? extends Task> getType() {
    return type;
  }

  @Override
  public boolean isVisible() {
    return visible;
  }

  @Override
  public boolean isExposed() {
    return exposed;
  }

  @Override
  public final List<FormField> getFormFields() {
    return formFields;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", type=" + type +
        ", visible=" + visible +
        ", exposed=" + exposed +
        '}';
  }

  @Override
  public TaskConfiguration createTaskConfiguration() {
    return new TaskConfiguration();
  }

  @Override
  public void initializeConfiguration(final TaskConfiguration configuration) {
    // no-op
  }

  /**
   * Creates a new {@link FormField} for tasks which can run on multiple-nodes at once.
   *
   * @since 3.1
   */
  protected static CheckboxFormField newMultinodeFormField() {
    return new CheckboxFormField(MULTINODE_KEY, MULTINODE_LABEL, MULTINODE_HELP, false);
  }

  /**
   * Creates a new {@link FormField} for tasks which are limited to a specific node.
   *
   * @since 3.2
   */
  protected static ComboboxFormField<String> newLimitNodeFormField() {
    return new ComboboxFormField<String>(LIMIT_NODE_KEY, LIMIT_NODE_LABEL, LIMIT_NODE_HELP, true)
        .withStoreApi("node_NodeAccess.nodes").withIdMapping("name").withNameMapping("displayName");
  }

  @Override
  public boolean isRecoverable() {
    return requestRecovery;
  }

  @Override
  public boolean allowConcurrentRun() {
    return true;
  }

  public void setReadOnlyUi(final boolean readOnlyUi) {
    this.isReadOnlyUi = readOnlyUi;
  }

  @Override
  public boolean isReadOnlyUi() {
    return isReadOnlyUi;
  }
}
