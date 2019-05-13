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
package org.sonatype.nexus.repository.rest.api

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.types.GroupType
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.types.ProxyType

import spock.lang.Specification
import spock.lang.Unroll

class RepositoryXOTest
    extends Specification
{
  @Unroll
  def 'It will convert a Repository to a RepositoryXO'() {
    given:
      def repository = Mock(Repository) {
        getName() >> name
        getFormat() >> format
        getType() >> type
        getUrl() >> url
        getConfiguration() >> new Configuration(attributes: [(type.value): attributes])
      }
    when:
      def repositoryXO = RepositoryXO.fromRepository(repository)
    then:
      repositoryXO.name == name
      repositoryXO.format == expectedFormat
      repositoryXO.type == expectedType
      repositoryXO.url == url
      repositoryXO.attributes == expectedAttributes

    where:
      name | format          | expectedFormat | type             | expectedType | url | attributes         | expectedAttributes
      'x'  | format('npm')   | 'npm'          | new ProxyType()  | 'proxy'      | 'u' | [remoteUrl: 'url'] | [proxy: [remoteUrl: 'url']]
      'y'  | format('maven') | 'maven'        | new HostedType() | 'hosted'     | 'u' | [remoteUrl: 'foo'] | [:]
      'z'  | format('nuget') | 'nuget'        | new GroupType()  | 'group'      | 'u' | [remoteUrl: 'foo'] | [:]
  }

  private Format format(value) {
    Mock(Format) {
      getValue() >> value
    }
  }
}
