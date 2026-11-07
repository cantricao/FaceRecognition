import os
import subprocess
import sys
from PIL import Image

BASE_DIR = r"C:\Users\admin\FaceRecognition\screenshots"
CHROME_PATH = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
TEMP_PROFILE = os.path.join(os.environ.get("TEMP", r"C:\Temp"), "chrome_temp_export")

PRESETS = {
    "google-play-phone": (1080, 1920),
    "google-play-tablet": (1440, 2560),
    "app-store-ios-6.3": (1206, 2622),
    "app-store-ios-6.9": (1320, 2868)
}

PANELS_DATA = [
    {
        "id": "01",
        "kicker": "LiteRT On-Device ML",
        "headline": "Instant Face <span class='accent'>Recognition</span>",
        "subline": "Real-time 60 FPS on-device artificial intelligence",
        "trust_top": "100% On-Device AI",
        "trust_sub": "Zero Cloud Latency · Private",
        "extra": "hero"
    },
    {
        "id": "02",
        "kicker": "Google Ecosystem",
        "headline": "Built for <span class='accent'>Every Screen</span>",
        "subline": "Phone, Tablet, Wear OS & Auto continuity",
        "trust_top": "Navigation 3 Adaptive",
        "trust_sub": "Optimized for Foldables & Tablets",
        "extra": "multidevice"
    },
    {
        "id": "03",
        "kicker": "Security & Privacy",
        "headline": "100% Private. <span class='accent'>Zero Cloud.</span>",
        "subline": "Biometric embeddings never leave your device",
        "trust_top": "Zero Network Access",
        "trust_sub": "No external telemetry or data uploads",
        "extra": "privacy"
    },
    {
        "id": "04",
        "kicker": "Batch Video Pipeline",
        "headline": "Batch Video <span class='accent'>Analysis</span>",
        "subline": "Scan recorded footage at full hardware speed",
        "trust_top": "Hardware Acceleration",
        "trust_sub": "ExoPlayer + Media3 Pipeline",
        "extra": "video"
    },
    {
        "id": "05",
        "kicker": "Wear OS Companion",
        "headline": "Glanceable on <span class='accent'>Wear OS</span>",
        "subline": "Instant identity alerts & counters on your wrist",
        "trust_top": "Wear OS Tiles & Complications",
        "trust_sub": "Real-time status right on your watch face",
        "extra": "watch"
    },
    {
        "id": "06",
        "kicker": "Hyperparameter Tuning",
        "headline": "Pro-Grade <span class='accent'>Precision</span>",
        "subline": "Tunable thresholds & custom AI model backends",
        "trust_top": "CameraX & OpenCV Core",
        "trust_sub": "Optimized for Android 15 & 16",
        "extra": "tuning"
    }
]

def make_panel_html(p):
    extra_content = ""
    
    if p["extra"] == "hero":
        extra_content = """
        <!-- Tablet Backdrop -->
        <div class="tablet-mockup" style="left: -120px; top: 40px; width: 560px; height: 780px; opacity: 0.72; transform: rotate(-5deg);">
          <div class="app-topbar">
            <span style="font-size: 20px; font-weight: 700; color: #64748B;">TELEMETRY INSPECTOR</span>
            <span style="font-size: 18px; color: #00E676; font-weight: 700;">ACTIVE</span>
          </div>
          <div style="padding: 24px; display: flex; flex-direction: column; gap: 14px;">
            <div style="background: #17212D; padding: 18px; border-radius: 20px; display: flex; justify-content: space-between; font-size: 20px;">
              <span>Alex Chen</span>
              <strong style="color: #00E676;">99.4% Match</strong>
            </div>
            <div style="background: #17212D; padding: 18px; border-radius: 20px; display: flex; justify-content: space-between; font-size: 20px;">
              <span>Sarah Jenkins</span>
              <strong style="color: #00E676;">98.7% Match</strong>
            </div>
            <div style="background: #17212D; padding: 18px; border-radius: 20px; display: flex; justify-content: space-between; font-size: 20px;">
              <span>Inference Time</span>
              <strong style="color: #38BDF8;">12.4 ms</strong>
            </div>
          </div>
        </div>

        <!-- Center Phone Hero -->
        <div class="phone-wrapper">
          <div class="phone-screen">
            <div class="viewfinder">
              <div class="laser-scan"></div>
              <div class="face-target">
                <svg width="220" height="260" viewBox="0 0 100 120" fill="none" opacity="0.45">
                  <ellipse cx="50" cy="50" rx="35" ry="45" stroke="#FFFFFF" stroke-width="1.5" stroke-dasharray="3 3"/>
                  <circle cx="38" cy="45" r="4" fill="#FFFFFF"/>
                  <circle cx="62" cy="45" r="4" fill="#FFFFFF"/>
                  <path d="M47 54 L53 54 L50 63 Z" stroke="#FFFFFF" stroke-width="1.2"/>
                  <path d="M40 76 Q50 84 60 76" stroke="#FFFFFF" stroke-width="1.8" fill="none"/>
                </svg>
                <div class="bbox-frame">
                  <div class="bbox-corner tl"></div>
                  <div class="bbox-corner tr"></div>
                  <div class="bbox-corner bl"></div>
                  <div class="bbox-corner br"></div>
                  <div class="identity-pill">
                    <span class="dot"></span>
                    Alex Chen · 99.4%
                  </div>
                  <div class="telemetry-tag">
                    BBox: [142, 280 - 410, 560]
                  </div>
                </div>
              </div>
            </div>
            <div class="camera-bar">
              <div class="stat-pill" style="color: #00E676;">🟢 2 Faces In Frame</div>
              <div class="stat-pill">60.2 FPS</div>
              <div class="stat-pill">LiteRT GPU</div>
            </div>
          </div>
        </div>

        <!-- Foreground Wear OS Watch -->
        <div class="watch-mockup" style="right: 32px; bottom: 80px; width: 310px; height: 310px;">
          <div class="watch-dial">
            <div style="font-size: 16px; font-weight: 800; color: #00E676; text-transform: uppercase; letter-spacing: 0.08em;">Wear OS</div>
            <div style="font-size: 48px; font-weight: 900; color: #FFFFFF; margin: 4px 0;">2</div>
            <div style="font-size: 18px; color: #94A3B8;">Identities Verified</div>
            <div style="margin-top: 12px; background: rgba(0, 230, 118, 0.2); border: 1px solid rgba(0, 230, 118, 0.5); color: #00E676; font-size: 17px; font-weight: 700; padding: 6px 22px; border-radius: 999px;">
              Open App
            </div>
          </div>
        </div>
        """
    elif p["extra"] == "multidevice":
        extra_content = """
        <!-- Tablet Central Mockup -->
        <div class="tablet-mockup" style="left: 50%; transform: translateX(-50%); top: 20px; width: 880px; height: 600px; z-index: 5;">
          <div class="app-topbar">
            <div class="app-title">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="#00E676"><circle cx="12" cy="12" r="10"/></svg>
              FaceRecognition Studio (Adaptive Two-Pane)
            </div>
            <span style="font-size: 18px; background: rgba(0, 230, 118, 0.2); color: #00E676; padding: 4px 14px; border-radius: 8px;">Adaptive Navigation 3</span>
          </div>
          <div style="display: flex; height: 100%;">
            <div style="flex: 1.2; background: #0A0E14; position: relative; border-right: 1px solid rgba(255,255,255,0.06); padding: 24px; display: flex; align-items: center; justify-content: center;">
              <div style="width: 260px; height: 320px; border: 3px solid #00E676; border-radius: 24px; position: relative; display: flex; align-items: center; justify-content: center;">
                <span style="font-size: 20px; color: #00E676; font-weight: 800;">Primary Stream</span>
              </div>
            </div>
            <div style="flex: 1; background: #111721; padding: 28px; display: flex; flex-direction: column; gap: 16px;">
              <div style="font-size: 18px; font-weight: 700; color: #64748B; text-transform: uppercase;">Real-time Telemetry</div>
              <div style="background: #17212D; border-radius: 14px; padding: 14px 18px; font-size: 19px;">
                <div>Alex Chen</div>
                <div style="color: #00E676; font-size: 17px;">Cosine: 0.994</div>
              </div>
              <div style="background: #17212D; border-radius: 14px; padding: 14px 18px; font-size: 19px;">
                <div>Sarah Jenkins</div>
                <div style="color: #00E676; font-size: 17px;">Cosine: 0.987</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Phone Foreground -->
        <div class="phone-wrapper" style="left: 28%; bottom: -80px; width: 500px; height: 860px; z-index: 15;">
          <div class="phone-screen">
            <div class="app-topbar">
              <span style="font-size: 18px; font-weight: 700; color: #FFFFFF;">Live Detection</span>
              <span style="font-size: 16px; color: #00E676;">60 FPS</span>
            </div>
            <div class="viewfinder" style="display: flex; align-items: center; justify-content: center;">
              <div style="width: 220px; height: 260px; border: 3px solid #00E676; border-radius: 20px; position: relative;">
                <div class="identity-pill" style="top: -36px; font-size: 18px; padding: 4px 16px;">Alex C. · 99%</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Wear OS Watch Floating Right -->
        <div class="watch-mockup" style="right: 40px; bottom: 120px; width: 320px; height: 320px; z-index: 20;">
          <div class="watch-dial">
            <div style="font-size: 16px; font-weight: 800; color: #00E676; text-transform: uppercase;">Wear OS 5</div>
            <div style="font-size: 48px; font-weight: 900; color: #FFFFFF; margin: 4px 0;">SYNC</div>
            <div style="font-size: 18px; color: #94A3B8;">Wrist Telemetry</div>
            <div style="margin-top: 12px; background: rgba(0, 230, 118, 0.2); border: 1px solid rgba(0, 230, 118, 0.5); color: #00E676; font-size: 17px; font-weight: 700; padding: 6px 24px; border-radius: 999px;">
              Connected
            </div>
          </div>
        </div>
        """
    elif p["extra"] == "privacy":
        extra_content = """
        <div class="phone-wrapper">
          <div class="phone-screen">
            <div class="app-topbar">
              <div class="app-title">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#00E676" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
                Enrolled Identities
              </div>
              <span style="font-size: 20px; color: #00E676; font-weight: 700;">+ Register</span>
            </div>

            <div style="padding: 28px; display: flex; flex-direction: column; gap: 18px; background: #0B0E14; flex: 1;">
              
              <div style="background: #151D28; border: 1.5px solid rgba(0, 230, 118, 0.35); border-radius: 22px; padding: 20px; display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center; gap: 18px;">
                  <div style="width: 64px; height: 64px; border-radius: 50%; background: #1E2B3C; display: flex; align-items: center; justify-content: center; font-weight: 800; color: #00E676; font-size: 24px;">AC</div>
                  <div>
                    <div style="font-size: 22px; font-weight: 700; color: #FFFFFF;">Alex Chen</div>
                    <div style="font-size: 17px; color: #64748B;">512-dim embedding · Local</div>
                  </div>
                </div>
                <span style="background: rgba(0, 230, 118, 0.15); color: #00E676; font-size: 16px; font-weight: 800; padding: 6px 14px; border-radius: 10px;">VERIFIED</span>
              </div>

              <div style="background: #151D28; border: 1px solid rgba(255,255,255,0.06); border-radius: 22px; padding: 20px; display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center; gap: 18px;">
                  <div style="width: 64px; height: 64px; border-radius: 50%; background: #1E2B3C; display: flex; align-items: center; justify-content: center; font-weight: 800; color: #38BDF8; font-size: 24px;">SJ</div>
                  <div>
                    <div style="font-size: 22px; font-weight: 700; color: #FFFFFF;">Sarah Jenkins</div>
                    <div style="font-size: 17px; color: #64748B;">512-dim embedding · Local</div>
                  </div>
                </div>
                <span style="background: rgba(0, 230, 118, 0.15); color: #00E676; font-size: 16px; font-weight: 800; padding: 6px 14px; border-radius: 10px;">VERIFIED</span>
              </div>

              <div style="background: #151D28; border: 1px solid rgba(255,255,255,0.06); border-radius: 22px; padding: 20px; display: flex; align-items: center; justify-content: space-between;">
                <div style="display: flex; align-items: center; gap: 18px;">
                  <div style="width: 64px; height: 64px; border-radius: 50%; background: #1E2B3C; display: flex; align-items: center; justify-content: center; font-weight: 800; color: #A78BFA; font-size: 24px;">DK</div>
                  <div>
                    <div style="font-size: 22px; font-weight: 700; color: #FFFFFF;">David Kim</div>
                    <div style="font-size: 17px; color: #64748B;">512-dim embedding · Local</div>
                  </div>
                </div>
                <span style="background: rgba(0, 230, 118, 0.15); color: #00E676; font-size: 16px; font-weight: 800; padding: 6px 14px; border-radius: 10px;">VERIFIED</span>
              </div>

            </div>

            <div style="padding: 24px; background: #111721; border-top: 1px solid rgba(255,255,255,0.08); text-align: center;">
              <span style="font-size: 20px; font-weight: 700; color: #94A3B8;">Encrypted Android DataStore Storage</span>
            </div>
          </div>
        </div>
        """
    elif p["extra"] == "video":
        extra_content = """
        <div class="phone-wrapper">
          <div class="phone-screen">
            <div class="app-topbar">
              <span style="font-size: 20px; font-weight: 700; color: #FFFFFF;">Media3 Video Inspector</span>
              <span style="font-size: 18px; background: rgba(0, 230, 118, 0.2); color: #00E676; padding: 4px 14px; border-radius: 10px;">GPU DECODE</span>
            </div>

            <div style="flex: 1; background: #0A0D13; position: relative; overflow: hidden; display: flex; flex-direction: column;">
              <div style="flex: 1; position: relative; display: flex; align-items: center; justify-content: center;">
                <div style="position: absolute; left: 48px; top: 70px; width: 190px; height: 230px; border: 3px solid #00E676; border-radius: 20px;">
                  <div class="identity-pill" style="top: -38px; font-size: 18px; padding: 4px 16px;">Alex C. · 99%</div>
                </div>
                <div style="position: absolute; right: 54px; top: 120px; width: 170px; height: 210px; border: 3px solid #38BDF8; border-radius: 20px;">
                  <div class="identity-pill" style="top: -38px; background: #38BDF8; color: #082F49; font-size: 18px; padding: 4px 16px;">Sarah J. · 98%</div>
                </div>
                <svg width="84" height="84" viewBox="0 0 24 24" fill="rgba(255,255,255,0.7)" style="opacity: 0.6;"><circle cx="12" cy="12" r="10"/><polygon points="10 8 16 12 10 16 10 8"/></svg>
              </div>

              <div style="padding: 24px; background: #131A25; border-top: 1px solid rgba(255,255,255,0.06);">
                <div style="display: flex; justify-content: space-between; font-size: 18px; color: #94A3B8; margin-bottom: 12px;">
                  <span>01:24</span>
                  <span style="color: #00E676; font-weight: 700;">8 Faces Detected</span>
                  <span>04:10</span>
                </div>
                <div style="height: 12px; background: #222F42; border-radius: 999px; position: relative; overflow: hidden;">
                  <div style="position: absolute; left: 0; top: 0; bottom: 0; width: 34%; background: #00E676;"></div>
                  <div style="position: absolute; left: 12%; top: 0; bottom: 0; width: 6px; background: #FFFFFF;"></div>
                  <div style="position: absolute; left: 34%; top: 0; bottom: 0; width: 6px; background: #FFFFFF;"></div>
                  <div style="position: absolute; left: 62%; top: 0; bottom: 0; width: 10px; background: #00E676;"></div>
                  <div style="position: absolute; left: 85%; top: 0; bottom: 0; width: 10px; background: #00E676;"></div>
                </div>
              </div>
            </div>

            <div class="camera-bar">
              <div class="stat-pill" style="color: #38BDF8;">Google Cast Ready</div>
              <div class="stat-pill">Car-Aware Mode</div>
            </div>
          </div>
        </div>
        """
    elif p["extra"] == "watch":
        extra_content = """
        <div class="watch-mockup" style="left: 50%; transform: translateX(-50%); top: 40px; width: 640px; height: 640px;">
          <div class="watch-dial" style="padding: 52px;">
            <div style="background: rgba(0, 230, 118, 0.15); border: 2px solid rgba(0, 230, 118, 0.4); padding: 8px 24px; border-radius: 999px; display: flex; align-items: center; gap: 10px; margin-bottom: 14px;">
              <span style="width: 12px; height: 12px; border-radius: 50%; background: #00E676;"></span>
              <span style="font-size: 20px; font-weight: 800; color: #00E676; text-transform: uppercase;">ACTIVE DETECTOR</span>
            </div>
            <div style="font-size: 76px; font-weight: 900; color: #FFFFFF; letter-spacing: -0.02em;">3 Faces</div>
            <div style="font-size: 24px; color: #94A3B8; margin-bottom: 24px;">Identities Verified Live</div>

            <div style="background: #17212D; border-radius: 24px; padding: 18px 32px; width: 85%; display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
              <span style="font-size: 24px; font-weight: 700; color: #FFFFFF;">Alex Chen</span>
              <span style="font-size: 22px; font-weight: 800; color: #00E676;">99%</span>
            </div>

            <div style="background: #00E676; color: #051A10; font-size: 22px; font-weight: 800; padding: 14px 44px; border-radius: 999px; box-shadow: 0 8px 24px rgba(0, 230, 118, 0.4);">
              View Identities
            </div>
          </div>
        </div>
        """
    elif p["extra"] == "tuning":
        extra_content = """
        <div class="phone-wrapper">
          <div class="phone-screen">
            <div class="app-topbar">
              <span style="font-size: 20px; font-weight: 700; color: #FFFFFF;">Inference Hyperparameters</span>
              <span style="font-size: 18px; color: #00E676; font-weight: 700;">SAVED</span>
            </div>

            <div style="padding: 28px; background: #0B0E14; flex: 1; display: flex; flex-direction: column; gap: 24px;">
              
              <div style="background: #151D28; border: 2px solid rgba(0, 230, 118, 0.35); border-radius: 26px; padding: 24px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px;">
                  <span style="font-size: 22px; font-weight: 700; color: #FFFFFF;">Cosine Threshold</span>
                  <span style="font-size: 26px; font-weight: 900; color: #00E676; font-family: 'JetBrains Mono', monospace;">0.75</span>
                </div>
                <div style="height: 14px; background: #222F42; border-radius: 999px; position: relative;">
                  <div style="width: 75%; height: 100%; background: #00E676; border-radius: 999px;"></div>
                  <div style="position: absolute; left: 75%; top: 50%; transform: translate(-50%, -50%); width: 34px; height: 34px; background: #FFFFFF; border-radius: 50%; border: 5px solid #00E676; box-shadow: 0 4px 12px rgba(0,0,0,0.5);"></div>
                </div>
                <div style="display: flex; justify-content: space-between; font-size: 17px; color: #64748B; margin-top: 10px;">
                  <span>Lenient (0.50)</span>
                  <span>Strict (0.90)</span>
                </div>
              </div>

              <div style="background: #151D28; border: 1px solid rgba(255,255,255,0.06); border-radius: 26px; padding: 24px;">
                <div style="font-size: 22px; font-weight: 700; color: #FFFFFF; margin-bottom: 16px;">LiteRT Model Architecture</div>
                <div style="display: flex; flex-direction: column; gap: 14px;">
                  <div style="background: #1F2B3A; border: 2px solid #00E676; border-radius: 18px; padding: 18px; display: flex; justify-content: space-between; align-items: center;">
                    <div>
                      <div style="font-size: 21px; font-weight: 700; color: #FFFFFF;">BlazeFace + MobileFaceNet</div>
                      <div style="font-size: 17px; color: #94A3B8;">Ultra-fast 512D Embeddings</div>
                    </div>
                    <span style="color: #00E676; font-size: 24px; font-weight: 900;">✓</span>
                  </div>
                  <div style="background: #121822; border: 1px solid rgba(255,255,255,0.06); border-radius: 18px; padding: 18px; display: flex; justify-content: space-between; align-items: center;">
                    <div>
                      <div style="font-size: 21px; font-weight: 700; color: #94A3B8;">MobileNet SSD v2</div>
                      <div style="font-size: 17px; color: #64748B;">Multi-Class Bounding Boxes</div>
                    </div>
                  </div>
                </div>
              </div>

              <div style="background: #151D28; border: 1px solid rgba(255,255,255,0.06); border-radius: 26px; padding: 24px;">
                <div style="font-size: 22px; font-weight: 700; color: #FFFFFF; margin-bottom: 14px;">Compute Delegate</div>
                <div style="display: flex; gap: 12px;">
                  <span style="flex: 1; text-align: center; background: #00E676; color: #051A10; font-size: 19px; font-weight: 800; padding: 12px; border-radius: 14px;">GPU (OpenGLES)</span>
                  <span style="flex: 1; text-align: center; background: #121822; color: #94A3B8; font-size: 19px; font-weight: 700; padding: 12px; border-radius: 14px;">NNAPI / NPU</span>
                  <span style="flex: 1; text-align: center; background: #121822; color: #94A3B8; font-size: 19px; font-weight: 700; padding: 12px; border-radius: 14px;">CPU</span>
                </div>
              </div>

            </div>

            <div class="camera-bar">
              <div class="stat-pill" style="color: #00E676;">Verified Calibration</div>
              <div class="stat-pill">DataStore Sync</div>
            </div>
          </div>
        </div>
        """

    return f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=1080, height=1920">
<title>Screenshot {p["id"]}</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&family=JetBrains+Mono:wght@600;700&display=swap" rel="stylesheet">
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{
    width: 1080px;
    height: 1920px;
    overflow: hidden;
    background: radial-gradient(circle at 50% 16%, #132E24 0%, #0B1015 52%, #05070A 100%);
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    color: #FFFFFF;
    position: relative;
    user-select: none;
    display: flex;
    flex-direction: column;
  }}
  .top-header {{
    padding: 104px 72px 32px;
    text-align: center;
    position: relative;
    z-index: 20;
  }}
  .kicker {{
    display: inline-flex;
    align-items: center;
    gap: 12px;
    background: rgba(0, 230, 118, 0.12);
    border: 2px solid rgba(0, 230, 118, 0.35);
    padding: 10px 28px;
    border-radius: 999px;
    color: #00E676;
    font-size: 25px;
    font-weight: 700;
    letter-spacing: 0.06em;
    text-transform: uppercase;
    margin-bottom: 28px;
  }}
  .headline {{
    font-size: 76px;
    font-weight: 900;
    line-height: 1.12;
    letter-spacing: -0.035em;
    color: #FFFFFF;
    margin-bottom: 20px;
  }}
  .headline span.accent {{
    background: linear-gradient(135deg, #00E676 0%, #00F5D4 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }}
  .subline {{
    font-size: 32px;
    font-weight: 500;
    color: #94A3B8;
    line-height: 1.4;
  }}
  .device-stage {{
    position: relative;
    flex: 1;
    width: 100%;
    margin-top: 20px;
  }}
  .phone-wrapper {{
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    bottom: -60px;
    width: 680px;
    height: 1220px;
    background: #000000;
    border-radius: 104px;
    padding: 20px;
    box-shadow: 0 70px 160px rgba(0,0,0,0.85), 0 0 0 4px rgba(255,255,255,0.15), 0 0 100px rgba(0, 230, 118, 0.12);
    z-index: 10;
  }}
  .phone-screen {{
    width: 100%;
    height: 100%;
    background: #0B0E14;
    border-radius: 88px;
    overflow: hidden;
    position: relative;
    display: flex;
    flex-direction: column;
  }}
  .viewfinder {{
    position: relative;
    flex: 1;
    background: radial-gradient(circle at 50% 38%, #162432 0%, #0E141D 70%, #07090D 100%);
    overflow: hidden;
  }}
  .laser-scan {{
    position: absolute;
    left: 0;
    right: 0;
    top: 36%;
    height: 4px;
    background: linear-gradient(90deg, transparent, #00E676, transparent);
    box-shadow: 0 0 28px #00E676;
    opacity: 0.85;
  }}
  .face-target {{
    position: absolute;
    left: 50%;
    top: 40%;
    transform: translate(-50%, -50%);
    width: 380px;
    height: 460px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }}
  .bbox-frame {{
    position: absolute;
    inset: 24px 36px;
    border: 4px solid rgba(0, 230, 118, 0.75);
    border-radius: 36px;
    box-shadow: 0 0 48px rgba(0, 230, 118, 0.28), inset 0 0 32px rgba(0, 230, 118, 0.1);
  }}
  .bbox-corner {{
    position: absolute;
    width: 32px;
    height: 32px;
    border-color: #00E676;
    border-style: solid;
  }}
  .tl {{ top: -4px; left: -4px; border-width: 7px 0 0 7px; border-top-left-radius: 28px; }}
  .tr {{ top: -4px; right: -4px; border-width: 7px 7px 0 0; border-top-right-radius: 28px; }}
  .bl {{ bottom: -4px; left: -4px; border-width: 0 0 7px 7px; border-bottom-left-radius: 28px; }}
  .br {{ bottom: -4px; right: -4px; border-width: 0 7px 7px 0; border-bottom-right-radius: 28px; }}
  .identity-pill {{
    position: absolute;
    top: -52px;
    left: 50%;
    transform: translateX(-50%);
    background: #00E676;
    color: #051A10;
    font-weight: 800;
    font-size: 24px;
    padding: 7px 24px;
    border-radius: 999px;
    display: flex;
    align-items: center;
    gap: 12px;
    white-space: nowrap;
    box-shadow: 0 8px 28px rgba(0, 230, 118, 0.45);
  }}
  .identity-pill .dot {{
    width: 13px;
    height: 13px;
    border-radius: 50%;
    background: #051A10;
  }}
  .telemetry-tag {{
    position: absolute;
    bottom: -48px;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(11, 14, 20, 0.88);
    border: 2px solid rgba(0, 230, 118, 0.4);
    backdrop-filter: blur(16px);
    font-family: 'JetBrains Mono', monospace;
    font-size: 18px;
    font-weight: 600;
    color: #00E676;
    padding: 5px 20px;
    border-radius: 12px;
    white-space: nowrap;
  }}
  .camera-bar {{
    padding: 28px 36px;
    background: rgba(14, 19, 27, 0.96);
    border-top: 2px solid rgba(255,255,255,0.08);
    display: flex;
    justify-content: space-between;
    align-items: center;
  }}
  .stat-pill {{
    background: rgba(255,255,255,0.07);
    padding: 8px 24px;
    border-radius: 16px;
    font-size: 22px;
    font-weight: 600;
    color: #E2E8F0;
  }}
  .floating-pill {{
    position: absolute;
    background: rgba(15, 23, 42, 0.88);
    border: 2px solid rgba(0, 230, 118, 0.35);
    backdrop-filter: blur(28px);
    border-radius: 32px;
    padding: 20px 32px;
    display: flex;
    align-items: center;
    gap: 20px;
    box-shadow: 0 32px 72px rgba(0,0,0,0.5);
    z-index: 25;
  }}
  .pill-title {{
    font-size: 25px;
    font-weight: 800;
    color: #FFFFFF;
  }}
  .pill-desc {{
    font-size: 21px;
    color: #94A3B8;
  }}
  .watch-mockup {{
    position: absolute;
    background: radial-gradient(circle, #17202B 0%, #080C10 100%);
    border-radius: 50%;
    border: 12px solid #283344;
    box-shadow: 0 40px 96px rgba(0,0,0,0.85), 0 0 0 4px rgba(0, 230, 118, 0.35);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    z-index: 30;
  }}
  .watch-dial {{
    width: 100%;
    height: 100%;
    padding: 32px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }}
  .tablet-mockup {{
    position: absolute;
    background: #10161F;
    border-radius: 64px;
    border: 8px solid #1E293B;
    box-shadow: 0 48px 100px rgba(0,0,0,0.6);
    overflow: hidden;
  }}
  .app-topbar {{
    padding: 24px 32px;
    background: #111721;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 2px solid rgba(255,255,255,0.06);
  }}
  .app-title {{
    font-size: 26px;
    font-weight: 800;
    color: #FFFFFF;
    display: flex;
    align-items: center;
    gap: 12px;
  }}
</style>
</head>
<body>

  <div class="top-header">
    <div class="kicker">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><path d="m9 12 2 2 4-4"/></svg>
      {p["kicker"]}
    </div>
    <div class="headline">{p["headline"]}</div>
    <div class="subline">{p["subline"]}</div>
  </div>

  <div class="device-stage">
    {extra_content}

    <div class="floating-pill" style="left: 40px; bottom: 70px;">
      <svg width="44" height="44" viewBox="0 0 24 24" fill="none" stroke="#00E676" stroke-width="2">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
      </svg>
      <div>
        <div class="pill-title">{p["trust_top"]}</div>
        <div class="pill-desc">{p["trust_sub"]}</div>
      </div>
    </div>
  </div>

</body>
</html>
"""

def export_all():
    export_dir = os.path.join(BASE_DIR, "export")
    os.makedirs(export_dir, exist_ok=True)

    # Make folders for each preset
    for preset_name in PRESETS.keys():
        os.makedirs(os.path.join(export_dir, preset_name), exist_ok=True)

    # Generate each panel's HTML file and render it
    base_1080_dir = os.path.join(export_dir, "base_1080")
    os.makedirs(base_1080_dir, exist_ok=True)

    for p in PANELS_DATA:
        panel_id = p["id"]
        html_file = os.path.join(BASE_DIR, f"panel_{panel_id}.html")
        with open(html_file, "w", encoding="utf-8") as f:
            f.write(make_panel_html(p))

        out_png = os.path.join(base_1080_dir, f"{panel_id}.png")
        url = f"file:///{html_file.replace(os.sep, '/')}"
        
        cmd = [
            CHROME_PATH,
            f"--user-data-dir={TEMP_PROFILE}",
            "--headless",
            "--disable-gpu",
            f"--screenshot={out_png}",
            "--window-size=1080,1920",
            url
        ]
        print(f"Rendering Panel {panel_id} at 1080x1920...")
        subprocess.run(cmd, check=True)

        # Convert to RGB (no alpha) and resize for each preset
        im = Image.open(out_png).convert("RGB")

        for preset_name, (pw, ph) in PRESETS.items():
            preset_file = os.path.join(export_dir, preset_name, f"{panel_id}.png")
            if (pw, ph) == (1080, 1920):
                im.save(preset_file, "PNG")
            else:
                resized = im.resize((pw, ph), Image.Resampling.LANCZOS)
                resized.save(preset_file, "PNG")
            print(f" -> Exported {preset_name}/{panel_id}.png ({pw}x{ph})")

    # Render Feature Graphic
    fg_html = os.path.join(BASE_DIR, "feature_graphic.html")
    fg_png = os.path.join(export_dir, "feature_graphic.png")
    fg_url = f"file:///{fg_html.replace(os.sep, '/')}"
    cmd_fg = [
        CHROME_PATH,
        f"--user-data-dir={TEMP_PROFILE}",
        "--headless",
        "--disable-gpu",
        f"--screenshot={fg_png}",
        "--window-size=1024,500",
        fg_url
    ]
    print("Rendering Feature Graphic (1024x500)...")
    subprocess.run(cmd_fg, check=True)
    Image.open(fg_png).convert("RGB").save(fg_png, "PNG")
    print(f" -> Exported feature_graphic.png (1024x500)")

    # Render Complete Set Preview
    set_html = os.path.join(BASE_DIR, "full_set.html")
    set_png = os.path.join(export_dir, "full_set_preview.png")
    set_url = f"file:///{set_html.replace(os.sep, '/')}"
    cmd_set = [
        CHROME_PATH,
        f"--user-data-dir={TEMP_PROFILE}",
        "--headless",
        "--disable-gpu",
        f"--screenshot={set_png}",
        "--window-size=3500,1200",
        set_url
    ]
    print("Rendering Full Set Preview...")
    subprocess.run(cmd_set, check=True)
    Image.open(set_png).convert("RGB").save(set_png, "PNG")
    print(" -> Exported full_set_preview.png")

if __name__ == "__main__":
    export_all()
