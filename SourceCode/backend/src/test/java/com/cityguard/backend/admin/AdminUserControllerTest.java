package com.cityguard.backend.admin;

import com.cityguard.backend.controller.AdminUserController;
import com.cityguard.backend.dto.admin.user.AdminUpdateUserStatusRequest;
import com.cityguard.backend.dto.admin.user.AdminUserListItemResponse;
import com.cityguard.backend.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class AdminUserControllerTest {

    private static final String STATUS_PATH = "/api/v1/admin/users/7/status";
    private static final String REQUEST_BODY = """
            {"status":"LOCKED","reason":"Moderation test"}
            """;

    private final AdminUserService service = mock(AdminUserService.class);
    private final org.springframework.test.web.servlet.MockMvc mvc =
            standaloneSetup(new AdminUserController(service)).build();

    @Test
    void patchReturnsUpdatedUser() throws Exception {
        when(service.updateUserStatus(eq(7L), any(AdminUpdateUserStatusRequest.class)))
                .thenReturn(new AdminUserListItemResponse(
                        7L, "demo", null, "Demo", null, "USER", "LOCKED", null));

        mvc.perform(patch(STATUS_PATH).contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    void getCannotChangeUserStatusEvenWithValidBody() throws Exception {
        mvc.perform(get(STATUS_PATH).contentType(MediaType.APPLICATION_JSON).content(REQUEST_BODY))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}
