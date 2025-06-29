package com.wewe.temjaimusic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/songs/**").permitAll()  // 👈 หรือ "/api/**"
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable()) // ถ้าเป็น REST API
                .httpBasic(withDefaults());

        return http.build();
    }

//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/", "/songs/**", "/css/**", "/js/**", "/images/**").permitAll() // อนุญาตหน้าเพลงกับหน้าแรก
//                        .anyRequest().authenticated()  // หน้าอื่นๆ ต้องล็อกอิน
//                )
//                .formLogin(login -> login
//                        .loginPage("/login")         // ถ้าต้องการหน้า login แบบ custom
//                        .permitAll()
//                )
//                .logout(logout -> logout.permitAll());
//
//        return http.build();
//    }
}

