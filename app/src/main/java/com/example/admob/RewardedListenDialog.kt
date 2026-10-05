package com.example.admob

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dialog shown before playing audio, informing the user about the rewarded ad.
 */
@Composable
fun RewardedListenDialog(
    storyTitle: String,
    onDismissRequest: () -> Unit,
    onRewardEarned: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.Headphones,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "ऑडियो कहानी सुनें",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "«$storyTitle» का संपूर्ण ऑडियो सुनने के लिए एक छोटा सा विज्ञापन देखें।\n\n(Watch a short ad to listen to this story.)",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismissRequest()
                    if (activity != null) {
                        RewardedAdManager.showRewardedAd(
                            activity = activity,
                            onRewardEarned = {
                                AdMobConfig.log("Rewarded", "Reward earned! Starting audio...")
                                onRewardEarned()
                            },
                            onDismissedWithoutReward = {
                                Toast.makeText(
                                    context,
                                    "विज्ञापन पूरा नहीं देखा गया। ऑडियो शुरू नहीं हुआ।",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onAdUnavailable = {
                                Toast.makeText(
                                    context,
                                    "विज्ञापन उपलब्ध नहीं है। ऑडियो शुरू किया जा रहा है...",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Fallback policy: allow audio playback if ad server is unreachable
                                onRewardEarned()
                            }
                        )
                    } else {
                        onRewardEarned()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Text(text = "विज्ञापन देखें (Watch Ad)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "रद्द करें (Cancel)", color = MaterialTheme.colorScheme.outline)
            }
        },
        modifier = modifier
    )
}
