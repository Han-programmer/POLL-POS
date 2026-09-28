package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R

/**
 * Data class representing a preset HD 1:1 product photo option.
 */
data class ProductImagePreset(
    val id: String,
    val title: String,
    val drawableResId: Int,
    val categoryHint: String
)

val HD_PRODUCT_PRESETS = listOf(
    ProductImagePreset("res:img_kopi_susu", "Kopi Susu Aren", R.drawable.img_kopi_susu, "Minuman / Kopi"),
    ProductImagePreset("res:img_nasi_goreng", "Nasi Goreng", R.drawable.img_nasi_goreng, "Makanan"),
    ProductImagePreset("res:img_croissant", "Butter Croissant", R.drawable.img_croissant, "Snack / Pastry"),
    ProductImagePreset("res:img_es_teh", "Es Teh Melati", R.drawable.img_es_teh, "Minuman Segar")
)

/**
 * Modern HD 1:1 Square Product Image Composable.
 *
 * Supports:
 * - Local HD Drawable presets ("res:img_kopi_susu", etc.)
 * - Automatic smart-mapping to local HD assets based on product name/category keywords
 * - External Web URLs (http/https) via Coil with smooth crossfade
 * - Local file/content URIs (content://, file://)
 * - Beautiful fallback gradient illustration when no custom image is assigned
 */
@Composable
fun ProductImageView(
    imageUri: String?,
    productName: String,
    modifier: Modifier = Modifier,
    categoryName: String = "",
    cornerRadius: Int = 12
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val resolvedDrawableRes = resolveLocalDrawableRes(imageUri, productName)
    val isExternalOrLocalFile = !imageUri.isNullOrBlank() && (
        imageUri.startsWith("http://") ||
        imageUri.startsWith("https://") ||
        imageUri.startsWith("content://") ||
        imageUri.startsWith("file://") ||
        imageUri.startsWith("/")
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            // Local HD Drawable Preset
            resolvedDrawableRes != null -> {
                Image(
                    painter = painterResource(id = resolvedDrawableRes),
                    contentDescription = productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Local Device Storage File or Web URL via Coil
            isExternalOrLocalFile -> {
                val dataToLoad: Any = if (imageUri!!.startsWith("/")) java.io.File(imageUri) else imageUri
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(dataToLoad)
                        .crossfade(true)
                        .build(),
                    contentDescription = productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Thematic Stylized Gradient Illustration Fallback
            else -> {
                ThemedProductIllustration(
                    productName = productName,
                    categoryName = categoryName
                )
            }
        }
    }
}

/**
 * Resolves local drawable resource ID based on explicit URI or product name keywords.
 */
private fun resolveLocalDrawableRes(imageUri: String?, productName: String): Int? {
    if (!imageUri.isNullOrBlank()) {
        if (imageUri.startsWith("res:")) {
            when {
                imageUri.contains("kopi", ignoreCase = true) -> return R.drawable.img_kopi_susu
                imageUri.contains("nasi", ignoreCase = true) || imageUri.contains("goreng", ignoreCase = true) -> return R.drawable.img_nasi_goreng
                imageUri.contains("croissant", ignoreCase = true) || imageUri.contains("roti", ignoreCase = true) -> return R.drawable.img_croissant
                imageUri.contains("teh", ignoreCase = true) -> return R.drawable.img_es_teh
            }
        }
        // If imageUri is an explicit local device file, content URI, or URL, do not override with preset
        return null
    }

    // Auto-match based on name keywords ONLY if imageUri is null or blank
    val lower = productName.lowercase()
    return when {
        lower.contains("kopi") || lower.contains("coffee") || lower.contains("espresso") || lower.contains("latte") || lower.contains("americano") -> R.drawable.img_kopi_susu
        lower.contains("nasi") || lower.contains("ayam") || lower.contains("mie") || lower.contains("rice") -> R.drawable.img_nasi_goreng
        lower.contains("croissant") || lower.contains("roti") || lower.contains("pastry") || lower.contains("toast") || lower.contains("snack") -> R.drawable.img_croissant
        lower.contains("teh") || lower.contains("tea") || lower.contains("matcha") || lower.contains("jus") || lower.contains("lemon") -> R.drawable.img_es_teh
        else -> null
    }
}

/**
 * Eye-catching modern gradient illustration for products without photos.
 */
@Composable
private fun ThemedProductIllustration(
    productName: String,
    categoryName: String
) {
    val (gradient, icon) = getThemedGradientAndIcon(productName, categoryName)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(gradient)),
        contentAlignment = Alignment.Center
    ) {
        // Decorative soft ambient inner circle
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

private fun getThemedGradientAndIcon(productName: String, categoryName: String): Pair<List<Color>, ImageVector> {
    val combined = "$productName $categoryName".lowercase()

    return when {
        combined.contains("kopi") || combined.contains("coffee") || combined.contains("minum") || combined.contains("drink") || combined.contains("cafe") -> {
            listOf(Color(0xFF8D6E63), Color(0xFF4E342E)) to Icons.Default.LocalCafe
        }
        combined.contains("nasi") || combined.contains("makan") || combined.contains("food") || combined.contains("resto") || combined.contains("ayam") -> {
            listOf(Color(0xFFE53935), Color(0xFFC62828)) to Icons.Default.Restaurant
        }
        combined.contains("snack") || combined.contains("roti") || combined.contains("kue") || combined.contains("croissant") || combined.contains("dessert") -> {
            listOf(Color(0xFFFB8C00), Color(0xFFE65100)) to Icons.Default.BakeryDining
        }
        combined.contains("teh") || combined.contains("jus") || combined.contains("segar") || combined.contains("es") -> {
            listOf(Color(0xFF00897B), Color(0xFF004D40)) to Icons.Default.LocalBar
        }
        else -> {
            listOf(Color(0xFF546E7A), Color(0xFF263238)) to Icons.Default.Inventory2
        }
    }
}
