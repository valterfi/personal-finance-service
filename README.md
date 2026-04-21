# Personal Finance Service

Small Spring Boot service with an email inbox listener.

## What It Does

When `mail.listener.enabled=true`, the application connects to the configured IMAP inbox, finds unread emails that match the configured subject text, prints the subject and body with `System.out.println`, and optionally marks the messages as read.

The listener currently runs on a cron schedule configured in `application.yml`.

## Main Files

- `src/main/java/com/valterfi/finance/PersonalFinanceServiceApplication.java`
  Spring Boot entry point. Scheduling is enabled here.
- `src/main/java/com/valterfi/finance/mail/EmailListenerProperties.java`
  Maps the `mail.listener.*` configuration from YAML into a typed Java class.
- `src/main/java/com/valterfi/finance/mail/EmailInboxListener.java`
  Connects to the inbox, fetches unread messages filtered by subject text, prints them, and marks them as read when configured.
- `src/main/resources/application.yml`
  Application configuration, including IMAP host, credentials, protocol, cron expression, and SSL flags.

## How The Listener Works

1. Spring starts the application.
2. If `mail.listener.enabled` is `true`, the scheduled method runs using the configured cron expression.
3. The listener opens the configured mail store and folder.
4. It searches for unread messages and filters them by the configured subject text when provided.
5. For each unread message, it prints:

```text
Unread email: subject=<subject>, body=<body>
```

6. If `mail.listener.mark-as-read=true`, the message is marked as read after processing.

## Configuration

Example:

```yaml
mail:
  listener:
    enabled: true
    host: "localhost"
    port: 143
    username: "any"
    password: "any"
    protocol: imap
    folder: INBOX
    subject-text: "test"
    cron-expression: "0 */1 * * * *"
    ssl-enable: false
    starttls-enable: false
    mark-as-read: true
```

Notes:

- Use `imap` for non-SSL connections.
- Use `imaps` when you want implicit SSL.
- `subject-text` is used to filter unread emails by subject.
- `cron-expression: "0 */1 * * * *"` means once per minute.
- The current body extraction is intentionally simple and uses `message.getContent()`.

## Local Testing With smtp4dev

You can run a local mail server and web UI with `smtp4dev`:

```bash
docker run --name smtp4dev -p 3000:80 -p 2525:25 -p 143:143 rnwood/smtp4dev
```

Ports:

- `3000`: smtp4dev web UI
- `2525`: SMTP port for sending test emails
- `143`: IMAP port used by this application

With the current `application.yml`, the listener is already configured to read from local IMAP on port `143`.

To test locally:

1. Start `smtp4dev` with the Docker command above.
2. Start this Spring Boot application.
3. Send a test email to smtp4dev on SMTP port `2525`.
4. Open `http://localhost:3000` to inspect the inbox.
5. Wait for the cron schedule to run and check the application logs for the printed subject and body.

## Run

```bash
./mvnw spring-boot:run
```
