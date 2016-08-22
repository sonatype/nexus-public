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
package org.sonatype.nexus.repository.config

import org.sonatype.nexus.common.entity.EntityMetadata

import spock.lang.Specification

/**
 * Tests for {@link Configuration}
 * @since 3.1
 */
class ConfigurationTest
    extends Specification
{

  def "Copy copies all properties."() {
    Configuration original = new Configuration(repositoryName: "myrepo", recipeName: "someRecipe", online: false,
        attributes: [foo: "bar"], entityMetadata: Mock(EntityMetadata))

    when:
      Configuration clone = original.copy()

    then:
      original.repositoryName == clone.repositoryName
      original.recipeName == clone.recipeName
      original.online == clone.online
      original.attributes == clone.attributes
      original.entityMetadata.is(clone.entityMetadata) // shallow copy
  }

  def "Copy makes a copy of attributes."() {
    Configuration original = new Configuration(repositoryName: "myrepo", recipeName: "someRecipe", online: false,
        attributes: [foo: "bar"])

    when:
      Configuration clone = original.copy()
      clone.attributes.blat = "greep"

    then:
      original.attributes.containsKey("blat") == false
  }

  def "Copy works when attributes is null."() {
    Configuration original = new Configuration(repositoryName: "myrepo", recipeName: "someRecipe", online: false,
        attributes: null)

    when:
      Configuration clone = original.copy()

    then:
      clone.attributes == null
  }

  def 'Copy performs a deep clone of attributes'() {
    Configuration original = new Configuration(repositoryName: 'myrepo', recipeName: 'someRecipe', online: false,
        attributes: [foo: 'bar', httpclient: [authorization: [password: 'secret']]],
        entityMetadata: Mock(EntityMetadata))

    when:
      Configuration clone = original.copy();
      clone.attributes.httpclient.authorization.password = 'top secret'

    then:
      original.attributes.httpclient.authorization.password == 'secret'
  }

}
