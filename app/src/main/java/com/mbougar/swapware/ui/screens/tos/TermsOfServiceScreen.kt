package com.mbougar.swapware.ui.screens.tos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("SwapWare Terms of Service", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                """
                1. Introduction
                   Welcome to SwapWare! These terms and conditions outline the rules and regulations for the use of SwapWare's Application.
                
                2. Acceptance of Terms
                   By accessing this application, we assume you accept these terms and conditions. Do not continue to use SwapWare if you do not agree to take all of the terms and conditions stated on this page.
                
                3. User Accounts
                   When you create an account with us, you must provide us information that is accurate, complete, and current at all times. Failure to do so constitutes a breach of the Terms, which may result in immediate termination of your account on our Service.
                
                4. Prohibited Uses
                   You may use the application only for lawful purposes and in accordance with Terms. You agree not to use the application:
                   - In any way that violates any applicable national or international law or regulation.
                   - To engage in any other conduct that restricts or inhibits anyone's use or enjoyment of the application.
                
                5. Content
                   Our Service allows you to post, link, store, share and otherwise make available certain information, text, graphics, videos, or other material ("Content"). You are responsible for the Content that you post on or through the Service, including its legality, reliability, and appropriateness.
                
                6. Termination
                   We may terminate or suspend your account immediately, without prior notice or liability, for any reason whatsoever, including without limitation if you breach the Terms.
                
                7. Changes to Terms
                   We reserve the right, at our sole discretion, to modify or replace these Terms at any time.
                
                8. Contact Us
                   If you have any questions about these Terms, please contact us.
                
                """.trimIndent(),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}