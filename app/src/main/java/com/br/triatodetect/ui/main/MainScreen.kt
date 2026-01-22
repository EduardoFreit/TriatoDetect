package com.br.triatodetect.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.br.triatodetect.R
import com.br.triatodetect.ui.component.GlobalLoadingOverlay
import com.br.triatodetect.ui.theme.*

@Composable
fun MainScreen(isLoading: Boolean, onLoginClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints {
            // RETRATO
            if (maxWidth < maxHeight) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(seed),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Imagem (substitua pelo seu drawable)
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                            .padding(horizontal = 30.dp)
                    )

                    // Texto
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Botão
                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.padding(top = 16.dp),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.google_logo_32x32),
                            contentDescription = "teste",
                            tint = seed,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            stringResource(R.string.login),
                            color = Color.Black,
                            fontSize = 16.sp,
                        )  // Adicione ícone se necessário
                    }
                }
                // PAISAGEM
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(seed)
                ) {

                    Box(
                        modifier = Modifier
                            .weight(1f)      // mesma largura da segunda
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                fontSize = 50.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Botão
                            Button(
                                onClick = onLoginClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                modifier = Modifier.padding(top = 16.dp),
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.google_logo_32x32),
                                    contentDescription = "teste",
                                    tint = seed,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    stringResource(R.string.login),
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                )  // Adicione ícone se necessário
                            }
                        }
                    }
                }
            }
            //LOADING
            if (isLoading) {
                GlobalLoadingOverlay()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(true, onLoginClick = {})
}