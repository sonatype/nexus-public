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
def ant = new AntBuilder()

def basedir = System.getProperty('basedir', '.') as File
def fixlast = System.getProperty('fixlast', 'false')

println "Normalizing line-endings to LF in: ${basedir.canonicalFile}"
println "    fixlast: $fixlast"

ant.fixcrlf(
    srcDir: basedir,
    eol: 'lf',
    eof: 'remove',
    fixlast: fixlast
) {
    include(name: '**/*.java')
    include(name: '**/*.groovy')
    include(name: '**/*.js')
    include(name: '**/*.css')
    include(name: '**/*.vm')
    include(name: '**/*.tpl')
    include(name: '**/*.properties')
    include(name: '**/*.xml')
    include(name: '**/*.yml')
    include(name: '**/*.txt')
}
