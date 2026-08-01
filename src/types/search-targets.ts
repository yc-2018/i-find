export type SearchLaunchMode = 'schemeFirst' | 'webOnly';
export type SearchTargetIconMode = 'builtin' | 'gallery' | 'generated';

export type SearchTarget = {
  id: string;
  name: string;
  launchMode: SearchLaunchMode;
  schemeTemplate?: string;
  webFallbackTemplate?: string;
  androidPackageName?: string;
  iconMode: SearchTargetIconMode;
  iconValue: string;
  hidden: boolean;
  sortOrder: number;
};
