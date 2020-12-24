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
package org.sonatype.repository.helm.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HelmListTestHelper
{
  public static List<Map<String, String>> getMaintainersList() {
    List<Map<String, String>> maintainers = new ArrayList<>();
    Map<String, String> map = new HashMap<>();

    map.put("email", "containers@bitnami.com");
    map.put("name", "Bitnami");

    maintainers.add(map);

    return maintainers;
  }

  public static List<String> getUrlList() {
    List<String> list = new ArrayList<>();

    list.add("mongodb-0.5.2.tgz");

    return list;
  }

  public static List<String> getSourcesList() {
    List<String> list = new ArrayList<>();

    list.add("https://github.com/bitnami/bitnami-docker-mongodb");

    return list;
  }
}
