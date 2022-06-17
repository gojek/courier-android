// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Courier Android',
  tagline: 'Information Superhighway',
  url: 'https://your-docusaurus-test-site.com',
  baseUrl: '/courier-android/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/courier-logo.ico',

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: 'gojek', // Usually your GitHub org/user name.
  projectName: 'courier-android', // Usually your repo name.

  // Even if you don't use internalization, you can use this field to set useful
  // metadata like html lang. For example, if your site is Chinese, you may want
  // to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          // Remove this to remove the "edit this page" links.
          editUrl:
            'https://github.com/gojek/courier-android/edit/main/docs/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: 'Courier',
        logo: {
          alt: 'Courier',
          src: 'img/courier-logo.svg',
        },
        items: [
          {
            type: 'doc',
            position: 'left',
            docId: 'Introduction',
            label: 'Docs',
          },
          {
            type: 'doc',
            position: 'left',
            docId: 'AdaptiveKeepAlive',
            label: 'Features',
          },
          {
            type: 'doc',
            position: 'left',
            docId: 'ConnectionSetup',
            label: 'Guides',
          },
          {
            href: 'https://github.com/gojek/courier-android',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              { label: 'Getting Started', to: '/docs/Installation' },
              { label: 'Guides', to: '/docs/ConnectionSetup' },
              { label: 'Features', to: '/docs/AdaptiveKeepAlive' },
            ],
          },
          {
            title: 'Community',
            items: [
              { label: 'Gojek open source', href: 'https://github.com/gojek/', },
              { label: 'Discord', href: 'https://discord.gg/C823qK4AK7', },
              { label: 'Twitter', href: 'https://twitter.com/gojektech', },
            ],
          },
          {
            title: 'More',
            items: [
              { label: 'Courier', href: 'https://gojek.github.io/courier/', },
              { label: 'E2E example', href: 'https://gojek.github.io/courier/docs/Introduction', },
              { label: 'Blogs', href: 'https://gojek.github.io/courier/blog', },
              { label: 'Github', href: 'https://github.com/gojek/courier-android', },
            ],
          },
        ],
        logo: {
          alt: 'Gojek Open Source Logo',
          src: 'img/gojek-logo-white.png',
          width: 250,
          height: 50,
          href: 'https://github.com/gojek/',
        },
        copyright: `Copyright © ${new Date().getFullYear()} Gojek`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: ['kotlin'],
      },
    }),
};

module.exports = config;
