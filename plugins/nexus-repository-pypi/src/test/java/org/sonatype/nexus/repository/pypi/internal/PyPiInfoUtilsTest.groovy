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
package org.sonatype.nexus.repository.pypi.internal

import java.lang.reflect.Field
import java.lang.reflect.Modifier

import org.slf4j.Logger
import spock.lang.Specification
import spock.lang.Unroll

/**
 * {@link PyPiInfoUtils} unit tests.
 */
class PyPiInfoUtilsTest
    extends Specification
{
  static final Map<String, String> METADATA_V1_1 = [
      metadata_version: '1.1',
      name            : 'sample',
      version         : '1.2.0',
      summary         : 'A sample Python project',
      home_page       : 'https://github.com/pypa/sampleproject',
      author          : 'The Python Packaging Authority',
      author_email    : 'pypa-dev@googlegroups.com',
      license         : 'MIT',
      description     : '''A sample Python project
=======================

A sample project that exists as an aid to the `Python Packaging User Guide
<https://packaging.python.org>`_'s `Tutorial on Packaging and Distributing
Projects <https://packaging.python.org/en/latest/distributing.html>`_.

This projects does not aim to cover best practices for Python project
development as a whole. For example, it does not provide guidance or tool
recommendations for version control, documentation, or testing.

----

This is the README file for the project.

The file should use UTF-8 encoding and be written using ReStructured Text. It
will be used to generate the project webpage on PyPI and will be displayed as
the project homepage on common code-hosting services, and should be written for
that purpose.

Typical contents for this file would include an overview of the project, basic
usage examples, etc. Generally, including the project changelog in here is not
a good idea, although a simple "What's New" section for the most recent version
may be appropriate.
''',
      keywords        : 'sample setuptools development',
      platform        : 'UNKNOWN',
      classifiers     : [
          'Development Status :: 3 - Alpha',
          'Intended Audience :: Developers',
          'Topic :: Software Development :: Build Tools',
          'License :: OSI Approved :: MIT License',
          'Programming Language :: Python :: 2',
          'Programming Language :: Python :: 2.6',
          'Programming Language :: Python :: 2.7',
          'Programming Language :: Python :: 3',
          'Programming Language :: Python :: 3.3',
          'Programming Language :: Python :: 3.4',
          'Programming Language :: Python :: 3.5'
      ].join("\n")
  ]

  static final Map<String, String> METADATA_V2_0 = [
      metadata_version: '2.0',
      name            : 'sample',
      version         : '1.2.0',
      summary         : 'A sample Python project',
      home_page       : 'https://github.com/pypa/sampleproject',
      author          : 'The Python Packaging Authority',
      author_email    : 'pypa-dev@googlegroups.com',
      license         : 'MIT',
      description     : '''A sample Python project
=======================

A sample project that exists as an aid to the `Python Packaging User Guide
<https://packaging.python.org>`_'s `Tutorial on Packaging and Distributing
Projects <https://packaging.python.org/en/latest/distributing.html>`_.

This projects does not aim to cover best practices for Python project
development as a whole. For example, it does not provide guidance or tool
recommendations for version control, documentation, or testing.

----

This is the README file for the project.

The file should use UTF-8 encoding and be written using ReStructured Text. It
will be used to generate the project webpage on PyPI and will be displayed as
the project homepage on common code-hosting services, and should be written for
that purpose.

Typical contents for this file would include an overview of the project, basic
usage examples, etc. Generally, including the project changelog in here is not
a good idea, although a simple "What's New" section for the most recent version
may be appropriate.
''',
      keywords        : 'sample setuptools development',
      platform        : 'UNKNOWN',
      classifiers     : [
          'Development Status :: 3 - Alpha',
          'Intended Audience :: Developers',
          'Topic :: Software Development :: Build Tools',
          'License :: OSI Approved :: MIT License',
          'Programming Language :: Python :: 2',
          'Programming Language :: Python :: 2.6',
          'Programming Language :: Python :: 2.7',
          'Programming Language :: Python :: 3',
          'Programming Language :: Python :: 3.3',
          'Programming Language :: Python :: 3.4',
          'Programming Language :: Python :: 3.5'
      ].join('\n'),
      provides_extra: [
          'dev',
          'test'
      ].join('\n'),
      requires_dist: [
          'peppercorn',
          'check-manifest; extra == \'dev\'',
          'coverage; extra == \'test\''
      ].join('\n')
  ]

  @Unroll
  def 'Extract metadata from archive #filename '() {
    expect:
      getClass().getResourceAsStream(filename).withCloseable { input ->
        PyPiInfoUtils.extractMetadata(input)
      } == attributes
    where:
      filename                            | attributes
      'sample-1.2.0.tar.bz2'              | METADATA_V1_1 + [archive_type: 'tar.bzip2']
      'sample-1.2.0.tar.gz'               | METADATA_V1_1 + [archive_type: 'tar.gz']
      'sample-1.2.0.zip'                  | METADATA_V1_1 + [archive_type: 'zip']
      'sample-1.2.0.tar.Z'                | METADATA_V1_1 + [archive_type: 'tar.z']
      'sample-1.2.0-py2.7.egg'            | METADATA_V1_1 + [archive_type: 'zip']
      'sample-1.2.0-py2.py3-none-any.whl' | METADATA_V2_0 + [archive_type: 'zip']
  }

  def 'Properly parse metadata lines containing non-ASCII characters'() {
    when: 'The metadata is extracted from a file known to contain non-ASCII characters'
      Map<String, List<String>> attributes =
          getClass().getResourceAsStream('sample-metadata.txt').withCloseable { input ->
            PyPiInfoUtils.parsePackageInfo(input)
          }
    then:
      attributes == [
          'entrya': ['Miguel Ángel García'],
          'entryb': ['随机字符'],
          'entryc': ['I\'m just some normal, boring text!'],
          'entryd': ['slumpmässiga tecken'],
          'entrye': ['caràcters aleatoris'],
          'entryf': ['أحرف عشوائية'],
          'entryg': ['بے ترتیب حروف'],
          'entryh': ['यादृच्छिक वर्ण'],
          'entryi': ['случайные символы']
      ]
  }

  def 'Log errors only when the file can not be read'() {
    given: 'A mocked logger is used'
      def logger = Mock(Logger)
      Field field = PyPiInfoUtils.getDeclaredField("log")
      field.setAccessible(true)
      Field modifiersField = Field.class.getDeclaredField("modifiers")
      modifiersField.setAccessible(true)
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL)
      field.set(null, logger)

    when: 'a file that is a compressed archive is provided'
      getClass().getResourceAsStream('sample-1.2.0.tar.bz2').withCloseable { input ->
        PyPiInfoUtils.extractMetadata(input)
      }

    then: 'no errors are logged'
      0 * _

    when: 'a valid archive is provided, but a compression type can not be determined'
      getClass().getResourceAsStream('sample-1.2.0.zip').withCloseable { input ->
        PyPiInfoUtils.extractMetadata(input)
      }

    then: 'no errors are logged'
      0 * _

    when: 'a file that is not an archive is provided'
      getClass().getResourceAsStream('sample-index.html').withCloseable { input ->
        PyPiInfoUtils.extractMetadata(input)
      }

    then: 'log all errors'
      1 * logger.error('Unable to decompress PyPI archive', _)
      1 * logger.error('Unable to extract PyPI archive', _)
      0 * _
  }
}
