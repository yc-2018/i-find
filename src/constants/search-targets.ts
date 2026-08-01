import type { SearchTarget } from '../types/search-targets';

export type BuiltinIconChoice = {
  key: string;
  label: string;
};

export const builtinIconChoices: BuiltinIconChoice[] = [
  { key: 'asset:douyin', label: '抖音' },
  { key: 'asset:bilibili', label: 'B 站' },
  { key: 'asset:meituan', label: '美团' },
  { key: 'asset:xhs', label: '小红书' },
  { key: 'asset:jd', label: '京东' },
  { key: 'asset:taobao', label: '淘宝' },
  { key: 'asset:pdd', label: '拼多多' },
  { key: 'mdi:magnify', label: '搜索' },
  { key: 'mdi:shopping-outline', label: '购物' },
  { key: 'mdi:food-fork-drink', label: '美食' },
  { key: 'mdi:play-circle-outline', label: '视频' },
  { key: 'mdi:note-text-outline', label: '笔记' },
  { key: 'mdi:book-open-page-variant-outline', label: '内容' },
  { key: 'mdi:earth', label: '网页' },
];

export const generatedIconPalette = [
  '#F0843D',
  '#D94827',
  '#356F58',
  '#3E6AA3',
  '#8A5C8D',
  '#B87333',
  '#8D6A45',
];

export const defaultSearchTargets: SearchTarget[] = [
  {
    id: 'douyin',
    name: '抖音',
    launchMode: 'schemeFirst',
    schemeTemplate: 'snssdk1128://search?keyword={keyword}&search_type=user',
    webFallbackTemplate: 'https://www.douyin.com/search/{keyword}',
    androidPackageName: 'com.ss.android.ugc.aweme',
    iconMode: 'builtin',
    iconValue: 'asset:douyin',
    hidden: false,
    sortOrder: 0,
  },
  {
    id: 'bilibili',
    name: 'B站',
    launchMode: 'schemeFirst',
    schemeTemplate: 'bilibili://search/?keyword={keyword}',
    webFallbackTemplate: 'https://search.bilibili.com/all?keyword={keyword}',
    androidPackageName: 'tv.danmaku.bili',
    iconMode: 'builtin',
    iconValue: 'asset:bilibili',
    hidden: false,
    sortOrder: 1,
  },
  {
    id: 'meituan',
    name: '美团',
    launchMode: 'schemeFirst',
    schemeTemplate: 'imeituan://www.meituan.com/search/result?q={keyword}',
    webFallbackTemplate: 'https://www.meituan.com/s/{keyword}',
    androidPackageName: 'com.sankuai.meituan',
    iconMode: 'builtin',
    iconValue: 'asset:meituan',
    hidden: false,
    sortOrder: 2,
  },
  {
    id: 'xiaohongshu',
    name: '小红书',
    launchMode: 'schemeFirst',
    schemeTemplate: 'xhsdiscover://search/result?keyword={keyword}',
    webFallbackTemplate: 'https://www.xiaohongshu.com/search_result?keyword={keyword}',
    androidPackageName: 'com.xingin.xhs',
    iconMode: 'builtin',
    iconValue: 'asset:xhs',
    hidden: false,
    sortOrder: 3,
  },
  {
    id: 'jd',
    name: '京东',
    launchMode: 'schemeFirst',
    schemeTemplate: 'openapp.jdmobile://virtual?params={"des":"productList","keyWord":"{keyword}","from":"search","category":"jump"}',
    webFallbackTemplate: 'https://so.m.jd.com/ware/search.action?keyword={keyword}',
    androidPackageName: 'com.jingdong.app.mall',
    iconMode: 'builtin',
    iconValue: 'asset:jd',
    hidden: false,
    sortOrder: 4,
  },
  {
    id: 'taobao',
    name: '淘宝',
    launchMode: 'schemeFirst',
    schemeTemplate: 'tbopen://m.taobao.com/tbopen/index.html?h5Url=https://s.taobao.com/search?q={keyword}',
    webFallbackTemplate: 'https://s.taobao.com/search?q={keyword}',
    androidPackageName: 'com.taobao.taobao',
    iconMode: 'builtin',
    iconValue: 'asset:taobao',
    hidden: false,
    sortOrder: 5,
  },
  {
    id: 'pdd',
    name: '拼多多',
    launchMode: 'schemeFirst',
    schemeTemplate: 'pinduoduo://com.xunmeng.pinduoduo/search_result.html?search_key={keyword}',
    webFallbackTemplate: 'https://mobile.yangkeduo.com/search_result.html?search_key={keyword}',
    androidPackageName: 'com.xunmeng.pinduoduo',
    iconMode: 'builtin',
    iconValue: 'asset:pdd',
    hidden: false,
    sortOrder: 6,
  },
  {
    id: 'baidu',
    name: '百度',
    launchMode: 'webOnly',
    webFallbackTemplate: 'https://www.baidu.com/s?wd={keyword}',
    iconMode: 'builtin',
    iconValue: 'mdi:earth',
    hidden: false,
    sortOrder: 7,
  },
];

export const builtinTargetDefaultsById = Object.fromEntries(
  defaultSearchTargets.map((target) => [target.id, target] as const)
) as Record<string, SearchTarget>;
