#!/usr/bin/env bash
# post-create.sh — runs once after the Dev Container is created

echo "╔══════════════════════════════════════════════════╗"
echo "║   Minecraft Mod Dev Container — Post-Create      ║"
echo "╚══════════════════════════════════════════════════╝"

# ── 1. Make Gradle wrapper executable (if present and writable) ──
if [ -f "./gradlew" ]; then
  if chmod +x ./gradlew 2>/dev/null; then
    echo "→ gradlew marked executable"
  else
    echo "ℹ  gradlew exists but chmod not permitted — skipping (it may already be executable)"
  fi

  if [ -x "./gradlew" ]; then
    echo "→ Running initial Gradle dependency sync..."
    ./gradlew dependencies --no-daemon -q 2>&1 || echo "⚠  Gradle sync had warnings."
  else
    echo "ℹ  gradlew is not executable — skipping Gradle sync."
    echo "   Fix ownership or run: sudo chmod +x gradlew"
  fi
else
  echo "ℹ  No gradlew found yet — expected on a fresh container with no project."
fi

# ── 2. Print environment info ──
echo ""
echo "── Environment ─────────────────────────────────────"
java -version 2>&1 | head -1 | xargs -I{} echo "Java 21:   {}" || true
/usr/lib/jvm/java-17-openjdk-amd64/bin/java -version 2>&1 | head -1 | xargs -I{} echo "Java 17:   {}" 2>/dev/null || true
gradle --version 2>/dev/null | grep "^Gradle " | xargs -I{} echo "Gradle:    {}" || true
node --version 2>/dev/null | xargs -I{} echo "Node.js:   {}" || true
claude --version 2>/dev/null | xargs -I{} echo "Claude:    {}" || true
codex --version 2>/dev/null | xargs -I{} echo "Codex:     {}" || true
git --version 2>/dev/null | xargs -I{} echo "Git:       {}" || true
echo "────────────────────────────────────────────────────"

# ── 3. Claude Code auth reminder ──
echo ""
echo "🤖  Claude Code is installed. To authenticate, run:"
echo ""
echo "      claude login"
echo ""
echo "🤖  Codex is installed. To authenticate, set your API key:"
echo ""
echo "      export OPENAI_API_KEY=<your-key>"
echo "      # Add to ~/.bashrc to persist across sessions"
echo ""
echo "✅  Container ready!"
echo ""
echo "  FABRIC (recommended for 1.20+):  https://fabricmc.net/develop/template/"
echo "  FORGE:                            https://files.minecraftforge.net"
echo "  NEOFORGE:                         https://github.com/neoforged/MDK"
echo ""
echo "  Build alias: 'gw build'  (= './gradlew build')"
echo ""

exit 0