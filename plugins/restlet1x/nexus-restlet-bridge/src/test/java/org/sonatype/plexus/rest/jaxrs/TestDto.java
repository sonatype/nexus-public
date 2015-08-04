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
package org.sonatype.plexus.rest.jaxrs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestDto
{
  private String aString;

  private Date aDate;

  private List<String> aStringList;

  private List<TestDto> children;

  public String getAString() {
    return aString;
  }

  public void setAString(String string) {
    aString = string;
  }

  public Date getADate() {
    return aDate;
  }

  public void setADate(Date date) {
    aDate = date;
  }

  public List<String> getAStringList() {
    if (aStringList == null) {
      aStringList = new ArrayList<String>();
    }

    return aStringList;
  }

  public void setAStringList(List<String> stringList) {
    aStringList = stringList;
  }

  public List<TestDto> getChildren() {
    if (children == null) {
      children = new ArrayList<TestDto>();
    }

    return children;
  }

  public void setChildren(List<TestDto> children) {
    this.children = children;
  }
}
