# MediaCmsLab

CMS для редакции мультиформатного медиапортала. Редактор публикует статьи, видео и подкасты, читатель открывает материалы и видит, что сегодня читают чаще всего.

Автор — Vilayat Badalov. [Репозиторий на GitHub](https://github.com/vilayatbadalov15-alt/MediaCmsLab)

## Запуск

Из корня проекта выполните команды в Bash. Нужны Docker Compose и OpenSSL. При первом запуске создаётся локальный файл с ключом JWT и паролем редактора.

```bash
test -f .env || (umask 077 && printf 'JWT_SECRET=%s\nDEMO_EDITOR_PASSWORD=%s\n' "$(openssl rand -hex 32)" "$(openssl rand -hex 12)" > .env)
docker compose up --build -d
```

Откройте [медиапортал](http://localhost:8080). Для публикации войдите как `editor@mediacms.local` с паролем `DEMO_EDITOR_PASSWORD` из `.env`. Кнопка «Регистрация читателя» создаёт аккаунт с ролью `USER`. Для остановки выполните `docker compose down`. Данные сохраняются в томах.

## Как устроен проект

| Компонент | Что можно показать |
| --- | --- |
| Java 21, Spring Boot, PostgreSQL | Статья хранит текст, автора и дату публикации, видео — URL и длительность в секундах, подкаст — URL аудио и упорядоченные эпизоды. Spring Security проверяет JWT. Редактор управляет своими материалами, администратор — всеми. Гостям видны опубликованные материалы. |
| Redis | При открытии публикации `INCR` увеличивает общий счётчик, `ZINCRBY` обновляет дневной рейтинг. Топ-10 охватывает все форматы за календарный день по UTC. HyperLogLog оценивает число авторизованных читателей за день. Дневные ключи живут 48 часов после последнего просмотра. |
| Spring Cache | `GET /api/content/{id}` кеширует карточку на 5 минут. После редактирования или удаления запись кеша сбрасывается. |
| Actuator и Prometheus | Доступны [health](http://localhost:8080/actuator/health), [info](http://localhost:8080/actuator/info) и [метрики](http://localhost:8080/actuator/prometheus). Счётчик `messages_created_total` увеличивается после фиксации новой записи контента. Логи приложения в JSON выводит `docker compose logs --no-log-prefix app`. |

Контроллеры принимают HTTP-запросы, сервисы проверяют права владельца и управляют транзакциями, репозитории сохраняют материалы. Поля запросов и операции доступны в [Swagger UI](http://localhost:8080/swagger-ui/index.html) и [OpenAPI JSON](http://localhost:8080/v3/api-docs). Для защищённых операций получите `token` через `POST /api/auth/login` и вставьте его в **Authorize**.

## Демонстрация за 4 минуты

Заранее поднимите сервисы, создайте видео и подкаст, откройте вкладки портала, Swagger и Prometheus.

| Время | Действие |
| --- | --- |
| 0:00–0:40 | Покажите ленту и объясните задачу редакции. PostgreSQL хранит публикации и связи, Redis обслуживает частые чтения и считает популярность. |
| 0:40–1:50 | Войдите редактором, создайте статью и измените её заголовок. В карточках видео и подкаста покажите длительность и порядок эпизодов. |
| 1:50–3:05 | Откройте статью дважды. Покажите рост счётчика и позицию в топе. В терминале выведите рейтинг с баллами и оставшееся время жизни карточки. |
| 3:05–4:00 | Покажите `messages_created_total` и Swagger. Переведите статью в черновик, выйдите и покажите публичную ленту. Оставшиеся 4 минуты — вопросы. |

Вместо `ID` подставьте идентификатор статьи из первого вызова. Рейтинг меняется при открытии материала, чтение ленты просмотры не начисляет.

```bash
docker compose exec -T redis redis-cli ZREVRANGE "analytics:top:views:$(date -u +%F)" 0 9 WITHSCORES
docker compose exec -T redis redis-cli TTL content::ID
docker compose exec -T redis redis-cli GET analytics:views:content:ID
```

## Сборка и проверка

`./gradlew clean build` требует JDK 21 и запущенный Docker. Тесты поднимают отдельные PostgreSQL и Redis через Testcontainers. Отчёт по тестам — `build/reports/tests/test/index.html`, покрытие строк — `build/reports/jacoco/test/html/index.html`. Сборка требует минимум 81% покрытия. Исполняемый JAR — `build/libs/mediacms.jar`.

При pull request в `main` и push в `main` GitHub Actions выводит версию Java и результаты тестов, проверяет покрытие и прикрепляет JAR к успешному запуску. [История сборок](https://github.com/vilayatbadalov15-alt/MediaCmsLab/actions/workflows/ci-build.yml)
