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
// Helper to consume the output of `mvn versions:display-dependency-updates versions:display-plugin-updates`
// and spit out unique version upgrades available.
//
// Usage:
//
//     mvn versions:display-dependency-updates versions:display-plugin-updates | groovy buildsupport/scripts/gobble.groovy
//

def lines = System.in.readLines()

def iter = lines.iterator()
while (iter.hasNext()) {
    def line = iter.next()
    if (line.startsWith('[INFO] --- versions-maven-plugin')) {
        break
    }
}

def chomp(line) {
    return line[6..-1].trim()
}

def updates = new HashSet()
while (iter.hasNext()) {
    def line = chomp(iter.next())

    if (line.endsWith(' ...')) {
        line = line + ' ' + chomp(iter.next())
    }

    if (line.contains('->')) {
        def parts = line.split('\\s')
        def update="${parts[0]}: ${parts[2]} -> ${parts[4]}"
        updates << update
    }
}

updates = new ArrayList(updates)
updates.sort()

for (update in updates) {
    println update
}