package com.example.data

data class Surah(
    val id: Int,
    val name: String,
    val englishName: String,
    val revelationType: String,
    val verses: List<String>,
    val englishVerses: List<String>
)

data class AzkarItem(
    val text: String,
    val translation: String,
    val count: Int,
    val benefit: String
)

data class AzkarCategory(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val iconName: String,
    val items: List<AzkarItem>
)

object QuranProvider {
    val surahs = listOf(
        Surah(
            id = 1,
            name = "الفاتحة",
            englishName = "Al-Fatihah (The Opening)",
            revelationType = "Meccan",
            verses = listOf(
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ",
                "الرَّحْمَٰنِ الرَّحِيمِ",
                "مَالِكِ يَوْمِ الدِّينِ",
                "إِيَّاكَ نَعْبُدُ وإِيَّاكَ نَسْتَعِينُ",
                "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ",
                "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
            ),
            englishVerses = listOf(
                "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                "[All] praise is [due] to Allah, Lord of the worlds -",
                "The Entirely Merciful, the Especially Merciful,",
                "Sovereign of the Day of Recompense.",
                "It is You we worship and You we ask for help.",
                "Guide us to the straight path -",
                "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray."
            )
        ),
        Surah(
            id = 2,
            name = "الملك",
            englishName = "Al-Mulk (The Sovereignty)",
            revelationType = "Meccan",
            verses = listOf(
                "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ",
                "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا ۚ وَهُوَ الْعَزِيزُ الْغَفُورُ",
                "الَّذِي خَلَقَ سَبْعَ سَمَاوَاتٍ طِبَاقًا ۖ مَّا تَرَىٰ فِي خَلْقِ الرَّحْمَٰنِ مِن تَفَاوُتٍ ۖ فَارْجِعِ الْبَصَرَ هَلْ تَرَىٰ مِن فُطُورٍ",
                "ثُمَّ ارْجِعِ الْبَصَرَ كَرَّتَيْنِ يَنقَلِبْ إِلَيْكَ الْبَصَرُ خَاسِئًا وَهُوَ حَسِيرٌ",
                "وَلَقَدْ زَيَّنَّا السَّمَاءَ الدُّنْيَا بِمَصَابِيحَ وَجَعَلْنَاهَا رُجُومًا لِّلشَّيَاطِينِ ۖ وَأَعْتَدْنَا لَهُمْ عَذَابَ السَّعِيرِ",
                "وَلِلَّذِينَ كَفَرُوا بِرَبِّهِمْ عَذَابُ جَهَنَّمَ ۖ وَبِئْسَ الْمَصِيرُ",
                "إِذَا أُلْقُوا فِيهَا سَمِعُوا لَهَا شَهِيقًا وَهِيَ تَفُورُ",
                "تَكَادُ تَمَيَّزُ مِنَ الْغَيْظِ ۖ كُلَّمَا أُلْقِيَ فِيهَا فَوْجٌ سَأَلَهُمْ خَزَنَتُهَا أَلَمْ يَأْتِكُمْ نَذِيرٌ",
                "قَالُوا بَلَىٰ قَدْ جَاءَنَا نَذِيرٌ فَكَذَّبْنَا وَقُلْنَا مَا نَزَّلَ اللَّهُ مِن شَيْءٍ إِنْ أَنتُمْ إِلَّا فِي ضَلَالٍ كَبِيرٍ",
                "وَقَالُوا لَوْ كُنَّا نَسْمَعُ أَوْ نَعْقِلُ مَا كُنَّا فِي أَصْحَابِ السَّعِيرِ",
                "فَاعْتَرَفُوا بِذَنبِهِمْ فَسُحْقًا لِّأَصْحَابِ السَّعِيرِ",
                "إِنَّ الَّذِينَ يَخْشَوْنَ رَبَّهُم بِالْغَيْبِ لَهُم مَّغْفِرَةٌ وَأَجْرٌ كَبِيرٌ",
                "وَأَسِرُّوا قَوْلَكُمْ أَوِ اجْهَرُوا بِهِ ۖ إِنَّهُ عَلِيمٌ بِذَاتِ الصُّدُورِ",
                "أَلَا يَعْلَمُ مَنْ خَلَقَ وَهُوَ اللَّطِيفُ الْخَبِيرُ"
            ),
            englishVerses = listOf(
                "Blessed is He in whose hand is dominion, and He is over all things competent -",
                "[He] who created death and life to test you [as to] which of you is best in deed - and He is the Exalted in Might, the Forgiving.",
                "[And] who created seven heavens in layers. You do not see in the creation of the Most Merciful any inconsistency. So return [your] vision [to the heaven]; do you see any breaks?",
                "Then return [your] vision twice again. [Your] vision will return to you humbled while it is fatigued.",
                "And We have certainly beautified the nearest heaven with stars and have made them thrown stones for the devils and have prepared for them the punishment of the Blaze.",
                "And for those who disbelieved in their Lord is the punishment of Hell, and wretched is the destination.",
                "When they are thrown into it, they hear from it a [inhaling] sighing while it boils up.",
                "It almost bursts with rage. Every time a company is thrown into it, its keepers ask them, 'Did there not come to you a warner?'",
                "They will say, 'Yes, a warner had come to us, but we denied and said, \"Allah has not sent down anything. You are not but in great error.\"'",
                "And they will say, 'If only we had been listening or reasoning, we would not be among the companions of the Blaze.'",
                "And they will admit their sin, so [it is] ruin for the companions of the Blaze.",
                "Indeed, those who fear their Lord unseen will have forgiveness and great reward.",
                "And conceal your speech or proclaim it; indeed, He is Knowing of that within the breasts.",
                "Does He who created not know, while He is the Subtle, the Acquainted?"
            )
        ),
        Surah(
            id = 18,
            name = "الكهف (١-١٠)",
            englishName = "Al-Kahf (Cave - First 10)",
            revelationType = "Meccan",
            verses = listOf(
                "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجًا",
                "قَيِّمًا لِّيُنذِرَ بَأْسًا شَدِيدًا مِّن لَّدُنْهُ وَيُبَشِّرَ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا حَسَنًا",
                "مَّاكِثِينَ فِيهِ أَبَدًا",
                "وَيُنذِرَ الَّذِينَ قَالُوا اتَّخَذَ اللَّهُ وَلَدًا",
                "مَّا لَهُم بِهِ مِنْ عِلْمٍ وَلَا لِآبَائِهِمْ ۚ كَبُرَتْ كَلِمَةً تَخْرُجُ مِنْ أَفْوَاهِهِمْ ۚ إِن يَقُولُونَ إِلَّا كَذِبًا",
                "فَلَعَلَّكَ بَاخِعٌ نَّفْسَكَ عَلَىٰ آثَارِهِمْ إِن لَّمْ يُؤْمِنُوا بِهَٰذَا الْحَدِيثِ أَسَفًا",
                "إِنَّ جَعَلْنَا مَا عَلَى الْأَرْضِ زِينَةً لَّهَا لِنَبْلُوَهُمْ أَيُّهُمْ أَحْسَنُ عَمَلًا",
                "وَإِنَّا لَجَاعِلُونَ مَا عَلَيْهَا صَعِيدًا جُرُزًا",
                "أَمْ حَسِبْتَ أَنَّ أَصْحَابَ الْكَهْفِ وَالرَّقِيمِ كَانُوا مِنْ آيَاتِنَا عَجَبًا",
                "إِذْ أَوَى الْفِتْيَةُ إِلَى الْكَهْفِ فَقَالُوا رَبَّنَا آتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا"
            ),
            englishVerses = listOf(
                "[All] praise is [due] to Allah, who has sent down upon His Servant the Book and has not made therein any deviance.",
                "[He has made it] straight, to warn of severe punishment from Him and to give good tidings to the believers who do righteous deeds that they will have a good reward,",
                "In which they will remain forever,",
                "And to warn those who say, 'Allah has taken a son.'",
                "They have no knowledge of it, nor had their fathers. Grave is the word that comes out of their mouths; they speak not except a lie.",
                "Then perhaps you would kill yourself through grief over them, [O Muhammad], if they do not believe in this message, out of sorrow.",
                "Indeed, We have made that which is on the earth adornment for it that We may test them [as to] which of them is best in deed.",
                "And indeed, We will make that which is upon it [into] a barren ground.",
                "Or have you thought that the companions of the cave and the inscription were, among Our signs, a wonder?",
                "[Mention] when the youths retreated to the cave and said, 'Our Lord, grant us from Yourself mercy and prepare for us from our affair right guidance.'"
            )
        ),
        Surah(
            id = 56,
            name = "الواقعة (١-١٥)",
            englishName = "Al-Waqi'ah (The Inevitable)",
            revelationType = "Meccan",
            verses = listOf(
                "إِذَا وَقَعَتِ الْوَاقِعَةُ",
                "لَيْسَ لِوَقْعَتِهَا كَاذِبَةٌ",
                "خَافِضَةٌ رَّافِعَةٌ",
                "إِذَا رُجَّتِ الْأَرْضُ رَجًّا",
                "وَبُسَّتِ الْجِبَالُ بَسًّا",
                "فَكَانَتْ هَبَاءً مُّنبَثًّا",
                "وَكُنتُمْ أَزْوَاجًا ثَلَاثَةً",
                "فَأَصْحَابُ الْمَيْمَنَةِ مَا أَصْحَابُ الْمَيْمَنَةِ",
                "وأَصْحَابُ الْمَشْأَمَةِ مَا أَصْحَابُ الْمَشْأَمَةِ",
                "وَالسَّابِقُونَ السَّابِقُونَ",
                "أُولَٰئِكَ الْمُقَرَّبُونَ",
                "فِي جَنَّاتِ النَّعِيمِ",
                "ثُلَّةٌ مِّنَ الْأَوَّلِينَ",
                "وَقَلِيلٌ مِّنَ الْآخِرِينَ",
                "عَلَىٰ سُرُرٍ مَّوْضُونَةٍ"
            ),
            englishVerses = listOf(
                "When the Inevitable Event occurs,",
                "There is, at its occurrence, no denial.",
                "It will bring down [some] and raise up [others].",
                "When the earth is shaken with a jolt,",
                "And the mountains are crumbled to powder,",
                "So they become dust dispersing,",
                "And you become [divided into] three kinds:",
                "Then the companions of the right - what are the companions of the right?",
                "And the companions of the left - what are the companions of the left?",
                "And the forerunners, the forerunners -",
                "Those are the ones brought near [to Allah]",
                "In the Gardens of Pleasure,",
                "A large company of the former peoples",
                "And a few of the later peoples,",
                "On thrones woven with gold and precious stones,"
            )
        ),
        Surah(
            id = 112,
            name = "الإخلاص",
            englishName = "Al-Ikhlas (Sincerity)",
            revelationType = "Meccan",
            verses = listOf(
                "قُلْ هُوَ اللَّهُ أَحَدٌ",
                "اللَّهُ الصَّمَدُ",
                "لَمْ يَلِدْ وَلَمْ يُولَدْ",
                "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ"
            ),
            englishVerses = listOf(
                "Say, He is Allah, [who is] One,",
                "Allah, the Eternal Refuge.",
                "He neither begets nor is born,",
                "Nor is there to Him any equivalent."
            )
        ),
        Surah(
            id = 113,
            name = "الفلق",
            englishName = "Al-Falaq (The Daybreak)",
            revelationType = "Meccan",
            verses = listOf(
                "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ",
                "مِن شَرِّ مَا خَلَقَ",
                "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبٌ",
                "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ",
                "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ"
            ),
            englishVerses = listOf(
                "Say, I seek refuge in the Lord of daybreak",
                "From the evil of that which He created",
                "And from the evil of darkness when it settles",
                "And from the evil of the blowers in knots",
                "And from the evil of an envier when he envies."
            )
        ),
        Surah(
            id = 114,
            name = "الناس",
            englishName = "An-Nas (Mankind)",
            revelationType = "Meccan",
            verses = listOf(
                "قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
                "مَلِكِ النَّاسِ",
                "إِلَٰهِ النَّاسِ",
                "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ",
                "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ",
                "مِنَ الْجِنَّةِ وَالنَّاسِ"
            ),
            englishVerses = listOf(
                "Say, I seek refuge in the Lord of mankind,",
                "The Sovereign of mankind,",
                "The God of mankind,",
                "From the evil of the retreating whisperer -",
                "Who whispers [evil] into the breasts of mankind -",
                "From among the jinn and mankind."
            )
        )
    )
}

object AzkarProvider {
    val categories = listOf(
        AzkarCategory(
            id = "sabah",
            titleAr = "أذكار الصباح",
            titleEn = "Morning Azkar",
            iconName = "WbSunny",
            items = listOf(
                AzkarItem(
                    text = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
                    translation = "We have entered a new day and with it all dominion is Allah's, praise is to Allah, none has the right to be worshipped but Allah alone...",
                    count = 1,
                    benefit = "الحفظ والبركة في الصباح وسعة الرزق والوقاية من السوء."
                ),
                AzkarItem(
                    text = "اللّهُ لاَ إِلَـهَ إِلاَّ هُوَ الْحَيُّ الْقَيُّومُ لاَ تَأْخُذُهُ سِنَةٌ وَلاَ نَوْمٌ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الأَرْضِ مَن ذَا الَّذِي يَشْفَعُ عِنْدَهُ إِلاَّ بِإِذْنِهِ...",
                    translation = "Ayat Al-Kursi: Allah! There is no deity except Him, the Ever-Living, the Sustainer of all existence...",
                    count = 1,
                    benefit = "لن يزال عليك من الله حافظ ولا يقربك شيطان حتى تصبح."
                ),
                AzkarItem(
                    text = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.",
                    translation = "O Allah, by Your leave we have entered the morning and by Your leave we have entered the evening, by Your leave we live...",
                    count = 1,
                    benefit = "تجديد التوكل على الله العلي العظيم."
                ),
                AzkarItem(
                    text = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ...",
                    translation = "Sayyid al-Istighfar: O Allah, You are my Lord, there is no deity except You, You created me and I am Your servant...",
                    count = 1,
                    benefit = "سيد الاستغفار: من قالها موقناً بها ومات من يومه، دخل الجنة."
                ),
                AzkarItem(
                    text = "سُبْحَانَ اللهِ وَبِحَمْدِهِ.",
                    translation = "Glory is to Allah and praise is to Him.",
                    count = 100,
                    benefit = "حطت خطاياه وإن كانت مثل زبد البحر، ولم يأتِ أحد بأفضل مما جاء به."
                )
            )
        ),
        AzkarCategory(
            id = "masaa",
            titleAr = "أذكار المساء",
            titleEn = "Evening Azkar",
            iconName = "NightsStay",
            items = listOf(
                AzkarItem(
                    text = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
                    translation = "We have entered the evening and with it all dominion is Allah's...",
                    count = 1,
                    benefit = "الحفظ الحصين في المساء ودفع الضر والبلاء."
                ),
                AzkarItem(
                    text = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
                    translation = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
                    count = 3,
                    benefit = "لم تضره حمةٌ ولا لدغة في تلك الليلة."
                ),
                AzkarItem(
                    text = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
                    translation = "In the name of Allah, with Whose name nothing can cause harm in the earth nor in the heaven...",
                    count = 3,
                    benefit = "لم يضره شيء أبداً في يومه وليلته."
                ),
                AzkarItem(
                    text = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَأَعُوذُ بِكَ مِنَ الْعَجْزِ وَالْكَسَلِ، وَأَعُوذُ بِكَ مِنَ الْجُبْنِ وَالْبُخْلِ، وَأَعُوذُ بِكَ مِنْ غَلَبَةِ الدَّيْنِ وَقَهْرِ الرِّجَالِ.",
                    translation = "O Allah, I seek refuge in You from anxiety and grief, and I seek refuge in You from disability and laziness...",
                    count = 1,
                    benefit = "تفريج الكروب والهموم وقضاء الديون والراحة النفسية."
                )
            )
        ),
        AzkarCategory(
            id = "post_prayer",
            titleAr = "أذكار بعد الصلاة",
            titleEn = "Post-Prayer Azkar",
            iconName = "TaskAlt",
            items = listOf(
                AzkarItem(
                    text = "أَسْتَغْفِرُ اللهَ (ثلاثاً) ... اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ.",
                    translation = "I ask Allah for forgiveness (3 times). O Allah, You are Peace and from You comes peace...",
                    count = 1,
                    benefit = "تكميل ما نقص من الفريضة والاعتراف بالتقصير والتقرب للباري."
                ),
                AzkarItem(
                    text = "سُبْحَانَ اللهِ.",
                    translation = "Glory be to Allah.",
                    count = 33,
                    benefit = "غفرت خطاياه وإن كانت مثل زبد البحر إذا أكمل المائة بالتوحيد."
                ),
                AzkarItem(
                    text = "الْحَمْدُ للهِ.",
                    translation = "Praise be to Allah.",
                    count = 33,
                    benefit = "عظيم الثواب والأجر المترتب عليها."
                ),
                AzkarItem(
                    text = "اللهُ أَكْبَرُ.",
                    translation = "Allah is the Greatest.",
                    count = 33,
                    benefit = "تثبيت الإيمان وتكبير الباري عز وجل."
                ),
                AzkarItem(
                    text = "لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
                    translation = "None has the right to be worshipped but Allah alone, without partner...",
                    count = 1,
                    benefit = "تمام المائة بعد التسبيح والتحميد والتكبير."
                )
            )
        ),
        AzkarCategory(
            id = "sleep",
            titleAr = "أذكار النوم",
            titleEn = "Sleep Azkar",
            iconName = "Bedtime",
            items = listOf(
                AzkarItem(
                    text = "بِاسْمِكَ رَبِّي وَضَعْتُ جَنْبِي، وَبِكَ أَرْفَعُهُ، فَإِنْ أَمْسَكْتَ نَفْسِي فَارْحَمْهَا، وَإِنْ أَرْسَلْتَهَا فَاحْفَظْهَا بِمَا تَحْفَظُ بِهِ عِبَادَكَ الصَّالِحِينَ.",
                    translation = "In Your name my Lord, I lie down, and in Your name I rise...",
                    count = 1,
                    benefit = "حماية الروح وحفظ الجسد خلال النوم والنوم بسلام وطمأنينة."
                ),
                AzkarItem(
                    text = "اللَّهُمَّ قِنِي عَذَابَكَ يَوْمَ تَبْعَثُ عِبَادَكَ.",
                    translation = "O Allah, protect me from Your punishment on the Day You resurrect Your servants.",
                    count = 3,
                    benefit = "الحفظ من عذاب الله والتقرب من رحمته العظيمة."
                ),
                AzkarItem(
                    text = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا.",
                    translation = "In Your name, O Allah, I die and I live.",
                    count = 1,
                    benefit = "أن تكون آخر كلمة يقولها المسلم هي ذكر الله تعالى."
                )
            )
        )
    )
}
