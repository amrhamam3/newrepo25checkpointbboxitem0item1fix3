package com.amr3d.preview.pro

import android.animation.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.InputStream

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 5500L
    private lateinit var wireframeView: WireframeSplashView
    private lateinit var ringView: RingView
    private lateinit var logoImg: ImageView
    private lateinit var titleText: LaserTextView
    private lateinit var subText: TextView
    private lateinit var progressBar: GlowProgressBarView
    private val handler = Handler(Looper.getMainLooper())
    private var animId: android.animation.ValueAnimator? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }

    private val LABELS = listOf("TAP TO START", "LOADING...", "OPTIMIZING...", "READY ✓")

    override fun onCreate(savedInstanceState: Bundle?) {
        AppDisplayMode.applySavedMode(this) // لازم قبل super.onCreate عشان يتطبّق قبل ما الشاشة تترسم
        super.onCreate(savedInstanceState)

        // Fullscreen كامل
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // من واتساب/تليجرام — تجاوز الـ Splash
        if (intent?.action == Intent.ACTION_VIEW && intent?.data != null) {
            val uri = intent.data
            startActivity(Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW; data = uri
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            })
            finish(); return
        }

        setContentView(R.layout.activity_splash)

        wireframeView = findViewById(R.id.wireframeAnim)
        ringView      = findViewById(R.id.splashRingView)
        logoImg       = findViewById(R.id.splashLogo)
        titleText     = findViewById(R.id.splashTitle)
        subText       = findViewById(R.id.splashDev)
        progressBar   = findViewById(R.id.splashProgress)

        // تطبيق لون الثيم
        val theme = AppTheme.getCurrent(this)
        wireframeView.accentColor = theme.accent
        wireframeView.backgroundColorValue =
            if (AppDisplayMode.isLight(this)) 0xFFF1F2F5.toInt() else 0xFF020510.toInt()
        ringView.accent1 = theme.accent
        ringView.accent2 = theme.accentDark
        titleText.accentColor = theme.accent
        progressBar.accentColor = theme.accent

        // تحميل اللوجو من assets
        loadLogoFromAssets()

        // اخفاء كل العناصر في البداية
        listOf(logoImg, titleText, subText, progressBar).forEach { it.alpha = 0f }
        // اللوجو بيبدأ صغير جدًا ومايل في الفضاء (زي إنه جاي من بعيد) عشان يدخل
        // بإحساس "3D انترو" لما يتحرك ويثبت في مكانه بدل ما يظهر بتكبير بسيط بس
        logoImg.scaleX = 0.05f; logoImg.scaleY = 0.05f
        logoImg.rotationX = 55f
        logoImg.rotationY = -40f
        logoImg.cameraDistance = 14000f * resources.displayMetrics.density

        // لمس الشاشة يؤثر على الـ Wireframe
        window.decorView.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                wireframeView.onTouch(e.x, e.y)
                createRippleEffect(e.x, e.y)
            }
            true
        }

        startSplashSequence()
        startRenderLoop()
    }

    private fun loadLogoFromAssets() {
        try {
            val stream: InputStream = assets.open("logo.jpg")
            val bmp = BitmapFactory.decodeStream(stream)
            stream.close()
            logoImg.setImageBitmap(bmp)
        } catch (e: Exception) {
            logoImg.setImageResource(R.drawable.splash_logo)
        }
    }

    private fun startSplashSequence() {
        // اللوجو يظهر من العدم بعد 300ms — بيدخل من بعيد ومايل (3D) وبعدين يثبت مسطّح
        handler.postDelayed({
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(logoImg, "alpha", 0f, 1f).setDuration(900),
                    ObjectAnimator.ofFloat(logoImg, "scaleX", 0.05f, 1.15f, 1f).setDuration(1100),
                    ObjectAnimator.ofFloat(logoImg, "scaleY", 0.05f, 1.15f, 1f).setDuration(1100),
                    ObjectAnimator.ofFloat(logoImg, "rotationX", 55f, 0f).setDuration(1100),
                    ObjectAnimator.ofFloat(logoImg, "rotationY", -40f, 0f).setDuration(1100)
                )
                interpolator = DecelerateInterpolator(2.2f)
                start()
            }
        }, 300)

        // نبضة توهج على اللوجو
        handler.postDelayed({
            ObjectAnimator.ofFloat(logoImg, "alpha", 1f, 0.7f, 1f, 0.85f, 1f).apply {
                duration = 600; interpolator = AccelerateDecelerateInterpolator(); start()
            }
        }, 1200)

        // العنوان
        handler.postDelayed({
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f).setDuration(600),
                    ObjectAnimator.ofFloat(titleText, "translationY", 40f, 0f).setDuration(600)
                )
                interpolator = OvershootInterpolator(1.5f); start()
            }
            titleText.translationY = 40f
        }, 1400)

        // النص الفرعي
        handler.postDelayed({
            AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(subText, "alpha", 0f, 1f).setDuration(500),
                    ObjectAnimator.ofFloat(subText, "translationY", 30f, 0f).setDuration(500)
                )
                interpolator = DecelerateInterpolator(); start()
            }
            subText.translationY = 30f
        }, 1800)

        // شريط التحميل
        handler.postDelayed({
            ObjectAnimator.ofFloat(progressBar, "alpha", 0f, 1f).apply { duration=300; start() }

            animId = ValueAnimator.ofInt(0, 100).apply {
                duration = SPLASH_DURATION - 2000
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    val p = anim.animatedValue as Int
                    progressBar.progress = p
                    val lblIdx = (p / 34).coerceAtMost(LABELS.size - 1)
                    subText.text = LABELS[lblIdx]
                    progressBar.statusText = LABELS[lblIdx]
                }
                start()
            }
        }, 2000)

        // الانتقال للـ MainActivity
        handler.postDelayed({
            val root = window.decorView
            ObjectAnimator.ofFloat(root, "alpha", 1f, 0f).apply {
                duration = 500; interpolator = AccelerateInterpolator()
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                        @Suppress("DEPRECATION")
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        finish()
                    }
                })
                start()
            }
        }, SPLASH_DURATION)
    }

    private fun createRippleEffect(x: Float, y: Float) {
        val ripple = View(this).apply {
            val size = 60
            layoutParams = FrameLayout.LayoutParams(size, size).also {
                it.leftMargin = (x - size/2).toInt()
                it.topMargin  = (y - size/2).toInt()
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(AppTheme.getCurrent(this@SplashActivity).accent and 0x00FFFFFF or 0x88000000.toInt())
            }
        }
        (window.decorView as? ViewGroup)?.addView(ripple)
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(ripple, "scaleX", 1f, 5f),
                ObjectAnimator.ofFloat(ripple, "scaleY", 1f, 5f),
                ObjectAnimator.ofFloat(ripple, "alpha", 0.6f, 0f)
            )
            duration = 600
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    (window.decorView as? ViewGroup)?.removeView(ripple)
                }
            })
            start()
        }
    }

    private var frameRunnable: Runnable? = null

    private fun startRenderLoop() {
        frameRunnable = object : Runnable {
            override fun run() {
                wireframeView.updatePhysics()
                wireframeView.invalidate()
                ringView.touchForce = wireframeView.touchForce
                ringView.tick()
                titleText.update()
                titleText.invalidate()
                progressBar.onFrameTick()
                handler.postDelayed(this, 16) // ~60fps
            }
        }
        handler.post(frameRunnable!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        frameRunnable?.let { handler.removeCallbacks(it) }
        animId?.cancel()
        handler.removeCallbacksAndMessages(null)
    }
}
