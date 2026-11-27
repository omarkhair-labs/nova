FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

ARG PLANTUPIA_HOSTED_LIVING_QA=0
ARG PLANTUPIA_HOSTED_ARM_VISION_QA=0
COPY hosted_living.py ./
COPY plants/room_colmap_cli.py ./plants/room_colmap_cli.py
RUN if [ "$PLANTUPIA_HOSTED_LIVING_QA" = "1" ]; then \
        apt-get update && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 libgomp1 libportaudio2 && \
        if [ "$PLANTUPIA_HOSTED_ARM_VISION_QA" = "1" ] && [ "$(uname -m)" = "aarch64" ]; then \
            apt-get install -y --no-install-recommends colmap=3.10-2 libegl1; \
        fi && \
        rm -rf /var/lib/apt/lists/* && \
        PLANTUPIA_HOSTED_ARM_VISION_QA="$PLANTUPIA_HOSTED_ARM_VISION_QA" python hosted_living.py prepare; \
    fi

COPY . .

EXPOSE 8000

CMD ["python", "hosted_living.py", "start"]
