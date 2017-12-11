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
package org.sonatype.nexus.repository.upload;

import java.util.Objects;

import org.sonatype.nexus.common.text.Strings2;

/**
 * Description of a field used when uploading a component.
 *
 * @since 3.next
 */
public class UploadFieldDefinition
{
  public enum Type
  {
    BOOLEAN, STRING
  }

  private String displayName;

  private String name;

  private boolean optional;

  private Type type;

  public UploadFieldDefinition(final String name, final boolean optional, final Type type) {
    this(name, Strings2.capitalize(name), optional, type);
  }

  public UploadFieldDefinition(final String name, final String displayName, final boolean optional, final Type type) {
    this.name = name;
    this.displayName = displayName;
    this.optional = optional;
    this.type = type;
  }

  /**
   * The name of the field
   */
  public String getName() {
    return name;
  }

  /**
   * The name to be displayed in UI
   */
  public String getDisplayName() {
    return this.displayName;
  }

  /**
   * The type of the field
   */
  public Type getType() {
    return type;
  }

  /**
   * Indicates whether this field is required
   */
  public boolean isOptional() {
    return optional;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, optional);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof UploadFieldDefinition) {
      UploadFieldDefinition other = (UploadFieldDefinition) obj;
      return Objects.equals(name, other.name) && Objects.equals(displayName, other.displayName) && type == other.type
          && optional == other.optional;
    }

    return false;
  }
}
