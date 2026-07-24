FROM python:3.13-slim
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

ENV UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PATH="/app/.venv/bin:$PATH"

COPY pyproject.toml uv.lock ./
RUN uv sync --locked --no-install-project --no-dev

COPY locustfile.py .
RUN uv sync --locked --no-dev

ENV LOCUST_WEB_PORT=8089 LOCUST_AUTOSTART=true LOCUST_USERS=10 LOCUST_SPAWN_RATE=1
ENV HOST_PORT="shortener:3000" 

EXPOSE ${LOCUST_WEB_PORT}

SHELL [ "/bin/sh", "-c" ]

ENTRYPOINT locust -f ./locustfile.py --host http://${HOST_PORT}
