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
package org.sonatype.nexus.coreui;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import org.sonatype.nexus.selector.UniqueSelectorName;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;
import org.sonatype.nexus.validation.group.Create;
import org.sonatype.nexus.validation.group.Update;

/**
 * Selector exchange object.
 *
 * @since 3.0
 */
public class SelectorXO
{
  @NotBlank(groups = Update.class)
  private String id;

  @Pattern(regexp = NamePatternConstants.REGEX, message = NamePatternConstants.MESSAGE)
  @NotBlank(groups = Create.class)
  @UniqueSelectorName(groups = Create.class)
  private String name;

  @NotBlank(groups = Create.class)
  private String type;

  private String description;

  @NotBlank
  private String expression;

  private List<String> usedBy;

  private int usedByCount;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public List<String> getUsedBy() {
    return usedBy;
  }

  public void setUsedBy(List<String> usedBy) {
    this.usedBy = usedBy;
  }

  public int getUsedByCount() {
    return usedByCount;
  }

  public void setUsedByCount(int usedByCount) {
    this.usedByCount = usedByCount;
  }

  @Override
  public String toString() {
    return "SelectorXO{" +
        "id='" + id + '\'' +
        ", name='" + name + '\'' +
        ", type='" + type + '\'' +
        ", description='" + description + '\'' +
        ", expression='" + expression + '\'' +
        ", usedBy=" + usedBy +
        ", usedByCount=" + usedByCount +
        '}';
  }
}
