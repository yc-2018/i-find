package com.cgl.ifind.data

data class BuiltinIconChoice(
  val key: String,
  val label: String
)

object DefaultTargets {
  val builtinIconChoices = listOf(
    BuiltinIconChoice("asset:douyin", "抖音"),
    BuiltinIconChoice("asset:bilibili", "B 站"),
    BuiltinIconChoice("asset:meituan", "美团"),
    BuiltinIconChoice("asset:xhs", "小红书"),
    BuiltinIconChoice("asset:jd", "京东"),
    BuiltinIconChoice("asset:taobao", "淘宝"),
    BuiltinIconChoice("asset:pdd", "拼多多"),
    BuiltinIconChoice("builtin:search", "搜索"),
    BuiltinIconChoice("builtin:shopping", "购物"),
    BuiltinIconChoice("builtin:play", "视频"),
    BuiltinIconChoice("builtin:note", "笔记"),
    BuiltinIconChoice("builtin:web", "网页")
  )

  fun create(): List<SearchTarget> = listOf(
    SearchTarget(
      id = "douyin",
      name = "抖音",
      primaryTemplate = "snssdk1128://search?keyword={keyword}&search_type=user",
      fallbackTemplate = "https://www.douyin.com/search/{keyword}",
      androidPackageName = "com.ss.android.ugc.aweme",
      iconMode = IconModes.BUILTIN,
      iconValue = "asset:douyin",
      hidden = false,
      sortOrder = 0
    ),
    SearchTarget(
      id = "bilibili",
      name = "B 站",
      primaryTemplate = "bilibili://search/?keyword={keyword}",
      fallbackTemplate = "https://search.bilibili.com/all?keyword={keyword}",
      androidPackageName = "tv.danmaku.bili",
      iconMode = IconModes.BUILTIN,
      iconValue = "asset:bilibili",
      hidden = false,
      sortOrder = 1
    ),
    SearchTarget(
      id = "meituan",
      name = "美团",
      primaryTemplate = "imeituan://www.meituan.com/search/result?q={keyword}",
      fallbackTemplate = "https://www.meituan.com/s/{keyword}",
      androidPackageName = "com.sankuai.meituan",
      iconMode = IconModes.BUILTIN,
      iconValue = "asset:meituan",
      hidden = false,
      sortOrder = 2
    ),
    SearchTarget(
      id = "xiaohongshu",
      name = "小红书",
      primaryTemplate = "xhsdiscover://search/result?keyword={keyword}",
      fallbackTemplate = "https://www.xiaohongshu.com/search_result?keyword={keyword}",
      androidPackageName = "com.xingin.xhs",
      iconMode = IconModes.BUILTIN,
      iconValue = "asset:xhs",
      hidden = false,
      sortOrder = 3
    ),
    SearchTarget(
      id = "jd",
      name = "京东",
      primaryTemplate = "openapp.jdmobile://virtual?params={\"des\":\"productList\",\"keyWord\":\"{keyword}\",\"from\":\"search\",\"category\":\"jump\"}",
      fallbackTemplate = "https://so.m.jd.com/ware/search.action?keyword={keyword}",
      androidPackageName = "com.jingdong.app.mall",
      iconMode = IconModes.BUILTIN,
      iconValue = "asset:jd",
      hidden = false,
      sortOrder = 4
    ),
    SearchTarget(
      id = "taobao",
      name = "淘宝",
      primaryTemplate = "tbopen://m.taobao.com/tbopen/index.html?h5Url=https://s.taobao.com/search?q={keyword}",
      fallbackTemplate = "https://s.taobao.com/search?q={keyword}",
      androidPackageName = "com.taobao.taobao",
      iconMode = IconModes.BUILTIN,
      iconValue = "asset:taobao",
      hidden = false,
      sortOrder = 5
    ),
    SearchTarget(
      id = "pdd",
      name = "拼多多",
      primaryTemplate = "pinduoduo://com.xunmeng.pinduoduo/search_result.html?search_key={keyword}",
      fallbackTemplate = "https://mobile.yangkeduo.com/search_result.html?search_key={keyword}",
      androidPackageName = "com.xunmeng.pinduoduo",
      iconMode = IconModes.BUILTIN,
      iconValue = "asset:pdd",
      hidden = false,
      sortOrder = 6
    ),
    SearchTarget(
      id = "baidu",
      name = "百度",
      primaryTemplate = "https://www.baidu.com/s?wd={keyword}",
      iconMode = IconModes.BUILTIN,
      iconValue = "builtin:web",
      hidden = false,
      sortOrder = 7
    )
  )
}
