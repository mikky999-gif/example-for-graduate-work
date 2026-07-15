# Как запустить тесты

## Проблема с компиляцией через командную строку

Из-за кириллицы в путях и особенностей Windows, Maven wrapper не работает корректно через командную строку.

## Решение: Использовать IDE (IntelliJ IDEA или Eclipse)

### Шаг 1: Открыть проект в IntelliJ IDEA
1. Запустите IntelliJ IDEA
2. Выберите `File` → `Open`
3. Перейдите в: `C:\Users\mikky\Downloads\telegram-bot (1) — копия\example-for-graduate-work`
4. Выберите папку и нажмите `OK`

### Шаг 2: Импортировать Maven проект
1. IDEA автоматически обнаружит pom.xml
2. Нажмите `Import Changes` или `Enable Auto-Import`
3. Подождите пока все зависимости загрузятся

### Шаг 3: Запустить тесты
1. Перейдите в `src/test/java/ru/skypro/homework`
2. Правый клик на любой тест-класе (например, `AuthControllerTest`)
3. Выберите `Run 'AuthControllerTest'`

### Альтернативно: Через Maven
1. Откройте панель Maven справа (или View → Tool Windows → Maven)
2. Раскройте `Lifecycle`
3. Дважды кликните на `test`

## Исправления, которые были сделаны

### 1. Добавлены сеттеры в Login.java
```java
public void setUsername(String username) {
    this.username = username;
}

public void setPassword(String password) {
    this.password = password;
}
```

### 2. Удалены text blocks из IntegrationTest.java
Заменены на обычные строки с конкатенацией для совместимости с Java 13.

## Ожидаемый результат

После запуска тестов должно появиться:
- ✅ All tests passed (все тесты пройдены)
- or
- ❌ Failures: 0, Errors: 0

## Если тесты не проходят

1. Проверьте, что в IDEA используется Java 17 (File → Project Structure → SDK)
2. Выполните `mvn clean test` через терминал внутри IDEA
3. Убедитесь, что все зависимости загружены (проверьте папку ~/.m2/repository)
