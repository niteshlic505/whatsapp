package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.IndiaGreenLight
import com.example.ui.theme.IndiaSaffron
import com.example.ui.theme.ProGold
import com.example.ui.theme.ProGoldLight
import com.example.ui.theme.WhatsAppGreenPrimary

@Composable
fun SubscriptionPaywallDialog(
    isPro: Boolean,
    onDismiss: () -> Unit,
    onTogglePro: (Boolean) -> Unit
) {
    var selectedPlan by remember { mutableStateOf("ANNUAL") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(IndiaSaffron.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🇮🇳", fontSize = 18.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Title
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "WA Automation Pro",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = IndiaSaffron.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "BHARAT ₹",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndiaSaffron,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Empower your Indian Business & Personal WhatsApp with Gemini AI",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Pro Features List
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FeatureRow("Unlimited Scheduled Messages & Festive Automations")
                        FeatureRow("AI Contact Importer (PDF, Image, Excel to +91)")
                        FeatureRow("WhatsApp Deleted Message Recovery & In/Out Audit")
                        FeatureRow("UPI Payment & GST Invoice WhatsApp Dispatch")
                        FeatureRow("Gemini 3.1 Pro AI with Thinking Mode & Indian Tone")
                        FeatureRow("Anti-Spam & TRAI Safety Quota Guards")
                    }
                }

                // Plan options (INR ₹ Pricing)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanOptionCard(
                        title = "Annual Bharat Pro (Best Value)",
                        price = "₹499 / year",
                        subtitle = "Only ₹41/month • Save 60% • GST included",
                        badge = "POPULAR",
                        isSelected = selectedPlan == "ANNUAL",
                        onSelect = { selectedPlan = "ANNUAL" }
                    )
                    PlanOptionCard(
                        title = "Monthly Plan",
                        price = "₹79 / month",
                        subtitle = "Flexible monthly recharge • Cancel anytime",
                        badge = null,
                        isSelected = selectedPlan == "MONTHLY",
                        onSelect = { selectedPlan = "MONTHLY" }
                    )
                    PlanOptionCard(
                        title = "Lifetime Indian Business License",
                        price = "₹1,499 once",
                        subtitle = "Single payment • Lifetime updates & AI access",
                        badge = "LIFETIME",
                        isSelected = selectedPlan == "LIFETIME",
                        onSelect = { selectedPlan = "LIFETIME" }
                    )
                }

                // UPI / Payment Badges Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚡ UPI • Google Pay • PhonePe • Paytm • Cards",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IndiaGreenLight
                        )
                    }
                }

                // Action button
                Button(
                    onClick = {
                        onTogglePro(true)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndiaSaffron)
                ) {
                    Text(
                        text = if (isPro) "Currently Active (Switch Plan)" else "Upgrade to Bharat Pro • Start Free Trial",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Free toggle for testing / demonstration
                if (isPro) {
                    OutlinedButton(
                        onClick = {
                            onTogglePro(false)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Downgrade to Free Tier (Demo)")
                    }
                }

                Text(
                    text = "Subscriptions renew automatically via Google Play. No commitment, cancel anytime in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = WhatsAppGreenPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontSize = 13.sp)
    }
}

@Composable
private fun PlanOptionCard(
    title: String,
    price: String,
    subtitle: String,
    badge: String?,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ProGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ProGold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ProGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProGold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = price,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) ProGold else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
