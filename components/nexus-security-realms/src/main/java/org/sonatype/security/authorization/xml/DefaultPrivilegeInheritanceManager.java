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
package org.sonatype.security.authorization.xml;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Typed;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Default implementation of PrivilegeInheritanceManager which adds read to each action. The way we see it, if you can
 * create/update/delete something then you automatically have access to 'read' it as well.
 *
 * @author Brian Demers
 */
@Singleton
@Typed(PrivilegeInheritanceManager.class)
@Named("default")
public class DefaultPrivilegeInheritanceManager
    implements PrivilegeInheritanceManager
{
  public List<String> getInheritedMethods(String method) {
    List<String> methods = new ArrayList<String>();

    methods.add(method);

    if ("create".equals(method)) {
      methods.add("read");
    }
    else if ("delete".equals(method)) {
      methods.add("read");
    }
    else if ("update".equals(method)) {
      methods.add("read");
    }

    return methods;
  }
}
