import { Linking } from 'react-native';

import { generatedIconPalette } from '../constants/search-targets';
import type {
  SearchLaunchMode,
  SearchTarget,
  SearchTargetIconMode,
} from '../types/search-targets';

type NormalizeDraftInput = {
  name: string;
  launchMode: SearchLaunchMode;
  schemeTemplate?: string;
  webFallbackTemplate?: string;
  androidPackageName?: string;
  iconMode: SearchTargetIconMode;
  iconValue: string;
};

export type OpenSearchTargetResult = {
  opened: boolean;
  openedVia: 'scheme' | 'web' | null;
  schemeAttempted: boolean;
  schemeFailed: boolean;
  webFallbackUrl?: string;
  message: string;
};

const androidPackageNamePattern = /^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/;

export function buildSearchUrl(template: string, keyword: string) {
  return template.replaceAll('{keyword}', encodeURIComponent(keyword));
}

export function sortTargetsBySortOrder(targets: SearchTarget[]) {
  return [...targets].sort((left, right) => left.sortOrder - right.sortOrder);
}

export function normalizeSortOrder(targets: SearchTarget[]) {
  return targets.map((target, index) => ({
    ...target,
    sortOrder: index,
  }));
}

export function createTargetId() {
  return `target-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function resolveGeneratedColor(value: string) {
  if (generatedIconPalette.includes(value)) {
    return value;
  }

  const hash = Array.from(value).reduce((total, character) => total + character.charCodeAt(0), 0);
  return generatedIconPalette[hash % generatedIconPalette.length];
}

export function normalizeEditableTarget(input: NormalizeDraftInput) {
  const name = input.name.trim();
  const schemeTemplate = input.schemeTemplate?.trim() ?? '';
  const webFallbackTemplate = input.webFallbackTemplate?.trim() ?? '';
  const androidPackageName = input.androidPackageName?.trim() ?? '';

  if (!name) {
    throw new Error('\u641c\u7d22\u9879\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a\u3002');
  }

  if (!schemeTemplate && !webFallbackTemplate) {
    throw new Error('\u81f3\u5c11\u586b\u5199\u4e00\u4e2a\u641c\u7d22\u6a21\u677f\u3002');
  }

  if (androidPackageName && !androidPackageNamePattern.test(androidPackageName)) {
    throw new Error('Android \u5305\u540d\u683c\u5f0f\u4e0d\u6b63\u786e\uff0c\u8bf7\u68c0\u67e5\u540e\u518d\u4fdd\u5b58\u3002');
  }

  const launchMode: SearchLaunchMode =
    schemeTemplate && input.launchMode === 'schemeFirst' ? 'schemeFirst' : 'webOnly';
  const iconMode: SearchTargetIconMode = input.iconMode;
  const iconValue =
    iconMode === 'generated'
      ? resolveGeneratedColor(input.iconValue || name)
      : input.iconValue;

  return {
    name,
    launchMode,
    schemeTemplate: schemeTemplate || undefined,
    webFallbackTemplate: webFallbackTemplate || undefined,
    androidPackageName: androidPackageName || undefined,
    iconMode,
    iconValue,
  };
}

export async function openExternalUrl(url: string) {
  try {
    await Linking.openURL(url);
    return true;
  } catch {
    return false;
  }
}

export async function openSearchTarget(
  target: SearchTarget,
  keyword: string
): Promise<OpenSearchTargetResult> {
  const schemeUrl =
    target.schemeTemplate && target.launchMode === 'schemeFirst'
      ? buildSearchUrl(target.schemeTemplate, keyword)
      : null;
  const webUrl = target.webFallbackTemplate
    ? buildSearchUrl(target.webFallbackTemplate, keyword)
    : null;

  if (schemeUrl) {
    try {
      await Linking.openURL(schemeUrl);
      return {
        opened: true,
        openedVia: 'scheme',
        schemeAttempted: true,
        schemeFailed: false,
        message: '',
      };
    } catch {
      if (!webUrl) {
        return {
          opened: false,
          openedVia: null,
          schemeAttempted: true,
          schemeFailed: true,
          message:
            '\u6ca1\u6709\u53ef\u7528\u7684\u7f51\u9875\u56de\u9000\u5730\u5740\uff0c\u8bf7\u68c0\u67e5\u641c\u7d22\u9879\u914d\u7f6e\u3002',
        };
      }

      return {
        opened: false,
        openedVia: null,
        schemeAttempted: true,
        schemeFailed: true,
        webFallbackUrl: webUrl,
        message: '\u6ca1\u6709\u6210\u529f\u6253\u5f00\u76ee\u6807\u5e94\u7528\u3002',
      };
    }
  }

  if (webUrl) {
    const opened = await openExternalUrl(webUrl);

    return opened
      ? {
          opened: true,
          openedVia: 'web',
          schemeAttempted: false,
          schemeFailed: false,
          webFallbackUrl: webUrl,
          message: '',
        }
      : {
          opened: false,
          openedVia: null,
          schemeAttempted: false,
          schemeFailed: false,
          webFallbackUrl: webUrl,
          message: '\u7f51\u9875\u641c\u7d22\u6ca1\u6709\u6210\u529f\u6253\u5f00\uff0c\u8bf7\u7a0d\u540e\u518d\u8bd5\u3002',
        };
  }

  return {
    opened: false,
    openedVia: null,
    schemeAttempted: false,
    schemeFailed: false,
    message: '\u8fd9\u4e2a\u641c\u7d22\u9879\u7f3a\u5c11\u53ef\u7528\u94fe\u63a5\uff0c\u8bf7\u5230\u914d\u7f6e\u9875\u4fee\u590d\u3002',
  };
}
