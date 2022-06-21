import React from 'react';
import clsx from 'clsx';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import styles from './index.module.css';
import HomepageFeatures from '@site/src/components/HomepageFeatures';
import useBaseUrl from '@docusaurus/useBaseUrl';

const HomepageHeader = () => {
  const {siteConfig} = useDocusaurusContext();
  return (
      <div className="homeHero">
        <div className="container banner">
          <div className="row">
            <div className="innerRow">
              <div className="homeTitle">{siteConfig.title}</div>
              <small className="homeSubTitle">{siteConfig.tagline}</small>
              <a className="button" href="docs/Introduction">Documentation</a>
            </div>
          </div>
        </div>
      </div >
    );
};

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout title={`${siteConfig.title}`}>
      <HomepageHeader />
      <div id="docs">
        <HomepageFeatures />
      </div>
    </Layout>
  );
}
