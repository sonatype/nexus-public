<!--
  Copyright 2017-present Sonatype, Inc.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
# Copy Modules Plugin

# Table Of Contents
* [Introduction](#introduction)
* [Installation](#installation)
* [Usage](#usage)
  * [Configuration](#configuration)
* [The Fine Print](#the-fine-print)
* [Getting help](#getting-help)

## Introduction

`copy-modules-webpack-plugin` is a Webpack plugin which copies all of the source files which go into the webpack
bundle(s) to a separate directory. This enables external tools such as Sonatype Nexus Lifecycle to analyze only those
source files which will be included in the final bundle.

### N.B. Since version 2.0 the plugin is only compatible with webpack 4.0 and later. Use 1.x releases for webpack 3.

## Installation

```
npm install --save-dev copy-modules-webpack-plugin
```

## Usage

Instantiate the plugin with the desired configuration options and include it in the `plugins` array of your webpack configuration:

```
const CopyModulesPlugin = require("copy-modules-webpack-plugin");

module.exports = {
  ...
  plugins: [
    new CopyModulesPlugin({
      destination: 'webpack-modules'
    })
  ]
}
```

### Configuration

`copy-modules-webpack-plugin` currently supports a single configuration option:

<dl>
  <dt>destination</dt>
  <dd>
    The destination directory where the modules will be copied.
  </dd>
</dl>

## The Fine Print

It is worth noting that this is **NOT SUPPORTED** by Sonatype, and is a contribution of ours
to the open source community (read: you!)

Remember:

* Use this contribution at the risk tolerance that you have
* Do NOT file Sonatype support tickets related to Webpack support
* DO file issues here on GitHub, so that the community can pitch in

Phew, that was easier than I thought. Last but not least of all:

Have fun creating and using this plugin, we are glad to have you here!

## Getting help

Looking to contribute to our code but need some help? There's a few ways to get information:

* Chat with us on [Gitter](https://gitter.im/sonatype/nexus-developers)
* Connect with [@sonatypeDev](https://twitter.com/sonatypedev) on Twitter
