package com.lexiflow.wordbook.data

object SeedWords {
    val books = listOf(
        BookEntity(101, "大学英语四级", "CET-4", true),
        BookEntity(102, "大学英语六级", "CET-6"),
        BookEntity(103, "考研英语", "考研核心"),
        BookEntity(104, "雅思词汇", "IELTS"),
        BookEntity(105, "托福词汇", "TOEFL")
    )

    val all = listOf(
        WordEntity(1, "serendipity", "/ˌserənˈdɪpəti/", "n. 意外发现美好事物的运气", "Finding this quiet bookstore was pure serendipity.", "发现这家安静的书店，纯属意外之喜。"),
        WordEntity(2, "resilient", "/rɪˈzɪliənt/", "adj. 有韧性的；能迅速恢复的", "She remained resilient after several setbacks.", "经历数次挫折后，她依然坚韧。"),
        WordEntity(3, "elaborate", "/ɪˈlæbərət/", "adj. 精心制作的；复杂的", "They prepared an elaborate plan for the project.", "他们为这个项目准备了一份周密的计划。"),
        WordEntity(4, "inevitable", "/ɪnˈevɪtəbl/", "adj. 不可避免的；必然发生的", "Change is inevitable, but growth is a choice.", "改变不可避免，成长却是一种选择。"),
        WordEntity(5, "subtle", "/ˈsʌtl/", "adj. 微妙的；不易察觉的", "There was a subtle change in his tone.", "他的语气有了微妙的变化。"),
        WordEntity(6, "contemplate", "/ˈkɒntəmpleɪt/", "v. 深思；考虑", "He sat quietly to contemplate his next move.", "他安静地坐着，思考下一步行动。"),
        WordEntity(7, "profound", "/prəˈfaʊnd/", "adj. 深刻的；意义深远的", "The book had a profound influence on her.", "这本书对她产生了深远影响。"),
        WordEntity(8, "meticulous", "/məˈtɪkjələs/", "adj. 一丝不苟的；缜密的", "She keeps meticulous records of every experiment.", "她一丝不苟地记录每次实验。"),
        WordEntity(9, "ambiguous", "/æmˈbɪɡjuəs/", "adj. 模棱两可的；含糊的", "The ending of the film is deliberately ambiguous.", "电影的结局被刻意处理得模棱两可。"),
        WordEntity(10, "endeavor", "/ɪnˈdevər/", "n./v. 努力；尽力", "Learning a language is a lifelong endeavor.", "学习一门语言是一生的努力。"),
        WordEntity(11, "tranquil", "/ˈtræŋkwɪl/", "adj. 宁静的；平静的", "The lake was tranquil in the early morning.", "清晨的湖面十分宁静。"),
        WordEntity(12, "versatile", "/ˈvɜːrsətl/", "adj. 多才多艺的；多用途的", "This versatile tool can handle many tasks.", "这个多用途工具能处理许多任务。"),
        WordEntity(13, "diligent", "/ˈdɪlɪdʒənt/", "adj. 勤奋的；用功的", "Diligent practice leads to steady improvement.", "勤奋练习会带来稳定的进步。"),
        WordEntity(14, "coherent", "/koʊˈhɪrənt/", "adj. 连贯的；有条理的", "Please present your ideas in a coherent way.", "请有条理地表达你的想法。"),
        WordEntity(15, "intricate", "/ˈɪntrɪkət/", "adj. 错综复杂的；精细的", "The watch contains an intricate mechanism.", "这块手表内部有精密复杂的机械结构。"),
        WordEntity(16, "spontaneous", "/spɒnˈteɪniəs/", "adj. 自发的；自然流露的", "Their spontaneous applause filled the hall.", "他们自发的掌声响彻大厅。"),
        WordEntity(17, "pragmatic", "/præɡˈmætɪk/", "adj. 务实的；讲求实际的", "We need a pragmatic solution to the problem.", "我们需要一个务实的解决方案。"),
        WordEntity(18, "compelling", "/kəmˈpelɪŋ/", "adj. 引人入胜的；令人信服的", "She made a compelling argument for change.", "她为改变提出了令人信服的论点。"),
        WordEntity(19, "reconcile", "/ˈrekənsaɪl/", "v. 调和；使和解", "It is hard to reconcile these two viewpoints.", "很难调和这两种观点。"),
        WordEntity(20, "sustainable", "/səˈsteɪnəbl/", "adj. 可持续的", "The city is investing in sustainable transport.", "这座城市正投资于可持续交通。"),
        WordEntity(21, "vulnerable", "/ˈvʌlnərəbl/", "adj. 脆弱的；易受伤害的", "Young plants are vulnerable to cold weather.", "幼苗容易受到寒冷天气的伤害。"),
        WordEntity(22, "unprecedented", "/ʌnˈpresɪdentɪd/", "adj. 前所未有的", "The team faced unprecedented challenges.", "团队面临前所未有的挑战。"),
        WordEntity(23, "articulate", "/ɑːrˈtɪkjələt/", "adj. 善于表达的；清晰的", "He is articulate and confident in interviews.", "他在面试中表达清晰且充满自信。"),
        WordEntity(24, "persevere", "/ˌpɜːrsəˈvɪr/", "v. 坚持不懈", "If you persevere, your skills will improve.", "只要坚持不懈，你的能力就会提高。")
    ).mapIndexed { index, word ->
        word.copy(
            bookId = 101,
            wordRoot = when (word.text) {
                "resilient" -> "re-（再次）+ salire（跳跃）"
                "contemplate" -> "con-（共同）+ templ（观察）"
                "sustainable" -> "sus-（从下）+ ten（保持）"
                "persevere" -> "per-（贯穿）+ severe（严格）"
                else -> "通过语境和词形联想记忆"
            },
            collocation = when (word.text) {
                "profound" -> "profound impact / profound change"
                "compelling" -> "compelling evidence / compelling reason"
                "sustainable" -> "sustainable development / sustainable growth"
                else -> "${word.text} example / ${word.text} idea"
            },
            synonyms = when (word.text) {
                "tranquil" -> "peaceful, calm"
                "diligent" -> "hard-working, industrious"
                "ambiguous" -> "unclear, vague"
                else -> ""
            }
        )
    }
}
