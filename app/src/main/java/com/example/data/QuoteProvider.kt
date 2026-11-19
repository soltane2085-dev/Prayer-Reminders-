package com.example.data

data class StaticQuote(
    val text: String,
    val author: String
)

object QuoteProvider {
    val predefinedQuotes = listOf(
        StaticQuote(
            "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            "القرآن الكريم، الشرح: 6"
        ),
        StaticQuote(
            "خَيْرُ النَّاسِ أَنْفَعُهُمْ لِلنَّاسِ",
            "الرسول محمد ﷺ"
        ),
        StaticQuote(
            "كُنْ كَالزَّهْرَةِ الَّتِي تَنْشُرُ عِطْرَهَا حَتَّى عَلَى الْيِدِ الَّتِي تَسْحَقُهَا",
            "علي بن أبي طالب"
        ),
        StaticQuote(
            "بِالأَمْسِ كُنْتُ ذَكِيَّاً فَأَرَدْتُ أَنْ أُغَيِّرَ الْعَالَمَ، الْيَوْمَ أَنَا حَكِيمٌ لِذَلِكَ أَنَا أُغَيِّرُ نَفْسِي",
            "جلال الدين الرومي"
        ),
        StaticQuote(
            "وَلَا تَهِنُوا وَلَا تَحْزَنُوا وَأَنْتُمُ الْأَعْلَوْنَ إِن كُنتُم مُّؤْمِنِينَ",
            "القرآن الكريم، آل عمران: 139"
        ),
        StaticQuote(
            "مَن كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَقُلْ خَيْرًا أَوْ لِيَصْمُتْ",
            "الرسول محمد ﷺ"
        ),
        StaticQuote(
            "الصَّمْتُ هُوَ بَحْرٌ مِنَ الإِجَابَاتِ",
            "جلال الدين الرومي"
        ),
        StaticQuote(
            "الصَّبْرُ لَيْسَ الْقُدْرَةَ عَلَى الِانْتِظَارِ، بَلْ هُوَ كَيْفَ نَتَصَرَّفُ وَنَحْنُ نَنْتَظِرُ",
            "حكمة صوفية"
        ),
        StaticQuote(
            "مَن لَا صَبْرَ لَهُ لَا إِيمَانَ لَهُ",
            "علي بن أبي طالب"
        ),
        StaticQuote(
            "فَاذْكُرُونِي أَذْكُرْكُمْ",
            "القرآن الكريم، البقرة: 152"
        ),
        StaticQuote(
            "وَرَحْمَتِي وَسِعَتْ كُلَّ شَيْءٍ",
            "القرآن الكريم، الأعراف: 156"
        ),
        StaticQuote(
            "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            "القرآن الكريم، الرعد: 28"
        ),
        StaticQuote(
            "لَيْسَ الْغِنَى عَنْ كَثْرَةِ الْعَرَضِ، وَلَكِنَّ الْغِنَى غِنَى النَّفْسِ",
            "الرسول محمد ﷺ"
        ),
        StaticQuote(
            "الْجُرْحُ هُوَ الْمَكَانُ الَّذِي يَدْخُلُ مِنْهُ النُّورُ إِلَيْكَ",
            "جلال الدين الرومي"
        ),
        StaticQuote(
            "كُنْ شَاكِرًا لِكُلِّ مَنْ يَأْتِي، لِأَنَّ كُلَّ وَاحِدٍ مِنْهُمْ جَاءَ كَمُرْشِدٍ مِنَ الْغَيْبِ",
            "جلال الدين الرومي"
        )
    )

    fun getRandomQuote(): StaticQuote {
        return predefinedQuotes.random()
    }

    fun getQuoteTitle(text: String, author: String, lang: String): String {
        if (lang != "AR") {
            val isHadith = author.contains("Prophet", ignoreCase = true) || 
                           author.contains("Muhammad", ignoreCase = true) ||
                           text.contains("Messenger", ignoreCase = true) || 
                           text.contains("Allah says", ignoreCase = true)
            return if (isHadith) "Hadith of the Day" else "Wisdom of the Day"
        }
        val isHadith = author.contains("الرسول") || 
                       author.contains("Prophet") || 
                       author.contains("Muhammad") || 
                       author.contains("حديث") || 
                       author.contains("مرفوع") || 
                       author.contains("البخاري") || 
                       author.contains("مسلم") || 
                       author.contains("الترمذي") || 
                       text.contains("قال رسول الله") || 
                       text.contains("قال النبي") || 
                       text.contains("ﷺ")
        return if (isHadith) "حديث اليوم" else "حكمة اليوم"
    }

    fun getQuoteLabel(text: String, author: String, lang: String): String {
        if (lang != "AR") {
            val isHadith = author.contains("Prophet", ignoreCase = true) || 
                           author.contains("Muhammad", ignoreCase = true) ||
                           text.contains("Messenger", ignoreCase = true)
            return if (isHadith) "Hadith" else "Wisdom"
        }
        val isHadith = author.contains("الرسول") || 
                       author.contains("Prophet") || 
                       author.contains("Muhammad") || 
                       author.contains("حديث") || 
                       author.contains("مرفوع") || 
                       author.contains("البخاري") || 
                       author.contains("مسلم") || 
                       author.contains("الترمذي") || 
                       text.contains("قال رسول الله") || 
                       text.contains("قال النبي") || 
                       text.contains("ﷺ")
        return if (isHadith) "الحديث" else "الحكمة"
    }
}
