package com.example.ppp.ui.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.ppp.data.model.Game
import com.example.ppp.data.model.PlayStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun generateReviewCardBitmap(
    context: Context,
    game: Game,
    rating: Float,
    status: PlayStatus,
    review: String,
    hasPlatinum: Boolean,
    playtimeHours: Int,
    completionYear: Int?
): Bitmap = withContext(Dispatchers.IO) {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 1. Dark Gradient Background (#0F101A to #1A1D2E)
    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.parseColor("#0F101A"), Color.parseColor("#1B1E30"),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // 2. Fetch Hero Backdrop / Cover Images via Coil
    val imageLoader = context.imageLoader

    var bgBitmap: Bitmap? = null
    val bgUrl = game.backgroundUrl ?: game.coverUrl
    if (!bgUrl.isNullOrBlank()) {
        try {
            val request = ImageRequest.Builder(context)
                .data(bgUrl)
                .allowHardware(false)
                .build()
            val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
            bgBitmap = (result as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {}
    }

    var coverBitmap: Bitmap? = null
    if (!game.coverUrl.isNullOrBlank()) {
        try {
            val request = ImageRequest.Builder(context)
                .data(game.coverUrl)
                .allowHardware(false)
                .build()
            val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
            coverBitmap = (result as? BitmapDrawable)?.bitmap
        } catch (_: Exception) {}
    }

    // 3. Draw Top Backdrop Image with Fade Overlay
    if (bgBitmap != null) {
        val destRect = RectF(0f, 0f, width.toFloat(), 550f)
        canvas.drawBitmap(bgBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

        // Gradient overlay to blend backdrop smoothly into background
        val fadePaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, 550f,
                Color.parseColor("#400F101A"), Color.parseColor("#FF0F101A"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(destRect, fadePaint)
    }

    // 4. Draw Cover Art Thumbnail (Rounded Corners)
    val coverLeft = 70f
    val coverTop = 380f
    val coverWidth = 240f
    val coverHeight = 320f

    if (coverBitmap != null) {
        val coverRect = RectF(coverLeft, coverTop, coverLeft + coverWidth, coverTop + coverHeight)
        val roundedBitmap = getRoundedCornerBitmap(coverBitmap, 24f, coverWidth.toInt(), coverHeight.toInt())
        canvas.drawBitmap(roundedBitmap, coverLeft, coverTop, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    // 5. Game Title Typography
    val titleLeft = coverLeft + coverWidth + 36f
    val titleTop = 410f
    val titleWidth = width - titleLeft - 70f

    val titlePaint = TextPaint().apply {
        color = Color.WHITE
        textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val titleLayout = StaticLayout.Builder.obtain(game.title, 0, game.title.length, titlePaint, titleWidth.toInt())
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(2)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    canvas.save()
    canvas.translate(titleLeft, titleTop)
    titleLayout.draw(canvas)
    canvas.restore()

    // 6. Release Date & Genres Subtitle
    val subtitleTop = titleTop + titleLayout.height + 12f
    val subtitlePaint = TextPaint().apply {
        color = Color.parseColor("#9AA0B4")
        textSize = 28f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }

    val dateStr = game.releaseDate ?: "N/A"
    val genresStr = if (game.genres.isNotEmpty()) " • " + game.genres.take(2).joinToString(", ") else ""
    canvas.drawText("$dateStr$genresStr", titleLeft, subtitleTop + 24f, subtitlePaint)

    // 7. Status & Platinum Badges
    val badgeTop = subtitleTop + 60f
    val statusPaint = Paint().apply {
        color = Color.parseColor("#0070D1")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val statusTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val yrStr = completionYear?.let { " ($it)" } ?: ""
    val statusLabel = "${status.displayName}$yrStr"
    val statusTextWidth = statusTextPaint.measureText(statusLabel)
    val statusBadgeWidth = statusTextWidth + 32f
    val statusRect = RectF(titleLeft, badgeTop, titleLeft + statusBadgeWidth, badgeTop + 48f)

    canvas.drawRoundRect(statusRect, 12f, 12f, statusPaint)
    canvas.drawText(statusLabel, titleLeft + 16f, badgeTop + 33f, statusTextPaint)

    if (hasPlatinum) {
        val platLeft = titleLeft + statusBadgeWidth + 16f
        val platPaint = Paint().apply {
            color = Color.parseColor("#FFD700")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val platTextPaint = Paint().apply {
            color = Color.parseColor("#0F101A")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val platLabel = "🏆 Platinum"
        val platTextWidth = platTextPaint.measureText(platLabel)
        val platRect = RectF(platLeft, badgeTop, platLeft + platTextWidth + 32f, badgeTop + 48f)

        canvas.drawRoundRect(platRect, 12f, 12f, platPaint)
        canvas.drawText(platLabel, platLeft + 16f, badgeTop + 33f, platTextPaint)
    }

    // 8. Rating Stars Bar
    val ratingTop = coverTop + coverHeight + 60f
    val ratingTextPaint = TextPaint().apply {
        color = Color.parseColor("#FFD700")
        textSize = 54f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val starsString = "★ ${if (rating > 0f) String.format(java.util.Locale.US, "%.1f", rating) else "Unrated"}"
    canvas.drawText(starsString, 70f, ratingTop + 45f, ratingTextPaint)

    val scorePaint = TextPaint().apply {
        color = Color.WHITE
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    val ratingValStr = if (rating > 0f) "/ 10.0" else ""
    val starsWidth = ratingTextPaint.measureText(starsString)
    canvas.drawText(ratingValStr, 70f + starsWidth + 16f, ratingTop + 40f, scorePaint)

    if (playtimeHours > 0) {
        val timePaint = TextPaint().apply {
            color = Color.parseColor("#00A2FF")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("⏱️ $playtimeHours hrs played", width - 360f, ratingTop + 40f, timePaint)
    }

    // 9. Review Quote Card Box
    val quoteCardTop = ratingTop + 90f
    val quoteCardHeight = 420f
    val quoteCardRect = RectF(70f, quoteCardTop, width - 70f, quoteCardTop + quoteCardHeight)

    val cardPaint = Paint().apply {
        color = Color.parseColor("#171A28")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawRoundRect(quoteCardRect, 28f, 28f, cardPaint)

    // Border line around quote box
    val strokePaint = Paint().apply {
        color = Color.parseColor("#2A2E45")
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    canvas.drawRoundRect(quoteCardRect, 28f, 28f, strokePaint)

    // Gold decorative quotation mark
    val quoteMarkPaint = TextPaint().apply {
        color = Color.parseColor("#FFD700")
        textSize = 90f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        isAntiAlias = true
    }
    canvas.drawText("“", 100f, quoteCardTop + 90f, quoteMarkPaint)

    // Review Text inside Card
    val reviewContent = if (review.isNotBlank()) review else "Completed and rated on GameBoxd."
    val reviewPaint = TextPaint().apply {
        color = Color.parseColor("#E0E4F0")
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }

    val reviewLayout = StaticLayout.Builder.obtain(reviewContent, 0, reviewContent.length, reviewPaint, (width - 240))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(5)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    canvas.save()
    canvas.translate(120f, quoteCardTop + 110f)
    reviewLayout.draw(canvas)
    canvas.restore()

    // 10. Footer Branding
    val footerTop = height - 90f
    val footerBrandPaint = TextPaint().apply {
        color = Color.WHITE
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }
    canvas.drawText("🎮 GameBoxd", 70f, footerTop, footerBrandPaint)

    val footerTaglinePaint = TextPaint().apply {
        color = Color.parseColor("#7A829E")
        textSize = 26f
        typeface = Typeface.DEFAULT
        isAntiAlias = true
    }
    val tagline = "Track, Rate & Share Your Gaming Journey"
    val taglineWidth = footerTaglinePaint.measureText(tagline)
    canvas.drawText(tagline, width - taglineWidth - 70f, footerTop, footerTaglinePaint)

    bitmap
}

private fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Float, width: Int, height: Int): Bitmap {
    val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint = Paint().apply {
        isAntiAlias = true
        color = Color.RED
    }
    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

    canvas.drawRoundRect(rect, pixels, pixels, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaled, 0f, 0f, paint)

    return output
}

suspend fun shareReviewCardImage(
    context: Context,
    game: Game,
    rating: Float,
    status: PlayStatus,
    review: String,
    hasPlatinum: Boolean,
    playtimeHours: Int,
    completionYear: Int?
) {
    val bitmap = generateReviewCardBitmap(
        context = context,
        game = game,
        rating = rating,
        status = status,
        review = review,
        hasPlatinum = hasPlatinum,
        playtimeHours = playtimeHours,
        completionYear = completionYear
    )

    withContext(Dispatchers.IO) {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "review_card_${game.id}.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val stars = if (rating > 0f) "${String.format(java.util.Locale.US, "%.1f", rating)} / 10.0 ★" else "Unrated"
        val shareText = "🎮 ${game.title} - $stars on GameBoxd"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "image/png"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, "Share Review Card")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
