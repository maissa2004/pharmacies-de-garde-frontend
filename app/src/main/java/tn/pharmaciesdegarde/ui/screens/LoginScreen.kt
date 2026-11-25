package tn.pharmaciesdegarde.ui.screens

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tn.pharmaciesdegarde.R
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onSuccess: (String) -> Unit,
    onGoRegister: () -> Unit,
    onBack: (() -> Unit)? = null,          // optionnel : la flèche n'apparaît que s'il est fourni
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    // validation locale (même principe que l'inscription)
    val emailError =
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) "Adresse e-mail invalide" else null
    val passwordError = if (password.isEmpty()) "Entrez votre mot de passe" else null

    fun submit() {
        submitted = true
        if (emailError == null && passwordError == null) {
            viewModel.login(email, password, onSuccess)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE3F1F8), Color.White, GreenSoft)))
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = this@BoxWithConstraints.maxHeight)                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center // centre le contenu quand il y a de la place
            ) {


                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_pharmacie),
                        contentDescription = "Logo",
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "PHARMACIES DE GARDE", color = GreenPharmacy,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                        Text(
                            "Connexion",
                            color = TextInk,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Content de vous revoir. Connectez-vous pour continuer.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(18.dp))

                // Bande Connexion | Inscription
                AuthTabs(selected = AuthTab.LOGIN, onLogin = {}, onRegister = onGoRegister)

                Spacer(Modifier.height(20.dp))

                AuthField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearError() },
                    label = "Adresse e-mail",
                    icon = Icons.Default.Email,
                    error = if (submitted) emailError else null,
                    keyboardType = KeyboardType.Email
                )
                Spacer(Modifier.height(10.dp))
                AuthField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearError() },
                    label = "Mot de passe",
                    icon = Icons.Default.Lock,
                    error = if (submitted) passwordError else null,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onDone = { focusManager.clearFocus(); submit() },
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )

                state.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = RedClosed, fontSize = 12.sp)
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = { submit() },
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Se connecter", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onGoRegister,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Pas de compte ? S'inscrire", color = PrimaryBlue, fontSize = 13.sp)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}