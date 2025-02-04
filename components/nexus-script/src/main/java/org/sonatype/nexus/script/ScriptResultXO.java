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

import java.util.Objects;
import javax.validation.constraints.NotEmpty;

/**
 * Script result exchange object.
 *
 * @since 3.0
 */
public class ScriptResultXO
{

  @NotEmpty
  private String name;

  private String result;

  public ScriptResultXO() {
  }

  public ScriptResultXO(String name, String result) {
    this.name = name;
    this.result = result;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ScriptResultXO that = (ScriptResultXO) o;
    return Objects.equals(name, that.name) && Objects.equals(result, that.result);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, result);
  }

  @Override
  public String toString() {
    return "ScriptResultXO{" +
        "name='" + name + '\'' +
        ", result='" + result + '\'' +
        '}';
  }
}
