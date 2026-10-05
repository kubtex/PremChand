package com.example.admob

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Reusable Native Story Ad component.
 *
 * Designed to fit seamlessly between story cards in the list
 * while strictly adhering to Google's Ad disclosure policy
 * by prominently displaying the "Ad / विज्ञापन" badge.
 */
@Composable
fun NativeStoryAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobConfig.NATIVE_AD_UNIT_ID
) {
    val context = LocalContext.current
    var nativeAdState by remember { mutableStateOf<NativeAd?>(null) }
    var isFailed by remember { mutableStateOf(false) }

    LaunchedEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                AdMobConfig.log("Native", "Native ad loaded successfully")
                nativeAdState = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdMobConfig.log("Native", "Native ad failed to load: ${error.message}")
                    isFailed = true
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(nativeAdState) {
        onDispose {
            nativeAdState?.destroy()
        }
    }

    val currentAd = nativeAdState
    if (currentAd != null && !isFailed) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("admob_native_story_ad"),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    buildNativeAdView(ctx, currentAd)
                },
                update = { view ->
                    populateNativeAdView(view, currentAd)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(14.dp)
            )
        }
    }
}

private fun buildNativeAdView(context: Context, nativeAd: NativeAd): NativeAdView {
    val nativeAdView = NativeAdView(context)
    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // Header Row: [Ad] Badge + Advertiser / Subtitle
    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // Google-required prominent "Ad" tag
    val adBadge = TextView(context).apply {
        text = "Ad • विज्ञापन"
        textSize = 10f
        setTypeface(null, Typeface.BOLD)
        setTextColor(AndroidColor.parseColor("#8B1D34"))
        setBackgroundColor(AndroidColor.parseColor("#FDE8EB"))
        setPadding(12, 4, 12, 4)
    }
    headerRow.addView(adBadge)

    rootLayout.addView(headerRow)

    // Middle Content Row: Icon + (Headline & Body)
    val contentRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 16, 0, 16)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val iconView = ImageView(context).apply {
        id = View.generateViewId()
        layoutParams = LinearLayout.LayoutParams(96, 96).apply {
            setMargins(0, 0, 20, 0)
        }
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    contentRow.addView(iconView)
    nativeAdView.iconView = iconView

    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
    }

    val headlineView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 15f
        setTypeface(null, Typeface.BOLD)
        setTextColor(AndroidColor.parseColor("#1F1618"))
        maxLines = 1
    }
    textCol.addView(headlineView)
    nativeAdView.headlineView = headlineView

    val bodyView = TextView(context).apply {
        id = View.generateViewId()
        textSize = 12f
        setTextColor(AndroidColor.parseColor("#665558"))
        maxLines = 2
        setPadding(0, 4, 0, 0)
    }
    textCol.addView(bodyView)
    nativeAdView.bodyView = bodyView

    contentRow.addView(textCol)
    rootLayout.addView(contentRow)

    // Call to action button
    val ctaButton = Button(context).apply {
        id = View.generateViewId()
        textSize = 12f
        setTypeface(null, Typeface.BOLD)
        setTextColor(AndroidColor.WHITE)
        setBackgroundColor(AndroidColor.parseColor("#8B1D34"))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            88
        )
    }
    rootLayout.addView(ctaButton)
    nativeAdView.callToActionView = ctaButton

    nativeAdView.addView(rootLayout)
    populateNativeAdView(nativeAdView, nativeAd)
    return nativeAdView
}

private fun populateNativeAdView(nativeAdView: NativeAdView, nativeAd: NativeAd) {
    (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline ?: "प्रायोजित"

    val bodyView = nativeAdView.bodyView as? TextView
    if (nativeAd.body != null) {
        bodyView?.text = nativeAd.body
        bodyView?.visibility = View.VISIBLE
    } else {
        bodyView?.visibility = View.GONE
    }

    val iconView = nativeAdView.iconView as? ImageView
    if (nativeAd.icon?.drawable != null) {
        iconView?.setImageDrawable(nativeAd.icon?.drawable)
        iconView?.visibility = View.VISIBLE
    } else {
        iconView?.visibility = View.GONE
    }

    val ctaView = nativeAdView.callToActionView as? Button
    if (nativeAd.callToAction != null) {
        ctaView?.text = nativeAd.callToAction
        ctaView?.visibility = View.VISIBLE
    } else {
        ctaView?.visibility = View.GONE
    }

    nativeAdView.setNativeAd(nativeAd)
}
