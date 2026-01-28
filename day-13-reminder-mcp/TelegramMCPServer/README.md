# Telegram MCP Server

Лёгкий MCP-сервер (Model Context Protocol), написанный на Python, который предоставляет данные из публичных Telegram-каналов как вызываемые инструменты для AI-ассистентов.

Сервер общается по HTTP через SSE (Server-Sent Events) и JSON-RPC 2.0 — стандартный формат MCP. Под капотом он использует [Telethon](https://github.com/LonaLiworworworworworworworworworworworworworworr/Telethon) для подключения к Telegram по протоколу MTProto — это единственный способ читать произвольные публичные каналы (Bot API требует добавления бота как члена канала).

---

## Предварительные требования

### Учётные данные Telegram API

1. Откройте [https://my.telegram.org](https://my.telegram.org) в браузере
2. Войдите в свой аккаунт Telegram
3. Нажмите **"Create new application"**
4. Укажите любое имя приложения и платформу — значения не важны
5. Скопируйте `App api_id` и `App api_hash` — они понадобятся ниже

Эти учётные данные **бесплатны**, без ограничений и остаются валидными неограниченно.

### Однократная настройка сессии

Telethon авторизуется через OTP (SMS или код в приложении). Это интерактивный шаг, который нужно выполнить один раз, после чего сервер работает без участия пользователя:

```bash
cd TelegramMCPServer
pip install -r requirements.txt

TELEGRAM_API_ID=your_id TELEGRAM_API_HASH=your_hash python setup_session.py
```

Скрипт попросит номер телефона, затем код OTP из Telegram. После успешной авторизации создаётся файл `telegram_session.session` — сервер переиспользует его для всех последующих запросов.

> **Берегите файл сессии.** Он содержит токены аутентификации, эквивалентные авторизованной сессии Telegram.

---

## Быстрый старт

### 1. Установить зависимости

```bash
pip install -r requirements.txt
```

### 2. Настроить сессию (один раз)

```bash
TELEGRAM_API_ID=your_id TELEGRAM_API_HASH=your_hash python setup_session.py
```

### 3. Запустить сервер

```bash
TELEGRAM_API_ID=your_id TELEGRAM_API_HASH=your_hash python main.py --no-auth
```

Сервер стартует на `http://localhost:8000`. Ключ MCP API не требуется — подходит для локального развития.

### 4. Проверить работу

```bash
curl http://localhost:8000/health
curl http://localhost:8000/tools
```

---

## Два независимых уровня авторизации

| Уровень | За что отвечает | Как настроить |
|---------|-----------------|---------------|
| **MCP Server Auth** | Кто может обращаться к MCP-серверу | Переменная `MCP_API_KEY` + заголовок `X-API-Key` |
| **Telegram Auth** | Доступ к каналам Telegram | Файл сессии, созданный `setup_session.py` |

### MCP Server Auth

- **Включена (по умолчанию):** установите `MCP_API_KEY` в окружении. Клиенты передают ключ через заголовок `X-API-Key` или параметр запроса `?api_key=`.
- **Отключена:** запускайте с флагом `--no-auth`. Подходит только для локального и dev окружения.

### Telegram Auth

- Файл сессии (`telegram_session.session`) работает как постоянный вход.
- Все чтения каналов идут через эту сессию.
- Если сессия истекла (редкость — сессии Telegram долгосрочные), запустите `setup_session.py` заново.

---

## Конфигурация

Сервер читает переменные из двух источников с чётким приоритетом:

| Приоритет | Источник | Когда работает |
|-----------|----------|----------------|
| 1 (высокий) | Файл `.env` в директории сервера | Если файл существует и содержит ключ |
| 2 (запас) | Переменные окружения (`export` / системные) | Если ключа нет в `.env` |

Это позволяет хранить всё в `.env` и запускать сервер без `source` или `export`:

```bash
cp .env.example .env
# отредактировать .env
python main.py --no-auth   # .env загружается автоматически
```

Или:
```bash
    python3 -m venv path/to/venv && \
    source path/to/venv/bin/activate && \
    python3 -m pip install fastapi && \
    python3 -m pip install uvicorn && \
    python3 -m pip install telethon && \
    python3 -m pip install python-dotenv && \
    python3 main.py --no-auth
```

При необходимости отдельные значения можно подать через окружение — например, для CI или при смене порта на лету:

```bash
# PORT из окружения будет использован только если его нет в .env
PORT=9000 python main.py --no-auth
```

### Переменные окружения

| Переменная | По умолчанию | Описание |
|------------|--------------|----------|
| `TELEGRAM_API_ID` | — | Telegram API ID (обязательна) |
| `TELEGRAM_API_HASH` | — | Telegram API Hash (обязательна) |
| `TELEGRAM_SESSION_FILE` | `telegram_session` | Имя файла сессии (без расширения `.session`) |
| `MCP_API_KEY` | — | Ключ API сервера (обязателен, если не `--no-auth`) |
| `HOST` | `0.0.0.0` | Адрес привязки |
| `PORT` | `8000` | Порт привязки |

### Флаги CLI

| Флаг | Описание |
|------|----------|
| `--no-auth` | Отключить аутентификацию по API ключу |
| `--host <addr>` | Переопределить адрес привязки |
| `--port <n>` | Переопределить порт |

---

## Доступные инструменты

### `get_channel_messages`

Получить последние сообщения из публичного Telegram-канала.

| Параметр | Обязательный | Описание |
|----------|--------------|----------|
| `channel` | да | Имя пользователя канала — `durov` или `@durov` |
| `count` | нет | Количество сообщений (1–100, по умолчанию 5) |

Возвращает: текст сообщения (до 500 символов), дату, количество просмотров, флаг медиа, ссылку на ответ.

**Пример промпта для AI:** *"Какие последние 3 поста на @durov?"*

### `get_channel_info`

Получить информацию о публичном Telegram-канале.

| Параметр | Обязательный | Описание |
|----------|--------------|----------|
| `channel` | да | Имя пользователя канала — `durov` или `@durov` |

Возвращает: название, описание, количество участников, тип канала (вещательный или интерактивный), статус верификации, дату создания.

**Пример промпта для AI:** *"Сколько участников в канале @python?"*

---

## Ограничения

| Что | Почему |
|-----|--------|
| **Только публичные каналы** | Приватные каналы требуют членства; аккаунт сессии должен быть членом |
| **Только текст** | Медиафайлы (фото, видео, документы) отмечаются флагом, но не загружаются |
| **Rate limits** | Telegram ограничивает интенсивное использование — `FloodWaitError` возвращается с необходимым временем ожидания |
| **Одна сессия** | Все запросы используют одну сессию Telegram; высокая нагрузка может вызвать throttling |

---

## Тестирование с curl

### Health check (без авторизации)

```bash
curl http://localhost:8000/health
```

### Список доступных инструментов

```bash
# Без авторизации
curl http://localhost:8000/tools

# С авторизацией
curl -H "X-API-Key: my-secret-key" http://localhost:8000/tools
```

### Полная MCP-сессия (вручную)

```bash
# 1. Открыть SSE-соединение (терминал A)
curl -s http://localhost:8000/sse

# 2. Скопировать sessionId из первого события, затем в терминале B:
SESSION_ID="<uuid-from-sse-output>"

# 3. Рукопожатие (handshake)
curl -X POST "http://localhost:8000/message?sessionId=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":1,"method":"initialize"}'

# 4. Список инструментов
curl -X POST "http://localhost:8000/message?sessionId=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/list"}'

# 5. Получить последние 3 сообщения из @durov
curl -X POST "http://localhost:8000/message?sessionId=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0","id":3,"method":"tools/call",
    "params":{
      "name":"get_channel_messages",
      "arguments":{"channel":"durov","count":3}
    }
  }'

# 6. Получить информацию о канале
curl -X POST "http://localhost:8000/message?sessionId=$SESSION_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc":"2.0","id":4,"method":"tools/call",
    "params":{
      "name":"get_channel_info",
      "arguments":{"channel":"durov"}
    }
  }'
```

---

## Подключение из Kotlin-приложения

В приложении GigaChat добавьте сервер через экран MCP Management:

| Поле | Значение |
|------|----------|
| Name | Telegram MCP |
| Type | HTTP |
| URL | `http://localhost:8000/sse` |
| API Key | *(ваш MCP_API_KEY, или оставить пустым при `--no-auth`)* |

Приложение автоматически обнаруживает 2 инструмента Telegram и делает их доступными для function calling AI.

---

## Структура проекта

```
TelegramMCPServer/
├── main.py              # Настройка FastAPI, парсинг CLI, старт сервера
├── config.py            # Конфигурация из переменных окружения
├── telegram_client.py   # Async-обёртка Telethon для чтения каналов
├── tools.py             # 2 реализации инструментов Telegram
├── mcp_protocol.py      # Обработчик JSON-RPC 2.0 / MCP протокола
├── sse_transport.py     # Управление SSE-сессиями и регистрация маршрутов
├── setup_session.py     # Однократная интерактивная авторизация Telegram
├── requirements.txt     # Зависимости Python
├── .env.example         # Шаблон переменных окружения
└── README.md            # Этот файл
```

---

## Требования

- Python 3.10+
- Аккаунт Telegram (для однократной верификации OTP)
- Учётные данные API с [my.telegram.org](https://my.telegram.org)

## Зависимости

| Пакет | Назначение |
|-------|------------|
| `fastapi` | HTTP-фреймворк (ASGI) |
| `uvicorn` | ASGI-сервер |
| `telethon` | Клиент Telegram MTProto — единственный способ читать произвольные публичные каналы |

---

## Решение проблем

### "Session file not found" / ошибки авторизации при старте

Запустите `setup_session.py` снова для воссоздания сессии.

### `FloodWaitError: 42 seconds`

Telegram ограничивает частоту запросов для этой сессии. В сообщении об ошибке указано необходимое время ожидания. Снизите частоту запросов или увеличьте паузы между вызовами инструментов.

### Канал показывает "not found", но существует публично

`@username` возможно изменился, или канал доступен только по ссылке-приглашению (`t.me/joinchat/...`). Поддерживаются только каналы с публичным `@username`.

### OTP не приходит во время настройки

Проверьте спам-фильтры в приложении Telegram. Если код всё ещё не приходит, попробуйте через несколько минут — у Telegram есть лимит кодов авторизации на аккаунт.
