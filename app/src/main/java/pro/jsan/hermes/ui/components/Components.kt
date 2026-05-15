package pro.jsan.hermes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jsan.hermes.ui.theme.*

@Composable
fun GoldButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.sizeIn(minHeight = 48.dp),
        shape = RoundedCornerShape(9999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            Modifier
                .background(
                    Brush.linearGradient(listOf(Primary, PrimaryContainer)),
                    RoundedCornerShape(9999.dp)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(text, color = OnPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun RecessedField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceContainerLowest)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .onFocusChanged { focused = it.isFocused },
            textStyle = TextStyle(color = OnSurface, fontSize = 15.sp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = OnSurfaceVariant, fontSize = 15.sp)
                inner()
            }
        )
        Box(Modifier.fillMaxWidth().height(2.dp).background(if (focused) Primary else Color.Transparent))
    }
}
