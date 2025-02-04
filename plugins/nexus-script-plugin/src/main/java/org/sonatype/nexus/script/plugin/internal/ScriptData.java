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
package org.sonatype.nexus.script.plugin.internal;

import java.util.Objects;

import org.sonatype.nexus.common.entity.HasName;
import org.sonatype.nexus.script.Script;
import org.sonatype.nexus.script.ScriptManager;

/**
 * {@link Script} data.
 *
 * @since 3.21
 */
public class ScriptData
    implements HasName, Script
{
  private String name;

  private String content;

  private String type = ScriptManager.DEFAULT_TYPE;

  public ScriptData() {
  }

  public ScriptData(String name, String content, String type) {
    this.name = name;
    this.content = content;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return "ScriptData{" +
        "name='" + name + '\'' +
        ", content='" + content + '\'' +
        ", type='" + type + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ScriptData that = (ScriptData) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(content, that.content) &&
        Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, content, type);
  }
}
