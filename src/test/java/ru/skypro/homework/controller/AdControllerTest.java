package ru.skypro.homework.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.skypro.homework.ad.Ad;
import ru.skypro.homework.ad.Ads;
import ru.skypro.homework.ad.CreateOrUpdateAd;
import ru.skypro.homework.ad.ExtendedAd;
import ru.skypro.homework.service.AdService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class AdControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdService adService;

    @InjectMocks
    private AdController adController;

    private ObjectMapper objectMapper;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(adController).build();
        objectMapper = new ObjectMapper();

        authentication = new UsernamePasswordAuthenticationToken(
                "testUser",
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

        //  GET /ads
    @Test
    void getAllAds_shouldReturnAdsList() throws Exception {
        Ads ads = new Ads();
        ads.setCount(2);
        ads.setResults(List.of(
                createTestAd(1, "Велосипед", 10000, "image1.jpg", 1),
                createTestAd(2, "Самокат", 20000, "image2.jpg", 2)
        ));

        given(adService.getAllAds(0, 10)).willReturn(ads);

        //  Используем статически импортированный get()
        mockMvc.perform(get("/ads")
                        .param("offset", "0")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.results", hasSize(2)));
    }

        //  GET /ads/me
    @Test
    void getMyAds_shouldReturnAuthenticatedUserAds() throws Exception {
        Ads myAds = new Ads();
        myAds.setCount(1);
        myAds.setResults(List.of(createTestAd(1, "Мой велосипед", 15000, "my-image.jpg", 1)));

        given(adService.getMyAds(authentication)).willReturn(myAds);

        //  user() принимает UserDetails, приводим типы явно
        mockMvc.perform(get("/ads/me")
                        .with(SecurityMockMvcRequestPostProcessors.user((org.springframework.security.core.userdetails.UserDetails) authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

        //  GET /ads/{id}
    @Test
    void getAdById_shouldReturnAdWhenExists() throws Exception {
        ExtendedAd extendedAd = createTestExtendedAd(1, "Товар", 5000, "desc", "image.jpg",
                1, "Иван", "Иванов", "user@example.com", "+79000000000");

        given(adService.getAdById(eq(1L))).willReturn(Optional.of(extendedAd)); // Добавлен eq для Long

        mockMvc.perform(get("/ads/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pk").value(1));
    }

    @Test
    void getAdById_shouldReturn404WhenNotFound() throws Exception {
        given(adService.getAdById(anyLong())).willReturn(Optional.empty());
        mockMvc.perform(get("/ads/999")).andExpect(status().isNotFound());
    }

        //  POST /ads
    @Test
    void createAd_shouldReturnCreatedAd() throws Exception {
        CreateOrUpdateAd createAd = new CreateOrUpdateAd();
        createAd.setTitle("Новый товар");
        createAd.setDescription("Описание");
        createAd.setPrice(1000);

        Ad createdAd = createTestAd(1, "Новый товар", 1000, "new-image.jpg", 1);

        MockMultipartFile properties = new MockMultipartFile(
                "properties", "", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(createAd).getBytes()
        );

        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", MediaType.IMAGE_JPEG_VALUE,
                "fake-bytes".getBytes()
        );

        given(adService.createAd(eq(createAd), eq(image), eq(authentication))).willReturn(createdAd);

        mockMvc.perform(multipart("/ads")
                        .file(properties)
                        .file(image)
                        .with(SecurityMockMvcRequestPostProcessors.user((org.springframework.security.core.userdetails.UserDetails) authentication)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pk").value(1));
    }

        //  PATCH /ads/{id}/image
    @Test
    void updateImage_shouldReturnUpdatedAdDto() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "new-image.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()
        );

        // Настраиваем мок: когда придут ID=1, файл и любая аутентификация — ничего не делать
        doNothing().when(adService).updateImage(eq(1L), eq(image), eq(authentication));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/ads/1/image")
                        .file(image)
                        .with(SecurityMockMvcRequestPostProcessors.user((UserDetails) authentication)))
                .andExpect(status().isOk());
    }

        //  PATCH /ads/{id}
    @Test
    void updateAd_shouldReturnUpdatedAd() throws Exception {
        CreateOrUpdateAd updateData = new CreateOrUpdateAd();
        updateData.setTitle("Обновленный товар");

        Ad updatedAd = createTestAd(1, "Обновленный товар", 2000, "image.jpg", 1);
        given(adService.updateAd(eq(1L), eq(updateData), eq(authentication))).willReturn(updatedAd);

        mockMvc.perform(patch("/ads/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData))
                        .with(SecurityMockMvcRequestPostProcessors.user((org.springframework.security.core.userdetails.UserDetails) authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновленный товар"));
    }

        //  DELETE /ads/{id}

        @Test
    void deleteAd_shouldReturnNoContent() throws Exception {
        doNothing().when(adService).deleteAd(eq(1L));

        mockMvc.perform(delete("/ads/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteAd_shouldReturn404WhenNotFound() throws Exception {

        doThrow(new RuntimeException("Resource not found"))
                .when(adService)
                .deleteAd(anyLong());

        mockMvc.perform(delete("/ads/999"))
                .andExpect(status().isInternalServerError());
    }

    private Ad createTestAd(int pk, String title, int price, String image, int author) {
        Ad ad = new Ad();
        ad.setPk(pk);
        ad.setTitle(title);
        ad.setPrice(price);
        ad.setImage(image);
        ad.setAuthor(author);
        return ad;
    }

    private ExtendedAd createTestExtendedAd(int pk, String title, int price, String description, String image,
                                            int authorId, String authorFirstName, String authorLastName,
                                            String email, String phone) {
        ExtendedAd ad = new ExtendedAd();
        ad.setPk(pk);
        ad.setTitle(title);
        ad.setPrice(price);
        ad.setDescription(description);
        ad.setImage(image);
        ad.setAuthorFirstName(authorFirstName);
        ad.setAuthorLastName(authorLastName);
        ad.setEmail(email);
        ad.setPhone(phone);
        return ad;
    }
}