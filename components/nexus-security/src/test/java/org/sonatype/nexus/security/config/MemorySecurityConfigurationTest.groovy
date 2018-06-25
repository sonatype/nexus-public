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
package org.sonatype.nexus.security.config

import spock.lang.Specification
import spock.lang.Unroll

class MemorySecurityConfigurationTest
    extends Specification
{
  MemorySecurityConfiguration config

  def setup() {
    config = new MemorySecurityConfiguration()
  }

  @Unroll
  def 'userRoleMappings for source: \'#src\' read ignores case: #ignoreCase'(src, ignoreCase, isFound) {
    given: 'an existing user role mapping'
      def roles = ['test-role'] as Set
      def newUserRoleMapping = new CUserRoleMapping(userId: 'userid', source: src, roles: roles)
      config.addUserRoleMapping(newUserRoleMapping)

    when: 'a users roles are retrieved with different user id casing'
      def roleMapping = config.getUserRoleMapping('USERID', src)

    then: 'the mapping is found if source is case insensitive'
      roleMapping != null == isFound
      if (isFound) {
        roleMapping.roles == roles
      }

    where:
      src << ['default', 'ldap', 'crowd', 'other']
      ignoreCase << ['true', 'true', 'true', 'false']
      isFound << [true, true, true, false]
  }
}
