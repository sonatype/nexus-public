package org.sonatype.nexus.repository.conda.internal.hosted

import org.sonatype.nexus.repository.conda.internal.hosted.metadata.ChannelData

/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

import org.sonatype.nexus.repository.conda.internal.hosted.metadata.PackageDesc
import org.sonatype.nexus.repository.conda.internal.hosted.metadata.RepoData

class MetaDataTest extends GroovyTestCase {

    // source https://docs.conda.io/projects/conda-build/en/latest/concepts/generating-index.html

    def channelDataJson = '''{
  "channeldata_version": 1,
  "packages": {
    "scipy": {
      "activate.d": false,
      "binary_prefix": true,
      "deactivate.d": false,
      "description": "SciPy is a Python-based ecosystem of open-source software for mathematics, science, and engineering.",
      "dev_url": "https://github.com/scipy/scipy",
      "doc_url": "http://www.scipy.org/docs.html",
      "home": "http://www.scipy.org/",
      "license": "BSD 3-Clause",
      "post_link": false,
      "pre_link": false,
      "pre_unlink": false,
      "run_exports": {},
      "source_url": "https://github.com/scipy/scipy/archive/v1.1.0.tar.gz",
      "subdirs": [
        "linux-64",
        "osx-64"
      ],
      "summary": "Scientific Library for Python",
      "text_prefix": true,
      "version": "1.1.0"
    }
  },
  "subdirs": [
    "linux-64",
    "noarch",
    "osx-64"
  ]
}'''
    def repoDataJson = '''{
  "info": {
    "subdir": "linux-64"
  },
  "packages": {
    "scipy-1.1.0-py37hfa4b5c9_1.tar.bz2": {
      "build": "py37hfa4b5c9_1",
      "build_number": 1,
      "depends": [
        "blas 1.0 mkl",
        "libgcc-ng >=7.3.0",
        "libgfortran-ng >=7,<8.0a0",
        "libstdcxx-ng >=7.3.0",
        "mkl >=2018.0.3",
        "numpy >=1.15.1,<2.0a0",
        "python >=3.7,<3.8.0a0"
      ],
      "license": "BSD 3-Clause",
      "md5": "24ed4bd83f7ef61c27c95d9139313b28",
      "name": "scipy",
      "sha256": "0395a6d51892989f0d8e0023cb025a854ae38ec40196122737612850afc32b53",
      "size": 18820408,
      "subdir": "linux-64",
      "timestamp": 1535416612069,
      "version": "1.1.0"
    },
    "_py-xgboost-mutex-2.0-cpu_0.tar.bz2": {
      "build": "cpu_0",
      "build_number": 1,
      "depends": [],
      "license": "Apache-2.0",
      "md5": "23b8f98a355030331f40d0245492f715",
      "name": "_py-xgboost-mutex",
      "sha256": "504b46c85d81269b24e7b22309064886cfc873955e77daf0eb794d8e7ec8cc06",
      "size": 7938,
      "subdir": "linux-64",
      "timestamp": 1538887428262,
      "version": "2.0"
    },
  },
  "packages.conda": {},
  "removed": [],
  "repodata_version": 1
}'''

    def packageJson = '''{
  "arch": null,
  "build": "py_0",
  "build_number": 0,
  "depends": [
    "alembic",
    "click >=7.0",
    "cloudpickle",
    "databricks-cli >=0.8.7",
    "docker-py >=3.6.0",
    "entrypoints",
    "flask",
    "gitpython >=2.1.0",
    "gorilla",
    "gunicorn",
    "numpy",
    "pandas",
    "pip",
    "protobuf >=3.6.0",
    "python >=3.6.0",
    "python-dateutil",
    "pyyaml",
    "querystring_parser",
    "requests >=2.17.3",
    "simplejson",
    "six >=1.10.0",
    "sqlalchemy",
    "sqlparse"
  ],
  "license": "Apache 2.0",
  "license_family": "APACHE",
  "name": "mlflow",
  "noarch": "python",
  "platform": null,
  "subdir": "noarch",
  "timestamp": 1566283016272,
  "version": "1.2.0"
}'''

    String sampleJson = '''{
  "arch": null,
  "build": "py_0",
  "build_number": 0,
  "depends": [
    "alabaster",
    "invoke",
    "python",
    "python-dateutil",
    "sphinx >=1.6",
    "sphinx-automodapi",
    "werkzeug"
  ],
  "license": "MIT",
  "license_family": "MIT",
  "name": "ablog",
  "noarch": "python",
  "platform": null,
  "subdir": "noarch",
  "version": "0.9.2"
}'''
    void test_read_index_json_from_tar_bz2_archive() {
        def input = MetaDataTest.getClass().getResourceAsStream("/sample_package-1.2.0-py_0.tar.bz2")
        def indexJson = org.sonatype.nexus.repository.conda.internal.hosted.metadata.MetaData.readIndexJson(input)
        assertToString(indexJson, sampleJson)
    }

    void test_can_parse_index_json() {
        PackageDesc pack = org.sonatype.nexus.repository.conda.internal.hosted.metadata.MetaData.asIndex(packageJson)
        assert pack.build == 'py_0'
        assert pack.depends.contains('click >=7.0')
        assert pack.timestamp == 1566283016272l
    }

    void test_can_parse_repodata_json() {
        RepoData repoData = org.sonatype.nexus.repository.conda.internal.hosted.metadata.MetaData.asRepoData(repoDataJson)
        assert repoData.info.subdir == 'linux-64'

        PackageDesc scipyPackage = repoData.packages['scipy-1.1.0-py37hfa4b5c9_1.tar.bz2']
        assert scipyPackage.build == 'py37hfa4b5c9_1'
        assert scipyPackage.depends.contains('libstdcxx-ng >=7.3.0')
    }

    void test_can_parse_channel_data_json() {
        ChannelData channelData = org.sonatype.nexus.repository.conda.internal.hosted.metadata.MetaData.asChannelData(channelDataJson)
        assert channelData.channeldata_version == 1
        assert channelData.subdirs.containsAll(['linux-64', 'noarch', 'osx-64'])

        def scipy = channelData.packages['scipy']
        assert scipy.doc_url == 'http://www.scipy.org/docs.html'
        assert scipy.subdirs.containsAll(['linux-64', 'osx-64'])
    }
}
