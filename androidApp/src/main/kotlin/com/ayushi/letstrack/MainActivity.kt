package com.ayushi.letstrack

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ayushi.letstrack.data.FirebaseAuthManager
import com.ayushi.letstrack.data.FirebaseGroupRepository
import com.ayushi.letstrack.data.TrackingEventRepository
import com.ayushi.letstrack.data.TrackingPreferences
import com.ayushi.letstrack.location.GeofenceManager
import com.ayushi.letstrack.notification.NotificationHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private val ForestGreen = Color(0xFF173F35)
private val Green = Color(0xFF2F6B58)
private val SoftGreen = Color(0xFFE4F0EA)
private val Cream = Color(0xFFF7F4EC)
private val WarmWhite = Color(0xFFFFFEFA)
private val TextDark = Color(0xFF18332C)
private val TextMuted = Color(0xFF708078)
private val Border = Color(0xFFDCE5DF)

class MainActivity : ComponentActivity() {

    private val authManager = FirebaseAuthManager()
    private val groupRepository = FirebaseGroupRepository()

    private lateinit var preferences: TrackingPreferences

    private var userId: String? = null

    private val eventRepository = TrackingEventRepository()

    private var eventListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val locationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (locationGranted) {
                startGeofence()
            } else {
                Toast.makeText(
                    this,
                    "Location permission is required",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences = TrackingPreferences(this)

        MainScope().launch {
            try {
                userId = authManager.signInAnonymously()

                val existingGroupId = preferences.getGroupId()

                if (existingGroupId != null) {
                    startEventListener(existingGroupId)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Authentication failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        setContent {
            MaterialTheme {
                TrackingScreen()
            }
        }
    }

    private fun createGroup(
        groupName: String,
        onSuccess: (String) -> Unit
    ) {
        val currentUser = userId ?: return

        MainScope().launch {
            try {
                val groupId = groupRepository.createGroup(
                    groupName = groupName,
                    userId = currentUser
                )

                preferences.saveGroupId(groupId)

                onSuccess(groupId)

                Toast.makeText(
                    this@MainActivity,
                    "Group created successfully",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Could not create group",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun joinGroup(
        groupId: String,
        onSuccess: () -> Unit
    ) {
        val currentUser = userId ?: return

        MainScope().launch {
            try {
                groupRepository.joinGroup(
                    groupId = groupId,
                    userId = currentUser
                )

                preferences.saveGroupId(groupId)

                onSuccess()

                Toast.makeText(
                    this@MainActivity,
                    "Joined group successfully",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Could not join group",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startEventListener(groupId: String) {

        val currentUserId = userId ?: return

        eventListener?.remove()

        eventListener =
            eventRepository.listenForGroupExits(
                groupId = groupId,
                currentUserId = currentUserId
            ) {
                NotificationHelper.showExitNotification(this)
            }
    }

    private fun requestLocationPermission() {

        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    private fun startGeofence() {

        val groupId = preferences.getGroupId()

        if (groupId == null) {
            Toast.makeText(
                this,
                "Create or join a group first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
            .addOnSuccessListener { location ->

                if (location == null) {
                    Toast.makeText(
                        this,
                        "Unable to get current location",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                GeofenceManager(this).addGeofence(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radius = 200f
                )

                Toast.makeText(
                    this,
                    "Tracking started",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    @androidx.compose.runtime.Composable
    private fun TrackingScreen() {

        var groupName by remember { mutableStateOf("") }
        var groupIdInput by remember { mutableStateOf("") }

        var savedGroupId by remember {
            mutableStateOf(preferences.getGroupId())
        }

        var tracking by remember {
            mutableStateOf(false)
        }

        val background = Color(0xFFF5F5EF)
        val dark = Color(0xFF18352D)
        val green = Color(0xFF3E715D)
        val lightGreen = Color(0xFFDDE9E1)
        val muted = Color(0xFF7A817D)
        val line = Color(0xFFD9DDD8)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = background
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {

                Spacer(modifier = Modifier.height(32.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "LetsTrack",
                            color = dark,
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Keep your people close.",
                            color = muted,
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (tracking) green else Color(0xFFB7BCB8)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(38.dp))

                // Main tracking area
                Text(
                    text = if (tracking) "You're tracking" else "Ready to track",
                    color = dark,
                    fontSize = 34.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.8).sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (tracking) {
                        "Your location is being monitored within the group's 200 m zone."
                    } else {
                        "Create or join a group to start location tracking."
                    },
                    color = muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Simple status row — no card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(lightGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (tracking) "✓" else "•",
                            color = green,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = if (tracking)
                                "Tracking active"
                            else
                                "Tracking inactive",
                            color = dark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Geofence radius · 200 m",
                            color = muted,
                            fontSize = 12.sp
                        )
                    }
                }

                androidx.compose.material3.HorizontalDivider(
                    color = line
                )

                Spacer(modifier = Modifier.height(28.dp))

                // GROUP SECTION
                Text(
                    text = "GROUP",
                    color = muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (savedGroupId != null) {

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "Tracking group",
                                    color = dark,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                Text(
                                    text = savedGroupId!!,
                                    color = muted,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = "ACTIVE",
                                color = green,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                } else {

                    OutlinedTextField(
                        value = groupName,
                        onValueChange = {
                            groupName = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Group name",
                                color = muted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {

                            if (groupName.isBlank()) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Enter a group name",
                                    Toast.LENGTH_SHORT
                                ).show()

                                return@Button
                            }

                            createGroup(groupName) { id ->
                                savedGroupId = id
                                startEventListener(id)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dark
                        )
                    ) {

                        Text(
                            text = "Create group",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = line
                        )

                        Text(
                            text = "  OR  ",
                            color = muted,
                            fontSize = 11.sp
                        )

                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = line
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = groupIdInput,
                        onValueChange = {
                            groupIdInput = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Group ID",
                                color = muted
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {

                            if (groupIdInput.isBlank()) {
                                return@OutlinedButton
                            }

                            joinGroup(groupIdInput) {
                                savedGroupId = groupIdInput
                                startEventListener(groupIdInput)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {

                        Text(
                            text = "Join group",
                            color = dark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom action
                Button(
                    onClick = {
                        requestLocationPermission()
                        tracking = true
                    },
                    enabled = savedGroupId != null && !tracking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dark,
                        disabledContainerColor = Color(0xFFD5D9D5),
                        disabledContentColor = Color(0xFF8A908C)
                    )
                ) {

                    Text(
                        text = if (tracking)
                            "Tracking is active"
                        else
                            "Start tracking",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your location is used only for tracking within your group.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}