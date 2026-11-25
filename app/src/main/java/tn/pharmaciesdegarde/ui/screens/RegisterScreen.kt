package tn.pharmaciesdegarde.ui.screens

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tn.pharmaciesdegarde.R
import tn.pharmaciesdegarde.ui.theme.*
import tn.pharmaciesdegarde.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onSuccess: (String) -> Unit,
    onGoLogin: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var role by rememberSaveable { mutableStateOf(AuthRole.UTILISATEUR) }
    var nom by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    // validation locale
    val nomError = if (nom.trim().length < 2) "Entrez votre nom complet" else null
    val emailError = if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) "Adresse e-mail invalide" else null
    val passwordError = if (password.length < 8) "8 caractères minimum" else null
    val confirmError = when {
        confirm.isEmpty() -> "Confirmez votre mot de passe"
        confirm != password -> "Les mots de passe ne correspondent pas"
        else -> null
    }

    fun submit() {
        submitted = true
        if (nomError == null && emailError == null && passwordError == null && confirmError == null) {
            viewModel.register(nom.trim(), email.trim(), password, onSuccess)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE3F1F8), Color.White, GreenSoft)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = TextInk)
            }

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
                    Text("Créer votre compte", color = TextInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Quelques informations suffisent pour commencer.", color = TextMuted, fontSize = 12.sp)

            Spacer(Modifier.height(18.dp))

            // Bande Connexion | Inscription
            AuthTabs(selected = AuthTab.REGISTER, onLogin = onGoLogin, onRegister = {})

            Spacer(Modifier.height(12.dp))

            // Choix Utilisateur | Pharmacien
            RoleSelector(selected = role, onSelect = { role = it; viewModel.clearError() })

            Spacer(Modifier.height(16.dp))

            if (role == AuthRole.PHARMACIEN) {
                Text(
                    "L'inscription des pharmaciens arrive bientôt : elle demandera des informations " +
                            "professionnelles et la validation de votre compte par l'administration.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlueSoft, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                )
            } else {
                AuthField(
                    value = nom,
                    onValueChange = { nom = it; viewModel.clearError() },
                    label = "Nom complet",
                    icon = Icons.Default.Person,
                    error = if (submitted) nomError else null
                )
                Spacer(Modifier.height(10.dp))
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
                    isPassword = true,
                    showPassword = showPassword,
                    onTogglePassword = { showPassword = !showPassword }
                )
                Spacer(Modifier.height(6.dp))
                PasswordStrength(password)

                Spacer(Modifier.height(10.dp))
                AuthField(
                    value = confirm,
                    onValueChange = { confirm = it; viewModel.clearError() },
                    label = "Confirmer le mot de passe",
                    icon = Icons.Default.Lock,
                    error = if (confirmError != null && (submitted || confirm.isNotEmpty())) confirmError else null,
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
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Créer mon compte", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "En continuant, vous acceptez les conditions d'utilisation et la politique de confidentialité.",
                    color = TextMuted, fontSize = 10.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
 fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = PrimaryBlue) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Afficher ou masquer le mot de passe",
                        tint = TextMuted
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = PrimaryBlue,
            unfocusedBorderColor = BorderLine
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

// 1 = faible, 2 = moyen, 3 = fort
private fun strengthLevel(p: String): Int {
    if (p.length < 8) return 1
    var s = 1
    if (p.any { it.isDigit() } && p.any { it.isLetter() }) s++
    if (p.length >= 12 || p.any { !it.isLetterOrDigit() }) s++
    return s
}

@Composable
private fun PasswordStrength(password: String) {
    if (password.isEmpty()) return
    val level = strengthLevel(password)
    val (label, color) = when {
        password.length < 8 -> "Trop court" to RedClosed
        level == 1 -> "Faible" to RedClosed
        level == 2 -> "Moyen" to Color(0xFFF59E0B)
        else -> "Fort" to GreenPharmacy
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < level) color else BorderLine)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}