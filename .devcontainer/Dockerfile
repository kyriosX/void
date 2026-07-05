FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
ENV PYTHONUNBUFFERED=1
ENV PIP_DISABLE_PIP_VERSION_CHECK=1

# -----------------------------------------------------------------------------
# Base system packages
# -----------------------------------------------------------------------------
RUN apt-get update && apt-get install -y \
    software-properties-common \
    curl \
    wget \
    git \
    unzip \
    zip \
    build-essential \
    pkg-config \
    cmake \
    jq \
    ripgrep \
    fd-find \
    tree \
    vim \
    nano \
    htop \
    zsh \
    ffmpeg \
    imagemagick \
    poppler-utils \
    ghostscript \
    tesseract-ocr \
    tesseract-ocr-eng \
    libreoffice \
    libreoffice-java-common \
    pandoc \
    antiword \
    unrtf \
    exiftool \
    python3 \
    python3-dev \
    python3-pip \
    python3-venv \
    openjdk-21-jdk \
    maven \
    gradle \
    && rm -rf /var/lib/apt/lists/*

# -----------------------------------------------------------------------------
# Node.js 22
# -----------------------------------------------------------------------------
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs

# -----------------------------------------------------------------------------
# Python virtual environment (fix for PEP 668 externally-managed-environment)
# -----------------------------------------------------------------------------

# Create isolated venv to avoid system Python restrictions (PEP 668)
RUN python3 -m venv /opt/venv

# Activate venv for all subsequent layers
ENV PATH="/opt/venv/bin:$PATH"

# Upgrade pip inside venv
RUN pip install --upgrade pip setuptools wheel

# -----------------------------------------------------------------------------
# Python libraries
# -----------------------------------------------------------------------------
RUN pip install --no-cache-dir \
    pymupdf \
    pypdf \
    pdfplumber \
    camelot-py \
    pdfminer.six \
    pikepdf \
    reportlab \
    borb \
    ocrmypdf \
    pytesseract \
    opencv-python-headless \
    easyocr \
    paddleocr \
    python-docx \
    docx2txt \
    mammoth \
    openpyxl \
    odfpy \
    python-pptx \
    "unstructured[all-docs]" \
    pandas \
    numpy \
    pyarrow \
    scikit-learn \
    sentence-transformers \
    transformers \
    langchain \
    llama-index \
    openai \
    anthropic \
    tiktoken \
    spacy \
    nltk \
    fastapi \
    uvicorn \
    jupyterlab \
    ipykernel \
    rich \
    typer \
    beautifulsoup4 \
    lxml \
    requests \
    httpx

# -----------------------------------------------------------------------------
# Download SpaCy model
# -----------------------------------------------------------------------------
RUN python3 -m spacy download en_core_web_sm

# -----------------------------------------------------------------------------
# Apache Tika
# -----------------------------------------------------------------------------
RUN mkdir -p /opt/tika

RUN wget -O /opt/tika/tika-server.jar \
    https://archive.apache.org/dist/tika/2.9.2/tika-server-standard-2.9.2.jar

# -----------------------------------------------------------------------------
# Java helper libraries
# -----------------------------------------------------------------------------
RUN mkdir -p /opt/java-libs

WORKDIR /opt/java-libs

RUN wget https://repo1.maven.org/maven2/org/apache/pdfbox/pdfbox-app/3.0.2/pdfbox-app-3.0.2.jar
RUN wget https://repo1.maven.org/maven2/org/apache/tika/tika-app/2.9.2/tika-app-2.9.2.jar

# -----------------------------------------------------------------------------
# Claude CLI
# -----------------------------------------------------------------------------
RUN npm install -g @anthropic-ai/claude-code

# -----------------------------------------------------------------------------
# Workspace
# -----------------------------------------------------------------------------
RUN mkdir -p /workspace/data
RUN mkdir -p /workspace/outputs
RUN mkdir -p /workspace/notebooks
RUN mkdir -p /workspace/temp

WORKDIR /workspace

# -----------------------------------------------------------------------------
# Aliases
# -----------------------------------------------------------------------------
RUN echo 'alias ll="ls -lah"' >> /root/.bashrc
RUN echo 'alias tika="java -jar /opt/java-libs/tika-app-2.9.2.jar"' >> /root/.bashrc
RUN echo 'alias pdfbox="java -jar /opt/java-libs/pdfbox-app-3.0.2.jar"' >> /root/.bashrc

# ------------------------------------------------------------
# Build-time user configuration (YOU CONTROL THIS)
# ------------------------------------------------------------
ARG USERNAME=devuser
ARG USER_UID=1001
ARG USER_GID=1001

# ------------------------------------------------------------
# Base packages
# ------------------------------------------------------------
RUN apt-get update && apt-get install -y \
    sudo \
    curl wget git \
    bash \
    && rm -rf /var/lib/apt/lists/*

# ------------------------------------------------------------
# Create user safely (idempotent + conflict-safe)
# ------------------------------------------------------------
RUN groupadd -g ${USER_GID} ${USERNAME} || true \
    && useradd -m -s /bin/bash -u ${USER_UID} -g ${USER_GID} ${USERNAME} || true \
    && usermod -aG sudo ${USERNAME} \
    && echo "${USERNAME} ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/${USERNAME} \
    && chmod 0440 /etc/sudoers.d/${USERNAME}

# ------------------------------------------------------------
# Workspace
# ------------------------------------------------------------
RUN mkdir -p /workspace && chown -R ${USERNAME}:${USERNAME} /workspace

WORKDIR /workspace

# ------------------------------------------------------------
# Switch to non-root user
# ------------------------------------------------------------
USER ${USERNAME}

# -----------------------------------------------------------------------------
# Exposed ports
# -----------------------------------------------------------------------------
EXPOSE 8000
EXPOSE 8888
EXPOSE 9998

# -----------------------------------------------------------------------------
# Default shell
# -----------------------------------------------------------------------------
CMD ["/bin/bash"]