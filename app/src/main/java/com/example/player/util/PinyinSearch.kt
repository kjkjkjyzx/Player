package com.example.player.util

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination

/**
 * 拼音搜索工具。
 *
 * 支持三种命中方式（取 OR）：
 *  1. 原文 contains（忽略大小写）—— 兼容全英文或本身含纯拼音的文件名
 *  2. 全拼连接 contains —— "春节晚会" → "chunjiewanhui"
 *  3. 拼音首字母连接 contains —— "春节晚会" → "cjwh"
 *
 * 只对 query 全为 ASCII 时触发拼音匹配；含中文的 query 走原文 contains。
 *
 * 依赖：Pinyin4j（com.belerweb:pinyin4j:2.5.1）—— 输出无声调小写拼音；
 * 对多音字取第一个读音（搜索场景下可接受）。非汉字字符原样保留。
 */
object PinyinSearch {

    private val outputFormat: HanyuPinyinOutputFormat = HanyuPinyinOutputFormat().apply {
        caseType  = HanyuPinyinCaseType.LOWERCASE
        toneType  = HanyuPinyinToneType.WITHOUT_TONE   // "chūn" → "chun"
        vCharType = HanyuPinyinVCharType.WITH_V        // "绿" → "lv"
    }

    /** 判断 [name] 是否命中 [rawQuery]。 */
    fun matches(name: String, rawQuery: String): Boolean {
        val q = rawQuery.trim()
        if (q.isEmpty()) return false

        // 原文匹配（大小写不敏感）
        if (name.contains(q, ignoreCase = true)) return true

        // query 含非 ASCII 字符时，不做拼音转换
        if (!q.all { it.code < 128 }) return false

        val full     = buildFullPinyin(name)
        val initials = buildPinyinInitials(name)
        val qLower   = q.lowercase()
        return full.contains(qLower) || initials.contains(qLower)
    }

    /** 输出文件名全拼（小写、无分隔）。 */
    private fun buildFullPinyin(name: String): String {
        val sb = StringBuilder(name.length * 4)
        for (ch in name) {
            val py = toPinyinOrNull(ch)
            if (py != null) sb.append(py)
            else sb.append(ch.lowercaseChar())
        }
        return sb.toString()
    }

    /** 输出文件名拼音首字母（小写、无分隔）。非汉字原样保留。 */
    private fun buildPinyinInitials(name: String): String {
        val sb = StringBuilder(name.length)
        for (ch in name) {
            val py = toPinyinOrNull(ch)
            if (py != null) {
                if (py.isNotEmpty()) sb.append(py[0])
            } else {
                sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    /** 汉字 → 小写拼音（多音字取第一个读音）；非汉字返回 null。 */
    private fun toPinyinOrNull(ch: Char): String? {
        if (ch.code !in 0x4E00..0x9FFF) return null
        return try {
            val arr = PinyinHelper.toHanyuPinyinStringArray(ch, outputFormat)
            arr?.firstOrNull()
        } catch (_: BadHanyuPinyinOutputFormatCombination) {
            null
        }
    }
}
