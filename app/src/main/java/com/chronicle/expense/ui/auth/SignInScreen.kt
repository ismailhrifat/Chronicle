package com.chronicle.expense.ui.auth

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chronicle.expense.MainViewModel
import com.chronicle.expense.R
import com.chronicle.expense.ui.components.FieldShape
import com.chronicle.expense.ui.theme.C
import com.chronicle.expense.ui.theme.Type

@Composable
fun SignInScreen(vm: MainViewModel) {
    val activity = LocalActivity.current
    Column(
        Modifier
            .fillMaxSize()
            .background(C.bg)
            .systemBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(C.primarySoft),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground), null,
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(C.primary),
                modifier = Modifier.size(132.dp),
            )
        }
        Text("Chronicle", style = Type.display.copy(fontSize = Type.display.fontSize * 0.88f), color = C.text, modifier = Modifier.padding(top = 28.dp))
        Text(
            "A calm, honest record of where\nyour money goes.",
            style = Type.body, color = C.textSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        Spacer(Modifier.weight(1.3f))

        vm.signInError?.let {
            Text(
                it, style = Type.caption, color = C.danger, textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 14.dp),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(FieldShape)
                .background(C.surface)
                .border(1.dp, C.hairline, FieldShape)
                .clickable(enabled = !vm.signingIn) { activity?.let(vm::signIn) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (vm.signingIn) {
                CircularProgressIndicator(Modifier.size(20.dp), color = C.primary, strokeWidth = 2.dp)
            } else {
                Image(painterResource(R.drawable.ic_google), null, Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Continue with Google", style = Type.bodyMedium, color = C.text)
        }
        Text(
            "Your records are stored privately under your Google account.",
            style = Type.caption, color = C.textTertiary, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 28.dp),
        )
    }
}
