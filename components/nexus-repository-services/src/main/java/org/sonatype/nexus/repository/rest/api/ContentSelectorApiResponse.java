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
package org.sonatype.nexus.repository.rest.api;

import java.util.Objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import static org.sonatype.nexus.repository.rest.internal.resources.doc.ContentSelectorsResourceDoc.DESCRIPTION_DESCRIPTION;
import static org.sonatype.nexus.repository.rest.internal.resources.doc.ContentSelectorsResourceDoc.EXPRESSION_DESCRIPTION;
import static org.sonatype.nexus.repository.rest.internal.resources.doc.ContentSelectorsResourceDoc.EXPRESSION_EXAMPLE;
import static org.sonatype.nexus.repository.rest.internal.resources.doc.ContentSelectorsResourceDoc.EXPRESSION_NOTES;
import static org.sonatype.nexus.repository.rest.internal.resources.doc.ContentSelectorsResourceDoc.NAME_DESCRIPTION;
import static org.sonatype.nexus.repository.rest.internal.resources.doc.ContentSelectorsResourceDoc.TYPE_ALLOWED_VALUES;
import static org.sonatype.nexus.repository.rest.internal.resources.doc.ContentSelectorsResourceDoc.TYPE_DESCRIPTION;
import static org.sonatype.nexus.repository.rest.internal.resources.doc.ContentSelectorsResourceDoc.TYPE_NOTES;

/**
 * ContentSelector transfer object for REST APIs.
 */
@ApiModel
public class ContentSelectorApiResponse
    implements ValidatableContentSelectorRequest
{
  @ApiModelProperty(value = NAME_DESCRIPTION)
  private String name;

  @ApiModelProperty(value = TYPE_DESCRIPTION, allowableValues = TYPE_ALLOWED_VALUES, notes = TYPE_NOTES)
  private String type;

  @ApiModelProperty(value = DESCRIPTION_DESCRIPTION)
  private String description;

  @ApiModelProperty(value = EXPRESSION_DESCRIPTION, example = EXPRESSION_EXAMPLE, notes = EXPRESSION_NOTES)
  private String expression;

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

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentSelectorApiResponse that = (ContentSelectorApiResponse) o;
    return Objects.equals(name, that.name) && Objects.equals(type, that.type) &&
        Objects.equals(description, that.description) && Objects.equals(expression, that.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, description, expression);
  }
}
