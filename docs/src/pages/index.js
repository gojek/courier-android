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
        <div className="logo"><img src={useBaseUrl('img/pattern.svg')} /></div>
        <div className="container banner">
          <div className="row">
            <div className={clsx('col col--5')}>
              <div className="homeTitle">{siteConfig.title}</div>
              <small className="homeSubTitle">{siteConfig.tagline}</small>
              <a className="button" href="docs/Introduction">Documentation</a>
            </div>
            <div className={clsx('col col--1')}></div>
            <div className={clsx('col col--6')}>
              <div className="text--right"><img src={useBaseUrl('img/courier.png')} /></div>
            </div>
          </div>
        </div>
      </div >
    );
};

export default function Home() {
  const {siteConfig} = useDocusaurusContext();
  return (
    <Layout
      title={`Hello from ${siteConfig.title}`}
      description="Description will go into a meta tag in <head />">
      <HomepageHeader />
    </Layout>
  );
}
