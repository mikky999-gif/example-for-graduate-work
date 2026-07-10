# Обзор приложения для управления объявлениями

## 1. Обзор кода приложения

### Архитектура и модель данных

Приложение построено по классической многослойной архитектуре Spring Boot с разделением ответственности на слои:

- **Controller Layer** — контроллеры обрабатывают HTTP-запросы
- **Service Layer** — бизнес-логика приложения
- **Repository Layer** — доступ к данным через JPA
- **Entity Layer** — сущности базы данных
- **DTO/Mapper Layer** — преобразование данных между слоями

### Модель данных

#### Таблица Users (UserEntity)
Сущность пользователя содержит базовую информацию:
- `id` — уникальный идентификатор
- `username` — логин (уникальный)
- `password` — хэш пароля (BCrypt)
- `firstName`, `lastName` — имя и фамилия
- `phone` — номер телефона
- `role` — роль пользователя (USER/ADMIN)
- `imageUrl` — ссылка на аватар

```java
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    // ... другие поля
}
```

#### Таблица Ads (AdEntity)
Объявления связаны с пользователями через внешний ключ `author_id`:

```java
@Entity
@Table(name = "ads")
@Data
@NoArgsConstructor
public class AdEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private Integer price;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = true, name = "image_url")
    private String imageUrl;
}
```

#### Таблица Comments (CommentEntity)
Комментарии связаны с объявлением и автором:

```java
@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
public class CommentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private AdEntity ad;
    
    @Column(nullable = false)
    private String text;
    
    @Column(nullable = false)
    private Long createdAt;
}
```

### Основные функции

#### Управление объявлениями (AdController)
Класс `AdController` предоставляет следующие эндпоинты:

- `GET /ads` — получение пагинированного списка всех объявлений
- `GET /ads/{id}` — получение подробной информации об объявлении
- `POST /ads` — создание объявления (требуется аутентификация)
- `PATCH /ads/{id}` — редактирование объявления (только владелец или админ)
- `PATCH /ads/{id}/image` — обновление изображения
- `DELETE /ads/{id}` — удаление объявления (только владелец или админ)

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/ads")
public class AdController {
    private final AdService adService;
    
    @GetMapping
    public ResponseEntity<Ads> getAllAds(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(adService.getAllAds(offset, limit));
    }
    
    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    public Ad createAd(
            @RequestPart(value = "properties", required = true) @Valid CreateOrUpdateAd data,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication auth) {
        return adService.createAd(data, image, auth);
    }
}
```

#### Управление комментариями (CommentController)
- `GET /ads/{id}/comments` — получение комментариев к объявлению
- `POST /ads/{id}/comments` — добавление комментария
- `PATCH /comments/{id}` — редактирование комментария
- `DELETE /comments/{id}` — удаление комментария

#### Аутентификация и авторизация (AuthController)
- `POST /login` — вход в систему
- `POST /register` — регистрация нового пользователя
- `PATCH /users/set_password` — изменение пароля

### Слой преобразования данных (Mappers)

Используется MapStruct для автоматического преобразования сущностей в DTO и обратно.

#### AdMapper
Преобразует `AdEntity` в `Ad` и `ExtendedAd`:

```java
@Mapper(componentModel = "spring")
public interface AdMapper {
    @Mappings({
        @Mapping(source = "id", target = "pk"),
        @Mapping(source = "title", target = "title"),
        @Mapping(source = "price", target = "price"),
        @Mapping(source = "imageUrl", target = "image"),
        @Mapping(source = "author.id", target = "author")
    })
    Ad entityToDto(AdEntity entity);
    
    @Mappings({
        @Mapping(source = "id", target = "pk"),
        @Mapping(source = "author.firstName", target = "authorFirstName"),
        @Mapping(source = "author.lastName", target = "authorLastName"),
        @Mapping(source = "author.username", target = "email"),
        @Mapping(source = "author.phone", target = "phone")
    })
    ExtendedAd entityToExtendedDto(AdEntity entity);
}
```

#### CommentMapper
Преобразует `CommentEntity` в `Comment`:

```java
@Mapper(componentModel = "spring")
public interface CommentMapper {
    @Mappings({
        @Mapping(source = "id", target = "pk"),
        @Mapping(source = "text", target = "text"),
        @Mapping(source = "author.id", target = "author"),
        @Mapping(source = "author.imageUrl", target = "authorImage"),
        @Mapping(source = "author.firstName", target = "authorFirstName")
    })
    Comment entityToDto(CommentEntity entity);
}
```

### Обработка ошибок

Класс `GlobalErrorControllerAdvice` обрабатывает исключения глобально:

```java
@RestControllerAdvice
public class GlobalErrorControllerAdvice {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<IdDto> handleResourceNotFoundException(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
    
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<IdDto> handleApplicationException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
```

## 2. Описание используемых технологий

### База данных и миграции

**Liquibase** — система управления миграциями базы данных.

Файл конфигурации: `src/main/resources/db/changelog/db.changelog-master.yaml`

Пример миграции (создание таблицы ads):

```yaml
databaseChangeLog:
  - changeSet:
      id: create-table-ads
      author: developer
      changes:
        - createTable:
            tableName: ads
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: title
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
              - column:
                  name: author_id
                  type: BIGINT
                  constraints:
                    nullable: false
        - addForeignKeyConstraint:
            baseColumnNames: author_id
            baseTableName: ads
            constraintName: fk_ads_author
            referencedColumnNames: id
            referencedTableName: users
```

### ORM и работа с данными

**Spring Data JPA + Hibernate** — для работы с базой данных.

Репозитории используют методы построения запросов по именам:

```java
public interface AdRepository extends JpaRepository<AdEntity, Long> {
    List<AdEntity> findByAuthorId(Long authorId);
    List<AdEntity> findByAuthorUsername(String username);
}
```

### Безопасность

**Spring Security** — для аутентификации и авторизации.

Конфигурация в `WebSecurityConfig`:
- Базовая аутентификация (Basic Auth)
- Пароли хэшируются через BCrypt
- Маппинг пользователей реализован в `CustomUserDetailsService`

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf().disable()
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/login").permitAll()
            .requestMatchers("/register").permitAll()
            .requestMatchers("/ads").permitAll()
            .anyRequest().permitAll())
        .httpBasic(Customizer.withDefaults());
    return http.build();
}

@Bean("passwordEncoder")
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### REST API и документация

**SpringDoc OpenAPI** — автоматическая генерация Swagger документации.

Доступна по адресу: `http://localhost:8080/swagger-ui.html`

Описание API с аннотациями:

```java
@Operation(summary = "Получение всех объявлений")
@GetMapping
public ResponseEntity<Ads> getAllAds(
    @RequestParam(defaultValue = "0") int offset,
    @RequestParam(defaultValue = "10") int limit) {
    // ...
}
```

### Тестирование

**JUnit 5 + Spring Test + Mockito** — для модульного тестирования.

Пример теста контроллера:

```java
class AdControllerTest {
    @Mock
    private AdService adService;
    
    @InjectMocks
    private AdController adController;
    
    private MockMvc mockMvc;
    
    @Test
    void getAllAds_shouldReturnAdsList() throws Exception {
        // Given
        Ad ad1 = createTestAd(1, "Велосипед", 10000, "image1.jpg", 1);
        Ads ads = new Ads();
        ads.setCount(1);
        ads.setResults(List.of(ad1));
        
        given(adService.getAllAds(0, 10)).willReturn(ads);
        
        // When & Then
        mockMvc.perform(get("/ads")
                .param("offset", "0")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1));
    }
}
```

### Сборка проекта

**Maven** — для управления зависимостями и сборки.

Ключевые зависимости в `pom.xml`:
- `spring-boot-starter-web` — для REST API
- `spring-boot-starter-data-jpa` — для JPA
- `spring-boot-starter-security` — для безопасности
- `spring-boot-starter-test` — для тестирования
- `mapstruct` — для преобразования объектов
- `liquibase-core` — для миграций
- `h2` — для тестов (in-memory БД)

### Конфигурация

Файл `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/example-for-graduate-work
spring.datasource.username=postgres
spring.datasource.password=skypro

spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
spring.liquibase.enabled=true

app.images.root-path=./uploads/
app.images.url-prefix=/images/
```

## 3. Обзор тестовых сценариев

### Тесты AdController (AdControllerTest.java)

1. **getAllAds** — проверка получения всех объявлений с пагинацией
   - Возвращает список объявлений с правильным количеством
   - Возвращает пустой список, если объявлений нет

2. **getMyAds** — получение объявлений текущего пользователя
   - Возвращает только объявления авторизованного пользователя
   - Требует аутентификацию

3. **getAdById** — получение объявления по ID
   - Возвращает подробную информацию при существовании
   - Возвращает 404 при отсутствии

4. **createAd** — создание объявления
   - Создает объявление с изображением и данными
   - Привязывает объявление к автору

5. **updateImage** — обновление изображения
   - Заменяет изображение объявления
   - Проверяет успешное обновление

6. **updateAd** — редактирование объявления
   - Обновляет данные объявления
   - Возвращает 404 при отсутствии

7. **deleteAd** — удаление объявления
   - Удаляет объявление
   - Возвращает 204 No Content
   - Возвращает 500 при ошибке

### Тесты CommentController (CommentControllerTest.java)

1. **getAllComments** — получение всех комментариев к объявлению
2. **addComment** — добавление комментария к объявлению
3. **updateComment** — редактирование комментария
4. **deleteComment** — удаление комментария

### Тесты AuthController (AuthControllerTest.java)

1. **login** — вход в систему
2. **register** — регистрация нового пользователя
3. **changePassword** — изменение пароля

### Тесты UserController (UserControllerTest.java)

1. **getCurrentUser** — получение данных текущего пользователя
2. **updateUser** — обновление профиля
3. **changePassword** — изменение пароля
4. **uploadAvatar** — загрузка аватара

### Тесты сервисов

Сервисы тестируются через контроллеры с мокированием зависимостей, что обеспечивает:
- Изоляцию тестов от внешних зависимостей
- Быстрое выполнение тестов
- Возможность проверки различных сценариев (успех, ошибка, пустые данные)

### Инструменты тестирования

- **MockMvc** — для тестирования HTTP-запросов
- **Mockito** — для мокирования сервисов
- **MockMultipartFile** — для тестирования загрузки файлов
- **SecurityMockMvcRequestPostProcessors** — для установки аутентификации

```java
mockMvc.perform(multipart("/ads")
    .file(properties)
    .file(image)
    .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
    .andExpect(status().isOk());
```

---

**Примечания:**
- Все тесты находятся в `src/test/java/ru/skypro/homework/`
- Для запуска тестов используется команда: `./mvnw test`
- Покрытие кода тестами охватывает основные сценарии использования приложения
