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
const { rspack } = require('@rspack/core');
const path = require('path');
const fs = require('fs');
const { execSync } = require('child_process');

const swcOptions = JSON.parse(fs.readFileSync(path.resolve(__dirname, '.swcrc')), 'utf-8');

let gitCommit = 'unknown';
try {
  gitCommit = execSync('git rev-parse --short=7 HEAD', { encoding: 'utf-8' }).trim();
} catch (_) { /* not in git */ }

module.exports = {
  entry: {
    'nexus-coreui-bundle': './src/frontend/src/index.js'
  },
  module: {
    rules: [
      {
        test: /\.[jt]sx?$/,
        exclude: /node_modules/,
        use: [
          {
            loader: 'builtin:swc-loader',
            options: swcOptions
          }
        ]
      },
      {
        test: /\.js$/,
        include: /node_modules[\/\\]fuse\.js/,
        use: [
          {
            loader: 'builtin:swc-loader'
          }
        ]
      },
      {
        test: /\.s?css$/,
        use: [
          {
            loader: rspack.CssExtractRspackPlugin.loader
          },
          {
            loader: 'css-loader',
            options: { url: false } // disable build-tile resolution of url() paths
          },
          {
            loader: 'sass-loader'
          }
        ]
      },
      {
        test: /\.(png)$/,
        type: 'asset',
        generator: {
          filename: 'img/[name][ext]'
        }
      },
      {
        test: /\.(ttf|eot|woff2?|svg)$/,
        type: 'asset/resource',
        generator: {
          filename: 'fonts/[name][ext]'
        }
      }
    ]
  },
  plugins: [
    new rspack.CssExtractRspackPlugin({
      filename: '[name].css',
      chunkFilename: '[name].[contenthash:8].css',
      // Suppress CSS chunk ordering warnings. These arise because nexus-ui-plugin
      // SCSS files are imported by many lazy-loaded chunks via different code paths,
      // so rspack cannot guarantee a consistent load order across chunks. The
      // warnings are harmless: our component styles are self-contained and do not
      // depend on a specific inter-file load order.
      ignoreOrder: true
    }),
    new rspack.DefinePlugin({
      __NX_BUILD_COMMIT__: JSON.stringify(gitCommit),
      // Set SONATYPE_INTERNAL=true in your environment to include internal test pages.
      // These are excluded from production customer builds when this is not set.
      // Usage: SONATYPE_INTERNAL=true yarn build-all
      __SONATYPE_INTERNAL__: JSON.stringify(process.env.SONATYPE_INTERNAL === 'true'),
    }),
    new rspack.CopyRspackPlugin({
      patterns: [{
        from: path.resolve(__dirname, '../../../../node_modules/@sonatype/react-shared-components/assets/'),
        to: path.resolve(__dirname, 'target/classes/assets')
      }]
    })
  ],
  resolve: {
    extensions: ['.js', '.jsx', '.ts', '.tsx'],
    alias: {
      '@': path.resolve(__dirname, './src/frontend/src'),
      '@nosc': path.resolve(__dirname, './src/frontend/src/nosc'),
      // Force @radix-ui/themes to always use its ESM build so that coreui's
      // own TypeScript (uses import → ESM) and nexus-ui-plugin's Babel-compiled
      // CJS (uses require → CJS) resolve to the SAME file, preventing duplicate
      // React.createContext() calls that break Theme context providers.
      '@radix-ui/themes': path.resolve(__dirname, '../../../../node_modules/@radix-ui/themes/dist/esm/index.js'),
    }
  },
  optimization: {
    splitChunks: false
  }
};
