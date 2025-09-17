package com.nusync

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nusync.ui.theme.Blue2
import com.nusync.ui.theme.Transparent
import com.nusync.ui.theme.White
import com.nusync.ui.theme.Yellow
import com.nusync.ui.theme.Yellow_Deep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(heading: String, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.background(Blue2),
                title = {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = heading,
                            fontSize = 30.sp,
                            color = White,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Transparent,
                    titleContentColor = White
                )
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp)
            ) {
                content(innerPadding)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarForLazyColumns(
    heading: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.background(Blue2),
                title = {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = heading,
                            fontSize = 30.sp,
                            color = White,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Transparent,
                    titleContentColor = White
                )
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                content()
            }
        }
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar3(heading: String, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.background(Blue2),
                title = {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = heading,
                            fontSize = 30.sp,
                            color = White,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Transparent,
                    titleContentColor = White
                )
            )
        },
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp)
            ) {
                content(innerPadding)
            }
        }
    )
}

@Composable
fun TextField(
    label: String,
    textValue: String,
    onValueChange: (String) -> Unit,
    textType: String = stringResource(id = R.string.regular),
    isPasswordTextField: Boolean = false,
    enabled: Boolean = true
) {
    val keyboardType = when (textType) {
        stringResource(id = R.string.regular) -> KeyboardType.Text
        stringResource(id = R.string.email) -> KeyboardType.Email
        stringResource(id = R.string.phone) -> KeyboardType.Phone
        stringResource(id = R.string.number) -> KeyboardType.Number
        else -> KeyboardType.Text
    }

    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = textValue,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 10.dp),
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPasswordTextField) KeyboardType.Password else keyboardType),
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        visualTransformation = if (isPasswordTextField) {
            if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (isPasswordTextField) {
            {
                val image =
                    if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        } else {
            null
        }
    )
}

@Composable
fun DividerLine(color1: Color, color2: Color, width: Float) {
    Column(
        modifier = Modifier
            .height(2.dp)
            .fillMaxWidth(width)
            .background(Brush.horizontalGradient(listOf(color1, color2)))
    ) {}
}

@Composable
fun WrapButtonWithBackground(toDoFunction: () -> Unit, label: String) {
    Button(
        onClick = { toDoFunction() },
        modifier = Modifier
            .padding(0.dp, 25.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(5000.dp),
        colors = ButtonDefaults.buttonColors(Blue2),
        contentPadding = PaddingValues(32.dp, 16.dp)
    ) {
        Text(
            text = label, color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun WrapButtonWithBackgroundEnabled(
    toDoFunction: () -> Unit,
    label: String,
    context: Context
) {
    var isButtonEnabled by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            if (isButtonEnabled) {
                isButtonEnabled = false
                Toast.makeText(context, "Downloading data. Button disabled for 10 seconds.", Toast.LENGTH_SHORT).show()

                toDoFunction()

                coroutineScope.launch {
                    delay(10000)
                    isButtonEnabled = true
                }
            }
        },
        modifier = Modifier
            .padding(0.dp, 25.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(5000.dp),
        colors = ButtonDefaults.buttonColors(Blue2),
        contentPadding = PaddingValues(32.dp, 16.dp),
        enabled = isButtonEnabled
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun VerticalOrLine() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .height(2.dp)
                .weight(5f)
                .background(Brush.horizontalGradient(listOf(Yellow_Deep, Yellow)))
        ) {}
        Text(
            text = stringResource(id = R.string.or),
            modifier = Modifier
                .weight(1f)
                .size(20.dp),
            textAlign = TextAlign.Center,
            color = Yellow,
        )
        Column(
            modifier = Modifier
                .height(2.dp)
                .weight(5f)
                .background(Brush.horizontalGradient(listOf(Yellow, Yellow_Deep)))
        ) {}
    }
}

@Composable
fun BorderButton(toDoFunction: () -> Unit, label: String) {
    Button(
        onClick = { toDoFunction() },
        modifier = Modifier.padding(0.dp, 25.dp),
        border = BorderStroke(2.dp, Yellow),
        shape = RoundedCornerShape(5000.dp),
        colors = ButtonDefaults.buttonColors(Transparent),
        contentPadding = PaddingValues(32.dp, 16.dp)
    ) {
        Text(
            text = label,
            color = Blue2,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun BorderRadioButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = if (isSelected) {
            ButtonDefaults.buttonColors(containerColor = Blue2)
        } else {
            ButtonDefaults.buttonColors(containerColor = Transparent)
        },
        border = BorderStroke(2.dp, Yellow),
        shape = RoundedCornerShape(5000.dp),
        contentPadding = PaddingValues(32.dp, 16.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Blue2,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun UnderlineButton(toDoFunction: () -> Unit, label: String) {
    Text(
        modifier = Modifier.clickable(enabled = true) {
            toDoFunction()
        },
        text = label,
        color = Blue2,
        fontStyle = FontStyle.Italic,
        fontSize = 24.sp,
        textDecoration = TextDecoration.Underline,
        fontWeight = FontWeight(1000),
        letterSpacing = 1.sp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownTextField(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        val screenWidth = LocalConfiguration.current.screenWidthDp.dp
        val menuWidth = screenWidth * 0.8f

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(menuWidth)
                .heightIn(max = 250.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun Top() {
    TopBarForLazyColumns("asd") { }
}