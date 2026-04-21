# ─────────────────────────────────────────────
#  Minecraft Java Mod Development Container
#  Base: Ubuntu 24.04 LTS (Noble)
# ─────────────────────────────────────────────
FROM ubuntu:24.04

ARG DEBIAN_FRONTEND=noninteractive

# ── System packages ──────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Java runtimes
    openjdk-21-jdk \
    openjdk-17-jdk \
    # Build tools
    git \
    curl \
    wget \
    unzip \
    zip \
    ca-certificates \
    # Node.js deps (needed by nvm/npm)
    gnupg \
    # Nice-to-haves
    bash-completion \
    nano \
    openssh-client \
    vim \
    htop \
    jq \
    findutils \
    sudo \
    && rm -rf /var/lib/apt/lists/*

# ── Set default Java to 21 ───────────────────
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# ── Install Gradle 8.7 ───────────────────────
ARG GRADLE_VERSION=8.7
RUN wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

# ── Install Node.js 22 LTS (system-wide) ─────
# Claude Code's npm method requires Node 18+; 22 is current LTS.
# Using NodeSource for an up-to-date version.
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# ── Repurpose the existing 'ubuntu' user (UID 1000) ──
# Ubuntu 24.04 ships with 'ubuntu' at UID/GID 1000 — rename instead of recreate.
ARG USERNAME=mcdev

RUN usermod  -l ${USERNAME}          ubuntu \
    && groupmod -n ${USERNAME}        ubuntu \
    && usermod  -d /home/${USERNAME} -m ${USERNAME} \
    && echo "${USERNAME} ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/${USERNAME} \
    && chmod 0440 /etc/sudoers.d/${USERNAME}

# ── Configure npm for user-level globals (avoids sudo npm) ───────────────────
# Must run as root before switching users so the profile is in place.
RUN mkdir -p /home/${USERNAME}/.npm-global \
    && chown -R ${USERNAME}:${USERNAME} /home/${USERNAME}/.npm-global

# ── Switch to dev user ────────────────────────
USER ${USERNAME}
WORKDIR /home/${USERNAME}

# ── Gradle cache home ─────────────────────────
ENV GRADLE_USER_HOME=/home/${USERNAME}/.gradle

# ── npm prefix + PATH (user-level, no sudo needed) ───────────────────────────
ENV NPM_CONFIG_PREFIX=/home/${USERNAME}/.npm-global
ENV PATH="/home/${USERNAME}/.npm-global/bin:${PATH}"

# ── Install Claude Code via npm ───────────────
# Native installer (curl | bash) cannot run non-interactively in Docker;
# npm install is the reliable headless method.
RUN npm config set prefix "${NPM_CONFIG_PREFIX}" \
    && npm install -g @anthropic-ai/claude-code

# ── Shell quality-of-life ─────────────────────
RUN echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc \
    && echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc \
    && echo 'export NPM_CONFIG_PREFIX=$HOME/.npm-global' >> ~/.bashrc \
    && echo 'export PATH=$HOME/.npm-global/bin:$PATH' >> ~/.bashrc \
    && echo 'alias ll="ls -la"' >> ~/.bashrc \
    && echo 'alias gw="./gradlew"' >> ~/.bashrc

# ── Verify ────────────────────────────────────
RUN java -version && gradle --version && node --version && claude --version