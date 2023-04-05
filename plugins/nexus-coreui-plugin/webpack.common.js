/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
const CopyModulesPlugin = require('copy-modules-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const path = require('path');
const {NormalModuleReplacementPlugin} = require('webpack');

module.exports = {
  entry: {
    'nexus-coreui-bundle': './src/frontend/src/index.js'
  },
  module: {
    rules: [
      {
        /*
        Required to fix a problem with the babel dependencies
          Module not found: Error: Can't resolve './nonIterableRest' in '/Users/mmartz/Code/nexus-internal/node_modules/@babel/runtime/helpers/esm'
          Did you mean 'nonIterableRest.js'?
        */
        test: /\.m?js/,
        resolve: {
          fullySpecified: false
        }
      },
      {
        test: /\.jsx?$/,
        exclude: /node_modules/,
        use: [
          {
            loader: 'babel-loader'
          }
        ]
      },
      {
        test: /\.js$/,
        include: /node_modules[\/\\]fuse\.js/,
        use: [
          {
            loader: 'babel-loader'
          }
        ]
      },
      {
        test: /\.s?css$/,
        use: [
          {
            loader: MiniCssExtractPlugin.loader
          },
          {
            loader: 'css-loader',
            options: { url: false } // disable build-tile resolution of url() paths
          },
          'sass-loader'
        ]
      },
      {
        test: /\.(png)$/,
        type: 'asset',
        generator: {
          filename: 'img/[name].[ext]'
        }
      },
      {
        test: /\.(ttf|eot|woff2?|svg)$/,
        type: 'asset',
        generator: {
          filename: 'fonts/[name].[ext]'
        }
      }
    ]
  },
  plugins: [
    new CopyModulesPlugin({
      destination: path.resolve(__dirname, 'target', 'webpack-modules')
    }),
    new MiniCssExtractPlugin({
      filename: '[name].css'
    }),
    new NormalModuleReplacementPlugin(
        // Replace scss from the RSC library with empty css since it's already included in nexus-rapture
        // make sure to use path.sep to cover varying OS's
        /.*@sonatype.*\.s?css/,
        function(resource) {
          const RSC_INDEX = resource.request.indexOf(
              'node_modules' + path.sep + '@sonatype' + path.sep + 'react-shared-components');

          resource.request = resource.request.substring(0, RSC_INDEX) + 'buildsupport' + path.sep + 'ui' +
              path.sep + 'empty.scss';
        }
    )
  ],
  resolve: {
    extensions: ['.js', '.jsx']
  },
  externals: {
    axios: 'axios',
    luxon: 'luxon',
    '@sonatype/nexus-ui-plugin': 'nxrmUiPlugin',
    '@sonatype/react-shared-components': 'rsc',
    react: 'react',
    xstate: 'xstate'
  }
};
