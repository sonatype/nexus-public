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
package org.sonatype.nexus.script.plugin.internal.security

import org.sonatype.nexus.security.authz.WildcardPermission2

import com.google.common.base.Joiner
import groovy.transform.CompileStatic

import static com.google.common.base.Preconditions.checkNotNull

/**
 * Script permission.
 * Allows for fine-grained permissions on Scripts based on their name.
 * 
 * @since 3.0
 */
@CompileStatic
class ScriptPermission
    extends WildcardPermission2
{
  public static final String SYSTEM = 'nexus'
  
  public static final String DOMAIN = 'script'
  
  final String name
  
  final List<String> actions 
  
  ScriptPermission(String name, List<String> actions) {
    this.name = checkNotNull(name)
    this.actions = checkNotNull(actions)
    
    setParts(Joiner.on(':').join(
        SYSTEM,
        DOMAIN,
        name,
        Joiner.on(',').join(actions)
    ))
  }
}
