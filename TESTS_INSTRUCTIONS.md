# Инструкция по запуску тестов

## Созданные тесты

Успешно созданы следующие тесты:

### 1. Unit-тесты контроллеров (4 файла)
- `AdControllerTest.java` - тесты для AdController
- `AuthControllerTest.java` - тесты для AuthController  
- `CommentControllerTest.java` - тесты для CommentController
- `UserControllerTest.java` - тесты для UserController

### 2. Unit-тесты сервисов (4 файла)
- `AdServiceImplTest.java` - тесты для AdServiceImpl
- `AuthServiceImplTest.java` - тесты для AuthServiceImpl
- `CommentServiceImplTest.java` - тесты для CommentServiceImpl
- `UserServiceImplTest.java` - тесты для UserServiceImpl

### 3. Unit-тесты мапперов (1 файл)
- `MapperTest.java` - тесты для AdMapper, CommentMapper, UserMapper

### 4. Integration-тесты (1 файл)
- `IntegrationTest.java` - интеграционные тесты всех эндпоинтов с H2 базой

## Запуск тестов

### Способ 1: Через Maven (рекомендуется)
```bash
./mvnw test
```

### Способ 2: Через IDE
Откройте проект в IntelliJ IDEA или Eclipse и запустите тесты через контекстное меню:
- Правый клик на файле теста → Run 'TestClass'
- Правый клик на методе → Run 'TestMethod'

### Способ 3: Через командную строку
```bash
# Для Windows
mvnw.cmd test

# Для Linux/Mac
./mvnw test
```

## Покрытие кода тестами

| Компонент | Методы тестов |
|-----------|---------------|
| **AdController** | getAllAds, getMyAds, getAdById, createAd, updateAd, deleteAd |
| **AuthController** | register, login |
| **CommentController** | getComments, createComment, updateComment, deleteComment |
| **UserController** | getCurrentUser, updateUser, changePassword, uploadAvatar |
| **AdServiceImpl** | getAllAds, getAdById, getMyAds, createAd, updateImage, updateAd, deleteAd, isOwner |
| **CommentServiceImpl** | getCommentsForAd, getMyAds, createComment, updateComment, deleteComment |
| **AuthServiceImpl** | register, login |
| **UserServiceImpl** | getCurrentUser, updateProfile, changePassword, uploadAvatar, register |
| **AdMapper** | entityToDto, entityToExtendedDto, dtoToEntity, updateFromDto |
| **CommentMapper** | entityToDto, dtoToEntity, initCreatedAt, updateFromDto |
| **UserMapper** | entityToDto, dtoToEntity, updateFromDto |

## Используемые фреймворки

- **JUnit 5** - основной фреймворк тестирования
- **Mockito** - мокирование зависимостей
- **Spring Boot Test** - интеграционное тестирование
- **H2 Database** - тестовая база данных

## Примечания

- Тесты используем H2 в памяти для интеграционного тестирования
- Все тесты написаны на русском языке для удобства
- Unit-тесты изолированы и не требуют запуска приложения
- Integration-тесты запускают полный Spring контекст
