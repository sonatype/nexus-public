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
package org.sonatype.nexus.util;

import org.codehaus.plexus.util.xml.Xpp3Dom;

public class ExternalConfigUtil
{
  public static void setNodeValue(Xpp3Dom parent, String name, String value) {
    // if we do not have a current value, then just return without setting the node;
    if (value == null) {
      return;
    }

    Xpp3Dom node = parent.getChild(name);

    if (node == null) {
      node = new Xpp3Dom(name);

      parent.addChild(node);
    }

    node.setValue(value);
  }

}
