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
package org.sonatype.nexus.repository.search.sql;

/**
 * SearchAssetRecord represents the data of an asset that is stored in the search index.
 */
public interface SearchAssetRecord
{
  Integer getRepositoryId();

  void setRepositoryId(Integer repositoryId);

  Integer getComponentId();

  void setComponentId(Integer componentId);

  void setAssetId(int assetId);

  int getAssetId();

  String getFormat();

  void setFormat(String format);

  String getPath();

  void setPath(String path);

  String getAssetFormatValue1();

  void setAssetFormatValue1(String assetFormatValue1);

  String getAssetFormatValue2();

  void setAssetFormatValue2(String assetFormatValue2);

  String getAssetFormatValue3();

  void setAssetFormatValue3(String assetFormatValue3);

  String getAssetFormatValue4();

  void setAssetFormatValue4(String assetFormatValue4);

  String getAssetFormatValue5();

  void setAssetFormatValue5(String assetFormatValue5);

  String getAssetFormatValue6();

  void setAssetFormatValue6(String assetFormatValue6);

  String getAssetFormatValue7();

  void setAssetFormatValue7(String assetFormatValue7);

  String getAssetFormatValue8();

  void setAssetFormatValue8(String assetFormatValue8);

  String getAssetFormatValue9();

  void setAssetFormatValue9(String assetFormatValue9);

  String getAssetFormatValue10();

  void setAssetFormatValue10(String assetFormatValue10);

  String getAssetFormatValue11();

  void setAssetFormatValue11(String assetFormatValue11);

  String getAssetFormatValue12();

  void setAssetFormatValue12(String assetFormatValue12);

  String getAssetFormatValue13();

  void setAssetFormatValue13(String assetFormatValue13);

  String getAssetFormatValue14();

  void setAssetFormatValue14(String assetFormatValue14);

  String getAssetFormatValue15();

  void setAssetFormatValue15(String assetFormatValue15);

  String getAssetFormatValue16();

  void setAssetFormatValue16(String assetFormatValue16);

  String getAssetFormatValue17();

  void setAssetFormatValue17(String assetFormatValue17);

  String getAssetFormatValue18();

  void setAssetFormatValue18(String assetFormatValue18);

  String getAssetFormatValue19();

  void setAssetFormatValue19(String assetFormatValue19);

  String getAssetFormatValue20();

  void setAssetFormatValue20(String assetFormatValue20);
}
