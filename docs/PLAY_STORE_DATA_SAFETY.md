# Google Play Console: Data Safety & Policy Questionnaire Guide
**Application**: SJ Player (Eyes-Free Recorder)  
**Package Identifier**: `com.jonlee.android.SJplayer`  
**Purpose**: Reference guide for completing the mandatory **Data Safety Section** and **App Content Declarations** in Google Play Console.

---

## 1. Data Safety Questionnaire Responses

When filling out the Google Play Console **Data safety** form under *Policy and programs > Data safety*, answer the questionnaire as follows:

### Step 1: Data Collection & Sharing Overview
- **Does your app collect or share any of the required user data types?**  
  $\rightarrow$ **No**  
  *(Explanation: All audio recordings and app settings remain strictly local on the user's physical device. Nothing is collected by the developer or sent to external servers).*

- **Is all of the user data collected by your app encrypted in transit?**  
  $\rightarrow$ **N/A** (No data is transmitted over the network).

- **Do you provide a way for users to request that their data is deleted?**  
  $\rightarrow$ **Yes** (Users can delete any audio file at any time directly within the app via the 4-finger swipe gesture or standard Android file manager).

---

### Step 2: Specific Permission & Data Type Breakdown

If Google asks specifically about **Audio Files** (`RECORD_AUDIO` / `READ_MEDIA_AUDIO`):

| Question | Correct Play Console Answer | Rationale |
| :--- | :--- | :--- |
| **Are audio recordings collected?** | **No** | Recording is saved locally on user device; developer never collects or receives the audio files. |
| **Is audio shared with third parties?** | **No** | Audio is never transferred to any third party. |
| **Is processing ephemeral?** | **Yes** / **Local on device** | Real-time audio stream is processed locally on CPU to generate file and VU meter. |
| **Location Data** | **Not Collected** | No GPS or location data collected. |
| **Personal Info (Name, Email)** | **Not Collected** | No account creation or personal info requested. |
| **Financial Info** | **Not Collected** | No in-app purchases or payment processing. |
| **Device or other IDs** | **Not Collected** | No advertising IDs or IMEI harvested. |

---

## 2. Target Audience & Content Declarations

Under *App Content* in Google Play Console:

1. **Target Age**:
   - Select: **18 and over** (or 13 and over).
   - Could your app unintentionally appeal to children? $\rightarrow$ **No**.
2. **Ads Declaration**:
   - Does your app contain ads? $\rightarrow$ **No** (SJ Player contains 0 ads).
3. **App Access**:
   - Are any parts of your app restricted (e.g. login credentials)? $\rightarrow$ **All functionality is available without restrictions**.
4. **Government Apps**:
   - Is this an official government app? $\rightarrow$ **No**.
5. **Financial Features**:
   - Does your app provide financial features? $\rightarrow$ **No**.
6. **Health Apps**:
   - Is this a medical app or health tracker? $\rightarrow$ **No**.
7. **Privacy Policy Link**:
   - Enter your public GitHub raw or pages link:  
     `https://raw.githubusercontent.com/jonlee0704/EYES-FREE-RECORDER/master/docs/PRIVACY_POLICY.md`  
     *(Or host on GitHub Pages: `https://jonlee0704.github.io/EYES-FREE-RECORDER/docs/PRIVACY_POLICY.html`)*

---

## 3. Play Store Listing Compliance Checklist

- [x] Microphone permission disclosure visible before recording prompt.
- [x] Privacy Policy publicly reachable on GitHub.
- [x] Terms of Service established with comprehensive warranty and wiretapping disclaimers.
- [x] Zero third-party ad networks or tracking trackers bundled in APK/AAB.
- [x] Foreground Service permission (`FOREGROUND_SERVICE_MICROPHONE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) declared with persistent notification icon.
