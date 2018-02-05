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
//
// Helper to generate NEXUS_RESOURCE_DIRS environment variable.
//
// Usage from project directory:
//
//     export NEXUS_RESOURCE_DIRS=`groovy ./buildsupport/scripts/nexusresourcedirs.groovy`
//
// Aggregate projects using 'roots' property, from parent of project directories:
//
//     export NEXUS_RESOURCE_DIRS=`groovy -Droots=nexus-oss,nexus-pro,nexus-bundles nexus-oss/buildsupport/scripts/nexusresourcedirs.groovy`
//

def roots = System.getProperty('roots', '.')
def dirs = []

roots.split(',').each { root ->
  def dir = new File(root)
  if (dir.exists()) {
    dir.eachDirRecurse {
      if (it.path.endsWith('src/main/resources/static')) {
        dirs << it.parentFile.canonicalPath
      }
      else if (it.path.endsWith('src/test/ft-resources')) {
        dirs << it.canonicalPath
      }
    }
  }
}

println dirs.join(',')
