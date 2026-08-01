const { withAndroidManifest } = require('@expo/config-plugins');

const SHIZUKU_PACKAGE_NAME = 'moe.shizuku.privileged.api';
const SHIZUKU_PROVIDER_NAME = 'rikka.shizuku.ShizukuProvider';

function ensurePackageQuery(androidManifest, packageName) {
  if (!androidManifest.manifest.queries) {
    androidManifest.manifest.queries = [{}];
  }

  if (androidManifest.manifest.queries.length === 0) {
    androidManifest.manifest.queries.push({});
  }

  const queriesRoot = androidManifest.manifest.queries[0];
  const packageEntries = queriesRoot.package ?? [];
  const hasPackageQuery = packageEntries.some(
    (entry) => entry?.$?.['android:name'] === packageName
  );

  if (!hasPackageQuery) {
    packageEntries.push({
      $: {
        'android:name': packageName,
      },
    });
  }

  queriesRoot.package = packageEntries;
}

function ensureShizukuProvider(androidManifest) {
  if (!androidManifest.manifest.application?.length) {
    return;
  }

  const mainApplication = androidManifest.manifest.application[0];
  const providerEntries = mainApplication.provider ?? [];
  const existingProvider = providerEntries.find(
    (entry) => entry?.$?.['android:name'] === SHIZUKU_PROVIDER_NAME
  );

  const providerAttributes = {
    'android:name': SHIZUKU_PROVIDER_NAME,
    'android:authorities': '${applicationId}.shizuku',
    'android:enabled': 'true',
    'android:exported': 'true',
    'android:multiprocess': 'false',
    'android:permission': 'android.permission.INTERACT_ACROSS_USERS_FULL',
  };

  if (existingProvider) {
    existingProvider.$ = {
      ...existingProvider.$,
      ...providerAttributes,
    };
  } else {
    providerEntries.push({
      $: providerAttributes,
    });
  }

  mainApplication.provider = providerEntries;
}

module.exports = function withStopappQueries(config) {
  return withAndroidManifest(config, (modConfig) => {
    ensurePackageQuery(modConfig.modResults, SHIZUKU_PACKAGE_NAME);
    ensureShizukuProvider(modConfig.modResults);
    return modConfig;
  });
};
