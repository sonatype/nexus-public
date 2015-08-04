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
package org.sonatype.plexus.rest.xstream.xml.test;

import java.util.ArrayList;
import java.util.List;


public class DataObject1
    extends BaseDataObject
{

  String dataObjectField1;

  String dataObjectField2;

  List dataList = new ArrayList();

  public String getDataObjectField1() {
    return dataObjectField1;
  }

  public void setDataObjectField1(String dataObjectField1) {
    this.dataObjectField1 = dataObjectField1;
  }

  public String getDataObjectField2() {
    return dataObjectField2;
  }

  public void setDataObjectField2(String dataObjectField2) {
    this.dataObjectField2 = dataObjectField2;
  }

  public List getDataList() {
    return dataList;
  }

  public void setDataList(List dataList) {
    this.dataList = dataList;
  }


}
