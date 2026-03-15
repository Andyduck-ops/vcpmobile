package com.naigebao.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerEffect(modifier: Modifier = Modifier) {
 val shimmerColors = listOf(
  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
 )

 val transition = rememberInfiniteTransition(label = "shimmer")
 val translateAnim by transition.animateFloat(
  initialValue = 0f,
  targetValue = 1000f,
  animationSpec = infiniteRepeatable(
   animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
   repeatMode = RepeatMode.Restart
  ),
  label = "shimmer_translate"
 )

 val brush = Brush.linearGradient(
  colors = shimmerColors,
  start = Offset(translateAnim - 500f, translateAnim - 500f),
  end = Offset(translateAnim, translateAnim)
 )

 Box(modifier = modifier.background(brush))
}

@Composable
fun SkeletonBox(
 modifier: Modifier = Modifier,
 shape: RoundedCornerShape = RoundedCornerShape(4.dp)
) {
 ShimmerEffect(modifier = modifier.clip(shape))
}

@Composable
fun SessionListSkeleton(
 modifier: Modifier = Modifier,
 itemCount: Int = 5
) {
 Column(
  modifier = modifier
   .fillMaxWidth()
   .padding(horizontal = 12.dp, vertical = 6.dp),
  verticalArrangement = Arrangement.spacedBy(12.dp)
 ) {
  repeat(itemCount) { SessionItemSkeleton() }
 }
}

@Composable
private fun SessionItemSkeleton() {
 Row(
  modifier = Modifier
   .fillMaxWidth()
   .clip(RoundedCornerShape(8.dp))
   .background(MaterialTheme.colorScheme.surface)
   .padding(12.dp),
  verticalAlignment = Alignment.CenterVertically
 ) {
  SkeletonBox(modifier = Modifier.size(44.dp).clip(CircleShape))
  Spacer(modifier = Modifier.width(12.dp))
  Column(modifier = Modifier.weight(1f)) {
   SkeletonBox(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp))
   Spacer(modifier = Modifier.height(8.dp))
   SkeletonBox(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp))
  }
  Column(horizontalAlignment = Alignment.End) {
   SkeletonBox(modifier = Modifier.width(40.dp).height(12.dp))
   Spacer(modifier = Modifier.height(8.dp))
   SkeletonBox(modifier = Modifier.size(20.dp).clip(CircleShape))
  }
 }
}

@Composable
fun MessageListSkeleton(
 modifier: Modifier = Modifier,
 itemCount: Int = 8
) {
 Column(
  modifier = modifier
   .fillMaxWidth()
   .padding(horizontal = 12.dp, vertical = 8.dp),
  verticalArrangement = Arrangement.spacedBy(16.dp)
 ) {
  repeat(itemCount) { index -> MessageItemSkeleton(isUser = index % 2 == 0) }
 }
}

@Composable
private fun MessageItemSkeleton(isUser: Boolean) {
 Row(
  modifier = Modifier.fillMaxWidth(),
  horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
 ) {
  if (!isUser) {
   SkeletonBox(modifier = Modifier.size(32.dp).clip(CircleShape))
   Spacer(modifier = Modifier.width(8.dp))
  }
  Column(
   modifier = Modifier
    .fillMaxWidth(0.7f)
    .clip(
     RoundedCornerShape(
      topStart = 16.dp, topEnd = 16.dp,
      bottomStart = if (isUser) 16.dp else 4.dp,
      bottomEnd = if (isUser) 4.dp else 16.dp
     )
    )
    .background(MaterialTheme.colorScheme.surface)
    .padding(12.dp)
  ) {
   SkeletonBox(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp))
   Spacer(modifier = Modifier.height(6.dp))
   SkeletonBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
   if (!isUser) {
    Spacer(modifier = Modifier.height(6.dp))
    SkeletonBox(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
   }
  }
  if (isUser) {
   Spacer(modifier = Modifier.width(8.dp))
   SkeletonBox(modifier = Modifier.size(32.dp).clip(CircleShape))
  }
 }
}