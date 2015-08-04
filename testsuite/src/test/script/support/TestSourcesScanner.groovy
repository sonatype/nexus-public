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
package support

import org.apache.tools.ant.DirectoryScanner

/**
 * Helper to scan for test .java sources.
 */
class TestSourcesScanner
{
    private DirectoryScanner scanner = new DirectoryScanner()

    File basedir

    String includes

    String excludes

    void scan() {
        println "Scanning test-sources in: $basedir"
        scanner.basedir = basedir

        def includes = []
        def excludes = []

        for (pattern in this.includes.trim().split(',')) {
            // includes can contain excludes pattern
            if (pattern.startsWith('!')) {
                excludes << pattern.trim()
            }
            else {
                includes << pattern.trim()
            }
        }

        for (pattern in this.excludes.trim().split(',')) {
            excludes << pattern.trim()
        }

        // always exclude some sources
        excludes << '**/Abstract*.java'

        scanner.includes = includes
        println "Includes: $includes"

        scanner.excludes = excludes
        println "Excludes: $excludes"

        scanner.scan()

        println "Found $scanner.includedFilesCount test-sources"
    }

    boolean isEmpty() {
        return scanner.includedFilesCount == 0
    }

    Iterator iterator() {
        // sorting to help get consistent order
        return scanner.includedFiles.toList().sort().iterator()
    }
}