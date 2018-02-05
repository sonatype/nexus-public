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
package org.sonatype.nexus.coreui

import org.sonatype.nexus.extdirect.model.PagedResponse
import org.sonatype.nexus.extdirect.model.StoreLoadParameters
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Filter
import org.sonatype.nexus.extdirect.model.StoreLoadParameters.Sort
import org.sonatype.nexus.security.SecuritySystem
import org.sonatype.nexus.security.privilege.Privilege

import spock.lang.Specification
import spock.lang.Subject

/**
 * @since 3.1
 */
class PrivilegeComponentTest
    extends Specification
{
  
  SecuritySystem securitySystem = Mock()
  
  @Subject
  PrivilegeComponent privilegeComponent = new PrivilegeComponent(securitySystem: securitySystem)

  def "Extract page with no results"() {
    when: 'Requesting a page and having no results to choose from'
      PagedResponse<PrivilegeXO> page = privilegeComponent.
          extractPage(parameters(0, 100, sort(), filter()), [])

    then: 'there are no results'
      page.total == 0
      !page.data
  }

  def "Can filter results by name, description, permission or type"() {
    given: 'Two test privileges'
      def privileges = [privilege('a', 'b', 'c', 'd'), privilege('w', 'x', 'y', 'z')]

    when: 'Filtering by name'
      PagedResponse<PrivilegeXO> page = privilegeComponent.
          extractPage(parameters(0, 100, sort(), filter('a')), privileges)

    then: 'we hit a single result'
      page.total == 1
      page.data[0] == privileges[0]

    when: 'Filtering by description'
      page = privilegeComponent.
          extractPage(parameters(0, 100, sort(), filter('x')), privileges)

    then: 'we hit a single result'
      page.total == 1
      page.data[0] == privileges[1]


    when: 'Filtering by permission'
      page = privilegeComponent.
          extractPage(parameters(0, 100, sort(), filter('c')), privileges)

    then: 'we hit a single result'
      page.total == 1
      page.data[0] == privileges[0]


    when: 'Filtering by type'
      page = privilegeComponent.
          extractPage(parameters(0, 100, sort(), filter('z')), privileges)

    then: 'we hit a single result'
      page.total == 1
      page.data[0] == privileges[1]


    when: 'Not filtering'
      page = privilegeComponent.
          extractPage(parameters(0, 100, sort(), null), privileges)

    then: 'we get both results'
      page.total == 2
  }
  
  def "Can sort on available fields"() {
    given: 'Three test privileges'
      def privileges = [privilege('a', 'b', 'c', 'd'), privilege('b', 'c', 'd', 'a'), privilege('c', 'd', 'a', 'b')]
    
    when: 'Sorting by name ASC'
      PagedResponse<PrivilegeXO> page = privilegeComponent.
          extractPage(parameters(0, 100, sort(), null), privileges)
    
    then: 'results are ordered by name'
      page.data == privileges
    
    when: 'Sorting by name DESC'
      page = privilegeComponent.extractPage(parameters(0, 100, sort('name', 'DESC'), null), privileges)
    
    then: 'results are ordered by description reversed'
      page.data == privileges.sort{it.description}.reverse()

    when: 'Sorting by description ASC'
      page = privilegeComponent.
          extractPage(parameters(0, 100, sort('description'), null), privileges)

    then: 'results are ordered by description'
      page.data == privileges.sort{it.description}

    when: 'Sorting by description DESC'
      page = privilegeComponent.extractPage(parameters(0, 100, sort('description', 'DESC'), null), privileges)

    then: 'results are ordered by description reversed'
      page.data == privileges.sort{it.description}.reverse()

    when: 'Sorting by permission ASC'
      page = privilegeComponent.
          extractPage(parameters(0, 100, sort('permission'), null), privileges)

    then: 'results are ordered by permission'
      page.data == privileges.sort{it.permission}

    when: 'Sorting by permission DESC'
      page = privilegeComponent.extractPage(parameters(0, 100, sort('permission', 'DESC'), null), privileges)

    then: 'results are ordered by permission reversed'
      page.data == privileges.sort{it.permission}.reverse()

    when: 'Sorting by type ASC'
      page = privilegeComponent.
          extractPage(parameters(0, 100, sort('type'), null), privileges)

    then: 'results are ordered by type'
      page.data == privileges.sort{it.type}

    when: 'Sorting by type DESC'
      page = privilegeComponent.extractPage(parameters(0, 100, sort('type', 'DESC'), null), privileges)

    then: 'results are ordered by type reversed'
      page.data == privileges.sort{it.type}.reverse()
  }
  
  def "Can page results"() {
    given: 'Three test privileges'
      def privileges = [privilege('a', 'a', 'a', 'a'), privilege('b', 'b', 'b', 'b'), privilege('c', 'c', 'c', 'c')]
    
    when: 'Requesting a page size smaller than the size of available results'
      PagedResponse<PrivilegeXO> page = privilegeComponent.
          extractPage(parameters(0, 2, sort(), null), privileges)
    
    then: 'we get back the first page of results'
      page.total == 3
      page.data == [privileges[0], privileges[1]]
    
    when: 'We request the second page'
      page = privilegeComponent.extractPage(parameters(2, 50, sort(), null), privileges)

    then: 'we get back the second page of results'
      page.total == 3
      page.data == [privileges[2]]

    when: 'We request a start value past our available results'
      page = privilegeComponent.extractPage(parameters(4, 2, sort(), null), privileges)

    then: 'an exception is thrown'
      thrown(IllegalArgumentException)
  }
  
  def "Can load a list of Privilege references"() {
    given: 'Three test privileges'
      def privileges = [privilege('a'), privilege('b'), privilege('c')] as Set
    
    when: 'Requesting references to existing Privileges'
      List<ReferenceXO> references = privilegeComponent.readReferences()
    
    then: 'All available Privileges are represented'
      1 * securitySystem.listPrivileges() >> privileges
      references.containsAll(
          [new ReferenceXO(id: 'a', name: 'a'), new ReferenceXO(id: 'b', name: 'b'), new ReferenceXO(id: 'c',
              name: 'c')])
  }

  static Privilege privilege(String text) {
    new Privilege(text, text, text, text, [:], false)
  }

  static PrivilegeXO privilege(String name, String description, String permission, String type) {
    new PrivilegeXO(
        name: name,
        description: description,
        permission: permission,
        type: type
    )
  }

  static StoreLoadParameters parameters(int start, int limit, Sort sort, Filter filter) {
    new StoreLoadParameters(
        start: start, limit: limit,
        filter: filter ? [filter] : [],
        sort: sort ? [sort] : [])
  }

  static Filter filter(String value = 'test') {
    new Filter(property: 'filter', value: value)
  }

  static Sort sort(String property = 'name', String direction = 'ASC') {
    new Sort(property: property, direction: direction)
  }

}
