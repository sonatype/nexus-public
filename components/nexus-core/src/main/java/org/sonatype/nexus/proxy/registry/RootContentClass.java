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
package org.sonatype.nexus.proxy.registry;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named(RootContentClass.ID)
public class RootContentClass
    extends AbstractIdContentClass
{
  public static final String ID = "any";

  public static final String NAME = "Any Content";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getName() {
    return NAME;
  }

  ;

  @Override
  public boolean isCompatible(ContentClass contentClass) {
    //root is compatible with all !
    return true;
  }

  @Override
  public boolean isGroupable() {
    //you can't create repos w/ 'root' type content, so groupable isn't an option
    return false;
  }
}
