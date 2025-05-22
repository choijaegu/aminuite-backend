package com.example.political_chat_backend;

import com.example.political_chat_backend.SignupRequestDto;
import com.example.political_chat_backend.User; // User 모델 임포트
import com.example.political_chat_backend.AuthService;
import com.example.political_chat_backend.LoginRequestDto;   // 추가
import com.example.political_chat_backend.JwtResponseDto;  // 추가
import com.example.political_chat_backend.JwtUtils; // 추가
import jakarta.validation.Valid; // @Valid 어노테이션을 위해 임포트
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager; // 추가
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; // 추가
import org.springframework.security.core.Authentication; // 추가
import org.springframework.security.core.context.SecurityContextHolder; // 추가
import org.springframework.security.core.userdetails.UserDetails; // 추가
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth") // 인증 관련 API들은 /api/auth 경로 사용
public class AuthController {

    @Autowired
    private AuthService authService; // 이미 선언되어 있음

    @Autowired
    AuthenticationManager authenticationManager; // SecurityConfig에서 빈으로 등록한 AuthenticationManager 주입

    @Autowired
    JwtUtils jwtUtils; // 우리가 만든 JwtUtils 주입

    // @Autowired
    // UserRepository userRepository; // JwtResponseDto에 사용자 ID를 넣기 위해 필요할 수 있음 (선택적)


    @PostMapping("/signup") // 주석 해제!
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequestDto signupRequest) {
        try {
            User registeredUser = authService.registerUser(signupRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("사용자 등록 성공: " + registeredUser.getUsername());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원가입 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDto loginRequest) {

        // 1. 사용자 인증 시도
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // 2. 인증 성공 시 SecurityContext에 인증 정보 저장
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. JWT 토큰 생성
        String jwt = jwtUtils.generateJwtToken(authentication);

        // 4. UserDetails에서 사용자 정보 가져오기 (JwtResponseDto에 담기 위함)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        // UserDetailsImpl userDetailsImpl = (UserDetailsImpl) authentication.getPrincipal(); // 만약 UserDetailsImpl을 사용한다면
        // Long id = userDetailsImpl.getId(); // UserDetailsImpl에 id getter가 있다면

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // 참고: 사용자 ID(Long id)를 가져오려면 UserDetails 구현체에 해당 정보가 있거나,
        //       DB에서 다시 조회해야 할 수 있습니다. UserDetailsImpl에 id를 포함시키는 것이 일반적입니다.
        //       여기서는 임시로 ID는 null 또는 다른 방식으로 가져온다고 가정. (또는 UserRepository를 주입받아 조회)
        //       가장 간단하게는 username과 roles, token만 반환할 수도 있습니다.
        //       User fullUser = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
        //       Long userId = (fullUser != null) ? fullUser.getId() : null;

        return ResponseEntity.ok(new JwtResponseDto(jwt,
                null, // 여기에 사용자 ID를 넣을 수 있으면 좋습니다. (예: UserDetailsImpl에서 가져오거나 DB 재조회)
                userDetails.getUsername(),
                roles));
    }
}
