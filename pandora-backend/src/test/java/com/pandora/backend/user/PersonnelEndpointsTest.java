package com.pandora.backend.user;

import com.pandora.backend.auth.SessionStore;
import com.pandora.backend.common.*;
import org.junit.jupiter.api.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real HTTP binding and response contracts, beyond directly calling service methods. */
class PersonnelEndpointsTest {
    JdbcTemplate db; MockMvc http; SessionStore sessions;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:personnel-http-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new FileSystemResource("src/main/resources/db/h2_schema.sql")).execute(source);
        db = new JdbcTemplate(source); db.update("DELETE FROM users");
        db.update("INSERT INTO users(id,username,password_hash,role,approval_status) VALUES(1,'admin0','unchanged','ADMIN','APPROVED'),(2,'000001','unchanged','FOUNDER','APPROVED'),(3,'000003','unchanged','FOUNDER','APPROVED')");
        sessions = new SessionStore();
        var users = new UserController();
        ReflectionTestUtils.setField(users, "jdbcTemplate", db);
        ReflectionTestUtils.setField(users, "accountCreationService", new AccountCreationService(db, new BCryptPasswordEncoder(4)));
        var people = new PersonnelController(db);
        ReflectionTestUtils.setField(people, "departureService", new AccountDepartureService(db, sessions));
        http = MockMvcBuilders.standaloneSetup(users, people).setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void numberPreviewRunsThroughHttpAndReturnsSmallestGap() throws Exception {
        http.perform(get("/user/next-employee-number")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNo").value("000002"));
        assertEquals("unchanged", db.queryForObject("SELECT password_hash FROM users WHERE id=1", String.class));
    }
    @Test void departureBindsPathIdAndRevokesTheSelectedAccount() throws Exception {
        sessions.put(2L, "old-token");
        http.perform(post("/user/2/departure").principal(SecurityContextHolder.getContext().getAuthentication())).andExpect(status().isOk());
        assertEquals(1, db.queryForObject("SELECT deleted FROM users WHERE id=2", Integer.class));
        assertEquals(0, db.queryForObject("SELECT deleted FROM users WHERE id=3", Integer.class));
        assertNull(sessions.get(2L));
    }
    @Test void employeeCannotAllocateOrDepartAndAdministratorCannotDepart() throws Exception {
        http.perform(post("/user/1/departure").principal(SecurityContextHolder.getContext().getAuthentication())).andExpect(status().isForbidden());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(2L, null, List.of()));
        http.perform(get("/user/next-employee-number")).andExpect(status().isForbidden());
        http.perform(post("/user/3/departure").principal(SecurityContextHolder.getContext().getAuthentication())).andExpect(status().isForbidden());
        assertEquals(0, db.queryForObject("SELECT deleted FROM users WHERE id=3", Integer.class));
    }
    @Test void unknownEndpointIsNotMisreportedAsInternalServerFailure() {
        var response = new GlobalExceptionHandler().handleMissingEndpoint(
                new NoResourceFoundException(HttpMethod.GET, "user/next-employee-number"));
        assertEquals(404, response.getStatusCode().value());
        assertEquals(404, response.getBody().getCode());
    }
}
