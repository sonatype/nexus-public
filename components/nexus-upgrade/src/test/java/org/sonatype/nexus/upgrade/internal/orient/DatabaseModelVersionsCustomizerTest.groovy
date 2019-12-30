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
package org.sonatype.nexus.upgrade.internal.orient

import org.sonatype.nexus.supportzip.SupportBundle

import groovy.json.JsonOutput
import spock.lang.Specification
import spock.lang.Subject

class DatabaseModelVersionsCustomizerTest
    extends Specification
{
  UpgradeManager upgradeManager = Mock()

  @Subject
  DatabaseModelVersionsCustomizer upgradeVersionCustomizer = new DatabaseModelVersionsCustomizer(upgradeManager)

  def 'Model versions are generated to a file'() {
    given:
      SupportBundle bundle = new SupportBundle()
      final Map upgradeVersions = [foo: 'bar'].asImmutable()

    when:
      upgradeVersionCustomizer.customize(bundle)

    then:
      bundle.sources.size() == 1

    when: 'Preparing the bundle content'
      SupportBundle.ContentSource source = bundle.sources.get(0)
      source.prepare()

    then: 'It is generated from the map of known model versions'
      upgradeManager.latestKnownModelVersions() >> upgradeVersions
      source.content.text == JsonOutput.prettyPrint(JsonOutput.toJson(upgradeVersions))
  }
}
