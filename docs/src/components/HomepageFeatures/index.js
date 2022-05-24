import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Secure',
    Svg: require('@site/static/img/lock.svg').default,
    description: (
      <>
        Supports SSL/TLS for secure connection
      </>
    ),
  },
  {
    title: 'Reliable Message Delivery',
    Svg: require('@site/static/img/mqtt-logo.svg').default,
    description: (
      <>
        Supports MQTTv3.1.1 for reliable message delivery through various QoS levels
      </>
    ),
  },
  {
    title: 'Android 12 compatible',
    Svg: require('@site/static/img/android-12.svg').default,
    description: (
      <>
        Courier library is compatible with Android 12
      </>
    ),
  },
];

function Feature({Svg, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center">
        <Svg className={styles.featureSvg} role="img" />
      </div>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
