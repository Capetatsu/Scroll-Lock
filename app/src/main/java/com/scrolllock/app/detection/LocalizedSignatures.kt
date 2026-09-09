package com.scrolllock.app.detection

object LocalizedSignatures {
    data class AppSignatures(
        val reels: List<String> = emptyList(),
        val stories: List<String> = emptyList(),
        val explore: List<String> = emptyList(),
        val direct: List<String> = emptyList(),
        val home: List<String> = emptyList(),
        val search: List<String> = emptyList(),
        val comments: List<String> = emptyList(),
        val shorts: List<String> = emptyList(),
        val likes: List<String> = emptyList()
    )

    private val instagramSignatures = AppSignatures(
        reels = listOf("Reels", "Reelek", "Reels", "Reels", "Reels", "Reels", "Reels", "Reels", "Reels", "Reels", "Reels", "Reels"),
        stories = listOf("Stories", "Historias", "Stories", "Histoires", "Geschichten", "Storie", "Historias", "Stories", "Stories", "Stories", "Stories", "Stories"),
        explore = listOf("Explore", "Explorar", "Explorer", "Entdecken", "Esplora", "Explorar", "Explora", "Penjelajah", "Eksplor", "Xem", "ดูรอบๆ", "جستجو"),
        direct = listOf("Direct", "Directos", "Messagerie", "Direkt", "Direct Messages", "Directa", "Mensajes", "Pesan", "Pesan Langsung", "Tin nhắn", "ข้อความ", "الرسائل المباشرة"),
        home = listOf("Home", "Inicio", "Accueil", "Startseite", "Home", "Início", "Beranda", "Trang chủ", "หน้าแรก", "الرئيسية"),
        search = listOf("Search", "Buscar", "Rechercher", "Suchen", "Cerca", "Pesquisar", "Buscar", "Telusuri", "Tìm kiếm", "ค้นหา", "بحث"),
        comments = listOf("Comments", "Comentarios", "Commentaires", "Kommentare", "Commenti", "Comentários", "Komentar", "Bình luận", "ความคิดเห็น", "تعليقات"),
        likes = listOf("Likes", "Me gusta", "J'aime", "Gefällt mir", "Mi piace", "Curtidas", "Suka", "Thích", "ถูกใจ", "إعجابات")
    )

    private val youtubeSignatures = AppSignatures(
        shorts = listOf("Shorts", "Shorts", "Shorts", "Shorts", "Shorts", "Shorts", "Shorts", "Shorts", "Shorts", "Shorts", "Shorts", "Shorts"),
        reels = listOf("Reels", "Reels"),
        home = listOf("Home", "Inicio", "Accueil", "Startseite", "Home", "Início", "Beranda", "Trang chủ", "หน้าแรก", "الرئيسية"),
        search = listOf("Search", "Buscar", "Rechercher", "Suchen", "Cerca", "Pesquisar", "Telusuri", "Tìm kiếm", "ค้นหา", "بحث")
    )

    private val tiktokSignatures = AppSignatures(
        reels = listOf("For You", "Para Ti", "Pour Toi", "Für Dich", "Per Te", "Para Si", "Untukmu", "Dành Cho Bạn", "สำหรับคุณ", "لك"),
        home = listOf("Home", "Inicio", "Accueil", "Startseite", "Home", "Início", "Beranda", "Trang chủ", "หน้าแรก", "الرئيسية"),
        search = listOf("Search", "Buscar", "Rechercher", "Suchen", "Cerca", "Pesquisar", "Telusuri", "Tìm kiếm", "ค้นหา", "بحث"),
        likes = listOf("Likes", "Me gusta", "J'aime", "Gefällt mir", "Mi piace", "Curtidas", "Suka", "Thích", "ถูกใจ", "إعجابات")
    )

    private val snapchatSignatures = AppSignatures(
        stories = listOf("Stories", "Historias", "Histoires", "Geschichten", "Storie"),
        reels = listOf("Spotlight", "Spotlight", "Spotlight", "Spotlight", "Spotlight")
    )

    private val facebookSignatures = AppSignatures(
        reels = listOf("Reels", "Reels", "Reels", "Reels", "Reels", "Reels"),
        stories = listOf("Stories", "Historias", "Histoires", "Geschichten", "Storie"),
        home = listOf("Home", "Inicio", "Accueil", "Startseite", "Home", "Início")
    )

    private val redditSignatures = AppSignatures(
        reels = listOf("Video", "Watch", "Premium"),
        home = listOf("Home", "Inicio", "Accueil", "Startseite", "Home", "Início")
    )

    private val linkedinSignatures = AppSignatures(
        reels = listOf("Video", "Watch", "Learning"),
        home = listOf("Home", "Inicio", "Accueil", "Startseite", "Home", "Início"),
        search = listOf("Search", "Buscar", "Rechercher", "Suchen", "Cerca", "Pesquisar")
    )

    fun forPackage(packageName: String): AppSignatures {
        return when (packageName) {
            "com.instagram.android" -> instagramSignatures
            "com.google.android.youtube" -> youtubeSignatures
            "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> tiktokSignatures
            "com.snapchat.android" -> snapchatSignatures
            "com.facebook.katana", "com.facebook.lite" -> facebookSignatures
            "com.reddit.frontpage" -> redditSignatures
            "com.linkedin.android" -> linkedinSignatures
            else -> AppSignatures()
        }
    }
}
