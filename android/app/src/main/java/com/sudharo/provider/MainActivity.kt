package com.sudharo.provider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudharo.provider.presentation.ProviderViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF123B70)
private val Blue = Color(0xFF1769D2)
private val PaleBlue = Color(0xFFEAF3FF)
private val Ink = Color(0xFF182433)
private val Muted = Color(0xFF687789)
private val Green = Color(0xFF168653)
private val Canvas = Color(0xFFF5F7FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Seed Room sample data once; repository is the source of truth.
        // ViewModel is created in SudharoApp() via the factory below.
        lifecycleScope.launch { (application as SudharoApplication).repository.seedIfEmpty() }
        setContent { SudharoTheme { SudharoApp() } }
    }
}

/** Manual ViewModel factory (no Hilt yet — see SudharoApplication). */
@Suppress("UNCHECKED_CAST")
class ProviderViewModelFactory(private val app: SudharoApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProviderViewModel(app.repository) as T
}

@Composable
private fun SudharoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Blue, onPrimary = Color.White, secondary = Green,
            background = Canvas, surface = Color.White, onSurface = Ink
        ),
        typography = Typography(),
        content = content
    )
}

private enum class Tab(val label: String) {
    Home("Home"), Requirements("Requirements"), Jobs("My Jobs"),
    Quotes("Quotations"), Profile("Profile")
}
private enum class Page { Login, Signup, VerifyEmail, ForgotPassword, Onboarding, Main, RequirementDetail, Bid, JobDetail, CreateQuote, QuoteDetail, PortfolioAdd, Notifications, EditProfile }

@Composable
private fun SudharoApp() {
    // Wired to Room + Supabase-ready repository. In demo mode (no keys in
    // local.properties) auth/bids/quotations persist locally; with keys,
    // the same calls go to Supabase + the NestJS API. See README/SETUP.
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as SudharoApplication
    val vm: ProviderViewModel = viewModel(factory = ProviderViewModelFactory(app))
    val vmMessage by vm.message.collectAsState()
    var page by remember { mutableStateOf(Page.Login) }
    var selectedTab by remember { mutableStateOf(Tab.Home) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var providerName by remember { mutableStateOf("Amit Sharma") }
    var selectedRequirement by remember { mutableStateOf("Bathroom plumbing repair") }
    var selectedJob by remember { mutableStateOf("SDH-2026-0012") }
    var selectedQuote by remember { mutableStateOf("QT-2026-0041") }
    var toast by remember { mutableStateOf<String?>(null) }

    fun goMain(tab: Tab = Tab.Home) { selectedTab = tab; page = Page.Main }

    when (page) {
        Page.Login -> AuthScreen(
            signup = { page = Page.Signup },
            forgot = { page = Page.ForgotPassword },
            login = { e, p -> email = e; password = p; vm.signIn(e, p) { name -> providerName = name; page = Page.Main } },
            google = { toast = "Connect Google OAuth / Supabase Auth to enable Google sign-in." }
        )
        Page.Signup -> SignupScreen(
            back = { page = Page.Login },
            submit = { name, e, p -> providerName = name; email = e; password = p; page = Page.VerifyEmail }
        )
        Page.VerifyEmail -> OtpScreen(
            title = "Verify your email",
            subtitle = "Enter the 6-digit code sent to $email",
            back = { page = Page.Signup },
            verify = { page = Page.Onboarding },
            resend = { toast = "OTP resend needs the configured email provider." }
        )
        Page.ForgotPassword -> ForgotScreen(
            back = { page = Page.Login },
            submit = { email = it; page = Page.VerifyEmail }
        )
        Page.Onboarding -> OnboardingScreen(
            finish = { goMain(); toast = "Profile saved locally in this UI demo." }
        )
        Page.Main -> Scaffold(
            containerColor = Canvas,
            topBar = { ProviderTopBar(providerName) { page = Page.Notifications } },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    Tab.values().forEach { tab ->
                        val icon = when (tab) {
                            Tab.Home -> Icons.Default.Home
                            Tab.Requirements -> Icons.Default.Search
                            Tab.Jobs -> Icons.Default.Work
                            Tab.Quotes -> Icons.Default.Description
                            Tab.Profile -> Icons.Default.Person
                        }
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(icon, null) },
                            label = { Text(tab.label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (selectedTab) {
                    Tab.Home -> DashboardScreen(
                        name = providerName,
                        onRequirements = { selectedTab = Tab.Requirements },
                        onQuotes = { selectedTab = Tab.Quotes },
                        onOpenRequirement = { selectedRequirement = it; page = Page.RequirementDetail },
                        onOpenJob = { selectedJob = it; page = Page.JobDetail }
                    )
                    Tab.Requirements -> RequirementsScreen(
                        onOpen = { selectedRequirement = it; page = Page.RequirementDetail }
                    )
                    Tab.Jobs -> JobsScreen(onOpen = { selectedJob = it; page = Page.JobDetail })
                    Tab.Quotes -> QuotationsScreen(
                        onCreate = { page = Page.CreateQuote },
                        onOpen = { selectedQuote = it; page = Page.QuoteDetail }
                    )
                    Tab.Profile -> ProfileScreen(
                        name = providerName,
                        onEdit = { page = Page.EditProfile },
                        onPortfolio = { page = Page.PortfolioAdd }
                    )
                }
            }
        }
        Page.RequirementDetail -> DetailScaffold("Requirement details", { goMain(Tab.Requirements) }) {
            RequirementDetailScreen(selectedRequirement, onBid = { page = Page.Bid })
        }
        Page.Bid -> DetailScaffold("Submit your bid", { page = Page.RequirementDetail }) {
            BidScreen(selectedRequirement) { goMain(Tab.Requirements); toast = "Bid saved in demo. Connect bids API." }
        }
        Page.JobDetail -> DetailScaffold("Job details", { goMain(Tab.Jobs) }) {
            JobDetailScreen(selectedJob, onQuote = { page = Page.CreateQuote }, onComplete = { toast = "Completion OTP flow must be connected to backend." })
        }
        Page.CreateQuote -> DetailScaffold("Create quotation", { goMain(Tab.Quotes) }) {
            CreateQuoteScreen { goMain(Tab.Quotes); toast = "Quotation draft created in demo." }
        }
        Page.QuoteDetail -> DetailScaffold("Quotation details", { goMain(Tab.Quotes) }) {
            QuoteDetailScreen(selectedQuote)
        }
        Page.PortfolioAdd -> DetailScaffold("Add work to portfolio", { goMain(Tab.Profile) }) {
            PortfolioAddScreen { goMain(Tab.Profile); toast = "Portfolio entry saved in demo." }
        }
        Page.Notifications -> DetailScaffold("Notifications", { goMain() }) { NotificationsScreen() }
        Page.EditProfile -> DetailScaffold("Edit profile", { goMain(Tab.Profile) }) {
            EditProfileScreen(providerName) { providerName = it; goMain(Tab.Profile) }
        }
    }
    // Surface repository/ViewModel messages (auth errors, bid saved, PDF saved…)
    LaunchedEffect(vmMessage) { vmMessage?.let { toast = it; vm.clearMessage() } }
    toast?.let { message ->
        LaunchedEffect(message) { kotlinx.coroutines.delay(2800); toast = null }
        SnackbarHost(hostState = remember { SnackbarHostState() }, modifier = Modifier.padding(12.dp)) {
            Snackbar { Text(message) }
        }
    }
}

@Composable
private fun ProviderTopBar(name: String, onNotifications: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("SUDHARO", color = Blue, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp)
            Text("Provider workspace", color = Muted, fontSize = 11.sp)
        }
        IconButton(onClick = onNotifications) {
            BadgedBox(badge = { Badge { Text("3") } }) { Icon(Icons.Default.NotificationsNone, "Notifications") }
        }
        Box(Modifier.size(38.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
            Text(name.firstOrNull()?.toString() ?: "P", color = Blue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AuthScreen(
    signup: () -> Unit, forgot: () -> Unit,
    login: (String, String) -> Unit, google: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color.White)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 26.dp).align(Alignment.Center),
            horizontalAlignment = Alignment.Start
        ) {
            BrandMark()
            Spacer(Modifier.height(34.dp))
            Text("Welcome back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Sign in to manage your service business.", color = Muted)
            Spacer(Modifier.height(26.dp))
            Field("Email address", email, { email = it }, "name@example.com")
            Spacer(Modifier.height(14.dp))
            PasswordField("Password", password, { password = it })
            TextButton(onClick = forgot, modifier = Modifier.align(Alignment.End)) { Text("Forgot password?") }
            Button(
                onClick = { login(email, password) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Login", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(16.dp))
            OrDivider()
            OutlinedButton(
                onClick = google, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("G", color = Blue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.width(10.dp)); Text("Continue with Google", color = Ink)
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("New to Sudharo? ", color = Muted)
                Text("Create account", color = Blue, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { signup() })
            }
            Spacer(Modifier.height(28.dp))
            Text("By continuing, you agree to Sudharo's Terms & Privacy Policy.", color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun BrandMark() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).background(Blue, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Handyman, null, tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("SUDHARO", color = Navy, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, letterSpacing = 1.5.sp)
            Text("Provider", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SignupScreen(back: () -> Unit, submit: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var accepted by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        TextButton(onClick = back) { Icon(Icons.Default.ArrowBack, null); Text(" Back") }
        Spacer(Modifier.height(18.dp))
        Text("Create provider account", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Start receiving genuine work opportunities.", color = Muted)
        Spacer(Modifier.height(25.dp))
        Field("Full name", name, { name = it }, "Your name")
        Spacer(Modifier.height(13.dp))
        Field("Email address", email, { email = it }, "name@example.com")
        Spacer(Modifier.height(13.dp))
        PasswordField("Password", password, { password = it })
        Spacer(Modifier.height(13.dp))
        PasswordField("Confirm password", confirm, { confirm = it })
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(accepted, { accepted = it })
            Text("I agree to the Terms of Service and Privacy Policy", fontSize = 12.sp, color = Muted)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (name.isNotBlank() && email.contains("@") && password.length >= 8 && password == confirm && accepted) submit(name, email, password) },
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
        ) { Text("Create account") }
        Text("Use at least 8 characters. Password and email validation must also run on the server.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun OtpScreen(title: String, subtitle: String, back: () -> Unit, verify: () -> Unit, resend: () -> Unit) {
    var code by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        TextButton(onClick = back) { Icon(Icons.Default.ArrowBack, null); Text(" Back") }
        Spacer(Modifier.height(45.dp))
        Box(Modifier.size(64.dp).background(PaleBlue, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.MarkEmailRead, null, tint = Blue, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(30.dp))
        OutlinedTextField(
            value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
            label = { Text("6-digit verification code") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = verify, enabled = code.length == 6, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Verify email")
        }
        TextButton(onClick = resend, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Didn't receive the code? Resend")
        }
        Text("Production: enforce OTP expiry, resend cooldown and attempt limits on the authentication provider.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ForgotScreen(back: () -> Unit, submit: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {
        TextButton(onClick = back) { Icon(Icons.Default.ArrowBack, null); Text(" Back") }
        Spacer(Modifier.height(40.dp))
        Text("Reset your password", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("We'll send a secure recovery code to your registered email.", color = Muted)
        Spacer(Modifier.height(25.dp))
        Field("Email address", email, { email = it }, "name@example.com")
        Spacer(Modifier.height(20.dp))
        Button(onClick = { if (email.contains("@")) submit(email) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Send recovery code")
        }
    }
}

@Composable
private fun OnboardingScreen(finish: () -> Unit) {
    var business by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Gwalior") }
    var category by remember { mutableStateOf("Plumbing") }
    var experience by remember { mutableStateOf("2–5 years") }
    Column(Modifier.fillMaxSize().background(Color.White).padding(22.dp)) {
        Text("Set up your business", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Help customers understand what you do.", color = Muted)
        Spacer(Modifier.height(22.dp))
        Field("Business / display name", business, { business = it }, "e.g. Sharma Home Services")
        Spacer(Modifier.height(14.dp))
        Field("Service city", city, { city = it }, "City")
        Spacer(Modifier.height(14.dp))
        Text("Primary service", fontWeight = FontWeight.SemiBold)
        listOf("Plumbing", "Electrical", "Carpentry", "Painting", "AC repair", "Cleaning").forEach {
            Row(Modifier.fillMaxWidth().clickable { category = it }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(category == it, { category = it })
                Text(it)
            }
        }
        Text("Experience", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("0–1 year", "2–5 years", "5+ years").forEach {
                FilterChip(selected = experience == it, onClick = { experience = it }, label = { Text(it) })
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = finish, enabled = business.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Complete profile")
        }
    }
}

@Composable
private fun DashboardScreen(
    name: String, onRequirements: () -> Unit, onQuotes: () -> Unit,
    onOpenRequirement: (String) -> Unit, onOpenJob: (String) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Spacer(Modifier.height(10.dp))
            Text("Good morning, ${name.substringBefore(" ")} 👋", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("Here's what's happening with your business today.", color = Muted, fontSize = 13.sp)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("New requests", "12", Icons.Default.Inbox, Blue, Modifier.weight(1f))
                StatCard("Pending quotes", "05", Icons.Default.Description, Color(0xFFB7791F), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Active jobs", "03", Icons.Default.Work, Green, Modifier.weight(1f))
                StatCard("This month", "₹8,500", Icons.Default.AccountBalanceWallet, Color(0xFF7654C8), Modifier.weight(1f))
            }
        }
        item { SectionHeader("New requirements", "View all", onRequirements) }
        items(requirementSamples.take(2)) { RequirementCard(it, { onOpenRequirement(it.title) }) }
        item { SectionHeader("Upcoming work", "View jobs", { onOpenJob("SDH-2026-0012") }) }
        item { JobCard("SDH-2026-0012", "Kitchen sink leakage repair", "Today, 4:00 PM", "Confirmed") { onOpenJob("SDH-2026-0012") } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PaleBlue), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Keep your profile updated", fontWeight = FontWeight.Bold, color = Navy)
                        Text("Add work photos to build customer trust.", fontSize = 12.sp, color = Muted)
                    }
                    Icon(Icons.Default.PhotoLibrary, null, tint = Blue, modifier = Modifier.size(32.dp))
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, null, tint = tint)
            Spacer(Modifier.height(13.dp))
            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 11.sp, color = Muted)
        }
    }
}

private data class Requirement(val title: String, val category: String, val location: String, val budget: String, val deadline: String, val posted: String)
private val requirementSamples = listOf(
    Requirement("Bathroom plumbing repair", "Plumbing", "City Centre, Gwalior", "₹1,000–₹2,000", "15 Nov 2026", "2 hours ago"),
    Requirement("Modular kitchen cabinet fitting", "Carpentry", "Thatipur, Gwalior", "₹3,000–₹5,000", "16 Nov 2026", "4 hours ago"),
    Requirement("AC servicing", "AC repair", "Morar, Gwalior", "₹500–₹900", "17 Nov 2026", "Yesterday")
)

@Composable
private fun RequirementsScreen(onOpen: (String) -> Unit) {
    var filter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text("Requirements", fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
        Text("Find work that matches your services.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(15.dp))
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search requirements") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(14.dp), singleLine = true)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("All", "New", "Bidding", "Shortlisted").forEach {
                FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it) })
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)) {
            items(requirementSamples.filter { it.title.contains(query, true) || it.location.contains(query, true) }) {
                RequirementCard(it) { onOpen(it.title) }
            }
        }
    }
}

@Composable
private fun RequirementCard(item: Requirement, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = PaleBlue, shape = RoundedCornerShape(8.dp)) {
                    Text(item.category, color = Blue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(item.posted, color = Muted, fontSize = 10.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Muted, modifier = Modifier.size(15.dp))
                Text(item.location, color = Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CUSTOMER BUDGET", fontSize = 9.sp, color = Muted)
                    Text(item.budget, color = Ink, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("DEADLINE", fontSize = 9.sp, color = Muted)
                    Text(item.deadline, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(11.dp)) {
                Text("View requirement"); Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun RequirementDetailScreen(title: String, onBid: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas)) {
        Column(Modifier.weight(1f).padding(18.dp)) {
            Text("REQ-2026-0184  •  OPEN", color = Blue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Posted 2 hours ago · Plumbing", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(18.dp))
            InfoCard("Customer description", "Water leakage under the bathroom washbasin. Need inspection and repair. Please share an estimate and availability.")
            Spacer(Modifier.height(12.dp))
            InfoCard("Location", "City Centre, Gwalior\nExact address shared after job confirmation.")
            Spacer(Modifier.height(12.dp))
            InfoCard("Budget and deadline", "Budget: ₹1,000–₹2,000\nComplete by: 15 November 2026")
            Spacer(Modifier.height(12.dp))
            InfoCard("Customer attachments", "2 photos attached · Preview available after secure media integration")
        }
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Button(onClick = onBid, modifier = Modifier.fillMaxWidth().padding(18.dp).height(52.dp)) { Text("Submit your bid") }
        }
    }
}

@Composable
private fun BidScreen(requirement: String, submit: () -> Unit) {
    var price by remember { mutableStateOf("1500") }
    var visit by remember { mutableStateOf("100") }
    var duration by remember { mutableStateOf("1 day") }
    var details by remember { mutableStateOf("Labour included. Material cost after inspection.") }
    Column(Modifier.fillMaxSize().background(Color.White).padding(18.dp)) {
        Text(requirement, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("Your offer to the customer", color = Muted)
        Spacer(Modifier.height(20.dp))
        Field("Estimated price (₹)", price, { price = it.filter(Char::isDigit) }, "1500", KeyboardType.Number)
        Spacer(Modifier.height(13.dp))
        Field("Visit charges (₹)", visit, { visit = it.filter(Char::isDigit) }, "100", KeyboardType.Number)
        Spacer(Modifier.height(13.dp))
        Field("Estimated completion time", duration, { duration = it }, "e.g. 1 day")
        Spacer(Modifier.height(13.dp))
        OutlinedTextField(details, { details = it }, label = { Text("Work details and conditions") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.weight(1f))
        Button(onClick = submit, enabled = price.toLongOrNull() != null && price.toLong() > 0, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Submit bid") }
        Text("A bid is an estimate, not a confirmed booking.", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp))
    }
}

@Composable
private fun JobsScreen(onOpen: (String) -> Unit) {
    var filter by remember { mutableStateOf("Active") }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("My jobs", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Track confirmed work and deadlines.", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Active", "Upcoming", "Completed").forEach { FilterChip(filter == it, { filter = it }, label = { Text(it) }) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 10.dp)) {
            items(listOf(
                Triple("SDH-2026-0012", "Kitchen sink leakage repair", "Today, 4:00 PM"),
                Triple("SDH-2026-0010", "Bathroom tap replacement", "Tomorrow, 11:00 AM"),
                Triple("SDH-2026-0008", "Water motor inspection", "18 Nov, 10:00 AM")
            )) { JobCard(it.first, it.second, it.third, if (filter == "Completed") "Completed" else "In progress") { onOpen(it.first) } }
        }
    }
}

@Composable
private fun JobCard(id: String, title: String, date: String, status: String, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(id, color = Muted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                StatusPill(status)
            }
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 7.dp)) {
                Icon(Icons.Default.CalendarToday, null, tint = Muted, modifier = Modifier.size(14.dp))
                Text("  $date", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text("Details →", color = Blue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun JobDetailScreen(id: String, onQuote: () -> Unit, onComplete: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas).padding(18.dp)) {
        Text(id, color = Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("Kitchen sink leakage repair", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Customer: Rahul M. · Gwalior", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        InfoCard("Schedule", "13 November 2026 · 4:00 PM\nDeadline: 14 November 2026")
        Spacer(Modifier.height(12.dp))
        InfoCard("Accepted quotation", "QT-2026-0041 · ₹2,000\nCustomer approved")
        Spacer(Modifier.height(12.dp))
        InfoCard("Work progress", "Confirmed  ✓\nVisit scheduled  ✓\nWork started  •\nCompletion pending")
        Spacer(Modifier.height(12.dp))
        InfoCard("Completion process", "Upload final work photos, request customer OTP, then enter the OTP shared by the customer. Backend verification is mandatory.")
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onQuote, modifier = Modifier.fillMaxWidth()) { Text("View / create quotation") }
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp)) { Text("Request completion verification") }
    }
}

@Composable
private fun QuotationsScreen(onCreate: () -> Unit, onOpen: (String) -> Unit) {
    var filter by remember { mutableStateOf("All") }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Quotations", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("Create, send and track estimates.", color = Muted, fontSize = 13.sp)
            }
            FilledTonalIconButton(onClick = onCreate) { Icon(Icons.Default.Add, "Create quotation") }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("All", "Draft", "Sent", "Accepted").forEach { FilterChip(filter == it, { filter = it }, label = { Text(it) }) }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 10.dp)) {
            items(listOf(
                Triple("QT-2026-0041", "Rahul M. · Kitchen repair", "₹2,000"),
                Triple("QT-2026-0039", "Priya S. · Bathroom fitting", "₹3,500"),
                Triple("QT-2026-0036", "Ankit R. · Tap replacement", "₹850")
            )) {
                Card(Modifier.fillMaxWidth().clickable { onOpen(it.first) }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Row { Text(it.first, color = Muted, fontSize = 11.sp); Spacer(Modifier.weight(1f)); StatusPill(if (it.first.endsWith("41")) "Accepted" else "Sent") }
                        Text(it.second, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(it.third, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Spacer(Modifier.weight(1f)); Text("View details →", color = Blue, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.Default.Add, null); Text("  Create quotation") }
    }
}

@Composable
private fun CreateQuoteScreen(done: () -> Unit) {
    var customer by remember { mutableStateOf("Rahul Mehta") }
    var work by remember { mutableStateOf("Kitchen sink leakage repair") }
    var labour by remember { mutableStateOf("1000") }
    var material by remember { mutableStateOf("800") }
    var visit by remember { mutableStateOf("100") }
    var other by remember { mutableStateOf("100") }
    var terms by remember { mutableStateOf("Estimate valid for 7 days. Additional work requires customer approval.") }
    val total = listOf(labour, material, visit, other).sumOf { it.toLongOrNull() ?: 0L }
    LazyColumn(Modifier.fillMaxSize().background(Color.White).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("New quotation", fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("Create an itemised estimate for your customer.", color = Muted, fontSize = 13.sp) }
        item { Field("Customer", customer, { customer = it }, "Customer name") }
        item { Field("Work title", work, { work = it }, "Description") }
        item { Text("Price breakdown", fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        item { Field("Labour charges (₹)", labour, { labour = it.filter(Char::isDigit) }, "0", KeyboardType.Number) }
        item { Field("Material charges (₹)", material, { material = it.filter(Char::isDigit) }, "0", KeyboardType.Number) }
        item { Field("Visiting charges (₹)", visit, { visit = it.filter(Char::isDigit) }, "0", KeyboardType.Number) }
        item { Field("Other charges (₹)", other, { other = it.filter(Char::isDigit) }, "0", KeyboardType.Number) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = PaleBlue), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Quotation total", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("₹$total", fontSize = 22.sp, color = Navy, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { OutlinedTextField(terms, { terms = it }, label = { Text("Terms and conditions") }, modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(14.dp)) }
        item { Button(onClick = done, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = customer.isNotBlank() && work.isNotBlank() && total > 0) { Text("Save quotation draft") } }
        item { Text("Sending, customer approval and PDF generation require backend integration.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun QuoteDetailScreen(id: String) {
    Column(Modifier.fillMaxSize().background(Canvas).padding(18.dp)) {
        Text(id, color = Blue, fontWeight = FontWeight.Bold)
        Text("Kitchen sink leakage repair", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        StatusPill("Accepted")
        Spacer(Modifier.height(16.dp))
        InfoCard("Customer", "Rahul Mehta\nGwalior, Madhya Pradesh")
        Spacer(Modifier.height(12.dp))
        InfoCard("Pricing", "Labour                 ₹1,000\nMaterials             ₹800\nVisiting fee           ₹100\nOther                   ₹100\n\nTOTAL                 ₹2,000")
        Spacer(Modifier.height(12.dp))
        InfoCard("Terms", "Estimate valid for 7 days. Additional work requires customer approval.")
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PictureAsPdf, null); Text("  Download PDF (connect PDF service)") }
    }
}

@Composable
private fun ProfileScreen(name: String, onEdit: () -> Unit, onPortfolio: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Text("My profile", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).background(PaleBlue, CircleShape), contentAlignment = Alignment.Center) {
                        Text(name.firstOrNull()?.toString() ?: "A", fontSize = 30.sp, color = Blue, fontWeight = FontWeight.Bold)
                    }
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.padding(top = 10.dp))
                    Text("Sharma Home Services", color = Muted, fontSize = 12.sp)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("4.8 ★", fontWeight = FontWeight.Bold); Text("Rating", color = Muted, fontSize = 10.sp) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("42", fontWeight = FontWeight.Bold); Text("Jobs done", color = Muted, fontSize = 10.sp) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("3 yrs", fontWeight = FontWeight.Bold); Text("Experience", color = Muted, fontSize = 10.sp) }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Edit business profile") }
                }
            }
        }
        item { SectionHeader("Services offered", "Edit", onEdit) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Plumbing", "Bathroom repair", "Pipe fitting").forEach { AssistChip(onClick = {}, label = { Text(it) }) } } }
        item { SectionHeader("Work portfolio", "Add work", onPortfolio) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhotoLibrary, null, tint = Blue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("Add completed projects", fontWeight = FontWeight.Bold); Text("Upload before/after photos and descriptions.", color = Muted, fontSize = 11.sp) }
                    IconButton(onClick = onPortfolio) { Icon(Icons.Default.Add, null) }
                }
            }
        }
        item { SectionHeader("Account", null, {}) }
        item { MenuRow(Icons.Default.Security, "Security and login") {} }
        item { MenuRow(Icons.Default.HelpOutline, "Help and support") {} }
        item { MenuRow(Icons.Default.Policy, "Privacy and terms") {} }
        item { MenuRow(Icons.Default.Logout, "Log out", {}) }
    }
}

@Composable
private fun PortfolioAddScreen(done: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Plumbing") }
    var description by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Color.White).padding(18.dp)) {
        Text("Add portfolio project", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Show customers the quality of your work.", color = Muted)
        Spacer(Modifier.height(20.dp))
        Card(colors = CardDefaults.cardColors(containerColor = PaleBlue), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().height(145.dp)) {
            Column(Modifier.fillMaxSize().clickable { }.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.AddPhotoAlternate, null, tint = Blue, modifier = Modifier.size(34.dp))
                Text("Upload project photos", color = Blue, fontWeight = FontWeight.Bold)
                Text("Before, during and after images", color = Muted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Field("Project title", title, { title = it }, "e.g. Bathroom renovation")
        Spacer(Modifier.height(12.dp))
        Text("Category", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Plumbing", "Electrical", "Carpentry").forEach { FilterChip(category == it, { category = it }, label = { Text(it) }) }
        }
        OutlinedTextField(description, { description = it }, label = { Text("What work did you do?") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.weight(1f))
        Button(onClick = done, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save portfolio project") }
    }
}

@Composable
private fun EditProfileScreen(current: String, save: (String) -> Unit) {
    var name by remember { mutableStateOf(current) }
    var business by remember { mutableStateOf("Sharma Home Services") }
    var about by remember { mutableStateOf("Residential plumbing and repair services.") }
    Column(Modifier.fillMaxSize().background(Color.White).padding(18.dp)) {
        Text("Business information", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Field("Provider name", name, { name = it }, "Full name")
        Spacer(Modifier.height(12.dp))
        Field("Business name", business, { business = it }, "Business name")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(about, { about = it }, label = { Text("About your business") }, modifier = Modifier.fillMaxWidth(), minLines = 4, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.weight(1f))
        Button(onClick = { save(name) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save changes") }
    }
}

@Composable
private fun NotificationsScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(listOf(
            "New requirement near City Centre" to "A customer posted a plumbing repair request.",
            "Quotation accepted" to "Rahul accepted quotation QT-2026-0041.",
            "Deadline reminder" to "Your upcoming job is due tomorrow."
        )) { (title, body) ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Notifications, null, tint = Blue)
                    Spacer(Modifier.width(12.dp))
                    Column { Text(title, fontWeight = FontWeight.Bold); Text(body, color = Muted, fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onValue: (String) -> Unit, placeholder: String, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        placeholder = { Text(placeholder) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
    )
}
@Composable
private fun PasswordField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(), singleLine = true,
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
    )
}
@Composable
private fun OrDivider() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f)); Text("  OR  ", color = Muted, fontSize = 11.sp); HorizontalDivider(Modifier.weight(1f))
    }
}
@Composable
private fun SectionHeader(title: String, action: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (action != null) Text(action, color = Blue, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.clickable { onClick() })
    }
}
@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp)) {
            Text(title, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(body, modifier = Modifier.padding(top = 7.dp), fontSize = 14.sp, lineHeight = 21.sp)
        }
    }
}
@Composable
private fun StatusPill(status: String) {
    val color = when (status.lowercase()) {
        "accepted", "completed" -> Green
        "sent", "in progress" -> Blue
        else -> Color(0xFFB7791F)
    }
    Surface(color = color.copy(alpha = .10f), shape = RoundedCornerShape(20.dp)) {
        Text(status, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}
@Composable
private fun MenuRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Muted)
        Spacer(Modifier.width(14.dp))
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}
@Composable
private fun DetailScaffold(title: String, back: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Canvas)) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, null) }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Column(Modifier.fillMaxSize(), content = content)
    }
}
