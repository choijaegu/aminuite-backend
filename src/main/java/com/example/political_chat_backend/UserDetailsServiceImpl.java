package com.example.political_chat_backend;

import com.example.political_chat_backend.User;
import com.example.political_chat_backend.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 추가

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // 사용자 정보를 읽어오므로 readOnly 트랜잭션
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // UserRepository를 사용하여 username으로 사용자를 조회합니다.
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        // 우리 User 엔티티의 역할(Set<String>)을 Spring Security의 GrantedAuthority 컬렉션으로 변환합니다.
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role))
                .collect(Collectors.toSet());

        // Spring Security가 사용하는 UserDetails 객체를 생성하여 반환합니다.
        // org.springframework.security.core.userdetails.User를 사용합니다.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // DB에 저장된 암호화된 비밀번호
                authorities);       // 변환된 역할 정보
    }
}
