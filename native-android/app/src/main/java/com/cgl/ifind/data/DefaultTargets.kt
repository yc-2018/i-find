package com.cgl.ifind.data

object DefaultTargets {
  fun create(): List<SearchTarget> = listOf(
    SearchTarget(
      id = "douyin",
      name = "抖音",
      primaryTemplate = "snssdk1128://search?keyword={keyword}&search_type=user",
      fallbackTemplate = "https://www.douyin.com/search/{keyword}",
      androidPackageName = "com.ss.android.ugc.aweme",
      iconMode = IconModes.BUILTIN,
      iconValue = "douyin.png",
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
      iconValue = "bilibili.png",
      hidden = false,
      sortOrder = 2
    ),
    SearchTarget(
      id = "meituan",
      name = "美团",
      primaryTemplate = "imeituan://www.meituan.com/search/result?q={keyword}",
      fallbackTemplate = "https://www.meituan.com/s/{keyword}",
      androidPackageName = "com.sankuai.meituan",
      iconMode = IconModes.BUILTIN,
      iconValue = "meituan.png",
      hidden = false,
      sortOrder = 3
    ),
    SearchTarget(
      id = "xiaohongshu",
      name = "小红书",
      primaryTemplate = "xhsdiscover://search/result?keyword={keyword}",
      fallbackTemplate = "https://www.xiaohongshu.com/search_result?keyword={keyword}",
      androidPackageName = "com.xingin.xhs",
      iconMode = IconModes.BUILTIN,
      iconValue = "xhs.png",
      hidden = false,
      sortOrder = 4
    ),
    SearchTarget(
      id = "jd",
      name = "京东",
      primaryTemplate = "openapp.jdmobile://virtual?params={\"des\":\"productList\",\"keyWord\":\"{keyword}\",\"from\":\"search\",\"category\":\"jump\"}",
      fallbackTemplate = "https://so.m.jd.com/ware/search.action?keyword={keyword}",
      androidPackageName = "com.jingdong.app.mall",
      iconMode = IconModes.BUILTIN,
      iconValue = "jd.png",
      hidden = false,
      sortOrder = 5
    ),
    SearchTarget(
      id = "taobao",
      name = "淘宝",
      primaryTemplate = "tbopen://m.taobao.com/tbopen/index.html?h5Url=https://s.taobao.com/search?q={keyword}",
      fallbackTemplate = "https://s.taobao.com/search?q={keyword}",
      androidPackageName = "com.taobao.taobao",
      iconMode = IconModes.BUILTIN,
      iconValue = "taobao.png",
      hidden = false,
      sortOrder = 6
    ),
    SearchTarget(
      id = "pdd",
      name = "拼多多",
      primaryTemplate = "pinduoduo://com.xunmeng.pinduoduo/search_result.html?search_key={keyword}",
      fallbackTemplate = "https://mobile.yangkeduo.com/search_result.html?search_key={keyword}",
      androidPackageName = "com.xunmeng.pinduoduo",
      iconMode = IconModes.BUILTIN,
      iconValue = "pdd.png",
      hidden = false,
      sortOrder = 8
    ),
    SearchTarget(
      id = "baidu",
      name = "百度",
      primaryTemplate = "https://www.baidu.com/s?wd={keyword}",
      iconMode = IconModes.BUILTIN,
      iconValue = "web.svg",
      hidden = false,
      sortOrder = 12
    ),
    builtinTarget(
      id = "zhihu",
      name = "知乎",
      packageName = "com.zhihu.android",
      iconValue = "zhihu.svg",
      primaryTemplate = "zhihu://search?query={keyword}",
      sortOrder = 10
    ),
    builtinTarget(
      id = "weibo",
      name = "微博",
      packageName = "com.sina.weibo",
      iconValue = "weibo.svg",
      primaryTemplate = "sinaweibo://searchall?q={keyword}",
      sortOrder = 11
    ),
    builtinTarget(
      id = "ctrip",
      name = "携程",
      packageName = "ctrip.android.view",
      iconValue = "ctrip.svg",
      primaryTemplate = "ctrip://wireless/search?keyword={keyword}",
      sortOrder = 15
    ),
    installedAppTarget(
      id = "douban",
      name = "豆瓣",
      packageName = "com.douban.frodo",
      primaryTemplate = "douban://douban.com/search?q={keyword}",
      sortOrder = 16
    ),
    installedAppTarget(
      id = "tmall",
      name = "天猫",
      packageName = "com.tmall.wireless",
      primaryTemplate = "tmall://page.tm/search?q={keyword}",
      sortOrder = 20
    ),
    builtinTarget(
      id = "xianyu",
      name = "闲鱼",
      packageName = "com.taobao.idlefish",
      iconValue = "fish.svg",
      primaryTemplate = "fleamarket://x_search_items?keyword={keyword}",
      sortOrder = 7,
      hidden = false
    ),
    installedAppTarget(
      id = "dianping",
      name = "大众点评",
      packageName = "com.dianping.v1",
      primaryTemplate = "dianping://shoplist?keyword={keyword}",
      sortOrder = 22
    ),
    builtinTarget(
      id = "netease_music",
      name = "网易云音乐",
      packageName = "com.netease.cloudmusic",
      iconValue = "网易云音乐.svg",
      primaryTemplate = "orpheus://search?key={keyword}",
      sortOrder = 13
    ),
    builtinTarget(
      id = "kuaishou",
      name = "快手",
      packageName = "com.smile.gifmaker",
      iconValue = "kuaishou.svg",
      primaryTemplate = "kwai://search?keyword={keyword}",
      sortOrder = 1
    ),
    installedAppTarget(
      id = "toutiao",
      name = "今日头条",
      packageName = "com.ss.android.article.news",
      primaryTemplate = "snssdk143://search?keyword={keyword}",
      sortOrder = 17
    ),
    installedAppTarget(
      id = "xiaoyuzhou",
      name = "小宇宙",
      packageName = "app.podcast.cosmos",
      primaryTemplate = "cosmos://page.cos/search?keyword={keyword}",
      sortOrder = 19
    ),
    installedAppTarget(
      id = "instagram",
      name = "Instagram",
      packageName = "com.instagram.android",
      primaryTemplate = "instagram://tag?name={keyword}",
      sortOrder = 24
    ),
    installedAppTarget(
      id = "spotify",
      name = "Spotify",
      packageName = "com.spotify.music",
      primaryTemplate = "spotify://search:{keyword}",
      sortOrder = 27
    ),
    installedAppTarget(
      id = "twitter",
      name = "X",
      packageName = "com.twitter.android",
      primaryTemplate = "x://search?query={keyword}&src=typed_query",
      sortOrder = 25
    ),
    installedAppTarget(
      id = "meituanwaimai",
      name = "美团外卖",
      packageName = "com.sankuai.meituan.takeoutnew",
      primaryTemplate = "meituanwaimai://waimai.meituan.com/search?query={keyword}",
      sortOrder = 21
    ),
    installedAppTarget(
      id = "weibointernational",
      name = "微博国际版",
      packageName = "com.weico.international",
      primaryTemplate = "weibointernational://searchall?q={keyword}",
      sortOrder = 18
    ),
    installedAppTarget(
      id = "amazon",
      name = "亚马逊",
      packageName = "com.amazon.mShop.android.shopping",
      primaryTemplate = "com.amazon.mobile.shopping.web://amazon.com/s?k={keyword}",
      sortOrder = 28
    ),
    installedAppTarget(
      id = "via_browser",
      name = "Via 浏览器",
      packageName = "mark.via",
      primaryTemplate = "via://search?q={keyword}",
      sortOrder = 23
    ),
    installedAppTarget(
      id = "bing",
      name = "Bing",
      packageName = "com.microsoft.bing",
      primaryTemplate = "sapphire://search?query={keyword}",
      sortOrder = 30
    ),
    installedAppTarget(
      id = "google",
      name = "Google",
      packageName = "com.google.android.googlequicksearchbox",
      primaryTemplate = "https://www.google.com/search?q={keyword}",
      sortOrder = 29
    ),
    builtinTarget(
      id = "amap",
      name = "高德地图",
      packageName = "com.autonavi.minimap",
      iconValue = "高德地图.svg",
      primaryTemplate = "amapuri://search/general?keyword={keyword}",
      sortOrder = 9
    ),
    builtinTarget(
      id = "chatgpt",
      name = "ChatGPT",
      packageName = "com.openai.chatgpt",
      iconValue = "ChatGPT.svg",
      primaryTemplate = "https://chat.openai.com/?q={keyword}",
      fallbackTemplate = "https://chat.openai.com/?q={keyword}&temporary=true",
      sortOrder = 14
    ),
    installedAppTarget(
      id = "youtube",
      name = "YouTube",
      packageName = "com.google.android.youtube",
      primaryTemplate = "youtube_media_search://{keyword}",
      fallbackTemplate = "youtube://results?search_query={keyword}",
      sortOrder = 26
    )
  ).sortedBy { it.sortOrder }

  private fun installedAppTarget(
    id: String,
    name: String,
    packageName: String,
    primaryTemplate: String,
    sortOrder: Int,
    fallbackTemplate: String? = null
  ) = SearchTarget(
    id = id,
    name = name,
    primaryTemplate = primaryTemplate,
    fallbackTemplate = fallbackTemplate,
    androidPackageName = packageName,
    iconMode = IconModes.INSTALLED_APP,
    iconValue = packageName,
    hidden = true,
    sortOrder = sortOrder
  )

  private fun builtinTarget(
    id: String,
    name: String,
    packageName: String,
    iconValue: String,
    primaryTemplate: String,
    sortOrder: Int,
    fallbackTemplate: String? = null,
    hidden: Boolean = true
  ) = SearchTarget(
    id = id,
    name = name,
    primaryTemplate = primaryTemplate,
    fallbackTemplate = fallbackTemplate,
    androidPackageName = packageName,
    iconMode = IconModes.BUILTIN,
    iconValue = iconValue,
    hidden = hidden,
    sortOrder = sortOrder
  )
}
