package com.trackflow.bootstrap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.trackflow.modules.logistics.application.RepublicarEventosLogisticos;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReconstruccionControllerTest {

    @Test
    void reconstruirDiceCuantosEventosSeRepublicaron() throws Exception {
        RepublicarEventosLogisticos republicar = mock(RepublicarEventosLogisticos.class);
        when(republicar.ejecutar()).thenReturn(42);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ReconstruccionController(republicar)).build();

        mvc.perform(post("/api/admin/rebuild-projections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventsRepublished").value(42));
        verify(republicar).ejecutar();
    }

    @Test
    void laRaizRedirigeASwagger() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new RootRedirectController()).build();

        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui.html"));
    }
}
