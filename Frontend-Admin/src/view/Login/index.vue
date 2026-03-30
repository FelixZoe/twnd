<!-- build: 20260330-v26 Login · Clean dark mobile bg, no color shifting -->
<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useUserStore } from '@/stores'
import { sendCode } from '@/api/auth'

const userStore = useUserStore()
const loginFormRef = ref(null)
const loading = ref(false)
const sending = ref(false)
const isCounting = ref(false)
const countdown = ref(60)
const showPassword = ref(false)
let timer = null

/* =====================================================
   Mouse tracking
   ===================================================== */
const mouseX = ref(0)
const mouseY = ref(0)

const onMouseMove = (e) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

/* =====================================================
   Character refs
   ===================================================== */
const purpleRef = ref(null)
const blackRef = ref(null)
const yellowRef = ref(null)
const orangeRef = ref(null)

/* =====================================================
   Blinking – random interval 3-7 s, duration 150 ms
   ===================================================== */
const isPurpleBlinking = ref(false)
const isBlackBlinking = ref(false)
let purpleBlinkTimer = null
let blackBlinkTimer = null

const scheduleBlink = (flagRef) => {
  const delay = Math.random() * 4000 + 3000
  return setTimeout(() => {
    flagRef.value = true
    setTimeout(() => {
      flagRef.value = false
      if (flagRef === isPurpleBlinking) purpleBlinkTimer = scheduleBlink(flagRef)
      else blackBlinkTimer = scheduleBlink(flagRef)
    }, 150)
  }, delay)
}

/* =====================================================
   Typing / Focus states
   ===================================================== */
const isTyping = ref(false)
const isLookingAtEachOther = ref(false)
const isPurplePeeking = ref(false)
const usernameFocused = ref(false)
const passwordFocused = ref(false)
const codeFocused = ref(false)
let lookTimer = null
let peekTimer = null

/* When user focuses an input → characters briefly look at each other */
watch(isTyping, (v) => {
  clearTimeout(lookTimer)
  if (v) {
    isLookingAtEachOther.value = true
    lookTimer = setTimeout(() => { isLookingAtEachOther.value = false }, 800)
  } else {
    isLookingAtEachOther.value = false
  }
})

/* Purple sneaks a peek when password is visible */
watch([() => loginForm.password, showPassword, isPurplePeeking], () => {
  clearTimeout(peekTimer)
  if (loginForm.password.length > 0 && showPassword.value) {
    peekTimer = setTimeout(() => {
      isPurplePeeking.value = true
      setTimeout(() => { isPurplePeeking.value = false }, 800)
    }, Math.random() * 3000 + 2000)
  } else {
    isPurplePeeking.value = false
  }
})

/* =====================================================
   Entrance animation
   ===================================================== */
const entered = ref(false)

/* =====================================================
   Position calculations (match reference exactly)
   ===================================================== */
const calcPosition = (el) => {
  if (!el) return { faceX: 0, faceY: 0, bodySkew: 0 }
  const rect = el.getBoundingClientRect()
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 3
  const dx = mouseX.value - cx
  const dy = mouseY.value - cy
  return {
    faceX: Math.max(-15, Math.min(15, dx / 20)),
    faceY: Math.max(-10, Math.min(10, dy / 30)),
    bodySkew: Math.max(-6, Math.min(6, -dx / 120))
  }
}

const purplePos = computed(() => calcPosition(purpleRef.value))
const blackPos = computed(() => calcPosition(blackRef.value))
const yellowPos = computed(() => calcPosition(yellowRef.value))
const orangePos = computed(() => calcPosition(orangeRef.value))

/* ---- Eyeball pupil calculation ---- */
const calcPupil = (eyeEl, maxDist = 5, forceX, forceY) => {
  if (forceX !== undefined && forceY !== undefined) return { x: forceX, y: forceY }
  if (!eyeEl) return { x: 0, y: 0 }
  const r = eyeEl.getBoundingClientRect()
  const cx = r.left + r.width / 2
  const cy = r.top + r.height / 2
  const dx = mouseX.value - cx
  const dy = mouseY.value - cy
  const dist = Math.min(Math.sqrt(dx * dx + dy * dy), maxDist)
  const angle = Math.atan2(dy, dx)
  return { x: Math.cos(angle) * dist, y: Math.sin(angle) * dist }
}

/* Per-eye refs for pupil tracking */
const purpleEyeL = ref(null)
const purpleEyeR = ref(null)
const blackEyeL = ref(null)
const blackEyeR = ref(null)

/* Computed pupil positions */
const pwdVisible = computed(() => loginForm.password.length > 0 && showPassword.value)
const pwdHidden = computed(() => loginForm.password.length > 0 && !showPassword.value)

const purplePupilForce = computed(() => {
  if (pwdVisible.value) return isPurplePeeking.value ? { x: 4, y: 5 } : { x: -4, y: -4 }
  if (isLookingAtEachOther.value) return { x: 3, y: 4 }
  return null
})
const blackPupilForce = computed(() => {
  if (pwdVisible.value) return { x: -4, y: -4 }
  if (isLookingAtEachOther.value) return { x: 0, y: -4 }
  return null
})
const orangePupilForce = computed(() => pwdVisible.value ? { x: -5, y: -4 } : null)
const yellowPupilForce = computed(() => pwdVisible.value ? { x: -5, y: -4 } : null)

const pPupilL = computed(() => calcPupil(purpleEyeL.value, 5, purplePupilForce.value?.x, purplePupilForce.value?.y))
const pPupilR = computed(() => calcPupil(purpleEyeR.value, 5, purplePupilForce.value?.x, purplePupilForce.value?.y))
const bPupilL = computed(() => calcPupil(blackEyeL.value, 4, blackPupilForce.value?.x, blackPupilForce.value?.y))
const bPupilR = computed(() => calcPupil(blackEyeR.value, 4, blackPupilForce.value?.x, blackPupilForce.value?.y))
const oPupilL = computed(() => calcPupil(null, 5, orangePupilForce.value?.x, orangePupilForce.value?.y))
const oPupilR = computed(() => calcPupil(null, 5, orangePupilForce.value?.x, orangePupilForce.value?.y))
const yPupilL = computed(() => calcPupil(null, 5, yellowPupilForce.value?.x, yellowPupilForce.value?.y))
const yPupilR = computed(() => calcPupil(null, 5, yellowPupilForce.value?.x, yellowPupilForce.value?.y))

/* =====================================================
   Form
   ===================================================== */
const loginForm = reactive({
  username: '',
  password: '',
  code: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

const codeButtonText = computed(() =>
  isCounting.value ? `${countdown.value}s` : '获取验证码'
)

const startCountdown = () => {
  isCounting.value = true
  countdown.value = 60
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      isCounting.value = false
      countdown.value = 60
    }
  }, 1000)
}

/* =====================================================
   Jigsaw Puzzle Captcha
   ===================================================== */
const showCaptcha = ref(false)
const captchaVerified = ref(false)
const sliderLeft = ref(0)
const isDragging = ref(false)
const captchaTarget = ref(0)
const captchaPassed = ref(false)
const captchaFailed = ref(false)
const captchaCanvasRef = ref(null)
const pieceCanvasRef = ref(null)
const captchaLoading = ref(false)
let dragStartX = 0

const CAPTCHA_TOLERANCE = 12
const CANVAS_W = 328
const CANVAS_H = 175
const PIECE_S = 50
const TAB_R = 10  // radius of the circular tab
const PIECE_FULL = PIECE_S + TAB_R * 2 + 6  // piece canvas size incl. tab overhang
const SLIDER_MAX = CANVAS_W - PIECE_FULL
const PIECE_Y_MIN = TAB_R + 12
const PIECE_Y_MAX = CANVAS_H - PIECE_S - TAB_R - 12
const captchaTargetY = ref(0)

/* ---- Jigsaw path (smooth Bezier curves – premium GeeTest-style) ---- */
const jigsawPath = (ctx, x, y, s, tabR) => {
  const r = tabR
  const neck = r * 0.65  // narrower neck for organic feel
  ctx.beginPath()
  ctx.moveTo(x, y)
  // Top edge → tab protruding up (smooth Bézier)
  ctx.lineTo(x + s * 0.36 - neck, y)
  ctx.bezierCurveTo(
    x + s * 0.36 - neck * 0.3, y,
    x + s * 0.36 - r * 0.9, y - r * 1.1,
    x + s * 0.38, y - r * 1.1
  )
  ctx.bezierCurveTo(
    x + s * 0.40 + r * 0.9, y - r * 1.1,
    x + s * 0.40 + neck * 0.3, y,
    x + s * 0.40 + neck, y
  )
  ctx.lineTo(x + s, y)
  // Right edge → tab protruding right (smooth Bézier)
  ctx.lineTo(x + s, y + s * 0.36 - neck)
  ctx.bezierCurveTo(
    x + s, y + s * 0.36 - neck * 0.3,
    x + s + r * 1.1, y + s * 0.36 - r * 0.9,
    x + s + r * 1.1, y + s * 0.38
  )
  ctx.bezierCurveTo(
    x + s + r * 1.1, y + s * 0.40 + r * 0.9,
    x + s, y + s * 0.40 + neck * 0.3,
    x + s, y + s * 0.40 + neck
  )
  ctx.lineTo(x + s, y + s)
  // Bottom edge → indent tab (smooth Bézier, reversed)
  ctx.lineTo(x + s * 0.64 + neck, y + s)
  ctx.bezierCurveTo(
    x + s * 0.64 + neck * 0.3, y + s,
    x + s * 0.64 + r * 0.9, y + s + r * 1.1,
    x + s * 0.62, y + s + r * 1.1
  )
  ctx.bezierCurveTo(
    x + s * 0.60 - r * 0.9, y + s + r * 1.1,
    x + s * 0.60 - neck * 0.3, y + s,
    x + s * 0.60 - neck, y + s
  )
  ctx.lineTo(x, y + s)
  // Left edge (straight)
  ctx.closePath()
}

/* ---- Real photo pool (loaded from picsum.photos) ---- */
const PHOTO_IDS = [10, 11, 15, 16, 17, 18, 19, 20, 21, 22, 24, 25, 27, 28, 29, 36, 37, 39, 42, 43, 47, 48, 49, 54, 56, 58, 59, 60, 64, 65, 67, 68, 69, 73, 74, 75, 76, 77, 78, 82, 83, 84, 85, 89, 90, 91, 92, 94, 95, 96, 100, 101, 102, 103, 104, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 139, 140, 141, 142, 143, 144, 145, 146]
let cachedImg = null

const loadCaptchaImage = () => {
  return new Promise((resolve) => {
    const id = PHOTO_IDS[Math.floor(Math.random() * PHOTO_IDS.length)]
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => { cachedImg = img; resolve(img) }
    img.onerror = () => {
      // Fallback: try another random id
      const id2 = PHOTO_IDS[Math.floor(Math.random() * PHOTO_IDS.length)]
      const img2 = new Image()
      img2.crossOrigin = 'anonymous'
      img2.onload = () => { cachedImg = img2; resolve(img2) }
      img2.onerror = () => resolve(null) // will fallback to procedural
      img2.src = `https://picsum.photos/id/${id2}/960/510`
    }
    img.src = `https://picsum.photos/id/${id}/960/510`
  })
}

/* ---- Procedural fallback (in case image loading fails) ---- */
const drawFallbackScene = (ctx, w, h) => {
  const seed = Date.now() % 100000
  const hue = (seed * 41) % 360
  const bg = ctx.createLinearGradient(0, 0, w, h)
  bg.addColorStop(0, `hsl(${hue}, 55%, 62%)`)
  bg.addColorStop(0.5, `hsl(${(hue + 40) % 360}, 50%, 52%)`)
  bg.addColorStop(1, `hsl(${(hue + 80) % 360}, 60%, 45%)`)
  ctx.fillStyle = bg
  ctx.fillRect(0, 0, w, h)
  for (let i = 0; i < 10; i++) {
    const cx = ((seed * (i + 2) * 89 + i * 37) % w)
    const cy = ((seed * (i + 5) * 53 + i * 23) % h)
    const r = 20 + ((seed * (i + 1) * 43) % 50)
    const rg = ctx.createRadialGradient(cx, cy, 0, cx, cy, r)
    rg.addColorStop(0, `hsla(${(hue + i * 35) % 360}, 50%, 80%, 0.3)`)
    rg.addColorStop(1, `hsla(${(hue + i * 35) % 360}, 50%, 80%, 0)`)
    ctx.fillStyle = rg
    ctx.fillRect(cx - r, cy - r, r * 2, r * 2)
  }
}

/* ---- Main draw routine ---- */
const drawCaptcha = (img) => {
  const bgCanvas = captchaCanvasRef.value
  const pieceCanvas = pieceCanvasRef.value
  if (!bgCanvas || !pieceCanvas) return

  /* No DPR scaling – keeps pixel coordinates 1:1 with CSS px.
     This avoids cross-browser bugs where drawImage(canvas→canvas)
     produces blank output on high-DPR displays. */
  const bgCtx = bgCanvas.getContext('2d')
  const w = CANVAS_W, h = CANVAS_H
  bgCanvas.width = w
  bgCanvas.height = h

  // Compute cover-fit source crop from original image
  let cropSx = 0, cropSy = 0, cropSw = 0, cropSh = 0
  if (img) {
    const imgRatio = img.width / img.height
    const canvasRatio = w / h
    cropSw = img.width
    cropSh = img.height
    if (imgRatio > canvasRatio) {
      cropSw = img.height * canvasRatio
      cropSx = (img.width - cropSw) / 2
    } else {
      cropSh = img.width / canvasRatio
      cropSy = (img.height - cropSh) / 2
    }
    bgCtx.drawImage(img, cropSx, cropSy, cropSw, cropSh, 0, 0, w, h)
  } else {
    drawFallbackScene(bgCtx, w, h)
  }

  const tx = captchaTarget.value
  const ty = captchaTargetY.value
  const s = PIECE_S
  const tabR = TAB_R

  /* -- Extract piece image directly from the ORIGINAL img -- */
  const margin = tabR + 6
  const pW = s + margin * 2
  const pH = s + margin * 2
  pieceCanvas.width = pW
  pieceCanvas.height = pH
  const pieceCtx = pieceCanvas.getContext('2d')
  pieceCtx.clearRect(0, 0, pW, pH)

  // Clip to jigsaw shape and draw the matching image region
  pieceCtx.save()
  jigsawPath(pieceCtx, margin, margin, s, tabR)
  pieceCtx.clip()
  if (img) {
    // Map canvas-space (tx-margin, ty-margin, pW, pH) back to image-space
    const scaleX = cropSw / w
    const scaleY = cropSh / h
    const imgSx = cropSx + (tx - margin) * scaleX
    const imgSy = cropSy + (ty - margin) * scaleY
    const imgSw = pW * scaleX
    const imgSh = pH * scaleY
    pieceCtx.drawImage(img, imgSx, imgSy, imgSw, imgSh, 0, 0, pW, pH)
  } else {
    // Fallback: copy from bgCanvas (procedural, no taint risk)
    pieceCtx.drawImage(bgCanvas, tx - margin, ty - margin, pW, pH, 0, 0, pW, pH)
  }
  // Very subtle brightness lift
  pieceCtx.globalCompositeOperation = 'overlay'
  pieceCtx.fillStyle = 'rgba(255,255,255,0.08)'
  pieceCtx.fillRect(0, 0, pW, pH)
  pieceCtx.globalCompositeOperation = 'source-over'
  pieceCtx.restore()

  // Drop shadow halo (gives floating depth – drawn under white edge)
  pieceCtx.save()
  jigsawPath(pieceCtx, margin, margin, s, tabR)
  pieceCtx.shadowColor = 'rgba(0,0,0,0.5)'
  pieceCtx.shadowBlur = 12
  pieceCtx.shadowOffsetX = 2
  pieceCtx.shadowOffsetY = 3
  pieceCtx.strokeStyle = 'rgba(0,0,0,0)'
  pieceCtx.lineWidth = 0.1
  pieceCtx.stroke()
  pieceCtx.restore()

  // Crisp white border – 1.5px (thinner = more premium)
  pieceCtx.save()
  jigsawPath(pieceCtx, margin, margin, s, tabR)
  pieceCtx.lineWidth = 1.5
  pieceCtx.strokeStyle = 'rgba(255,255,255,0.92)'
  pieceCtx.shadowColor = 'rgba(0,0,0,0.35)'
  pieceCtx.shadowBlur = 3
  pieceCtx.stroke()
  pieceCtx.restore()

  /* -- Draw hole on background (realistic recessed look) -- */
  // Semi-transparent dark fill to darken the cutout area
  bgCtx.save()
  jigsawPath(bgCtx, tx, ty, s, tabR)
  bgCtx.fillStyle = 'rgba(0,0,0,0.38)'
  bgCtx.fill()
  bgCtx.restore()

  // Inner shadow – clip inside hole and draw offset shadows
  bgCtx.save()
  jigsawPath(bgCtx, tx, ty, s, tabR)
  bgCtx.clip()
  // Pass 1: strong top-left directional shadow
  bgCtx.shadowColor = 'rgba(0,0,0,0.75)'
  bgCtx.shadowBlur = 10
  bgCtx.shadowOffsetX = 3
  bgCtx.shadowOffsetY = 3
  bgCtx.fillStyle = 'rgba(0,0,0,0.12)'
  bgCtx.fillRect(tx - 20, ty - 20, s + 40, s + 40)
  // Pass 2: softer ambient occlusion
  bgCtx.shadowColor = 'rgba(0,0,0,0.4)'
  bgCtx.shadowBlur = 6
  bgCtx.shadowOffsetX = -2
  bgCtx.shadowOffsetY = -1
  bgCtx.fillStyle = 'rgba(0,0,0,0.06)'
  bgCtx.fillRect(tx - 20, ty - 20, s + 40, s + 40)
  bgCtx.restore()

  // Thin semi-transparent white outline for definition
  bgCtx.save()
  jigsawPath(bgCtx, tx, ty, s, tabR)
  bgCtx.strokeStyle = 'rgba(255,255,255,0.22)'
  bgCtx.lineWidth = 1
  bgCtx.stroke()
  bgCtx.restore()
}

const initCaptcha = async () => {
  captchaLoading.value = true
  captchaTarget.value = Math.floor(Math.random() * (SLIDER_MAX - 100)) + 80
  captchaTargetY.value = Math.floor(Math.random() * (PIECE_Y_MAX - PIECE_Y_MIN)) + PIECE_Y_MIN
  try {
    const img = await loadCaptchaImage()
    await nextTick()
    setTimeout(() => {
      drawCaptcha(img)
      captchaLoading.value = false
    }, 50)
  } catch {
    await nextTick()
    setTimeout(() => {
      drawCaptcha(null)
      captchaLoading.value = false
    }, 50)
  }
}

const openCaptcha = () => {
  if (!loginForm.username.trim()) {
    ElMessage.warning('请先输入用户名')
    return
  }
  if (!loginForm.password.trim()) {
    ElMessage.warning('请先输入密码')
    return
  }
  captchaPassed.value = false
  captchaFailed.value = false
  sliderLeft.value = 0
  showCaptcha.value = true
  initCaptcha()
}

const onSliderDown = (e) => {
  if (captchaPassed.value) return
  isDragging.value = true
  captchaFailed.value = false
  const SNAP_RANGE = 6  // magnetic snap range
  dragStartX = (e.touches ? e.touches[0].clientX : e.clientX) - sliderLeft.value
  const onMove = (ev) => {
    if (!isDragging.value) return
    if (ev.cancelable) ev.preventDefault()
    const clientX = ev.touches ? ev.touches[0].clientX : ev.clientX
    let newLeft = clientX - dragStartX
    newLeft = Math.max(0, Math.min(SLIDER_MAX, newLeft))
    // Gentle magnetic snap when very close to target
    const diff = Math.abs(newLeft - captchaTarget.value)
    if (diff <= SNAP_RANGE) {
      newLeft = captchaTarget.value
    }
    sliderLeft.value = newLeft
  }
  const onUp = () => {
    isDragging.value = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
    document.removeEventListener('touchmove', onMove)
    document.removeEventListener('touchend', onUp)
    if (Math.abs(sliderLeft.value - captchaTarget.value) <= CAPTCHA_TOLERANCE) {
      // Snap to exact position for satisfying visual
      sliderLeft.value = captchaTarget.value
      captchaPassed.value = true
      captchaVerified.value = true
      setTimeout(() => {
        showCaptcha.value = false
        doSendCode()
      }, 700)
    } else {
      captchaFailed.value = true
      setTimeout(() => {
        sliderLeft.value = 0
        captchaFailed.value = false
        initCaptcha()
      }, 700)
    }
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
  document.addEventListener('touchmove', onMove, { passive: false })
  document.addEventListener('touchend', onUp)
}

const refreshCaptcha = () => {
  captchaPassed.value = false
  captchaFailed.value = false
  sliderLeft.value = 0
  initCaptcha()
}

const closeCaptcha = () => {
  showCaptcha.value = false
  sliderLeft.value = 0
  isDragging.value = false
  captchaFailed.value = false
}

const doSendCode = async () => {
  sending.value = true
  try {
    await sendCode({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('验证码已发送至您的邮箱')
    startCountdown()
  } catch {
    // handled by interceptor
  } finally {
    sending.value = false
    captchaVerified.value = false
  }
}

const onSendCode = () => {
  openCaptcha()
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.loginAction({ ...loginForm })
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

/* =====================================================
   Lifecycle
   ===================================================== */
onMounted(() => {
  window.addEventListener('mousemove', onMouseMove)
  purpleBlinkTimer = scheduleBlink(isPurpleBlinking)
  blackBlinkTimer = scheduleBlink(isBlackBlinking)
  // Entrance: stagger characters in
  setTimeout(() => { entered.value = true }, 100)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  clearTimeout(purpleBlinkTimer)
  clearTimeout(blackBlinkTimer)
  clearTimeout(lookTimer)
  clearTimeout(peekTimer)
  window.removeEventListener('mousemove', onMouseMove)
})
</script>

<template>
  <div class="login-page">
    <!-- =============== Left: Characters Panel =============== -->
    <div class="login-left">
      <!-- Brand -->
      <div class="brand-area">
        <div class="brand-icon">
          <span class="iconfont icon-guanliduan" />
        </div>
        <span class="brand-name">FelixZoe</span>
      </div>

      <!-- Characters container -->
      <div class="characters-area">
        <div class="characters-wrap">

          <!-- ── Purple tall rectangle ── -->
          <div
            ref="purpleRef"
            :class="['char char-purple', { entered }]"
            :style="{
              height: (isTyping || pwdHidden)
                ? '440px' : '400px',
              transform: pwdVisible
                ? 'skewX(0deg)'
                : (isTyping || pwdHidden)
                  ? `skewX(${(purplePos.bodySkew || 0) - 12}deg) translateX(40px)`
                  : `skewX(${purplePos.bodySkew || 0}deg)`
            }"
          >
            <div
              class="char-eyes"
              :style="{
                left: pwdVisible ? '20px'
                  : isLookingAtEachOther ? '55px'
                  : `${45 + purplePos.faceX}px`,
                top: pwdVisible ? '35px'
                  : isLookingAtEachOther ? '65px'
                  : `${40 + purplePos.faceY}px`,
                gap: '32px'
              }"
            >
              <div ref="purpleEyeL" class="eyeball" :style="{ height: isPurpleBlinking ? '2px' : '18px', width: '18px' }">
                <div v-if="!isPurpleBlinking" class="pupil" :style="{ width: '7px', height: '7px', transform: `translate(${pPupilL.x}px,${pPupilL.y}px)` }" />
              </div>
              <div ref="purpleEyeR" class="eyeball" :style="{ height: isPurpleBlinking ? '2px' : '18px', width: '18px' }">
                <div v-if="!isPurpleBlinking" class="pupil" :style="{ width: '7px', height: '7px', transform: `translate(${pPupilR.x}px,${pPupilR.y}px)` }" />
              </div>
            </div>
          </div>

          <!-- ── Black tall rectangle ── -->
          <div
            ref="blackRef"
            :class="['char char-black', { entered }]"
            :style="{
              transform: pwdVisible
                ? 'skewX(0deg)'
                : isLookingAtEachOther
                  ? `skewX(${(blackPos.bodySkew || 0) * 1.5 + 10}deg) translateX(20px)`
                  : (isTyping || pwdHidden)
                    ? `skewX(${(blackPos.bodySkew || 0) * 1.5}deg)`
                    : `skewX(${blackPos.bodySkew || 0}deg)`
            }"
          >
            <div
              class="char-eyes"
              :style="{
                left: pwdVisible ? '10px'
                  : isLookingAtEachOther ? '32px'
                  : `${26 + blackPos.faceX}px`,
                top: pwdVisible ? '28px'
                  : isLookingAtEachOther ? '12px'
                  : `${32 + blackPos.faceY}px`,
                gap: '24px'
              }"
            >
              <div ref="blackEyeL" class="eyeball" :style="{ height: isBlackBlinking ? '2px' : '16px', width: '16px' }">
                <div v-if="!isBlackBlinking" class="pupil" :style="{ width: '6px', height: '6px', transform: `translate(${bPupilL.x}px,${bPupilL.y}px)` }" />
              </div>
              <div ref="blackEyeR" class="eyeball" :style="{ height: isBlackBlinking ? '2px' : '16px', width: '16px' }">
                <div v-if="!isBlackBlinking" class="pupil" :style="{ width: '6px', height: '6px', transform: `translate(${bPupilR.x}px,${bPupilR.y}px)` }" />
              </div>
            </div>
          </div>

          <!-- ── Orange semi-circle ── -->
          <div
            ref="orangeRef"
            :class="['char char-orange', { entered }]"
            :style="{ transform: pwdVisible ? 'skewX(0deg)' : `skewX(${orangePos.bodySkew || 0}deg)` }"
          >
            <div
              class="char-dots"
              :style="{
                left: pwdVisible ? '50px' : `${82 + (orangePos.faceX || 0)}px`,
                top: pwdVisible ? '85px' : `${90 + (orangePos.faceY || 0)}px`
              }"
            >
              <div class="dot" :style="{ transform: `translate(${oPupilL.x}px,${oPupilL.y}px)` }" />
              <div class="dot" :style="{ transform: `translate(${oPupilR.x}px,${oPupilR.y}px)` }" />
            </div>
          </div>

          <!-- ── Yellow rounded rectangle ── -->
          <div
            ref="yellowRef"
            :class="['char char-yellow', { entered }]"
            :style="{ transform: pwdVisible ? 'skewX(0deg)' : `skewX(${yellowPos.bodySkew || 0}deg)` }"
          >
            <div
              class="char-dots"
              :style="{
                left: pwdVisible ? '20px' : `${52 + (yellowPos.faceX || 0)}px`,
                top: pwdVisible ? '35px' : `${40 + (yellowPos.faceY || 0)}px`
              }"
            >
              <div class="dot" :style="{ transform: `translate(${yPupilL.x}px,${yPupilL.y}px)` }" />
              <div class="dot" :style="{ transform: `translate(${yPupilR.x}px,${yPupilR.y}px)` }" />
            </div>
            <div
              class="char-mouth"
              :style="{
                left: pwdVisible ? '10px' : `${40 + (yellowPos.faceX || 0)}px`,
                top: pwdVisible ? '88px' : `${88 + (yellowPos.faceY || 0)}px`
              }"
            />
          </div>

        </div>
      </div>

      <!-- Footer -->
      <div class="left-footer">
        <span>&copy; 2026 FelixZoe</span>
      </div>

      <!-- Decorative blurs -->
      <div class="deco-blur deco-blur-1" />
      <div class="deco-blur deco-blur-2" />
    </div>

    <!-- =============== Right: Login Form =============== -->
    <div class="login-right">
      <div class="login-form-container">
        <!-- Mobile brand -->
        <div class="mobile-brand">
          <div class="brand-icon brand-icon-sm">
            <span class="iconfont icon-guanliduan" />
          </div>
          <span class="brand-name">FelixZoe</span>
        </div>

        <!-- Header -->
        <div class="form-header">
          <h1 class="form-title">管理控制台</h1>
          <p class="form-subtitle">请输入账户信息以登录</p>
        </div>

        <!-- Form -->
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          class="login-form"
          label-position="top"
          hide-required-asterisk
        >
          <!-- Username -->
          <el-form-item prop="username" class="float-field">
            <div class="float-input-wrap" :class="{ focused: usernameFocused, filled: loginForm.username.length > 0 }">
              <label class="float-label">
                <svg class="float-label-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                <span>用户名</span>
              </label>
              <el-input
                v-model="loginForm.username"
                placeholder=" "
                size="large"
                clearable
                @focus="usernameFocused = true; isTyping = true"
                @blur="usernameFocused = false; isTyping = false"
              />
            </div>
          </el-form-item>

          <!-- Password -->
          <el-form-item prop="password" class="float-field">
            <div class="float-input-wrap" :class="{ focused: passwordFocused, filled: loginForm.password.length > 0 }">
              <label class="float-label">
                <svg class="float-label-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                <span>登录密码</span>
              </label>
              <el-input
                v-model="loginForm.password"
                placeholder=" "
                :type="showPassword ? 'text' : 'password'"
                size="large"
                @focus="passwordFocused = true; isTyping = true"
                @blur="passwordFocused = false; isTyping = false"
              >
                <template #suffix>
                  <span class="pwd-toggle" @click.stop="showPassword = !showPassword" tabindex="-1">
                    <svg v-if="showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                  </span>
                </template>
              </el-input>
            </div>
          </el-form-item>

          <!-- Email code -->
          <el-form-item prop="code" class="float-field">
            <div class="code-row">
              <div class="float-input-wrap code-input" :class="{ focused: codeFocused, filled: loginForm.code.length > 0 }">
                <label class="float-label">
                  <svg class="float-label-icon" width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M12 16v.01"/><path d="M8 16v.01"/><path d="M16 16v.01"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  <span>邮箱验证码</span>
                </label>
                <el-input
                  v-model="loginForm.code"
                  placeholder=" "
                  size="large"
                  @focus="codeFocused = true; isTyping = true"
                  @blur="codeFocused = false; isTyping = false"
                />
              </div>
              <button
                type="button"
                class="code-btn"
                :disabled="isCounting || sending"
                @click.stop="onSendCode"
              >
                {{ codeButtonText }}
              </button>
            </div>
          </el-form-item>

          <!-- Submit -->
          <el-form-item class="submit-item">
            <button
              type="button"
              class="login-btn"
              :disabled="loading"
              @click="handleLogin"
            >
              <span v-if="loading" class="btn-loading" />
              <span>{{ loading ? '登录中...' : '登 录' }}</span>
            </button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- =============== Jigsaw Captcha Modal =============== -->
    <Teleport to="body">
      <transition name="captcha-fade">
        <div v-if="showCaptcha" class="captcha-overlay" @click.self="closeCaptcha">
          <div class="captcha-card" :class="{ shake: captchaFailed }">
            <!-- Header -->
            <div class="captcha-header">
              <div class="captcha-header-left">
                <svg class="captcha-shield" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
                <span class="captcha-title">安全验证</span>
              </div>
              <div class="captcha-header-right">
                <button class="captcha-refresh" @click="refreshCaptcha" title="刷新">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
                </button>
                <button class="captcha-close" @click="closeCaptcha">
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </button>
              </div>
            </div>

            <!-- Canvas jigsaw area -->
            <div class="captcha-canvas-wrap">
              <canvas ref="captchaCanvasRef" class="captcha-canvas" />
              <!-- Piece canvas: real image clipped to jigsaw shape -->
              <!-- Loading overlay -->
              <div v-if="captchaLoading" class="captcha-loading-overlay">
                <div class="google-balls">
                  <span class="gball gball-blue" />
                  <span class="gball gball-red" />
                  <span class="gball gball-yellow" />
                  <span class="gball gball-green" />
                </div>
              </div>
              <canvas
                ref="pieceCanvasRef"
                class="captcha-piece"
                :class="{ passed: captchaPassed, failed: captchaFailed }"
                :style="{ left: (sliderLeft - TAB_R - 6) + 'px', top: (captchaTargetY - TAB_R - 6) + 'px', width: (PIECE_S + (TAB_R + 6) * 2) + 'px', height: (PIECE_S + (TAB_R + 6) * 2) + 'px' }"
              />
            </div>

            <!-- Slider track -->
            <div class="captcha-slider-wrap">
              <div class="captcha-slider-bar" :class="{ passed: captchaPassed, failed: captchaFailed }">
                <div class="captcha-slider-fill" :style="{ width: (sliderLeft + 24) + 'px' }" :class="{ passed: captchaPassed, failed: captchaFailed }" />
                <div class="captcha-slider-label" v-if="captchaPassed">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                  <span style="color:#67c23a;font-weight:600">验证成功</span>
                </div>
                <div class="captcha-slider-label" v-else-if="captchaFailed">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  <span style="color:#f56c6c;font-weight:600">请再试一次</span>
                </div>
                <div class="captcha-slider-label" v-else-if="!isDragging && sliderLeft === 0">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
                  <span>按住滑块，拖动完成拼图</span>
                </div>
                <div
                  class="captcha-slider-thumb"
                  :class="{ dragging: isDragging, passed: captchaPassed, failed: captchaFailed }"
                  :style="{ left: sliderLeft + 'px' }"
                  @mousedown.prevent="onSliderDown"
                  @touchstart.prevent="onSliderDown"
                >
                  <svg v-if="captchaPassed" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                  <svg v-else-if="captchaFailed" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                  <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
                </div>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ======== Page Layout ======== */
.login-page {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 100vh;
  max-height: 100vh;
  overflow: hidden;
}

/* ======== Left Panel ======== */
.login-left {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px;
  background: linear-gradient(135deg, #9ca3af 0%, #6b7280 50%, #4b5563 100%);
  overflow: hidden;
}

.brand-area {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 10;
}

.brand-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-icon .iconfont { font-size: 18px; color: #ffffff; }
.brand-icon-sm { width: 32px; height: 32px; background: #000000; border-radius: 8px; }
.brand-icon-sm .iconfont { font-size: 16px; color: #ffffff; }

.brand-name {
  font-size: 17px;
  font-weight: 600;
  color: #ffffff;
  letter-spacing: 0.3px;
}

/* ---- Characters ---- */
.characters-area {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  flex: 1;
  position: relative;
  z-index: 10;
  padding-bottom: 0;
}

.characters-wrap {
  position: relative;
  width: 550px;
  height: 400px;
}

.char {
  position: absolute;
  bottom: 0;
  transition: all 0.7s cubic-bezier(.4,0,.2,1);
  transform-origin: bottom center;
}

/* ---- Entrance animations ---- */
.char-purple {
  left: 70px;
  width: 180px;
  height: 400px;
  background: #6C3FF5;
  border-radius: 10px 10px 0 0;
  z-index: 1;
  opacity: 0;
  transform: translateY(120%) skewX(0deg);
}
.char-purple.entered {
  opacity: 1;
  transform: translateY(0) skewX(0deg);
  transition: opacity 0.6s ease-out 0.1s, transform 0.7s cubic-bezier(.34,1.56,.64,1) 0.1s, height 0.7s cubic-bezier(.4,0,.2,1);
}

.char-black {
  left: 240px;
  width: 120px;
  height: 310px;
  background: #2D2D2D;
  border-radius: 8px 8px 0 0;
  z-index: 2;
  opacity: 0;
  transform: translateY(120%) skewX(0deg);
}
.char-black.entered {
  opacity: 1;
  transform: translateY(0) skewX(0deg);
  transition: opacity 0.6s ease-out 0.25s, transform 0.7s cubic-bezier(.34,1.56,.64,1) 0.25s, height 0.7s cubic-bezier(.4,0,.2,1);
}

.char-orange {
  left: 0;
  width: 240px;
  height: 200px;
  background: #FF9B6B;
  border-radius: 120px 120px 0 0;
  z-index: 3;
  opacity: 0;
  transform: translateY(120%) skewX(0deg);
}
.char-orange.entered {
  opacity: 1;
  transform: translateY(0) skewX(0deg);
  transition: opacity 0.6s ease-out 0.4s, transform 0.7s cubic-bezier(.34,1.56,.64,1) 0.4s;
}

.char-yellow {
  left: 310px;
  width: 140px;
  height: 230px;
  background: #E8D754;
  border-radius: 70px 70px 0 0;
  z-index: 4;
  opacity: 0;
  transform: translateY(120%) skewX(0deg);
}
.char-yellow.entered {
  opacity: 1;
  transform: translateY(0) skewX(0deg);
  transition: opacity 0.6s ease-out 0.55s, transform 0.7s cubic-bezier(.34,1.56,.64,1) 0.55s;
}

/* ---- Eyeballs ---- */
.char-eyes {
  position: absolute;
  display: flex;
  transition: left 0.7s cubic-bezier(.4,0,.2,1), top 0.7s cubic-bezier(.4,0,.2,1);
}

.eyeball {
  border-radius: 50%;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: height 0.15s ease;
}

.pupil {
  border-radius: 50%;
  background: #2D2D2D;
  transition: transform 0.1s ease-out;
}

.char-dots {
  position: absolute;
  display: flex;
  gap: 32px;
  transition: left 0.2s ease-out, top 0.2s ease-out;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #2D2D2D;
  transition: transform 0.1s ease-out;
}

.char-mouth {
  position: absolute;
  width: 80px;
  height: 4px;
  background: #2D2D2D;
  border-radius: 4px;
  transition: left 0.2s ease-out, top 0.2s ease-out;
}

/* ---- Decorative ---- */
.deco-blur {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.deco-blur-1 {
  width: 256px;
  height: 256px;
  background: rgba(255, 255, 255, 0.1);
  filter: blur(80px);
  top: 25%;
  right: 25%;
}

.deco-blur-2 {
  width: 384px;
  height: 384px;
  background: rgba(255, 255, 255, 0.05);
  filter: blur(80px);
  bottom: 25%;
  left: 25%;
}

.left-footer {
  position: relative;
  z-index: 10;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

/* ======== Right Panel ======== */
.login-right {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #f8f9fb;
}

.login-form-container {
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 8px 24px rgba(0,0,0,0.06);
}

.mobile-brand {
  display: none;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 32px;
}

.mobile-brand .brand-name { color: #303133; }

.form-header {
  text-align: center;
  margin-bottom: 32px;
}

.form-title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.5px;
}

.form-subtitle {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

/* ---- Form styling ---- */
.login-form {
  display: flex;
  flex-direction: column;
}

/* ---- Floating label system ---- */
.float-field {
  margin-bottom: 20px !important;
}

/* Hide el-form-item default label for float fields */
:deep(.float-field > .el-form-item__label-wrap),
:deep(.float-field > .el-form-item__label) {
  display: none !important;
}

.float-input-wrap {
  position: relative;
  width: 100%;
}

.float-label {
  position: absolute;
  top: 50%;
  left: 14px;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #b0b4bb;
  pointer-events: none;
  z-index: 2;
  transition: all 0.22s cubic-bezier(.4,0,.2,1);
  transform-origin: left center;
  background: transparent;
  padding: 0 2px;
  line-height: 1;
}

.float-label-icon {
  flex-shrink: 0;
  transition: color 0.22s ease;
}

/* Float up when focused or has content */
.float-input-wrap.focused .float-label,
.float-input-wrap.filled .float-label {
  top: -1px;
  transform: translateY(-50%) scale(0.82);
  color: #909399;
  background: linear-gradient(180deg, transparent 45%, #fff 45%);
  padding: 0 5px;
  left: 10px;
}

.float-input-wrap.focused .float-label {
  color: #6366f1;
}

.float-input-wrap.focused .float-label-icon {
  color: #6366f1;
}

/* Filled (has content, not focused): subtle neutral label */
.float-input-wrap.filled:not(.focused) .float-label {
  color: #909399;
}
.float-input-wrap.filled:not(.focused) .float-label-icon {
  color: #909399;
}

/* Validation error state: label turns red */
:deep(.el-form-item.is-error) .float-label {
  color: #f56c6c !important;
}
:deep(.el-form-item.is-error) .float-label-icon {
  color: #f56c6c !important;
}

/* Field prefix icon color */
.field-icon {
  color: #c0c4cc;
  transition: color 0.2s;
}

/* Remove default el-form-item label styling for float fields */
:deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  padding-bottom: 6px;
  line-height: 1;
}

/* ---- Input wrapper ---- */
:deep(.el-input__wrapper) {
  height: 50px;
  border-radius: 12px !important;
  box-shadow: 0 0 0 1.5px #e4e7ed inset !important;
  background: #fff !important;
  transition: box-shadow 0.25s ease, background 0.25s ease !important;
  outline: none !important;
  padding-left: 14px !important;
}

/* Filled (has content, not focused): green border */
.float-input-wrap.filled:not(.focused) :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1.5px #c0c4cc inset !important;
}

/* Validation error state: red border (overrides green) */
:deep(.el-form-item.is-error) .float-input-wrap.filled:not(.focused) :deep(.el-input__wrapper),
:deep(.el-form-item.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 1.5px #f56c6c inset !important;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1.5px #d0d3da inset !important;
  background: #ffffff !important;
}

.float-input-wrap.focused :deep(.el-input__wrapper),
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px #6366f1 inset !important;
  background: #ffffff !important;
  outline: none !important;
}

:deep(.el-input__wrapper:focus-within) {
  outline: none !important;
}

/* Kill ALL focus-visible outlines globally inside login page */
.login-page *:focus-visible,
.login-page :deep(*:focus-visible) {
  outline: none !important;
  outline-offset: 0 !important;
}

:deep(.el-input__prefix) {
  display: none;
}

:deep(.el-input__inner) {
  color: #303133 !important;
  font-size: 14px;
}

:deep(.el-input__inner::placeholder) {
  color: transparent !important;
}

:deep(.el-form-item) {
  margin-bottom: 22px;
}

:deep(.el-form-item__error) {
  display: none !important;
}

.submit-item {
  margin-bottom: 0 !important;
}
:deep(.submit-item .el-form-item__content) {
  margin-top: 4px;
}

/* ---- Password eye toggle ---- */
.pwd-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #c0c4cc;
  transition: color 0.2s;
  padding: 2px;
  border-radius: 4px;
  -webkit-tap-highlight-color: transparent;
  line-height: 1;
}
.pwd-toggle:hover {
  color: #303133;
}

:deep(.el-input__suffix) {
  display: flex;
  align-items: center;
}

/* ---- Code row ---- */
.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.code-input { flex: 1; }

.code-btn {
  flex-shrink: 0;
  width: 110px;
  height: 50px;
  border: 1.5px solid #e4e7ed;
  border-radius: 12px;
  background: #fff;
  color: #606266;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
  -webkit-tap-highlight-color: transparent;
}
.code-btn:hover {
  border-color: #6366f1;
  color: #6366f1;
  background: #f0f0ff;
}
.code-btn:active {
  transform: scale(0.97);
}
.code-btn:disabled {
  background: #f5f7fa;
  border-color: #e4e7ed;
  color: #c0c4cc;
  cursor: not-allowed;
  transform: none;
}

/* ---- Login button ---- */
.login-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 12px;
  background: #1a1a1a;
  color: #ffffff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 3px;
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  -webkit-tap-highlight-color: transparent;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.login-btn:hover {
  background: #333333;
  box-shadow: 0 4px 14px rgba(0,0,0,0.2);
  transform: translateY(-1px);
}
.login-btn:active {
  transform: translateY(0) scale(0.98);
  box-shadow: 0 1px 4px rgba(0,0,0,0.15);
}
.login-btn:disabled {
  background: #909399;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.btn-loading {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

/* ======== Mobile ======== */
@media (max-width: 768px) {
  .login-page {
    grid-template-columns: 1fr;
    position: relative;
  }
  .login-left { display: none; }
  .login-right {
    padding: 20px;
    background: linear-gradient(160deg, #0f0c29 0%, #302b63 40%, #24243e 100%);
    min-height: 100vh;
    position: relative;
    overflow: hidden;
  }
  /* Floating decorative orbs */
  .login-right::before {
    content: '';
    position: absolute;
    top: -60px;
    right: -40px;
    width: 200px;
    height: 200px;
    border-radius: 50%;
    background: rgba(99, 102, 241, 0.15);
    filter: blur(40px);
    animation: mobileFloat1 20s ease-in-out infinite;
    pointer-events: none;
  }
  .login-right::after {
    content: '';
    position: absolute;
    bottom: 80px;
    left: -30px;
    width: 140px;
    height: 140px;
    border-radius: 50%;
    background: rgba(168, 85, 247, 0.12);
    filter: blur(40px);
    animation: mobileFloat2 25s ease-in-out infinite;
    pointer-events: none;
  }
  .mobile-brand { display: flex; }
  .mobile-brand .brand-name { color: #ffffff; }
  .mobile-brand .brand-icon-sm {
    background: rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.15);
  }
  .mobile-brand .brand-icon-sm .iconfont { color: #ffffff; }
  .form-title { font-size: 22px; }
  .login-form-container {
    max-width: 100%;
    padding: 32px 24px;
    background: rgba(255, 255, 255, 0.97);
    backdrop-filter: blur(24px) saturate(180%);
    -webkit-backdrop-filter: blur(24px) saturate(180%);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow:
      0 8px 32px rgba(0, 0, 0, 0.25),
      0 2px 8px rgba(0, 0, 0, 0.1),
      inset 0 1px 0 rgba(255, 255, 255, 0.6);
    position: relative;
    z-index: 1;
  }
}

@media (max-width: 480px) {
  .login-right { padding: 16px; }
  .login-form-container {
    padding: 28px 20px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.95);
  }
  .code-btn { width: 96px; font-size: 12px; }
  .login-btn { height: 44px; font-size: 14px; }
}

/* ---- Mobile background animations ---- */


@keyframes mobileFloat1 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.8; }
  50% { transform: translate(-15px, 20px) scale(1.05); opacity: 1; }
}

@keyframes mobileFloat2 {
  0%, 100% { transform: translate(0, 0) scale(1); opacity: 0.7; }
  50% { transform: translate(20px, -15px) scale(1.08); opacity: 1; }
}

/* ======== Jigsaw Captcha ======== */
.captcha-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: captchaFadeIn 0.25s ease;
}

@keyframes captchaFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.captcha-card {
  background: #ffffff;
  border-radius: 16px;
  padding: 20px 20px 18px;
  width: 372px;
  max-width: 92vw;
  box-shadow: 0 25px 60px rgba(0,0,0,0.16), 0 8px 24px rgba(0,0,0,0.08), 0 0 0 1px rgba(0,0,0,0.03);
  animation: captchaSlideUp 0.35s cubic-bezier(.34,1.15,.64,1);
}

.captcha-card.shake {
  animation: captchaShake 0.45s ease;
}

@keyframes captchaSlideUp {
  from { opacity: 0; transform: translateY(30px) scale(0.94); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes captchaShake {
  0%, 100% { transform: translateX(0); }
  12% { transform: translateX(-10px); }
  25% { transform: translateX(8px); }
  37% { transform: translateX(-6px); }
  50% { transform: translateX(5px); }
  62% { transform: translateX(-3px); }
  75% { transform: translateX(2px); }
}

/* ---- Captcha Header ---- */
.captcha-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.captcha-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.captcha-shield {
  color: #6366f1;
}

.captcha-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  letter-spacing: 0.5px;
}

.captcha-header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.captcha-refresh,
.captcha-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  border: none;
  background: transparent;
  color: #bfc3ca;
  cursor: pointer;
  transition: all 0.18s ease;
  -webkit-tap-highlight-color: transparent;
}

.captcha-refresh:hover,
.captcha-close:hover {
  background: #f2f4f7;
  color: #505662;
}

.captcha-refresh:active,
.captcha-close:active {
  transform: scale(0.88);
}

/* ---- Canvas Area ---- */
.captcha-canvas-wrap {
  position: relative;
  width: 328px;
  height: 175px;
  margin: 0 auto 14px;
  border-radius: 12px;
  overflow: hidden;
  background: #e8eaed;
  user-select: none;
  -webkit-user-select: none;
  box-shadow: inset 0 1px 4px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.05);
}

.captcha-canvas {
  display: block;
  width: 328px;
  height: 175px;
}

/* ---- Puzzle piece (canvas) ---- */
.captcha-piece {
  position: absolute;
  pointer-events: none;
  filter: drop-shadow(2px 3px 5px rgba(0,0,0,0.4)) drop-shadow(-1px -1px 2px rgba(0,0,0,0.1));
  transition: filter 0.4s ease, opacity 0.35s ease, transform 0.3s ease;
  will-change: filter, transform;
}

.captcha-piece.passed {
  filter: drop-shadow(0 0 12px rgba(103, 194, 58, 0.6)) drop-shadow(0 2px 6px rgba(0,0,0,0.15));
  transform: scale(1.01);
}

.captcha-piece.failed {
  filter: drop-shadow(0 0 12px rgba(245, 108, 108, 0.6)) drop-shadow(0 2px 6px rgba(0,0,0,0.15));
  opacity: 0.88;
}

/* ---- Loading overlay ---- */
.captcha-loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(248,249,252,0.9);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  border-radius: 12px;
}

/* Google four-color bouncing balls loader */
.google-balls {
  display: flex;
  align-items: center;
  gap: 7px;
}

.gball {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
  animation: gBounce 1.6s ease-in-out infinite;
  opacity: 0.85;
}

.gball-blue   { background: #4285F4; }
.gball-red    { background: #EA4335; animation-delay: 0.15s; }
.gball-yellow { background: #FBBC05; animation-delay: 0.3s; }
.gball-green  { background: #34A853; animation-delay: 0.45s; }

@keyframes gBounce {
  0%, 80%, 100% { transform: translateY(0) scale(1); opacity: 0.85; }
  40%           { transform: translateY(-14px) scale(1.15); opacity: 1; }
}

/* ---- Slider ---- */
.captcha-slider-wrap {
  padding: 0 2px;
}

.captcha-slider-bar {
  position: relative;
  height: 44px;
  background: #f5f7fa;
  border-radius: 22px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
  user-select: none;
  -webkit-user-select: none;
  transition: border-color 0.3s ease, background 0.3s ease, box-shadow 0.3s ease;
}

.captcha-slider-bar.passed {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.captcha-slider-bar.failed {
  border-color: #f5a5a5;
  background: #fef0f0;
}

.captcha-slider-fill {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  background: linear-gradient(90deg, rgba(99,102,241,0.04) 0%, rgba(99,102,241,0.12) 100%);
  border-radius: 22px 0 0 22px;
  transition: background 0.3s ease;
  pointer-events: none;
}

.captcha-slider-fill.passed {
  background: linear-gradient(90deg, rgba(103,194,58,0.08) 0%, rgba(103,194,58,0.22) 100%);
}

.captcha-slider-fill.failed {
  background: linear-gradient(90deg, rgba(245,108,108,0.08) 0%, rgba(245,108,108,0.22) 100%);
}

.captcha-slider-label {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  font-size: 13px;
  color: #c0c4cc;
  pointer-events: none;
  letter-spacing: 0.3px;
  font-weight: 400;
}

.captcha-slider-thumb {
  position: absolute;
  top: 2px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ffffff;
  border: 1.5px solid #dcdfe6;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  transition: border-color 0.2s, box-shadow 0.25s, background 0.25s, transform 0.18s cubic-bezier(.34,1.15,.64,1);
  color: #909399;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  touch-action: none;
}

.captcha-slider-thumb:hover {
  border-color: #b0b3bb;
  box-shadow: 0 3px 12px rgba(0,0,0,0.14), 0 0 0 3px rgba(99,102,241,0.06);
  color: #606266;
}

.captcha-slider-thumb.dragging {
  cursor: grabbing;
  border-color: #6366f1;
  background: #f8f7ff;
  box-shadow: 0 4px 18px rgba(99,102,241,0.25), 0 0 0 3px rgba(99,102,241,0.1);
  transform: scale(1.06);
  color: #6366f1;
}

.captcha-slider-thumb.passed {
  background: linear-gradient(145deg, #7dce52 0%, #57b831 100%);
  border-color: transparent;
  color: #ffffff;
  cursor: default;
  transform: scale(1.04);
  box-shadow: 0 3px 12px rgba(103, 194, 58, 0.4);
}

.captcha-slider-thumb.failed {
  background: linear-gradient(145deg, #f98080 0%, #f56c6c 100%);
  border-color: transparent;
  color: #ffffff;
  box-shadow: 0 3px 12px rgba(245,108,108,0.35);
  animation: captchaShake 0.45s ease;
}

/* ---- Transitions ---- */
.captcha-fade-enter-active { transition: opacity 0.25s ease; }
.captcha-fade-leave-active { transition: opacity 0.2s ease; }
.captcha-fade-enter-from,
.captcha-fade-leave-to { opacity: 0; }
</style>
