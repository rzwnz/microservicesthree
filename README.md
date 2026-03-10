# File Service — микросервис файлового хранилища

[![CI](https://github.com/rzwnz/microservicesthree/actions/workflows/ci.yml/badge.svg)](https://github.com/rzwnz/microservicesthree/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/rzwnz/microservicesthree/branch/main/graph/badge.svg?flag=microservicesthree)](https://codecov.io/gh/rzwnz/microservicesthree)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka_KRaft-7.6.0-231F20?logo=apachekafka&logoColor=white)

Spring Boot микросервис файлового хранилища с S3-бэкендом, событийной моделью Kafka, Redis-кешированием, circuit breaker и антивирусной проверкой.

---

## Оглавление

1. [Архитектура](#1-архитектура)
2. [Технологический стек](#2-технологический-стек)
3. [Структура проекта](#3-структура-проекта)
4. [Быстрый запуск](#4-быстрый-запуск)
5. [REST API](#5-rest-api)
6. [Модель аутентификации](#6-модель-аутентификации)
7. [Хранилище и S3](#7-хранилище-и-s3)
8. [Событийная модель (Kafka)](#8-событийная-модель-kafka)
9. [Отказоустойчивость (Resilience4j)](#9-отказоустойчивость-resilience4j)
10. [Антивирусная проверка (ClamAV)](#10-антивирусная-проверка-clamav)
11. [Фоновые задачи](#11-фоновые-задачи)
12. [Конфигурация](#12-конфигурация)
13. [Тестирование](#13-тестирование)
14. [Kubernetes и Helm](#14-kubernetes-и-helm)
15. [CI/CD](#15-cicd)
16. [Зависимости](#16-зависимости)

---

## 1. Архитектура

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Docker Compose стек                         │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  file-service :8087  (Spring Boot 3.2.1)                       │  │
│  │                                                                │  │
│  │  FileController ─► FileService ─► StorageService (S3)          │  │
│  │       │                │              │                        │  │
│  │       │           FileShareService    ├─ @CircuitBreaker       │  │
│  │       │                │              ├─ @Retry                │  │
│  │       │           FileCleanupService  └─ GarageHealthIndicator │  │
│  │       │                                                        │  │
│  │       ▼                                                        │  │
│  │  FileEventPublisher ──► Kafka (DLT на ошибки)                  │  │
│  │  VirusScanService ──► ClamAV (опционально)                     │  │
│  └────────────┬──────────────┬──────────────┬─────────────────────┘  │
│               │              │              │                        │
│  ┌────────────▼───┐  ┌───────▼──────┐  ┌───▼──────────┐              │
│  │  PostgreSQL 16 │  │  Redis 7     │  │  Kafka KRaft │              │
│  │  :5432         │  │  :6379       │  │  :9092       │              │
│  │  (5 таблиц,    │  │  (кеш мета-  │  │  (3 топика + │              │
│  │   Flyway)      │  │   данных)    │  │   DLT)       │              │
│  └────────────────┘  └──────────────┘  └──────────────┘              │
│                                                                      │
│  ┌────────────────┐                                                  │
│  │  Garage (S3)   │                                                  │
│  │  :3900 (API)   │                                                  │
│  │  :3902 (Admin) │                                                  │
│  └────────────────┘                                                  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 2. Технологический стек

| Компонент | Технология | Версия |
|-----------|------------|--------|
| Язык | Java | 17 |
| Фреймворк | Spring Boot | 3.2.1 |
| БД | PostgreSQL | 16-alpine |
| ORM | JOOQ | 3.19.1 |
| Миграции | Flyway | (BOM) |
| Кеш | Redis | 7-alpine |
| Очереди | Apache Kafka (KRaft) | 7.6.0 (Confluent) |
| Объектное хранилище | Garage (S3-совместимое) | v0.9.4 |
| Отказоустойчивость | Resilience4j | 2.2.0 |
| S3 SDK | AWS SDK v2 | 2.24.0 |
| Документация API | springdoc-openapi + Scalar UI | 2.6.0 |
| Покрытие кода | JaCoCo | 0.8.10 (порог ≥ 80%) |
| Сканирование CVE | OWASP dependency-check | 9.0.9 |
| Логирование | Logstash Logback Encoder | 7.4 |

---

## 3. Структура проекта

```
microservicesthree/
├── docker-compose.yml                # Полный стек (5 сервисов)
├── Dockerfile                        # Multi-stage сборка
├── pom.xml                           # Maven (Spring Boot parent 3.2.1)
├── ansible/templates/                # Docker Compose Jinja2-шаблон
├── k8s/
│   ├── file-service.yaml             # Deployment + Service
│   ├── file-service-config.yaml      # ConfigMap / Secret
│   ├── file-service-ingress.yaml     # Ingress (prod)
│   ├── dev-service-ingress.yaml      # Ingress (dev)
│   └── garage-helm/                  # Helm-чарт для Garage S3
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/                # StatefulSet, Service, ConfigMap, Secret,
│                                     # Ingress, init-buckets Job, helpers
└── src/
    ├── main/java/com/sthree/file/
    │   ├── FileServiceApplication.java
    │   ├── aspect/                   # LoggingAspect (@Around)
    │   ├── config/                   # AuthenticationFilter, GarageConfig,
    │   │                             # RedisConfig, OpenApiConfig, *Properties (5)
    │   ├── controller/               # FileController (13 эндпоинтов)
    │   ├── dto/                      # ApiResponse, File*Request/Response (8 DTO)
    │   ├── entity/                   # FileEntity, FileMetadata, FileThumbnail,
    │   │                             # FileShare, FileAccess
    │   ├── event/                    # FileEventPublisher (Kafka + DLT)
    │   ├── exception/                # 7 типов + GlobalExceptionHandler
    │   ├── health/                   # GarageHealthIndicator (Actuator)
    │   ├── repository/               # 5 JOOQ-репозиториев
    │   ├── service/                  # FileService, StorageService, FileShareService,
    │   │                             # FileCleanupService, VirusScanService
    │   └── util/                     # FileUtils, StorageKeyBuilder
    ├── main/resources/
    │   ├── application.yml           # 201 строка конфигурации
    │   └── db/migration/
    │       └── V1__initial_schema.sql
    └── test/java/com/sthree/file/    # 12 тестовых классов, 229 тестов
```

**53 класса** в main, **12 тестовых классов**.

---

## 4. Быстрый запуск

### Docker Compose (полный стек)

```bash
# Задать обязательные переменные
export SPRING_DATASOURCE_PASSWORD=secret
export POSTGRES_PASSWORD=secret
export GARAGE_ACCESS_KEY=... GARAGE_SECRET_KEY=...

docker compose up -d --build
```

Проверка:
```bash
curl http://localhost:8087/actuator/health
# {"status":"UP","components":{"db":...,"garage":...,"redis":...,"kafka":...}}

# API-документация
open http://localhost:8087/scalar.html
```

### Только приложение (без Docker)

```bash
mvn clean package -DskipTests
java -jar target/file-service-1.0.0-SNAPSHOT.jar
```

### Запуск тестов

```bash
mvn test
# 229 тестов, 0 failures
```

---

## 5. REST API

Базовый путь: `/api/files`
Документация: Scalar UI (`/scalar.html`), OpenAPI JSON (`/v3/api-docs`)

| Метод | Эндпоинт | Описание |
|-------|----------|----------|
| `POST` | `/upload` | Multipart-загрузка файла |
| `GET` | `/{fileId}` | Метаданные + presigned URL для скачивания |
| `GET` | `/{fileId}/download` | Потоковая выдача файла |
| `DELETE` | `/{fileId}` | Мягкое удаление файла |
| `GET` | `/{fileId}/metadata` | Пользовательские метаданные |
| `POST` | `/{fileId}/share` | Создать share-ссылку (токен, пароль, лимит, TTL) |
| `GET` | `/share/{shareToken}` | Информация о расшаренном файле |
| `GET` | `/share/{shareToken}/download` | Скачивание через share-ссылку |
| `DELETE` | `/share/{shareToken}` | Отозвать share-ссылку |
| `POST` | `/presigned-upload` | Получить presigned PUT URL |
| `POST` | `/confirm-upload` | Подтвердить загрузку через presigned URL |
| `GET` | `/user/{userId}` | Список файлов пользователя |
| `GET` | `/user/{userId}/storage` | Использование хранилища + квота |

### Actuator-эндпоинты

| Путь | Описание |
|------|----------|
| `/actuator/health` | Health check (DB, Redis, Kafka, Garage) |
| `/actuator/info` | Информация о приложении |
| `/actuator/metrics` | Spring Boot метрики |
| `/actuator/prometheus` | Prometheus-формат метрик |

---

## 6. Модель аутентификации

`AuthenticationFilter` проверяет запросы:

1. Заголовок `X-User-Id` (UUID) от upstream-шлюза — основной механизм
2. `Authorization: Bearer <token>` — в текущей реализации валидируется как UUID (dev-режим)

При отсутствии аутентификации на защищённых эндпоинтах — **HTTP 401** (JSON).

> **Примечание:** Для продакшена необходимо интегрировать валидацию токенов через JWT / auth-gateway.

---

## 7. Хранилище и S3

- Единый сконфигурированный бакет (`GARAGE_DATA_BUCKET`, по умолчанию `file-service-dev-data`)
- Объекты хранятся с генерируемыми ключами (domain-agnostic стратегия через `StorageKeyBuilder`)
- Метаданные в PostgreSQL, связаны по UUID файла

При загрузке:
- Валидация типа и размера файла (`FILE_MAX_SIZE_MB` = 50 МБ)
- Сохранение контрольной суммы (SHA-256)
- Генерация миниатюр (для изображений: 200×200 @ 80q)
- Антивирусная проверка (если ClamAV включён)
- Публикация события `file.uploaded` в Kafka

Presigned URL:
- Upload: 30 мин. срок жизни
- Download: 60 мин. срок жизни

---

## 8. Событийная модель (Kafka)

3 топика + Dead Letter Topic (DLT):

| Топик | Событие |
|-------|---------|
| `file-uploaded` | Файл загружен |
| `file-deleted` | Файл удалён |
| `file-shared` | Файл расшарен |
| `*.DLT` | Сообщения, не обработанные после 3 попыток |

Конфигурация:
- Consumer group: `file-service-consumer`
- Auto-offset-reset: `earliest`
- Producer acks: `all`, retries: `3`

---

## 9. Отказоустойчивость (Resilience4j)

### Circuit Breaker (`garageStorage`)

Применён к `StorageService` (12 методов — upload, download, delete, exists, copy, presigned URL и т.д.):

| Параметр | Значение |
|----------|----------|
| Тип окна | count-based |
| Размер окна | 10 вызовов |
| Мин. вызовов для оценки | 5 |
| Порог отказа | 50% |
| Wait в open-состоянии | 30 секунд |

### Retry (`garageStorage`)

| Параметр | Значение |
|----------|----------|
| Макс. попыток | 3 |
| Начальная задержка | 500 мс |
| Множитель экспоненциального backoff | ×2 |

### Health Indicator

`GarageHealthIndicator` проверяет доступность S3 через `headBucket` — результат виден в `/actuator/health`.

---

## 10. Антивирусная проверка (ClamAV)

`VirusScanService` — опциональная интеграция с ClamAV:

- Отключена по умолчанию (`virus-scan.enabled=false`)
- При включении: проверяет файлы через ClamAV daemon (TCP)
- Заражённые файлы блокируются до сохранения в S3

Конфигурация: `CLAMAV_HOST`, `CLAMAV_PORT`

---

## 11. Фоновые задачи

`FileCleanupService` (`@EnableScheduling`):

| Задача | Расписание по умолчанию | Описание |
|--------|------------------------|----------|
| Очистка просроченных share-ссылок | Каждый час | Удаляет expired `FileShare` |
| Очистка просроченных доступов | Каждые 30 мин. | Удаляет expired `FileAccess` |
| Жёсткое удаление файлов | Раз в день (3:00) | Удаляет soft-deleted файлы после периода хранения (30 дней) |

Расписания настраиваются через `CLEANUP_*_CRON` переменные.

---

## 12. Конфигурация

Все параметры через `application.yml`, переопределяются переменными окружения.

### Основные переменные

| Переменная | По умолчанию | Описание |
|------------|-------------|----------|
| `SERVER_PORT` | `8087` | Порт приложения |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/file_db` | PostgreSQL URL |
| `SPRING_DATASOURCE_USERNAME` | `file_user` | Пользователь БД |
| `SPRING_DATASOURCE_PASSWORD` | — | Пароль БД |
| `SPRING_REDIS_HOST` | `localhost` | Redis хост |
| `SPRING_REDIS_PORT` | `6379` | Redis порт |
| `SPRING_REDIS_DATABASE` | `7` | Redis database |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap |
| `GARAGE_ENDPOINT` | — | S3-совместимый endpoint |
| `GARAGE_ACCESS_KEY` | — | S3 access key |
| `GARAGE_SECRET_KEY` | — | S3 secret key |
| `GARAGE_REGION` | `garage` | S3 region |
| `GARAGE_DATA_BUCKET` | `file-service-dev-data` | Бакет для данных |
| `FILE_MAX_SIZE_MB` | `50` | Макс. размер файла (МБ) |
| `FILE_MAX_SIZE_AVATAR_MB` | `5` | Макс. размер аватара (МБ) |
| `STORAGE_QUOTA_DEFAULT_BYTES` | `1073741824` (1 ГБ) | Квота хранилища по умолчанию |
| `PRESIGNED_URL_UPLOAD_EXPIRATION_MINUTES` | `30` | TTL presigned upload URL |
| `PRESIGNED_URL_DOWNLOAD_EXPIRATION_MINUTES` | `60` | TTL presigned download URL |

### HikariCP пул соединений

| Параметр | Значение |
|----------|----------|
| `maximum-pool-size` | 10 |
| `minimum-idle` | 5 |
| `connection-timeout` | 30 000 мс |

---

## 13. Тестирование

```bash
mvn test
# 229 тестов, 0 failures
```

**12 тестовых классов:**

| Класс | Что проверяет |
|-------|---------------|
| `FileControllerTest` | REST API, multipart upload, download, CRUD, share, presigned, валидация |
| `AuthenticationFilterTest` | X-User-Id, Bearer token, 401 на защищённых путях |
| `FileServiceTest` | Бизнес-логика: загрузка, удаление, метаданные, квоты |
| `FileShareServiceTest` | Создание/отзыв share-ссылок, пароль, лимиты |
| `StorageServiceTest` | S3 операции: upload/download/delete, circuit breaker, retry |
| `FileCleanupServiceTest` | Scheduled-задачи очистки |
| `StorageKeyBuilderTest` | Генерация S3-ключей |
| `FileUtilsTest` | Валидация файлов, MIME-типы, размер |
| `DtoTest` | Все DTO: значения по умолчанию, builders, валидация |
| `EntityTest` | JPA-сущности: поля, equals/hashCode |
| `ExceptionTest` | Кастомные исключения: сообщения, HTTP-коды |
| `GlobalExceptionHandlerTest` | Маппинг исключений в HTTP-ответы |

### Покрытие

JaCoCo ≥ **80%** instruction coverage (enforced в `verify` фазе Maven). Отчёт: `target/site/jacoco/index.html`.

---

## 14. Kubernetes и Helm

### Манифесты приложения (`k8s/`)

| Файл | Ресурс |
|------|--------|
| `file-service.yaml` | Deployment + Service |
| `file-service-config.yaml` | ConfigMap + Secret |
| `file-service-ingress.yaml` | Ingress (prod) |
| `dev-service-ingress.yaml` | Ingress (dev) |

### Helm-чарт Garage (`k8s/garage-helm/`)

| Шаблон | Ресурс |
|--------|--------|
| `statefulset.yaml` | StatefulSet Garage |
| `service.yaml` | Service |
| `configmap.yaml` | ConfigMap |
| `secret.yaml` | Secret |
| `ingress.yaml` | Ingress |
| `init-buckets-job.yaml` | Job инициализации бакетов |

```bash
helm install garage k8s/garage-helm/ -f k8s/garage-helm/values.yaml
kubectl apply -f k8s/
```

---

## 15. CI/CD

GitHub Actions:
- **Триггер:** push / PR в `main`
- **JDK:** Temurin 17, Maven cache
- **Шаги:** `mvn clean verify` → JaCoCo (≥80%) → OWASP dependency-check (CVSS < 7) → upload артефактов

OWASP dependency-check: plugin 9.0.9, отчёты HTML + JSON, порог CVSS ≥ 7 — fail build.

---

## 16. Зависимости

| Зависимость | Версия | Назначение |
|-------------|--------|------------|
| spring-boot-starter-web | (BOM) | REST API |
| spring-boot-starter-validation | (BOM) | Jakarta Bean Validation |
| spring-boot-starter-actuator | (BOM) | Health, metrics, Prometheus |
| spring-boot-starter-data-redis | (BOM) | Redis-кеширование |
| spring-boot-starter-aop | (BOM) | AOP для логирования и Resilience4j |
| spring-boot-starter-jdbc | (BOM) | JDBC + HikariCP |
| spring-security-crypto | (BOM) | BCrypt для паролей share-ссылок |
| spring-kafka | (BOM) | Kafka producer/consumer |
| resilience4j-spring-boot3 | 2.2.0 | Circuit breaker + Retry |
| jooq / jooq-meta | 3.19.1 | Type-safe SQL |
| postgresql | (BOM) | JDBC-драйвер |
| flyway-core | (BOM) | Миграции БД |
| aws-java-sdk-s3 | 2.24.0 | S3-клиент (Garage) |
| thumbnailator | 0.4.20 | Генерация миниатюр |
| springdoc-openapi | 2.6.0 | OpenAPI 3.1 + Scalar UI |
| logstash-logback-encoder | 7.4 | JSON-логирование |
| lombok | 1.18.30 | Codegen |
| spring-boot-starter-test | (BOM) | JUnit 5, Mockito |
| spring-kafka-test | (BOM) | Embedded Kafka |
| testcontainers | 1.19.3 | PostgreSQL в Docker для тестов |
