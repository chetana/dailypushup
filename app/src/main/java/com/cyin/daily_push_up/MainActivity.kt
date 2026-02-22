package com.cyin.daily_push_up

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cyin.daily_push_up.auth.GoogleAuthManager
import com.cyin.daily_push_up.auth.TokenStore
import com.cyin.daily_push_up.data.PushUpEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel

    // Stats
    private lateinit var statCurrentStreak: TextView
    private lateinit var statTotalPushups: TextView
    private lateinit var statTotalDays: TextView
    private lateinit var statBestStreak: TextView

    // Today card
    private lateinit var todayTargetText: TextView
    private lateinit var pushupCountText: TextView
    private lateinit var btnMinus: TextView
    private lateinit var btnPlus: TextView
    private lateinit var btnValidate: TextView
    private lateinit var stepperRow: LinearLayout
    private lateinit var doneBadge: LinearLayout
    private lateinit var doneBadgeText: TextView

    // Calendar
    private lateinit var monthYearText: TextView
    private lateinit var btnPrevMonth: ImageView
    private lateinit var btnNextMonth: ImageView
    private lateinit var weekDayHeaders: GridLayout
    private lateinit var calendarGrid: GridLayout

    private lateinit var swipeRefresh: SwipeRefreshLayout

    private var displayedYear = 0
    private var displayedMonth = 0 // 0-based

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (TokenStore.isLoggedIn(this)) {
            initApp()
        } else {
            performSignIn()
        }
    }

    override fun onResume() {
        super.onResume()
        // Silently refresh token if it's about to expire (within 5 min)
        if (TokenStore.isLoggedIn(this) && TokenStore.isExpiringSoon(this)) {
            lifecycleScope.launch {
                try {
                    val credential = GoogleAuthManager.signIn(this@MainActivity)
                    TokenStore.saveToken(
                        this@MainActivity,
                        credential.idToken,
                        credential.id,
                        credential.displayName
                    )
                } catch (_: Exception) {
                    // Silent refresh failed — will retry on next 401
                }
            }
        }
    }

    private fun performSignIn() {
        lifecycleScope.launch {
            try {
                val credential = GoogleAuthManager.signIn(this@MainActivity)
                TokenStore.saveToken(
                    this@MainActivity,
                    credential.idToken,
                    credential.id,       // email
                    credential.displayName
                )
                initApp()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Sign-in failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun initApp() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        bindViews()
        setupListeners()
        setupCalendarHeaders()
        observeViewModel()

        val now = Calendar.getInstance()
        displayedYear = now.get(Calendar.YEAR)
        displayedMonth = now.get(Calendar.MONTH)
    }

    private fun bindViews() {
        swipeRefresh = findViewById(R.id.swipeRefresh)
        statCurrentStreak = findViewById(R.id.statCurrentStreak)
        statTotalPushups = findViewById(R.id.statTotalPushups)
        statTotalDays = findViewById(R.id.statTotalDays)
        statBestStreak = findViewById(R.id.statBestStreak)
        todayTargetText = findViewById(R.id.todayTargetText)
        pushupCountText = findViewById(R.id.pushupCountText)
        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        btnValidate = findViewById(R.id.btnValidate)
        stepperRow = findViewById(R.id.stepperRow)
        doneBadge = findViewById(R.id.doneBadge)
        doneBadgeText = findViewById(R.id.doneBadgeText)
        monthYearText = findViewById(R.id.monthYearText)
        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)
        weekDayHeaders = findViewById(R.id.weekDayHeaders)
        calendarGrid = findViewById(R.id.calendarGrid)
    }

    private fun setupListeners() {
        swipeRefresh.setColorSchemeColors(ContextCompat.getColor(this, R.color.gold_accent))
        swipeRefresh.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.bg_card))
        swipeRefresh.setOnRefreshListener { viewModel.refresh() }

        btnMinus.setOnClickListener { viewModel.adjustPushups(-5) }
        btnPlus.setOnClickListener { viewModel.adjustPushups(5) }
        btnValidate.setOnClickListener { viewModel.validate() }

        btnPrevMonth.setOnClickListener {
            displayedMonth--
            if (displayedMonth < 0) {
                displayedMonth = 11
                displayedYear--
            }
            renderCalendar(viewModel.entries.value ?: emptyList())
        }
        btnNextMonth.setOnClickListener {
            displayedMonth++
            if (displayedMonth > 11) {
                displayedMonth = 0
                displayedYear++
            }
            renderCalendar(viewModel.entries.value ?: emptyList())
        }

        // Long press on title bar for sign-out
        findViewById<View>(R.id.statCurrentStreak).rootView.findViewById<TextView>(R.id.todayTargetText)?.setOnLongClickListener {
            showSignOutDialog()
            true
        }
    }

    private fun showSignOutDialog() {
        val email = TokenStore.getUserEmail(this) ?: "Unknown"
        AlertDialog.Builder(this)
            .setTitle("Sign Out")
            .setMessage("Signed in as $email.\nDo you want to sign out?")
            .setPositiveButton("Sign Out") { _, _ ->
                lifecycleScope.launch {
                    try {
                        GoogleAuthManager.signOut(this@MainActivity)
                    } catch (_: Exception) { }
                    TokenStore.clear(this@MainActivity)
                    recreate()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupCalendarHeaders() {
        val days = arrayOf("M", "T", "W", "T", "F", "S", "S")
        weekDayHeaders.removeAllViews()
        for (day in days) {
            val tv = TextView(this).apply {
                text = day
                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                textSize = 12f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            weekDayHeaders.addView(tv)
        }
    }

    private fun observeViewModel() {
        viewModel.stats.observe(this) { stats ->
            if (stats != null) {
                statCurrentStreak.text = stats.currentStreak.toString()
                statTotalPushups.text = stats.totalPushups.toString()
                statTotalDays.text = stats.totalDays.toString()
                statBestStreak.text = stats.longestStreak.toString()
                todayTargetText.text = "Target: ${stats.todayTarget} push-ups"

                if (stats.todayValidated) {
                    showDoneBadge(stats.totalPushups)
                } else {
                    showStepper()
                }
            }
        }

        viewModel.entries.observe(this) { entries ->
            renderCalendar(entries)
        }

        viewModel.isLoading.observe(this) { loading ->
            swipeRefresh.isRefreshing = loading
        }

        viewModel.pushupCount.observe(this) { count ->
            pushupCountText.text = count.toString()
        }

        viewModel.validateResult.observe(this) { result ->
            if (result != null) {
                if (result) {
                    Toast.makeText(this, "Push-ups validated!", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearValidateResult()
            }
        }

        viewModel.error.observe(this) { error ->
            if (error != null) {
                // If we get an auth error, re-trigger sign-in
                if (error.contains("401") || error.contains("Unauthorized", ignoreCase = true)) {
                    if (!TokenStore.isLoggedIn(this)) {
                        performSignIn()
                        return@observe
                    }
                }
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun showDoneBadge(pushups: Int) {
        stepperRow.visibility = View.GONE
        btnValidate.visibility = View.GONE
        doneBadge.visibility = View.VISIBLE
        doneBadgeText.text = "Done! $pushups push-ups validated"
    }

    private fun showStepper() {
        stepperRow.visibility = View.VISIBLE
        btnValidate.visibility = View.VISIBLE
        doneBadge.visibility = View.GONE
    }

    private fun renderCalendar(entries: List<PushUpEntry>) {
        calendarGrid.removeAllViews()

        val cal = Calendar.getInstance()
        cal.set(displayedYear, displayedMonth, 1)

        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        monthYearText.text = monthFormat.format(cal.time)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val entryMap = entries.associateBy { it.date }

        val today = Calendar.getInstance()
        val todayStr = dateFormat.format(today.time)

        // Day of week for first day (Monday=1 ... Sunday=7)
        var firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        if (firstDayOfWeek < 0) firstDayOfWeek = 6 // Sunday

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Empty cells for days before the 1st
        for (i in 0 until firstDayOfWeek) {
            val empty = FrameLayout(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = resources.getDimensionPixelSize(R.dimen.calendar_cell_height)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            calendarGrid.addView(empty)
        }

        for (day in 1..daysInMonth) {
            val dateCal = Calendar.getInstance()
            dateCal.set(displayedYear, displayedMonth, day)
            val dateStr = dateFormat.format(dateCal.time)
            val entry = entryMap[dateStr]
            val isToday = dateStr == todayStr
            val isFuture = dateCal.after(today) && !isToday

            val cellView = LayoutInflater.from(this)
                .inflate(R.layout.item_calendar_day, calendarGrid, false)
            val dayText = cellView.findViewById<TextView>(R.id.dayText)

            dayText.text = day.toString()

            when {
                entry != null && entry.validated -> {
                    dayText.background = ContextCompat.getDrawable(this, R.drawable.calendar_day_validated)
                    dayText.setTextColor(ContextCompat.getColor(this, R.color.gold_accent))
                    dayText.text = "\u2713"
                }
                isToday -> {
                    dayText.background = ContextCompat.getDrawable(this, R.drawable.calendar_day_today)
                    dayText.setTextColor(ContextCompat.getColor(this, R.color.gold_accent))
                }
                isFuture -> {
                    dayText.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
                }
                entry != null && !entry.validated -> {
                    dayText.background = ContextCompat.getDrawable(this, R.drawable.calendar_day_missed)
                    dayText.setTextColor(ContextCompat.getColor(this, R.color.missed_red))
                    dayText.text = "\u2717"
                }
                !isFuture -> {
                    // Past day with no entry - missed
                    dayText.background = ContextCompat.getDrawable(this, R.drawable.calendar_day_missed)
                    dayText.setTextColor(ContextCompat.getColor(this, R.color.missed_red))
                    dayText.text = "\u2717"
                }
            }

            // Set GridLayout params with weight
            cellView.layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = resources.getDimensionPixelSize(R.dimen.calendar_cell_height)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }

            calendarGrid.addView(cellView)
        }
    }
}
