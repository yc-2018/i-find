package com.cgl.ifind.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultTargetsTest {
  @Test
  fun defaultsFollowTheCuratedDisplayOrder() {
    val targets = DefaultTargets.create()

    assertEquals(
      listOf(
        "douyin",
        "kuaishou",
        "bilibili",
        "meituan",
        "xiaohongshu",
        "jd",
        "taobao",
        "xianyu",
        "pdd",
        "amap",
        "zhihu",
        "weibo",
        "baidu",
        "netease_music",
        "chatgpt",
        "ctrip",
        "douban",
        "toutiao",
        "weibointernational",
        "xiaoyuzhou",
        "tmall",
        "meituanwaimai",
        "dianping",
        "via_browser",
        "instagram",
        "twitter",
        "youtube",
        "spotify",
        "amazon",
        "google",
        "bing"
      ),
      targets.map { it.id }
    )
    assertEquals(targets.indices.toList(), targets.map { it.sortOrder })
    assertEquals(false, targets.single { it.id == "xianyu" }.hidden)
    assertEquals(
      mapOf(
        "amap" to "高德地图.svg",
        "zhihu" to "zhihu.svg",
        "netease_music" to "网易云音乐.svg"
      ),
      targets
        .filter { it.id in setOf("amap", "zhihu", "netease_music") }
        .associate { it.id to it.iconValue }
    )
    assertEquals(
      setOf(IconModes.BUILTIN),
      targets
        .filter { it.id in setOf("amap", "zhihu", "netease_music") }
        .mapTo(hashSetOf()) { it.iconMode }
    )
  }
}
