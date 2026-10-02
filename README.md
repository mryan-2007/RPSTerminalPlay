# RPS // BATTLE TERMINAL

A futuristic, cyberpunk-styled 1v1 Rock-Paper-Scissors battle terminal for Android.
One Android phone acts as the server using Termux, and another connects over the network.

---

## Termux Server Setup Guide (Host Android)

### Step 1: Install Termux
Install **Termux** on your Android phone (recommended from F-Droid or GitHub releases for the most up-to-date packages).

### Step 2: Update & Install Python
Open Termux and run the following commands:
```bash
pkg update && pkg upgrade -y
pkg install python git -y
pip install websockets
```

### Step 3: Run the Server
Copy or clone the `server/` directory onto your phone, or create the files directly.
Inside the directory containing `server.py`:
```bash
python server.py
```

The terminal will display:
```
============================================================
      RPS // BATTLE TERMINAL - AUTHORITATIVE SERVER
============================================================
Status: ONLINE
Listening Port : 8765
Host Local IP  : 192.168.1.15
============================================================
```

### Step 4: Connecting the Phones

#### Scenario A: Same Wi-Fi (or Hotspot)
1. Both phones connect to the same Wi-Fi network (or Host turns on a portable hotspot and Friend connects to it).
2. Look at the Host Local IP printed in Termux (e.g. `192.168.1.15`).
3. In the Android App:
   - **Host (Player 1):** Connects to `ws://127.0.0.1:8765`
   - **Friend (Player 2):** Connects to `ws://192.168.1.15:8765`

---

## Playing Online (When You & Your Friend Are Far Away)

When you and your friend are on different Wi-Fi networks (miles away), use any of these **100% free** methods to connect over the Internet without touching router port-forwarding:

### Option 1: Pinggy Tunnel (Easiest — 1 Command, Zero Install)
While `server.py` is running in Termux, open a new Termux session (swipe from left edge -> "New Session") and run:
```bash
ssh -p 443 -R0:localhost:8765 a.pinggy.io
```
Termux will display a public URL such as:
`https://xyz.a.pinggy.link`

1. Share that link with your friend.
2. In the app on **both** phones, enter:
   `wss://xyz.a.pinggy.link`
3. Tap **CONNECT** and you are instantly linked over the Internet!

---

### Option 2: Cloudflare Tunnel (Free & Unlimited)
Install Cloudflared in Termux:
```bash
pkg install cloudflared -y
cloudflared tunnel --url http://localhost:8765
```
Cloudflare will give you a public URL like:
`https://xxxx.trycloudflare.com`

In the app, connect using:
`wss://xxxx.trycloudflare.com`

---

### Option 3: Tailscale (Private Mesh Network)
1. Both you and your friend install **Tailscale** from the Google Play Store (free).
2. Log in with your accounts and connect.
3. Tailscale assigns the host phone an internal IP (like `100.85.12.34`).
4. In the app, your friend connects to:
   `ws://100.85.12.34:8765`

---

### Option 4: Free Cloud Deployment (Always-On Server)
You can also host `server.py` on free services like **Render.com** or **Railway.app**:
1. Push this repo to GitHub.
2. Create a free Web Service on Render (Build: `pip install websockets`, Start: `python server/server.py`).
3. You get a permanent address like `wss://rps-battle.onrender.com` that you and your friend can play on 24/7!

---

## Building on GitHub Codespaces

The repository includes complete **Gradle wrapper (`gradlew`)**, **Android SDK automated installer**, and **Dev Container (`.devcontainer`)** configurations for GitHub Codespaces.

### Option 1: Automatic Setup via Dev Container
1. Open this repository on GitHub and click **Code -> Codespaces -> Create codespace on main**.
2. The dev container automatically runs the Android SDK setup in the background, installing:
   - Command-Line Tools
   - SDK Platform 36 (`platforms;android-36`)
   - Build-Tools 36 (`build-tools;36.0.0`)
   - Platform-Tools
   - Python dependencies
3. Run the build:
   ```bash
   ./gradlew assembleDebug
   ```

### Option 2: One-Command Manual Setup
If you are running in a default Codespaces container or any Linux shell without the Dev Container:
```bash
bash setup-android.sh
./gradlew assembleDebug
```

The compiled APK will be output to:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

### Running the Server in Codespaces
You can also run the authoritative Python game server directly inside Codespaces:
```bash
python server/server.py
```
*(Under the **Ports** tab in Codespaces, set port `8765` visibility to **Public** to obtain an instant public URL to connect with friends anywhere!)*

---

## Game Rules & Commands

### Initial Setup
- **Initial Score:** 50 points each
- **Initial Deck:** 6 cards:
  - 2 ROCK
  - 2 PAPER
  - 2 SCISSORS

### Wager Ranks
Every card is wagered with a rank:
- **C** = 3 points
- **B** = 7 points
- **A** = 15 points
- **S** = 25 points

> **Score Transfer:**
> - Winner gains the opponent's wager.
> - Loser loses their own wager.
> - Draw returns both wagers.
> - Total score remains conserved!

### Available Commands
- `/start` - Ready up for the round. Both players must type `/start`.
- `/cards` - Show remaining cards formatted in colored bracket notation:
  ```
  [ROCK] [ROCK]
  [PAPER] [PAPER]
  [SCISSORS] [SCISSORS]
  ```
- `/choose <card> <rank>` - Choose card and wager (e.g. `/choose rock A`).
  - The opponent only sees: `[ ??????  A ]`
  - The card type is revealed only after both players have locked in!
- `/clear` - Clears the visible terminal log for your screen without resetting the score or game.
- `/help` - Display available commands and rules.
- `/status` - Display connection and round status.
